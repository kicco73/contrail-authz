package org.ow2.contrail.authorization.cnr.old.core.ucon;

import java.net.ConnectException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;
import org.ow2.contrail.authorization.cnr.core.db.UconSession;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.ow2.contrail.authorization.cnr.old.core.db.AccessDBOngoing;
import org.ow2.contrail.authorization.cnr.old.core.utils.UconRequestContext;
import org.ow2.contrail.authorization.cnr.old.core.utils.UconResponseContext;
import org.ow2.contrail.authorization.cnr.old.core.utils.XacmlSamlCoreUtilsOLD;
import org.ow2.contrail.authorization.cnr.utils.Communication;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

import static org.ow2.contrail.authorization.cnr.utils.UconConstants.VERBOSE_LOW;
import static org.ow2.contrail.authorization.cnr.utils.UconConstants.VERBOSE_HIGH;

public class OngoingEvaluator implements Callable<UconSession> {

	private UconSession session;
	private AccessDBOngoing accessDB;
	private XacmlSamlCoreUtilsOLD utils;
	private ServiceContext sc;
	private int verbosity;
	private BlockingQueue<ServiceClient> revokeServiceClients;

	public OngoingEvaluator(XacmlSamlCoreUtilsOLD utils,
			UconSession session,
			AccessDBOngoing accessDB,
			ServiceContext sc,
			BlockingQueue<ServiceClient> revokeServiceClients,
			int verbosity) {
		this.session = session;
		this.accessDB = accessDB;
		this.utils = utils;
		this.sc = sc;
		this.verbosity = verbosity;
		this.revokeServiceClients = revokeServiceClients;
	}

	@Override
	public UconSession call() throws SQLException, XacmlSamlException, ConnectException {
		//THIS THREAD DO A UNCANCELLABLE TASK (ININTERRUPTIBLE)
		boolean interrupted = false;
		try {
		// System.out.println("[OE]: evaluating session " + session.getSessionKey());
		//get the initial request
//		UconRequestContext req = session.getInitialRequestContext();
		//get from db the attributes related to session
		List<UconAttribute> attributeForSession = null;
//		try {
//			while (attributeForSession == null) {
//				try {
//					attributeForSession = accessDB.getAttributeForSession(session.getSessionKey());
//				} catch (InterruptedException e) {
//					interrupted = true;
//				}
//			}
//		} catch (SQLException e) {
//			throw e;
//		} 
		//run the re-evalutaion
		ContextHandlerOLD chOn = new ContextHandlerOLD(sc, CorePhase.ON, utils);
		UconResponseContext res = null;
//		try {
//			while(res == null) {
//				try {
//					res = chOn.runPdpEvaluation(req, attributeForSession);
//				} catch (InterruptedException e) {
//					interrupted = true;
//				}
//			}
//		} catch (XacmlSamlException e) {
//			throw e;
//		} 
		//parse result
		String result = (res.getAccessDecision())? "Permit":"Not applicable";
		LOG(verbosity, "reevaluating session  " + session.getSessionKey() +": "+result, VERBOSE_LOW);
		LOG(verbosity, "reevaluating session  " + session.getSessionKey() + "\n" + res.getXACMLResponse(), VERBOSE_HIGH);
		if (!res.getAccessDecision()) {
			LOG(verbosity, "[OE] The session UconSession_" + session.getSession_id_string() + "  will be revoked", VERBOSE_LOW);
			//set session status
//			session.setStatus(UconConstantsCore.SESSION_POST);
			//take a service client from the queue
//			ServiceClient sender = null;
//			while(sender == null) {
//				try {
//					sender = revokeServiceClients.take();
//				} catch (InterruptedException e) {
//					interrupted = true;
//				}
//			}
//			//send the revoke message to the client
//			try {
//				Communication.sendStartAccessResponseSoap("revoke", session.getMessageId(), session.getReplyTo(), sc.getConfigurationContext(), sender);
//			} catch (AxisFault e) {
//				throw new ConnectException(e.getMessage());
//			} finally {
//				//put the service client in the queue
//				revokeServiceClients.offer(sender);
//			}
		}
		return session;
		} finally {
			if(interrupted)
				Thread.currentThread().interrupt(); //TODO LANCIA EXCEPTION?
		}
	}

	private void LOG(int verbosity, String text, int mode) {
		if (mode <= verbosity) {
			System.out.println("[UCON]: " + text);
		}
	}
}