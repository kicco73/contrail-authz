package org.ow2.contrail.authorization.cnr.old.core.ucon;

import static org.ow2.contrail.authorization.cnr.utils.UconConstants.VERBOSE_HIGH;
import static org.ow2.contrail.authorization.cnr.utils.UconConstants.VERBOSE_LOW;
import static org.ow2.contrail.authorization.cnr.utils.UconConstants.VERBOSE_NONE;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import javax.ws.rs.core.MediaType;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;
import org.ow2.contrail.authorization.cnr.core.db.UconSession;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.old.core.db.AccessDBOngoing;
import org.ow2.contrail.authorization.cnr.old.core.db.AccessDBRead;
import org.ow2.contrail.authorization.cnr.old.core.db.AccessDBUconRead;
import org.ow2.contrail.authorization.cnr.old.core.db.AccessDBUconWrite;
import org.ow2.contrail.authorization.cnr.old.core.db.AccessDBWrite;
import org.ow2.contrail.authorization.cnr.old.core.utils.OpenSamlCoreOLD;
import org.ow2.contrail.authorization.cnr.old.core.utils.UconRequestContext;
import org.ow2.contrail.authorization.cnr.old.core.utils.XacmlSamlCoreUtilsOLD;
import org.ow2.contrail.authorization.cnr.utils.Communication;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
//import org.ow2.contrail.authorization.cnr.utils.pip.UconConstantsPip;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class SessionManagerOLD {

	private XacmlSamlCoreUtilsOLD utils;
	private Semaphore usageControl = new Semaphore(1);

	private int verbosity = 0;
	// access db (usage control)
	private AccessDBUconWrite accessDBuconwr = null;
	// access db (usage control), read only
	private AccessDBUconRead accessDBuconrd = null;
	// access db
	private AccessDBWrite accessDBwrite = null;
	// access db, read only
	private AccessDBRead accessDBread = null;
	// access db (ongoing evaluator)
	private AccessDBOngoing accessDBongoing = null;
	// list of ongoing sessions
	private ConcurrentLinkedQueue<UconSession> onSessions = new ConcurrentLinkedQueue<UconSession>(),
	// list of post sessions
			postSessions = new ConcurrentLinkedQueue<UconSession>();
	// list of attribute per session
	private ConcurrentLinkedQueue<AttrPerSession> attrPerSession = new ConcurrentLinkedQueue<AttrPerSession>();
	// list of attribute changed (due to a pull or a notify)
	private ConcurrentLinkedQueue<UconAttribute> changedAttribute = new ConcurrentLinkedQueue<UconAttribute>();
	// service client for communication with pip (pull attribute)
	private LinkedBlockingQueue<ServiceClient> pipServiceClients = new LinkedBlockingQueue<ServiceClient>();
	// service client for communication with pep (revoke)
	private LinkedBlockingQueue<ServiceClient> revokeServiceClients = new LinkedBlockingQueue<ServiceClient>();
	// parallel executors
	private ExecutorService executorOngoing = null, executorPip = null;

	private long cyclePause = 20000;

	private Thread usageControlThread;

	public SessionManagerOLD(ServiceContext serviceContext, XacmlSamlCoreUtilsOLD utils, Properties properties, int verbosity)
			throws InvalidPropertiesFormatException, SQLException, AxisFault {
		this.utils = utils;
		this.verbosity = verbosity;
		// properties should be already loaded

		try {
			cyclePause = (Long) serviceContext.getProperty(UconConstantsCore.CYCLE_PAUSE_DURATION);
		} catch (ClassCastException e) {
			LOG(verbosity, "Unable to cast " + serviceContext.getProperty(UconConstantsCore.CYCLE_PAUSE_DURATION)
					+ " in Long. Cycle pause set to default value: " + cyclePause, VERBOSE_NONE);
		}

		String url = properties.getProperty(UconConstantsCore.DB_URL);
		String user = properties.getProperty(UconConstantsCore.DB_USER);
		String password = properties.getProperty(UconConstantsCore.DB_PASSWORD);

		if (url == null || user == null || user == password)
			throw new InvalidPropertiesFormatException("Invalid url, user or password while accessing db");

		try {
			int concurrency = 32;
			try {
				concurrency = (Integer) serviceContext.getProperty(UconConstantsCore.ACCESS_DB_PARALLELISM);
			} catch (ClassCastException e) {
				LOG(verbosity, "Unable to cast " + serviceContext.getProperty(UconConstantsCore.ACCESS_DB_PARALLELISM)
						+ " in Integer. Concurrency set to default value: " + concurrency, VERBOSE_NONE);
			}
			accessDBwrite = new AccessDBWrite(concurrency, false, url, user, password);
			accessDBread = new AccessDBRead(concurrency, true, url, user, password);
			accessDBuconrd = new AccessDBUconRead(true, url, user, password);
			accessDBuconwr = new AccessDBUconWrite(false, url, user, password);

			int numThread = 32;
			try {
				numThread = (Integer) serviceContext.getProperty(UconConstantsCore.PARALLEL_THREAD_NUMBER);
			} catch (ClassCastException e) {
				LOG(verbosity, "Unable to cast " + serviceContext.getProperty(UconConstantsCore.PARALLEL_THREAD_NUMBER)
						+ " in Integer. Thread number set to default value: " + numThread, VERBOSE_NONE);
			}
			// executorOngoing = Executors.newCachedThreadPool(); //it's sloooow!
			executorOngoing = Executors.newFixedThreadPool(numThread);
			accessDBongoing = new AccessDBOngoing(numThread, true, url, user, password);
			for (int i = 0; i < numThread; i++) {
				ServiceClient sc = new ServiceClient();
				revokeServiceClients.add(sc);
			}

			String eprPIP = properties.getProperty(UconConstantsCore.EPR_PIP);
			int pipPoolSize = numThread;
			for (int i = 0; i < pipPoolSize + concurrency; i++) {
				// init service client in order to communicate to pip
				ServiceClient sc = new ServiceClient();
				Options synchOpts = new Options();
				synchOpts.setTo(new EndpointReference(eprPIP));
				synchOpts.setAction("urn:attrQuery");
				synchOpts.setUseSeparateListener(false);
				sc.setOptions(synchOpts);
				pipServiceClients.add(sc);
			}
			executorPip = Executors.newFixedThreadPool(pipPoolSize);

		} catch (SQLException e) {
			destroy();
			throw new SQLException("Error while connecting to database (" + e.getMessage() + ")", e);
		} catch (AxisFault e) {
			destroy();
			throw new AxisFault("Error while creating service clients (" + e.getMessage() + ")", e);
		}
		LOG(verbosity, "Pip service client and attribute retrieval threads created", VERBOSE_LOW);

		serviceContext.getRootContext().setProperty("uconSem", usageControl);
	}

	public void destroy() {
		if (accessDBuconwr != null)
			accessDBuconwr.destroyAll();
		if (accessDBuconrd != null)
			accessDBuconrd.destroyAll();
		if (accessDBwrite != null)
			accessDBwrite.destroyAll();
		if (accessDBread != null)
			accessDBread.destroyAll();
		if (accessDBongoing != null)
			accessDBongoing.destroyAll();
		if (executorOngoing != null)
			executorOngoing.shutdown();
		if (executorPip != null)
			executorPip.shutdown();
	}

	public List<UconSession> usageControl() throws ConnectException, SQLException {
		List<UconSession> sessionsToRevoke = new LinkedList<UconSession>();
		if (usageControl.tryAcquire()) {
			LOG(verbosity, "usage control started", VERBOSE_LOW);
			usageControlThread = Thread.currentThread(); //for concurrency
			
			try {
				phaseAttributeRetrieval();
				
				try {
					sessionsToRevoke = phaseNewOngoingReevaluation();
				} catch (XacmlSamlException e) {
					LOG(verbosity, e.getMessage(), VERBOSE_LOW);
				}
				
				
			} catch (ConnectException e) {
				usageControlThread = null;
				usageControl.release();
				throw e;
			} catch (SQLException e) {
				usageControlThread = null;
				usageControl.release();
				throw e;
			}
			
		}
		usageControlThread = null;
		usageControl.release();
		return sessionsToRevoke;
	}
	
	public void startUsageControlCycle() throws SQLException, ConnectException {
		if (usageControl.tryAcquire()) {
			LOG(verbosity, "usage control started", VERBOSE_LOW);

			usageControlThread = Thread.currentThread();

			// PHASE 1: wait cycle pause duration
			// PHASE 2: attributes retrieval
			// PHASE 3: policies reevaluation

			try {
				int phase = 1;
				while (phase < 4) {
					LOG(0, "current phase " + phase, VERBOSE_NONE);
					switch (phase) {
					// PHASE 1: wait cycle pause duration
					case 1: {
						try {
							Thread.sleep(cyclePause);
							phase = 2;
						} catch (InterruptedException e) {
							LOG(0, "interrupted while sleeping", VERBOSE_NONE);
							phase = 3;
						}
						break;
					}
					// PHASE 2: attributes retrieval
					case 2: {
						phaseAttributeRetrieval();
						if (Thread.interrupted()) // clear the interrupted flag
							LOG(0, "interrupted while attr retrieval", VERBOSE_NONE);
						phase = 3;
						break;
					}
					case 3: {
						boolean cycleIsNecessary = true;
						try {
							cycleIsNecessary = phaseOngoingReevaluation();
						} catch (XacmlSamlException e) {
							LOG(verbosity, e.getMessage(), VERBOSE_LOW);
							cycleIsNecessary = true; // just continue the cycle
						}
						if (cycleIsNecessary) {
							phase = 1;
							if (Thread.interrupted()) { // clear the interrupted flag
								LOG(0, "interrupted while ongoing", VERBOSE_NONE);
								phase = 3; // CHECKME: unfair!!!
							}
						} else {
							phase = 4;
						}
						break;
					}
					}
				}

			} catch (ConnectException e) {
				usageControlThread = null;
				usageControl.release();
				throw e;
			} catch (SQLException e) {
				usageControlThread = null;
				usageControl.release();
				throw e;
			}

			LOG(verbosity, "No active session! Usage control cycle will be interrupted", VERBOSE_LOW);
			usageControlThread = null;
			usageControl.release();
			LOG(verbosity, "usage control stopped", VERBOSE_LOW);
		}
	}

	private long resultTime = 0, start = 0, end = 0;
	private boolean routine = true;

	// PHASE 2: ATTRIBUES RETRIEVAL
	private void phaseAttributeRetrieval() throws SQLException, ConnectException {

		LOG(verbosity, UconConstants.PRINTSTAR, VERBOSE_LOW);
		LOG(verbosity, "usage control cycle (Phase 2: Attribute retrieval)", VERBOSE_LOW);

		// 1) update database session from list (onSessions / postSessions)
		updateSessionInDB();

		// 2) take a list of attribute could be updated (get from DB all attributes which should be pulled)
		List<UconAttribute> attributes = accessDBuconrd.attributeToBePulled();
		LOG(verbosity, "attribute to check: " + attributes.size(), VERBOSE_LOW);

		// /////////////**********************************************
		// ///////////// PERFORMANCE TEST CODE //CHECKME
		// /////////////**********************************************
		resultTime = 0;
		routine = true;
		start = System.currentTimeMillis();
		end = 0;
		// /////////////**********************************************

		// 3) retrieve the updated attributes list
		// ask CH to create a SAML attribute query and collect fresh attributes from the PIP
		// List<UconAttribute> refreshedAttribute = parallelAttributesRefresh(attributes);
		parallelAttributesRefresh(attributes);
	}

	private List<UconSession> phaseNewOngoingReevaluation() throws SQLException, XacmlSamlException, ConnectException {
		
		List<UconAttribute> refreshedAttribute = new LinkedList<UconAttribute>();
		int n = 0;
		while (!changedAttribute.isEmpty()) {
			UconAttribute attr = changedAttribute.poll();
			if (attr != null) {
				refreshedAttribute.add(attr);
//				LOG(0, "The value of '" + attr.getXacmlId() + "' for the user '" + 
//				attr.getHolderId() + "' is changed " + "to "+ attr.getValue(), VERBOSE_NONE);
				n++;
			}
		}

		LOG(verbosity, "updated attributes: " + n, VERBOSE_LOW);

		if (n > 0) {
			// 5) update attribute on database
			accessDBuconwr.updateAttributes(refreshedAttribute);
			LOG(verbosity, "[SM]: updated db", VERBOSE_HIGH);
		}
		
		// 6) take a list of session involved (get from DB all sessions which should be reevaluated)
		List<UconSession> sessionsToReevaluate = accessDBuconrd.sessionsToReevaluate(refreshedAttribute, (OpenSamlCoreOLD) utils);
		LOG(verbosity, "[SM]: session to reevaluate " + sessionsToReevaluate.size(), VERBOSE_HIGH);
		List<UconSession> sessionsToRevoke = new LinkedList<UconSession>();
		if (sessionsToReevaluate.size() > 0) {

			LOG(verbosity, "reevaluating sessions ", VERBOSE_LOW);

			// 7) evaluate the session taken (and eventually revoke it)
			List<UconSession> responses = parallelOngoingEvaluation(sessionsToReevaluate);

			// 8) update the postSessions list
			for (UconSession s : responses) {
//				String status = s.getStatus();

//				if(status.equals(UconConstantsCore.SESSION_NEW)) {
					LOG(verbosity, "the session " + s.getSession_id_string() + " can starts!", VERBOSE_LOW);
//					s.setStatus(UconConstantsCore.SESSION_ON);
					onSessions.add(s);
//				}
				
//				if (status.equals(UconConstantsCore.SESSION_POST)) {
//					doRevoke();
					sessionsToRevoke.add(s);
					postSessions.add(s);
//				}
			}
		}

		// 9) remove post sessions from database
		updateSessionInDB();
		removePostSessions();

		// 2) return true if there're active sessions
		int activeSessions = accessDBuconrd.activeSessions();
		LOG(verbosity, "usage control end (still active sessions: " + activeSessions + ")", VERBOSE_LOW);


		return sessionsToRevoke;
	}
	
	private boolean phaseOngoingReevaluation() throws SQLException, XacmlSamlException, ConnectException {
		List<UconAttribute> refreshedAttribute = new LinkedList<UconAttribute>();
		int n = 0;
		while (!changedAttribute.isEmpty()) {
			UconAttribute attr = changedAttribute.poll();
			if (attr != null) {
				refreshedAttribute.add(attr);
//				LOG(0,
//						"The value of '" + attr.getXacmlId() + "' for the user '" + attr.getHolderId() + "' is changed " + "to "
//								+ attr.getValue(), VERBOSE_NONE);
				n++;
			}
		}

		LOG(verbosity, "updated attributes: " + n, VERBOSE_LOW);

		if (n > 0) {
			// 5) update attribute on database
			accessDBuconwr.updateAttributes(refreshedAttribute);
			LOG(verbosity, "[SM]: updated db", VERBOSE_HIGH);

			// 6) take a list of session involved (get from DB all sessions which should be reevaluated)
			List<UconSession> sessionsToReevaluate = accessDBuconrd.sessionsToReevaluate(refreshedAttribute, (OpenSamlCoreOLD) utils);
			LOG(verbosity, "[SM]: session to reevaluate " + sessionsToReevaluate.size(), VERBOSE_HIGH);

			if (sessionsToReevaluate.size() > 0) {

				LOG(verbosity, "reevaluating sessions ", VERBOSE_LOW);

				// 7) evaluate the session taken (and eventually revoke it)
				List<UconSession> responses = parallelOngoingEvaluation(sessionsToReevaluate);

				// 8) update the postSessions list
				for (UconSession s : responses) {
//					String status = s.getStatus();

//					if (status.equals(UconConstantsCore.SESSION_POST)) {
//						doRevoke();
//						postSessions.add(s);
//						// ///////////**********************************************
//						// /////////// PERFORMANCE TEST CODE //CHECKME
//						// ///////////**********************************************
//						routine = false;
//						// ///////////**********************************************
//					}
				}

				// /////////////**********************************************
				// ///////////// PERFORMANCE TEST CODE //CHECKME
				// /////////////**********************************************
				end = System.currentTimeMillis();
				// /////////////**********************************************

			}
			// /////////////**********************************************
			// ///////////// PERFORMANCE TEST CODE //CHECKME
			// /////////////**********************************************

			if (end > 0 && routine) {
				resultTime = end - start;
			} else {
				resultTime = 0 - start;
			}
			LOG(verbosity, "********** result time: " + resultTime, VERBOSE_LOW);
			// /////////////**********************************************
		}

		// 9) remove post sessions from database
		updateSessionInDB();
		removePostSessions();

		// 2) return true if there're active sessions
		int activeSessions = accessDBuconrd.activeSessions();
		LOG(verbosity, "usage control cycle end (active sessions: " + activeSessions + ")", VERBOSE_LOW);

		// /////////////**********************************************
		// ///////////// PERFORMANCE TEST CODE //CHECKME
		// /////////////**********************************************
		MessageContext.getCurrentMessageContext().getRootContext().setProperty("time", resultTime);
		// /////////////**********************************************
		return (activeSessions > 0);
	}

	/*
	 * // 1) update database session from list (onSessions / postSessions) // 2) "return" true if there're active sessions // 3) take a list
	 * of attribute could be updated // 4) retrieve the updated attributes list // 5) update attribute on database // 6) take a list of
	 * session involved // 7) evaluate the session taken (and eventually revoke it) // 8) update the postSessions list // 9) remove post
	 * sessions from database private boolean runUsageControl() throws SQLException, XacmlSamlException, ConnectException,
	 * InterruptedException {
	 * 
	 * // 1) update database session from list (onSessions / postSessions) updateSessionInDB();
	 * 
	 * // /////////////********************************************** // ///////////// PERFORMANCE TEST CODE //CHECKME //
	 * /////////////********************************************** long resultTime = 0; long start = 0, end = 0; //
	 * /////////////**********************************************
	 * 
	 * LOG(verbosity, UconConstants.PRINTSTAR, VERBOSE_LOW); LOG(verbosity, "usage control cycle", VERBOSE_LOW);
	 * 
	 * // 3) take a list of attribute could be updated (get from DB all attributes which should be pulled) List<UconAttribute> attributes =
	 * accessDBuconrd.attributeToBePulled(); LOG(verbosity, "attribute to check: " + attributes.size(), VERBOSE_LOW);
	 * 
	 * // /////////////********************************************** // ///////////// PERFORMANCE TEST CODE //CHECKME //
	 * /////////////********************************************** boolean routine = true; start = System.currentTimeMillis(); end = 0; //
	 * /////////////**********************************************
	 * 
	 * // 4) retrieve the updated attributes list // ask CH to create a SAML attribute query and collect fresh attributes from the PIP //
	 * List<UconAttribute> refreshedAttribute = parallelAttributesRefresh(attributes); parallelAttributesRefresh(attributes);
	 * 
	 * 
	 * List<UconAttribute> refreshedAttribute = new LinkedList<UconAttribute>(); while(!changedAttribute.isEmpty()) {
	 * refreshedAttribute.add(changedAttribute.poll()); }
	 * 
	 * LOG(verbosity, "updated attributes: " + refreshedAttribute.size(), VERBOSE_LOW);
	 * 
	 * if (refreshedAttribute.size() > 0) { // 5) update attribute on database accessDBuconwr.updateAttributes(refreshedAttribute);
	 * LOG(verbosity, "[SM]: updated db", VERBOSE_HIGH);
	 * 
	 * // 6) take a list of session involved (get from DB all sessions which should be reevaluated) List<UconSessionContext>
	 * sessionsToReevaluate = accessDBuconrd.sessionsToReevaluate(refreshedAttribute, (OpenSamlCore) utils); LOG(verbosity,
	 * "[SM]: session to reevaluate " + sessionsToReevaluate.size(), VERBOSE_HIGH);
	 * 
	 * if (sessionsToReevaluate.size() > 0) {
	 * 
	 * LOG(verbosity, "reevaluating sessions ", VERBOSE_LOW); // 7) evaluate the session taken (and eventually revoke it)
	 * List<UconSessionContext> responses = parallelOngoingEvaluation(sessionsToReevaluate);
	 * 
	 * // 8) update the postSessions list for (UconSessionContext s : responses) { String status = s.getStatus();
	 * 
	 * if (status.equals(UconConstantsCore.SESSION_POST)) { postSessions.add(s); //
	 * ///////////********************************************** // /////////// PERFORMANCE TEST CODE //CHECKME //
	 * ///////////********************************************** routine = false; //
	 * ///////////********************************************** } }
	 * 
	 * // /////////////********************************************** // ///////////// PERFORMANCE TEST CODE //CHECKME //
	 * /////////////********************************************** end = System.currentTimeMillis(); //
	 * /////////////**********************************************
	 * 
	 * } // /////////////********************************************** // ///////////// PERFORMANCE TEST CODE //CHECKME //
	 * /////////////**********************************************
	 * 
	 * if (end > 0 && routine) { resultTime = end - start; } else { resultTime = 0 - start; } LOG(verbosity, "********** result time: " +
	 * resultTime, VERBOSE_LOW); // /////////////********************************************** }
	 * 
	 * // 9) remove post sessions from database updateSessionInDB(); removePostSessions();
	 * 
	 * // 2) return true if there're active sessions int activeSessions = accessDBuconrd.activeSessions();
	 * 
	 * LOG(verbosity, "usage control cycle end (active sessions: " + activeSessions + ")", VERBOSE_LOW);
	 * 
	 * // /////////////********************************************** // ///////////// PERFORMANCE TEST CODE //CHECKME //
	 * /////////////**********************************************
	 * MessageContext.getCurrentMessageContext().getRootContext().setProperty("time", resultTime); //
	 * /////////////**********************************************
	 * 
	 * return (activeSessions > 0); }
	 */

	/**
	 * Ask attribute values to PIP
	 * 
	 * @param subject
	 * @param object
	 * @return A list of UconAttribute
	 * @throws XacmlSamlException
	 *             If an error occur while parsing messages
	 * @throws InterruptedException
	 *             If the thread is interrupted while waiting
	 * @throws ConnectException
	 *             If a problem occur connecting to PIP. Could be a UconConstantsPip.errorIP, UconConstantsPip.errorIP_URL or an AxisFault.
	 */
	public List<UconAttribute> pullAttributesFromPip(String subject, String object) throws XacmlSamlException, ConnectException,
			InterruptedException {
		LOG(verbosity, "\n" + UconConstants.PRINTLINE, VERBOSE_LOW);
		LOG(verbosity, "query attributes to PIP", VERBOSE_LOW);
		OMElement method = Communication.createPayload(UconConstantsCore.PIP_NAMESPACE, "attrQuery", "subject", subject, "object", object);
		LOG(verbosity, "[CH] message sent to PIP:\n" + method, VERBOSE_HIGH);
		String response = "";
		ServiceClient sc;
		try {
			sc = pipServiceClients.take();
		} catch (InterruptedException e) {
			throw new InterruptedException("An interrupt occur while waiting for a PIP service client");
		}
		OMElement res = null;
		try {
			res = sc.sendReceive(method);
			response = "ATTRIBUTE RETRIEVAL " + res; // i need this, i don't know why
			sc.cleanupTransport();
		} catch (AxisFault e) {
			throw new ConnectException(e.getMessage());
		} finally {
			pipServiceClients.offer(sc);
		}
		response = res.getFirstElement().getText();
//		if ((response).equals(UconConstants.errorIP)) { throw new ConnectException(UconConstantsPip.errorIP); }
//		if ((response).equals(UconConstantsPip.errorIP_URL)) { throw new ConnectException(UconConstantsPip.errorIP); }
//		if ((response).equals(UconConstantsPip.errorMSG)) { throw new XacmlSamlException("Unable to have a response for ('" + subject
//				+ "','" + object + "') request from PIP"); }

		LOG(verbosity, "pip response:\n" + response, VERBOSE_HIGH);
		LOG(verbosity, "got pip response", VERBOSE_LOW);
		// transform response in a UconAttribute list
//		List<UconAttribute> list = utils.getAttributeFromPipResponse(response, subject);
		List<UconAttribute> list = null;
		return list;
	}

	public void updateSubscriptedAttribute(String message) throws XacmlSamlException {
		// get the attributes updated
		List<UconAttribute> list = utils.getAttributeFromPipUpdate(message);
		// add to changed attribute list
		for (UconAttribute attr : list) {
			changedAttribute.offer(attr);
		}
		// send interrupt signal to usage control thread (if exist)
		if (usageControlThread != null)
			usageControlThread.interrupt();
	}

	// FUTURE VERSION (optimized)
	private void parallelAttributesRefresh(List<UconAttribute> dbAttributes) throws ConnectException {
		boolean interrupted = false;
		try {
			ExecutorCompletionService<List<UconAttribute>> exec = new ExecutorCompletionService<List<UconAttribute>>(executorPip);
			int count = 0, holderFound = 0;
			while (count < dbAttributes.size()) {
				int firstHolder = count;
				holderFound++;
				// Get the holder
//				String holder = dbAttributes.get(count).getHolderId();
				count++;
				// move to the next holder in the list (ordered list by database)
//				while (count < dbAttributes.size() && holder.equals(dbAttributes.get(count).getHolderId()))
					count++;

				List<UconAttribute> holderAttributes = dbAttributes.subList(firstHolder, count); // endpoint is exclusive

//				exec.submit(new AttributeRetrieval(holder, pipServiceClients, utils, holderAttributes));
			}

			// List<UconAttribute> completeList = new ArrayList<UconAttribute>();
			for (int i = 0; i < holderFound; i++) {
				try {
					List<UconAttribute> attrList = null;
					while (attrList == null) {
						try {
							attrList = exec.take().get();
						} catch (ExecutionException e) {
							if (e.getCause() instanceof InterruptedException) {
								interrupted = true;
							}
							if (e.getCause() instanceof XacmlSamlException) {
								LOG(verbosity, e.getCause().getMessage(), VERBOSE_LOW);
							}
							throw e;
						} catch (InterruptedException e) {
							// throw new
							// InterruptedException("An interrupt occur while waiting for refresh attributes from PIP (executor service take())");
							interrupted = true;
						}
					}
					for (UconAttribute attr : attrList) {
						// completeList.add(attr);
						changedAttribute.offer(attr);
						// System.out.println("The value of '" + attr.getXacmlId() + "' for the user '" + attr.getHolderId() +
						// "' is changed " +
						// "to "+attr.getValue());
					}
				} catch (ExecutionException e) {
					if (e.getCause() instanceof ConnectException) { throw (ConnectException) e.getCause(); }
					e.printStackTrace(); // every possible Exception should be catched before
				}
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
		// return completeList;
	}
	
	
	private static void doRevoke() {
		// first version (parallel)
		// ServiceClient sender = null;
		// while(sender == null) {
		// try {
		// sender = revokeServiceClients.take();
		// } catch (InterruptedException e) {
		// interrupted = true;
		// }
		// }
		// //send the revoke message to the client
		// ServiceContext sc = MessageContext.getCurrentMessageContext().getServiceContext();
		// try {
		// Communication.sendStartAccessResponseSoap("revoke", sess.getMessageId(), sess.getReplyTo(),
		// sc.getConfigurationContext(), sender);
		// } catch (AxisFault e) {
		// throw new ConnectException(e.getMessage());
		// } finally {
		// //put the service client in the queue
		// revokeServiceClients.offer(sender);
		// }

		// base version (sequential)
		// ServiceClient sender = null;
		// try {
		// sender = new ServiceClient(MessageContext.getCurrentMessageContext().getConfigurationContext(), null);
		// OMElement method = Communication.createPayload(UconConstants.UCON_NAMESPACE, "startaccessResponse", "return",
		// "revoke");
		//
		// EndpointReference targetEPR = new EndpointReference(sess.getReplyTo());
		// Options options = new Options();
		// options.setTo(targetEPR);
		// options.setAction("urn:startaccessResponse");
		// options.setRelationships(new RelatesTo[] { new RelatesTo(sess.getMessageId()) });
		// // options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
		// // options.activate(configContext);
		// // ServiceClient sender = new ServiceClient(configContext, null);
		// sender.setOptions(options);
		// sender.fireAndForget(method);
		// // System.out.println("[COM]: "+method);
		// sender.cleanupTransport();
		// sender.cleanup();
		// } catch (AxisFault e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// my json version
		// try {
		// //build uri for request
		// UriBuilder uriBuilder = UriBuilder.fromUri(getProviderAddress()+"/application-mgmt/deploy/"+ appId);
		// ClientRequest.Builder requestBuilder = ClientRequest.create();
		// requestBuilder.type("application/json");
		// requestBuilder.accept("application/json");
		// requestBuilder.entity(jobject);
		//
		// //create request
		// Client client = Client.create();
		// ClientRequest clientRequest;
		// clientRequest = requestBuilder.build(uriBuilder.build(), "PUT");
		//
		// //execute the request
		// ClientResponse answer = client.handle(clientRequest);
		// boolean result = this.parseReturnStatus(answer);
		//
		// if (result){
		// LOG(verbosity," appliances successfully deployed with Provider APP-ID " + appId + "\n", VERBOSE_NONE);
		// }
		// else {
		// LOG(verbosity," user \"" + user.getUsername() + "\" error in performing deployment", VERBOSE_NONE);
		// }
		// }
		// catch (ClientHandlerException e1) {
		// throw new ProviderComunicationException(e1.toString());
		// }

		String uuid = "af22bc00-3f0b-480b-9da6-05110892e7ab";
		String appUuid = "b0bebfe7-343e-4a33-b586-3e7db3b9cc43";
		String urlBase = "http://146.48.81.249:8080/federation-api/";

		int id = 1;
		JSONObject j = new JSONObject();
		try {
			j.put("pep-id", id);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		ClientConfig cc = new DefaultClientConfig();
		Client client = Client.create(cc);
		WebResource r = client.resource(urlBase + "users/" + uuid + "/applications/" + appUuid + "/revoke");
		try {
	        String entity = r.accept(
			         MediaType.APPLICATION_JSON_TYPE,
			         MediaType.APPLICATION_XML_TYPE).
			         entity(j, MediaType.APPLICATION_JSON_TYPE).
			         put(String.class);
	        System.out.println("First Response" + entity);
	    } catch (UniformInterfaceException ue) {
	        ClientResponse response = ue.getResponse();
	        System.out.println("Response is " + response.getStatus() + " " +  response.getClientResponseStatus());
	    }
	}
	

	private List<UconSession> parallelOngoingEvaluation(List<UconSession> sessionsToReevaluate) throws SQLException,
			ConnectException {
		boolean interrupted = false;
		try {
			int size = sessionsToReevaluate.size();

			ExecutorCompletionService<UconSession> exec = new ExecutorCompletionService<UconSession>(executorOngoing);
			for (int i = 0; i < size; i++) {
				exec.submit(new OngoingEvaluator(utils, sessionsToReevaluate.get(i), accessDBongoing, MessageContext
						.getCurrentMessageContext().getServiceContext(), revokeServiceClients, verbosity));
			}
			LOG(verbosity, "[SM]: waiting for result", VERBOSE_HIGH);
			List<UconSession> result = new ArrayList<UconSession>(size);
			for (int i = 0; i < size; i++) {
				try {
					UconSession sess = null;
					while (sess == null) {
						try {
							sess = exec.take().get();
						} catch (ExecutionException e) {
							if (e.getCause() instanceof InterruptedException) {
								interrupted = true;
							}
							if (e.getCause() instanceof XacmlSamlException) {
								LOG(verbosity, e.getCause().getMessage(), VERBOSE_LOW);
							}
							throw e;
						} catch (InterruptedException e) {
							// throw new
							// InterruptedException("An interrupt occur while waiting for ongoing evaluation (executor service take())");
							interrupted = true;
						}
					}
					result.add(sess);
				} catch (ExecutionException e) {
					if (e.getCause() instanceof ConnectException) { throw (ConnectException) e.getCause(); }
					if (e.getCause() instanceof SQLException) { throw (SQLException) e.getCause(); }
					e.printStackTrace(); // every Exception should be catched before
				}
			}
			return result;
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}

	public String insertSessionInDB(UconSession newSessionContext) throws XacmlSamlException, InterruptedException, SQLException {
		String str = null;
		try {
			str = accessDBwrite.insertSession(newSessionContext);
		} catch (SQLException e) {
			// access to database failed to much time. try to reconnect to database
			accessDBwrite.initAll(); // if it fails again, throws a SQLException
			str = UconConstants.NO_SESSION_ID; // else return NO_SESSION_ID
		}
		return str;
	}

	/**
	 * get the session related to request from database
	 * 
	 * @param request
	 * @return the request or null if not exist
	 * @throws XacmlSamlException
	 *             Error parsing the request
	 * @throws InterruptedException
	 *             Waiting for resource availability
	 * @throws SQLException
	 *             If it's impossible to connect with database anymore
	 */
	public UconSession getSessionFromDB(UconRequestContext request) throws XacmlSamlException, InterruptedException, SQLException {
		try {
			UconSession uconSessionContext = accessDBread.getSession(request.getSessionId(), (OpenSamlCoreOLD) utils);
			return uconSessionContext;
		} catch (SQLException e) {
			// access to database failed to much time. try to reconnect to database
			accessDBread.initAll(); // if it fails again, throws a SQLException
			return null;
		}
	}

	public boolean mapId(String old_id, String ovf_id) throws InterruptedException, SQLException {
		try {
			return accessDBwrite.mapId(old_id, ovf_id);
		} catch (SQLException e) {
			// access to database failed to much time. try to reconnect to database
			accessDBwrite.initAll(); // if it fails again, throws a SQLException
			return false;
		}
	}

	/**
	 * Update one of the two list of sessions (ongoing or post) with the new one (not in database) (startaccess/endaccess)
	 * 
	 * @param sessionContext
	 *            The session to add
	 * @throws SQLException 
	 */
	public void addSessionToUpdateList(UconSession sessionContext) throws SQLException {
		if (sessionContext.getStatus().equals(UconConstantsCore.SESSION_ON))
			onSessions.add(sessionContext);
		else
			postSessions.add(sessionContext);
		
	}

	/**
	 * Add the attribute for the session to a list for a future database update
	 * 
	 * @param attrNeededbySession
	 *            The list of attribute
	 * @param sessionContext
	 *            The related session
	 */
	public void addAttrPerSession(List<UconAttribute> attrNeededbySession, UconSession sessionContext) {
//		attrPerSession.add(new AttrPerSession(attrNeededbySession, sessionContext.getSessionKey()));
	}

	private class AttrPerSession {
		public List<UconAttribute> attrNeededbySession;
		public String sessionKey;

		public AttrPerSession(List<UconAttribute> attrNeededbySession, String sessionKey) {
			this.attrNeededbySession = attrNeededbySession;
			this.sessionKey = sessionKey;
		}
	}

	/**
	 * Empty the list containing the sessions and the attributes to update putting it in database!
	 * 
	 * @throws SQLException
	 * @throws InterruptedException
	 */
	public void updateSessionInDB() throws SQLException {
		List<UconSession> temp = new LinkedList<UconSession>();
		// those are concurrent list... they could receive other elements during emptying
		while (!onSessions.isEmpty()) {
			UconSession sessionContext = onSessions.poll();
			if (sessionContext != null) {
				temp.add(sessionContext);
			}
		}
		while (!postSessions.isEmpty()) {
			UconSession sessionContext = postSessions.poll();
			if (sessionContext != null) {
				temp.add(sessionContext);
			}
		}
		try {
			for (UconSession sessionContext : temp)
				accessDBuconwr.updateSession(sessionContext);
			while (!attrPerSession.isEmpty()) {
				AttrPerSession attr = attrPerSession.poll();
				if (attr != null)
					accessDBuconwr.insertAttributesForSession(attr.attrNeededbySession, attr.sessionKey);
			}
		} catch (SQLException e) {
			// if something goes wrong on db: restore both the list
			for (UconSession ses : temp) {
				if (ses.getStatus().equals(UconConstantsCore.SESSION_ON) ||
						ses.getStatus().equals(UconConstantsCore.SESSION_NEW))
					onSessions.offer(ses);
				if (ses.getStatus().equals(UconConstantsCore.SESSION_POST))
					postSessions.offer(ses);
			}
			throw e;
		}
	}

	private void removePostSessions() throws SQLException {
		accessDBuconwr.deletePostSessions();
		accessDBuconwr.deleteUnreferredAttributes();
	}

	private void LOG(int verbosity, String text, int mode) {
		if (mode <= verbosity) {
			System.out.println("[UCON]: " + text);
		}
	}
	
//	public static void main(String[] args){
//		doRevoke();
//	}
	
	public List<UconSession> getAllSession() throws SQLException, InterruptedException, XacmlSamlException {
		try {
			List<UconSession> sessions = accessDBread.getAllSessions((OpenSamlCoreOLD) utils);
			return sessions;
		} catch (SQLException e) {
			// access to database failed to much time. try to reconnect to database
			accessDBread.initAll(); // if it fails again, throws a SQLException
			return null;
		}
		
	}
}
