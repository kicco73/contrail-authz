package org.ow2.contrail.authorization.cnr.old.core.ucon;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;
import org.ow2.contrail.authorization.cnr.core.pdp.UconPdp;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.ow2.contrail.authorization.cnr.old.core.utils.UconRequestContext;
import org.ow2.contrail.authorization.cnr.old.core.utils.UconResponseContext;
import org.ow2.contrail.authorization.cnr.old.core.utils.XacmlSamlCoreUtilsOLD;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

import static org.ow2.contrail.authorization.cnr.utils.UconConstants.VERBOSE_HIGH;

public class ContextHandlerOLD {

	private XacmlSamlCoreUtilsOLD utils;
	private int verbosity = 0;
	private CorePhase policy;
	private ServiceContext serviceContext;

	public ContextHandlerOLD(ServiceContext serviceContext, CorePhase policy, XacmlSamlCoreUtilsOLD utils) {
		this.utils = utils;
		this.verbosity = 0;
		this.policy = policy;
		this.serviceContext = serviceContext;
	}

	@SuppressWarnings("unchecked")
	private String doEvaluation(String req) throws InterruptedException, XacmlSamlException{
		String entry = "";
		switch (policy) {
		case PRE:
			entry = UconConstantsCore.PRE_PDP;
			break;
		case ON:
			entry = UconConstantsCore.ON_PDP;
			break;
		case POST:
			entry = UconConstantsCore.POST_PDP;
			break;
		}
		// query pdp
		UconPdp pdp = null;
		try {
			pdp = ((LinkedBlockingQueue<UconPdp>) serviceContext.getProperty(entry)).take();
		} catch (InterruptedException e) {
			throw new InterruptedException("An interrupt occur while waiting for an available PDP");
		}
		
		String result;
		try {
			result = pdp.evaluate(req);
		} catch (XacmlSamlException e) {
			throw e;
		} finally {
			((LinkedBlockingQueue<UconPdp>) serviceContext.getProperty(entry)).offer(pdp);
		}
		return result;
	}

	public UconResponseContext runPdpEvaluation(UconRequestContext requestContext) throws XacmlSamlException, InterruptedException {
		return runPdpEvaluation(requestContext, new LinkedList<UconAttribute>());
	}
	
	public UconResponseContext runPdpEvaluation(UconRequestContext requestContext, List<UconAttribute> attributes) throws XacmlSamlException, InterruptedException {
		// add attributes to the request
		// String xacmlRequest = utils.formXacmlAuthzQueryAttribute(requestContext.getXACMLRequest(), attributes);
		String xacmlRequest = utils.convertXacmlAuthzQuery20to30(requestContext.getXACMLRequest(), attributes);

		LOG(verbosity, "[CH] policy: " + policy + " xacmlRequest:\n" + xacmlRequest, VERBOSE_HIGH);

		String xacmlResponse = doEvaluation(xacmlRequest); //throws InterruptedException
		
		LOG(verbosity, "[CH] PDP evaluation: \n" + xacmlResponse, VERBOSE_HIGH);
		// form response context
		xacmlResponse = xacmlResponse
				.replace(
						"<Response xmlns=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\">",
						"<Response xmlns=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">");

		// set XACML response
		UconResponseContext responseContext = new UconResponseContext();
		responseContext.setXACMLResponse(xacmlResponse);

		// set decision response
		// responseContext.setAccessDecision(utils.getAccessDecision(xacmlResponse));
		responseContext.setAccessDecision(utils.getAccessDecision30(xacmlResponse));
		// set SAML response
		String saml = "";
		switch (policy) {
		case PRE:
			saml = "";// utils.formResponse(xacmlResponse); //I need a new session id from database before!
			break;
		case ON:
			saml = UconConstants.REVOKE_MESSAGE;
			break; // I don't check decision!!!!
		case POST:
			saml = UconConstants.END_MESSAGE;
			break; // I don't check decision!!!!
		}

		LOG(verbosity, "[CH] Response:\n" + saml, VERBOSE_HIGH);

		responseContext.setSAMLResponse(saml);

		return responseContext;
	}

	public UconRequestContext createRequestContext(String requestString) throws XacmlSamlException {
		// NOTICE: request - is any piece of data we receive from the user, i.e., tryaccess, startaccess, endaccess - are all request

		// set SAML request
		UconRequestContext requestContext = null;// new UconRequestContext(type);
		// String sessionID = "";
		// if the message isn't well-formed, an exception will be thrown
		switch (policy) {
		case PRE:
			// set XACML request and other fields
			requestContext = utils.getRequestContextFromAccessRequest(requestString);
			break;
		case ON:
//			requestContext = utils.getIdFromStart(requestString); // take id from message
			break;
		case POST:
//			requestContext = utils.getIdFromEnd(requestString); // take id from message
			break;
		}
		// requestContext.setSAMLRequest(requestString);// CHECKME: the entire message? how (and when) do I use this field?
		// requestContext.setSessionId(sessionID);
		return requestContext;
	}

	/**
	 * ask to pip for refreshing attributes value
	 * 
	 * @param attributes
	 *            list of attributes to update (ordered by holderId)
	 * @return a list of updated attributes
	 */
	public List<UconAttribute> attributesRefresh(List<UconAttribute> attributes) {
		return null; // TODO: ask to pip for updated attributes
	}

	public UconResponseContext setSamlResponse(UconResponseContext respPre, String sessionId) throws XacmlSamlException {
		// TODO: use a proper method in order to convert in xacml 3.0
		String response = respPre.getXACMLResponse();
		response = response.replace("<Response xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\">",
				"<Response xmlns=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">");
		String saml = utils.formResponse(response, sessionId);
		respPre.setSAMLResponse(saml);
		return respPre;
	}

	private void LOG(int verbosity, String text, int mode) {
		if (mode <= verbosity) {
			System.out.println("[UCON]: " + text);
		}
	}
}
