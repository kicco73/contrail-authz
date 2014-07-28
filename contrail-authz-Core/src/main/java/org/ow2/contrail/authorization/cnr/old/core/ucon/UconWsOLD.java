package org.ow2.contrail.authorization.cnr.old.core.ucon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

import javax.ws.rs.core.MediaType;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.phaseresolver.PhaseException;
import org.apache.axis2.service.Lifecycle;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;
import org.ow2.contrail.authorization.cnr.core.db.UconSession;
import org.ow2.contrail.authorization.cnr.core.pdp.BalanaPdp;
import org.ow2.contrail.authorization.cnr.core.pdp.UconPdp;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.ow2.contrail.authorization.cnr.old.core.utils.OpenSamlCoreOLD;
import org.ow2.contrail.authorization.cnr.old.core.utils.UconRequestContext;
import org.ow2.contrail.authorization.cnr.old.core.utils.UconResponseContext;
import org.ow2.contrail.authorization.cnr.old.core.utils.XacmlSamlCoreUtilsOLD;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

import com.mysql.jdbc.Driver;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class UconWsOLD implements Lifecycle {

	private static Logger logger = Logger.getLogger(UconWsOLD.class);
//	private static String verboseTableEntry = "VERBOSITY";

	// this is to create a service state, a data shared between all service instances
	// here there should be a connection to DB and a thread which enforces attribute retrieval
	public void init(ServiceContext serviceContext) throws AxisFault {
		
		logger.info(LOG("service init"));
		// load MySql driver
		try {
			DriverManager.registerDriver(new Driver());
		} catch (SQLException e) {
			logger.fatal(LOG("Initialization failed (Unable to load MySql driver manager)"));
			return;
		}
		// initialize ucon state
		if (!initState(serviceContext))
			return;
		// instead of adding module manually, we add it in code
		addResponseAbortHandler(serviceContext);
		logger.debug(LOG("Init completed"));
	}

	// //LOAD FROM JAR code
	// String configFile = "config.properties";
	// ClassLoader classLoader = getClass().getClassLoader();
	// InputStream in = classLoader.getResourceAsStream(configFile);
	// properties = new Properties();
	// try {
	// properties.load(in);
	// } catch (Exception e) {
	// e.printStackTrace();
	// LOG("didn't find config file in " + configFile);
	// return;
	// }

	private boolean initState(ServiceContext serviceContext) {

		String policyPath = "";//UconConstantsCore.POLICY_DIR;
		long cycle_pause_duration = 20000;
		int access_db_parallelism = 32, parallel_thread_number = 32;
		// load config file
		Properties properties = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(UconConstantsCore.configFile);
			properties.load(in);
		} catch (FileNotFoundException e) {
			logger.fatal(LOG(" ERROR: unable to find configuration file in " + UconConstantsCore.configFile));
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			logger.fatal(LOG(" ERROR: while reading configuration file in " + UconConstantsCore.configFile));
			return false;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
				}
		}

		// value read from configuration file or set to default
		String temp = "";
		if ((temp = properties.getProperty(UconConstantsCore.CYCLE_PAUSE_DURATION)) != null)
			try {
				cycle_pause_duration = Long.parseLong(temp);
			} catch (NumberFormatException e) {
				logger.warn(LOG("Invalid value for "+UconConstantsCore.CYCLE_PAUSE_DURATION+". Please, check the configuration file "+UconConstantsCore.configFile));
			}
		serviceContext.setProperty(UconConstantsCore.CYCLE_PAUSE_DURATION, cycle_pause_duration);
		if ((temp = properties.getProperty(UconConstantsCore.ACCESS_DB_PARALLELISM)) != null)
			try {
				access_db_parallelism = Integer.parseInt(temp);
			} catch (NumberFormatException e) {
				logger.warn(LOG("Invalid value for "+UconConstantsCore.ACCESS_DB_PARALLELISM+". Please, check the configuration file "+UconConstantsCore.configFile));
			}
		serviceContext.setProperty(UconConstantsCore.ACCESS_DB_PARALLELISM, access_db_parallelism);
		if ((temp = properties.getProperty(UconConstantsCore.PARALLEL_THREAD_NUMBER)) != null)
			try {
				parallel_thread_number = Integer.parseInt(temp);
			} catch (NumberFormatException e) {
				logger.warn(LOG("Invalid value for "+UconConstantsCore.PARALLEL_THREAD_NUMBER+". Please, check the configuration file "+UconConstantsCore.configFile));
			}
		serviceContext.setProperty(UconConstantsCore.PARALLEL_THREAD_NUMBER, parallel_thread_number);

		// looking for debug mode
		String newPolicyPath = debug(serviceContext);
		int verbosity = 0;
		if (newPolicyPath != null) {
//			verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);
			policyPath = newPolicyPath;
			logger.warn(LOG("Debug mode ON:\n- verbose set to: " + verbosity + "\n- Policies path: " + policyPath
					+ "\n- cycle pause duration: " + serviceContext.getProperty(UconConstantsCore.CYCLE_PAUSE_DURATION)
					+ "\n- access db parallelism: " + serviceContext.getProperty(UconConstantsCore.ACCESS_DB_PARALLELISM)
					+ "\n- parallel thread number: " + serviceContext.getProperty(UconConstantsCore.PARALLEL_THREAD_NUMBER)));
		}

		// init OpenSaml
		XacmlSamlCoreUtilsOLD utils = null;
//		try {
//			utils = new OpenSamlCoreOLD();
//			logger.debug(LOG("OpenSaml initialization"));
//			serviceContext.setProperty(UconConstantsCore.OPENSAML_UTILS, utils);
//		} catch (XacmlSamlException e) {
//			logger.fatal(LOG(" " + e.getMessage()));
//			logger.fatal(LOG("Initialization failed (Unable to initialize OpenSaml library)"));
//			return false;
//		}
		SessionManagerOLD sm;
		try {
			sm = new SessionManagerOLD(serviceContext, utils, properties, verbosity);
		} catch (Exception e) { // it catches InvalidPropertiesFormatException, SQLException and AxisFault
			e.printStackTrace();
			logger.fatal(LOG("Session manager creation error: "+e.getMessage())); // all this exceptions have textual info in getMessage
			return false;
		}
		serviceContext.setProperty(UconConstantsCore.SESSION_MANAGER, sm);

		int concurrency = Integer.parseInt(properties.getProperty(UconConstantsCore.ACCESS_DB_PARALLELISM));
		LinkedBlockingQueue<UconPdp> prePdp = new LinkedBlockingQueue<UconPdp>(concurrency);
		for (int i = 0; i < concurrency; i++)
			prePdp.add(new BalanaPdp(CorePhase.PRE, policyPath));
		serviceContext.setProperty(UconConstantsCore.PRE_PDP, prePdp);

		LinkedBlockingQueue<UconPdp> postPdp = new LinkedBlockingQueue<UconPdp>(concurrency);
		for (int i = 0; i < concurrency; i++)
			postPdp.add(new BalanaPdp(CorePhase.POST, policyPath));
		serviceContext.setProperty(UconConstantsCore.POST_PDP, postPdp);

		concurrency = Integer.parseInt(properties.getProperty(UconConstantsCore.PARALLEL_THREAD_NUMBER));
		LinkedBlockingQueue<UconPdp> onPdp = new LinkedBlockingQueue<UconPdp>(concurrency);
		for (int i = 0; i < concurrency; i++)
			onPdp.add(new BalanaPdp(CorePhase.ON, policyPath));
		serviceContext.setProperty(UconConstantsCore.ON_PDP, onPdp);

		return true;
	}

	/**
	 * Read values from ~/testmanagerconfig.properties. If file exist, it could set - verbosity (log information mode) - policy path
	 * (different location for policy) - cycle pause duration (the time between two attribute update) - access db parallelism (how many
	 * thread can access to database concurrently - parallel thread number (how many thread use for attribute update/ongoing re-evaluation
	 * phase)
	 */
	private String debug(ServiceContext serviceContext) {
		File configFile = new File(System.getProperty("user.home") + "/testmanagerconfig.properties");
		if (!configFile.exists()) { return null; // It means no debug mode
		}
//		int verbosity = 0;
		String policyPath = null;
		Properties properties = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(configFile);
			properties.load(in);
			String temp = properties.getProperty("policyPath");
			if (temp != null)
				policyPath = System.getProperty("user.home") + "/" + temp;
//			if ((temp = properties.getProperty("verbosity")) != null)
//				verbosity = Integer.parseInt(temp);
			if ((temp = properties.getProperty(UconConstantsCore.CYCLE_PAUSE_DURATION)) != null)
				serviceContext.setProperty(UconConstantsCore.CYCLE_PAUSE_DURATION, Long.parseLong(temp));
			if ((temp = properties.getProperty(UconConstantsCore.ACCESS_DB_PARALLELISM)) != null)
				serviceContext.setProperty(UconConstantsCore.ACCESS_DB_PARALLELISM, Integer.parseInt(temp));
			if ((temp = properties.getProperty(UconConstantsCore.PARALLEL_THREAD_NUMBER)) != null)
				serviceContext.setProperty(UconConstantsCore.PARALLEL_THREAD_NUMBER, Integer.parseInt(temp));

		} catch (FileNotFoundException e) {
			return null; // It means no debug mode
		} catch (NumberFormatException e) {
			logger.error(LOG(" ERROR: while reading debug configuration file in " + configFile + ". Found invalid number ( properties are set to "
							+ properties.getProperty("verbosity") + ", " + properties.getProperty(UconConstantsCore.CYCLE_PAUSE_DURATION)
							+ ", " + properties.getProperty(UconConstantsCore.ACCESS_DB_PARALLELISM) + ", "
							+ properties.getProperty(UconConstantsCore.PARALLEL_THREAD_NUMBER) + ").\n" +
									"Default values will be used."));
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(LOG(" ERROR: while reading debug configuration file in " + configFile));
			return null;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
				}
//			serviceContext.setProperty(verboseTableEntry, verbosity);
		}
		return policyPath;
	}

	public void destroy(ServiceContext serviceContext) {
		destroyAll(serviceContext);
	}

	private void destroyAll(ServiceContext serviceContext) {
		
		logger.info(LOG("destroying service"));
		SessionManagerOLD sm = (SessionManagerOLD) serviceContext.getProperty(UconConstantsCore.SESSION_MANAGER);
		if(sm != null) sm.destroy();
		serviceContext.setProperty(UconConstantsCore.SESSION_MANAGER, null);
		serviceContext.setProperty(UconConstantsCore.OPENSAML_UTILS, null);
		serviceContext.setProperty(UconConstantsCore.PRE_PDP, null);
		serviceContext.setProperty(UconConstantsCore.ON_PDP, null);
		serviceContext.setProperty(UconConstantsCore.POST_PDP, null);
		serviceContext.setProperty(UconConstantsCore.CYCLE_PAUSE_DURATION, null);
		serviceContext.setProperty(UconConstantsCore.ACCESS_DB_PARALLELISM, null);
		serviceContext.setProperty(UconConstantsCore.PARALLEL_THREAD_NUMBER, null);

		logger.info(LOG("service destroyed"));
	}

	public String restart() {
		MessageContext messageContext = MessageContext.getCurrentMessageContext();
		ServiceContext serviceContext = messageContext.getServiceContext();
		destroyAll(serviceContext);
		initState(serviceContext);
		System.gc();
		return "done";
	}

	// ucon ws implementation
	public String tryaccess(String request) {
		ServiceContext serviceContext = MessageContext.getCurrentMessageContext().getServiceContext();
//		int verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);

		logger.info(LOG(UconConstants.PRINTSTAR));
		logger.info(LOG("tryaccess received"));
		XacmlSamlCoreUtilsOLD utils = (XacmlSamlCoreUtilsOLD) serviceContext.getProperty(UconConstantsCore.OPENSAML_UTILS);
		if(utils == null) {
			logger.fatal(LOG("Tryaccess: open saml utils is null"));
			return UconConstants.GENERIC_ERROR;			
		}
		ContextHandlerOLD chPre = new ContextHandlerOLD(serviceContext, CorePhase.PRE, utils);
		logger.debug(LOG("request content:\n" + request));

		UconRequestContext reqPre;
		try {
			reqPre = chPre.createRequestContext(request);
		} catch (XacmlSamlException e) {
			logger.info(LOG(e.getMessage()));
			// this is the only case when I have to allert PEP
			return UconConstants.INPUT_MESSAGE_ERROR;
		}

		if (reqPre.getSessionId() == null || reqPre.getSessionId().startsWith(UconConstantsCore.UCON_SESSION_ID_PREFIX)) {
			logger.info(LOG("Invalid id found in tryaccess: " + reqPre.getSessionId()));
			return UconConstants.ID_INVALID_ERROR;
		}
		SessionManagerOLD sm = (SessionManagerOLD) serviceContext.getProperty(UconConstantsCore.SESSION_MANAGER);
		if(sm == null) {
			logger.error(LOG("Tryaccess: session manager is null"));
			return UconConstants.GENERIC_ERROR;			
		}
		try {
			List<UconAttribute> attr = sm.pullAttributesFromPip(reqPre.getSubjectId(), reqPre.getObjectId());
			UconResponseContext respPre = chPre.runPdpEvaluation(reqPre, attr);
			String sessionId = UconConstants.NO_SESSION_ID;
			if (respPre.getAccessDecision()) {
				// insert request into dataBase (I do it only if it's permit)
				UconSession newSessionContext = new UconSession();
				newSessionContext.setSession_id_string(reqPre.getSessionId());
//				newSessionContext.setInitialRequestContext(reqPre);
				newSessionContext.setReplyTo(""); // no replyto in tryaccess!!

				sessionId = sm.insertSessionInDB(newSessionContext);

				if (sessionId.equals(UconConstants.NO_SESSION_ID)) { throw new SQLException("Unable"); }
			}
			respPre = chPre.setSamlResponse(respPre, sessionId);
			logger.info(LOG("\n" + UconConstants.PRINTLINE));
			String result = (respPre.getAccessDecision()) ? "Permit" : "Not applicable";
			logger.info(LOG("tryaccess response: " + result));
			logger.debug(LOG("tryaccess response:\n" + respPre.getSAMLResponse()));
			// CHECKME: in RunPreAuthorization I set UconResponseContext field but i use only the saml response
			return respPre.getSAMLResponse();
		} catch(NullPointerException e) {
			e.printStackTrace();
			return UconConstants.GENERIC_ERROR;
		} catch (ConnectException e) {
			logger.error(LOG("Connect Exception: " + e.getMessage()));
			errorLog("tryaccess", e.getMessage());
			return UconConstants.GENERIC_ERROR;
		} catch (SQLException e) {
			logger.error(LOG("Database Exception: " + e.getMessage()));
			errorLog("tryaccess", e.getMessage());
			return UconConstants.GENERIC_ERROR;
		} catch (InterruptedException e) {
			logger.error(LOG("Interrupted Exception: " + e.getMessage()));
			return UconConstants.GENERIC_ERROR;
		} catch (XacmlSamlException e) {
			logger.error(LOG(e.getMessage()));
			errorLog("tryaccess", e.getMessage() + "\n" + request);
			return UconConstants.GENERIC_ERROR;
		}
	}// */

	// long-standing action. it starts attribute retrieval when the first
	// request comes and ends when the last request ends/is revoked
//	public String startaccessAsynch(String ackAssertion) {
//		
//		//set the environvment
//		ServiceContext serviceContext = MessageContext.getCurrentMessageContext().getServiceContext();
////		int verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);
//		logger.info(LOG(UconConstants.PRINTSTAR));
//		logger.info(LOG("startaccess received"));
//
//		XacmlSamlCoreUtilsOLD utils = (XacmlSamlCoreUtilsOLD) serviceContext.getProperty(UconConstantsCore.OPENSAML_UTILS);
//		if(utils == null) {
//			logger.fatal(LOG("Startaccess: open saml utils is null"));
//			return UconConstants.ERROR_GENERIC_MESSAGE;
//		}
//		SessionManagerOLD sm = (SessionManagerOLD) serviceContext.getProperty(UconConstantsCore.SESSION_MANAGER);
//		if(sm == null) {
//			logger.error(LOG("Startaccess: session manager is null"));
//			return UconConstants.ERROR_GENERIC_MESSAGE;
//		}
//		// process the ackAssertion by Context Handler
//		ContextHandlerOLD chOn = new ContextHandlerOLD(serviceContext, UconPhase.ON, utils);
//		// if the message isn't well-formed, an exception will be thrown
//		UconRequestContext start;
//		try {
//			start = chOn.createRequestContext(ackAssertion);
//		} catch (XacmlSamlException e) {
//			logger.info(LOG(e.getMessage()));
//			// this is a case when I have to alert PEP
//			return UconConstants.ERROR_INPUT_MESSAGE;
//		}
//
//		logger.info(LOG("for the session with id: " + start.getSessionId()));
//		try {
//			// get a session from DB
//			UconSession sessionContext = sm.getSessionFromDB(start);
//			if (sessionContext == null) { // no session found
//				logger.info(LOG("(startaccess) no session in db with id " + start.getSessionId()));
//				// this is a case when I have to alert PEP
//				return UconConstants.ERROR_INPUT_MESSAGE;
//			}
//
//			if (sessionContext.getStatus().equals(UconConstantsCore.SESSION_PRE)) {
//				// TODO: (Sasha) find all mutable attributes needed for this session. have no idea how to implement this function
//				// TODO: get all mutable attribute which are needed to service the request (usage session)
//				// TODO: search it in database before asking it to pip
//				List<UconAttribute> attrNeededbySession = sm.pullAttributesFromPip(
//						sessionContext.getInitialRequestContext().getSubjectId(), sessionContext.getInitialRequestContext().getObjectId());
//				UconResponseContext resp = chOn.runPdpEvaluation(sessionContext.getInitialRequestContext(), attrNeededbySession);
//				if (resp.getAccessDecision()) {
//					logger.info(LOG("the session " + start.getSessionId() + " can starts!"));
//					// update status of the session and in DB also
//					sessionContext.setStatus(UconConstantsCore.SESSION_ON);
//					sessionContext.setReplyTo(getCurrentReplyTo());
//					sessionContext.setMessageId(getCurrentMessageId());
//
//					// sessions and attributes will be updated in db in usage control cycle
//					sm.addSessionToUpdateList(sessionContext);
//					sm.addAttrPerSession(attrNeededbySession, sessionContext);
//
//					// run usage control cycle, if there isn't one already
//					// if there's already a cycle running this call return quickly
//					// otherwise it will end when the last session end
//					sm.startUsageControlCycle();
//
//					// LOG("\nSTART request content:\n" + ackAssertion));
//					// LOG("\nSTART response (revoke) content:\n" + UconConstantsCore.REVOKE_MESSAGE));
//
//					// with the help of the ResponseAbortHandler this response will go nowhere
//					return UconConstantsCore.ABORT_MESSAGE;
//				} else {
//					logger.info(LOG("the session " + start.getSessionId() + " can't starts! The ongoing policy is violated"));
//					sessionContext.setStatus(UconConstantsCore.SESSION_POST);
//					sm.addSessionToUpdateList(sessionContext);
//					return UconConstantsCore.REVOKE_MESSAGE;
//				}
//			} else {
//				if (sessionContext.getStatus().equals(UconConstantsCore.SESSION_ON)) {
//					logger.info(LOG("(Startaccess) Session found in db was already started"));
//					return UconConstants.SESSION_ALREADY_STARTED;
//				}
//				if (sessionContext.getStatus().equals(UconConstantsCore.SESSION_POST)) {
//					logger.info(LOG("(Startaccess) Session found in db was already stopped or revoked"));
//					return UconConstants.SESSION_ALREADY_STOPPED_REVOKED;
//				}
//				logger.warn(LOG("(Startaccess) Session found in db has a corrupted status value " + sessionContext.getStatus()));
//				return UconConstants.ERROR_GENERIC_MESSAGE;
//			}
//		} catch (ConnectException e) {
//			logger.error(LOG("Connect Exception: " + e.getMessage()));
//			errorLog("startaccess", e.getMessage());
//			return UconConstants.ERROR_GENERIC_MESSAGE;
//		} catch (SQLException e) {
//			logger.error(LOG("Database Exception: " + e.getMessage()));
//			errorLog("startaccess", e.getMessage(), e);
//			return UconConstants.ERROR_GENERIC_MESSAGE;
//		} catch (InterruptedException e) {
//			logger.error(LOG("Interrupted Exception: " + e.getMessage()));
//			return UconConstants.ERROR_GENERIC_MESSAGE;
//		} catch (XacmlSamlException e) {
//			logger.info(LOG(e.getMessage()));
//			errorLog("startaccess", e.getMessage() + "\n" + ackAssertion);
//			return UconConstants.ERROR_GENERIC_MESSAGE;
//		}
//	}// */

	public String startaccess(String message) {
			
			//set the environvment
			MessageContext messageContext = MessageContext.getCurrentMessageContext();
			ServiceContext serviceContext = messageContext.getServiceContext();
	//		int verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);
			logger.info(LOG(UconConstants.PRINTSTAR));
			logger.info(LOG("startaccess received"));
	
			logger.debug(LOG("message: "+message));
			
			XacmlSamlCoreUtilsOLD utils = (XacmlSamlCoreUtilsOLD) serviceContext.getProperty(UconConstantsCore.OPENSAML_UTILS);
			if(utils == null) {
				logger.fatal(LOG("Startaccess: open saml utils is null"));
				return UconConstants.GENERIC_ERROR;
			}
			SessionManagerOLD sm = (SessionManagerOLD) serviceContext.getProperty(UconConstantsCore.SESSION_MANAGER);
			if(sm == null) {
				logger.error(LOG("Startaccess: session manager is null"));
				return UconConstants.GENERIC_ERROR;
			}
			// process the ackAssertion by Context Handler
			ContextHandlerOLD chOn = new ContextHandlerOLD(serviceContext, CorePhase.ON, utils);
			// if the message isn't well-formed, an exception will be thrown
			String[] str = message.split("%");
			if(str.length != 2) {
				logger.warn(LOG("Invalid startaccess message:\n"+message));
				return UconConstants.INPUT_MESSAGE_ERROR;
			}
			
			//TODO: should be removed, once the fed core knows its address
			String ackAssertion = str[0];
			String replyTo = str[1];
			String ipAddr = (String) messageContext.getProperty(MessageContext.REMOTE_ADDR); //the actual client addr
			String[] split = replyTo.split(":",3);
			replyTo = "http://" + ipAddr + ":" + split[2];
			
	//		System.out.println("ack "+ackAssertion);
	//		System.out.println("rT "+replyTo);
			UconRequestContext start;
			try {
				start = chOn.createRequestContext(ackAssertion);			
			} catch (XacmlSamlException e) {
				logger.info(LOG(e.getMessage()));
				// this is a case when I have to alert PEP
				return UconConstants.INPUT_MESSAGE_ERROR;
			}
	
			logger.info(LOG("for the session with id: " + start.getSessionId()));
			try {
				// get a session from DB
				UconSession sessionContext = sm.getSessionFromDB(start);
				if (sessionContext == null) { // no session found
					logger.info(LOG("(startaccess) no session in db with id " + start.getSessionId()));
					// this is a case when I have to alert PEP
					return UconConstants.INPUT_MESSAGE_ERROR;
				}
	
				if (sessionContext.getStatus().equals(UconConstantsCore.SESSION_PRE)) {
	//				LOG("the session " + start.getSessionId() + " can starts!"));
					// update status of the session and in DB also
//					sessionContext.setStatus(UconConstantsCore.SESSION_NEW);
					sessionContext.setReplyTo(replyTo);
					sessionContext.setMessageId(getCurrentMessageId());
				
//					List<UconAttribute> attrNeededbySession = sm.pullAttributesFromPip(
//							sessionContext.getInitialRequestContext().getSubjectId(), sessionContext.getInitialRequestContext().getObjectId());
	
					// sessions and attributes will be updated in db NOW!
//					sm.addAttrPerSession(attrNeededbySession, sessionContext);
					sm.addSessionToUpdateList(sessionContext);
					sm.updateSessionInDB(); //TODO: HOW IS THE DB ACCESS IN THAT FUNCTION?
		
					return "OK";
				} else {
					if (sessionContext.getStatus().equals(UconConstantsCore.SESSION_ON)) {
						logger.info(LOG("(Startaccess) Session found in db was already started"));
						return UconConstants.SESSION_ALREADY_STARTED;
					}
					if (sessionContext.getStatus().equals(UconConstantsCore.SESSION_POST)) {
						logger.info(LOG("(Startaccess) Session found in db was already stopped or revoked"));
						return UconConstants.SESSION_ALREADY_STOPPED_REVOKED;
					}
					logger.warn(LOG("(Startaccess) Session found in db has a corrupted status value " + sessionContext.getStatus()));
					return UconConstants.GENERIC_ERROR;
				}
//			} catch (ConnectException e) {
//				logger.error(LOG("Connect Exception: " + e.getMessage()));
//				errorLog("startaccess", e.getMessage());
//				return UconConstants.ERROR_GENERIC_MESSAGE;
			} catch (SQLException e) {
				logger.error(LOG("Database Exception: " + e.getMessage()));
				errorLog("startaccess", e.getMessage(), e);
				return UconConstants.GENERIC_ERROR;
			} catch (InterruptedException e) {
				logger.error(LOG("Interrupted Exception: " + e.getMessage()));
				return UconConstants.GENERIC_ERROR;
			} catch (XacmlSamlException e) {
				logger.error(LOG(e.getMessage()));
				errorLog("startaccess", e.getMessage() + "\n" + ackAssertion);
				return UconConstants.GENERIC_ERROR;
			}
		}

	public String endaccess(String endAssertion) {
		ServiceContext serviceContext = MessageContext.getCurrentMessageContext().getServiceContext();
//		int verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);
		logger.info(LOG(UconConstants.PRINTSTAR));
		logger.info(LOG("endaccess received"));

		XacmlSamlCoreUtilsOLD utils = (XacmlSamlCoreUtilsOLD) serviceContext.getProperty(UconConstantsCore.OPENSAML_UTILS);
		if(utils == null) {
			logger.fatal(LOG("Endaccess: open saml utils is null"));
			return UconConstants.GENERIC_ERROR;			
		}	
		SessionManagerOLD sm = (SessionManagerOLD) serviceContext.getProperty(UconConstantsCore.SESSION_MANAGER);
		if(sm == null) {
			logger.error(LOG("Endaccess: session manager is null"));
			return UconConstants.GENERIC_ERROR;			
		}	
		// process the endAssertion by Context Handler
		ContextHandlerOLD chEnd = new ContextHandlerOLD(serviceContext, CorePhase.POST, utils);
		UconRequestContext end = null;
		try {
			end = chEnd.createRequestContext(endAssertion);
		} catch (XacmlSamlException e) {
			return UconConstants.INPUT_MESSAGE_ERROR;
		}
		try {
			// get a session from DB
			UconSession sessionContext = sm.getSessionFromDB(end);
			if (sessionContext == null) { // no session found
				logger.info(LOG("(Endaccess) no session in db with id " + end.getSessionId()));
				return UconConstants.INPUT_MESSAGE_ERROR;
			}
			// check if some post-updates are needed?
//			if (sessionContext.getStatus().equals(UconConstantsCore.SESSION_ON)
//					|| sessionContext.getStatus().equals(UconConstantsCore.SESSION_PRE)) { // if endaccess arrived before startaccess
//				chEnd.runPdpEvaluation(sessionContext.getInitialRequestContext());
//				// UconResponseContext respEnd = chEnd.runPdpEvaluation(sessionContext.getInitialRequestContext());
//				// if (respEnd.getAccessDecision()) { //CHECKME: Why do I run pdp post evalutation if I don't check it?
//
//				// update DB appropriately.
////				sessionContext.setStatus(UconConstantsCore.SESSION_POST);
//				sm.addSessionToUpdateList(sessionContext);
//
//				logger.info(LOG("endaccess response is being sent"));
//			} else {
//				if (sessionContext.getStatus().equals(UconConstantsCore.SESSION_POST)) {
//					logger.info(LOG("(Endaccess) Session found in db was already stopped or revoked"));
//					return UconConstants.SESSION_ALREADY_STOPPED_REVOKED;
//				}
//				logger.warn(LOG("(Endaccess) Session found in db has a corrupted status value " + sessionContext.getStatus()));
//			}

			// LOG("\nEND request content:\n" + endAssertion));
			// LOG("\nEND response content:\n" + UconConstants.END_MESSAGE));

			return UconConstants.END_MESSAGE;
		} catch (SQLException e) {
			logger.error(LOG("Database Exception: " + e.getMessage()));
			errorLog("endaccess", e.getMessage());
			return UconConstants.GENERIC_ERROR;
		} catch (InterruptedException e) {
			logger.error(LOG("Interrupted Exception: " + e.getMessage()));
			return UconConstants.GENERIC_ERROR;
		} catch (XacmlSamlException e) {
			errorLog("endaccess", e.getMessage() + "\n" + endAssertion);
			return UconConstants.GENERIC_ERROR;
		}
	}

	public void mapId(String old_id, String ovf_id) {
		ServiceContext serviceContext = MessageContext.getCurrentMessageContext().getServiceContext();
		logger.info(LOG("\n" + UconConstants.PRINTSTAR));
		logger.info(LOG("mapid received"));
		logger.info(LOG("old id: " + old_id + " ovf_id: " + ovf_id));
		SessionManagerOLD sm = (SessionManagerOLD) serviceContext.getProperty(UconConstantsCore.SESSION_MANAGER);
		// update session in DB
		try {
			sm.mapId(old_id, ovf_id);
		} catch (InterruptedException e) {
			logger.error(LOG("Interrupted Exception: " + e.getMessage()));
		} catch (SQLException e) {
			logger.error(LOG("Database Exception: " + e.getMessage()));
			errorLog("endaccess", e.getMessage());
		}
		return;
	}

	public String subscription(String message) {
		logger.info(LOG("messaggio ricevuto: "+message));
		ServiceContext serviceContext = MessageContext.getCurrentMessageContext().getServiceContext();
		SessionManagerOLD sm = (SessionManagerOLD) serviceContext.getProperty(UconConstantsCore.SESSION_MANAGER);
		
		try {
			sm.updateSubscriptedAttribute(message);
		} catch (XacmlSamlException e) {
			logger.warn(LOG("bad message received as pip update: "+e.getMessage()));
			return "err";
		}
		return "done";
	}
	
	
	private String getCurrentMessageId() {
		MessageContext messageContext = MessageContext.getCurrentMessageContext();
		return messageContext.getMessageID();
	}

//	private String getCurrentReplyTo(String addr) {
//		MessageContext messageContext = MessageContext.getCurrentMessageContext();
//		String replyTo = messageContext.getReplyTo().getAddress().toString();
//		System.out.println("[UCON] REPLYTO value: " + replyTo);
//		messageContext.setReplyTo(new EndpointReference(addr));
//		System.out.println("[UCON] REPLYTO real value: " + messageContext.getReplyTo().getAddress().toString());
//		// return "http://localhost:7070";
//		return replyTo;
//	}
	
	private String getCurrentReplyTo() {
		MessageContext messageContext = MessageContext.getCurrentMessageContext();
		// System.out.println("[UCON] REPLYTO value: " + messageContext.getReplyTo().getAddress().toString());

		// return "http://localhost:7070";
		return messageContext.getReplyTo().getAddress().toString();
	}

	// UCON note: this is needed to accomplish the step 10
	// adds a handler to the out flow which does abortion of the message
	private void addResponseAbortHandler(ServiceContext serviceContext) {

		//int verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);
		AxisConfiguration config = null;
		config = serviceContext.getAxisService().getAxisConfiguration();

		List<Phase> phasesOut = null;
		phasesOut = config.getOutFlowPhases();
		// for (Iterator<Phase> iterator = phasesOut.iterator(); iterator.hasNext();) {
		// Phase phase = (Phase) iterator.next();
		for (Phase phase : phasesOut) { // we don't need iterator anymore :)
			if (UconConstantsCore.RESPONSE_ABORT_PHASE.equals(phase.getPhaseName())) {
				ResponseAbortHandler handler = new ResponseAbortHandler();
				try {
					// This will be the last handler under RESPONSE_ABORT_PHASE phase
					phase.setPhaseLast(handler);
					logger.debug("ResponseAbortHandler was engaged");
//					LOG("ResponseAbortHandler was engaged"));
				} catch (PhaseException e) {
					// if the handler is already engagged, this exception will be throw!
					logger.warn("UconWs.addResponseAbortHandled (the handler is already engagged)");
//					LOG("UconWs.addResponseAbortHandled (the handler is already engagged)"));
				}
			}
		}
	}

	// not used, but may be could be useful
	@SuppressWarnings("unused")
	private void howToChangeWSAReplyToFromCode(MessageContext messageContext) {
		Options opts = messageContext.getOptions();

		opts.setProperty(org.apache.axis2.addressing.AddressingConstants.REPLACE_ADDRESSING_HEADERS, Boolean.TRUE);
		EndpointReference replyToEPR = opts.getReplyTo();

		String newReplyTo = "http://localhost:7070";
		replyToEPR.setAddress(newReplyTo);
		opts.setReplyTo(replyToEPR);

		messageContext.setOptionsExplicit(opts);
	}

	private void errorLog(String method, String message) {
		try {
			PrintWriter out = new PrintWriter(new FileWriter(System.getProperty("user.home") + "/ERROR_LOG", true));
			SimpleDateFormat a = new SimpleDateFormat("dd.MM.yyyy 'at' HH:mm:ss");
			out.println("*********INIT*******");
			out.println("An error occour at " + a.format(new Date()) + " executing method " + method);
			out.println("This is the message received:\n" + message);
			out.println("*********END********");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void errorLog(String method, String message, Exception e) {
		StringWriter str = new StringWriter();
		e.printStackTrace(new PrintWriter(str));
		errorLog(method, message + "\n" + str.toString());
	}
	
	private String LOG(String text) {
		return "[UCON]: " + text;
	}

	public void reevaluation() {
//		String uuid = "af22bc00-3f0b-480b-9da6-05110892e7ab";
//		String appUuid = "b0bebfe7-343e-4a33-b586-3e7db3b9cc43";
//		String urlBase = "http://146.48.81.249:8080/federation-api/";
		
		//set the environvment
		ServiceContext serviceContext = MessageContext.getCurrentMessageContext().getServiceContext();
		logger.info(LOG(UconConstants.PRINTSTAR));
		logger.info(LOG("reevaluation received"));
				
		SessionManagerOLD sm = (SessionManagerOLD) serviceContext.getProperty(UconConstantsCore.SESSION_MANAGER);
		if(sm == null) {
			logger.warn(LOG("reevaluation: session manager is null"));
		}
		
		List<UconSession> sessionsToRevoke = null;
		try {
			sessionsToRevoke = sm.usageControl();
		} catch (ConnectException e) {
			logger.error(LOG("Connect Exception: " + e.getMessage()));
			errorLog("startaccess", e.getMessage());
		} catch (SQLException e) {
			logger.error(LOG("Database Exception: " + e.getMessage()));
			errorLog("startaccess", e.getMessage(), e);
		} 
		
		for(UconSession s: sessionsToRevoke) {
			JSONObject j = new JSONObject();
			ClientConfig cc = new DefaultClientConfig();
			Client client = Client.create(cc);
			WebResource r = client.resource(s.getReplyTo());
			try {
				String entity = r.accept(
				MediaType.APPLICATION_JSON_TYPE, 
				MediaType.APPLICATION_XML_TYPE).entity(j, MediaType.APPLICATION_JSON_TYPE).put(String.class);
				System.out.println("Response " + entity);
			} catch (UniformInterfaceException ue) {
				ClientResponse response = ue.getResponse();
				System.out.println("Response " + response.getStatus() + " " +  response.getClientResponseStatus());
			}
		}
	}
	
}