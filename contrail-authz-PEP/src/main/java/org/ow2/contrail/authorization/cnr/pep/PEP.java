package org.ow2.contrail.authorization.cnr.pep;

import java.io.IOException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;

import org.apache.axis2.AxisFault;
import org.ow2.contrail.authorization.cnr.pep.utils.OpenSamlPep;
import org.ow2.contrail.authorization.cnr.pep.utils.PepAction;
import org.ow2.contrail.authorization.cnr.pep.utils.PepRequestAttribute;
import org.ow2.contrail.authorization.cnr.pep.utils.PepState;
import org.ow2.contrail.authorization.cnr.pep.utils.XacmlSamlPepUtils;
import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import eu.contrail.security.SecurityCommons;

public class PEP {

    private static Logger log = LoggerFactory.getLogger(PEP.class);
    private static final String logTag = "[PEP]: ";

    private XacmlSamlPepUtils utils;

    private PepCommunicator communicator;

    private HashSet<String> asynchSession = new HashSet<String>();

    public PEP(String endpointAddress, String host, String port) throws AxisFault {

	communicator = new PepCommunicator(endpointAddress, host, port);

	try {
	    utils = OpenSamlPep.getInstance();
	} catch (XacmlSamlException e) {
	    log.error("{} Initialization failed (Unable to initialize OpenSaml library)", logTag);
	    throw new RuntimeException(e);
	}

    }

    public PEP_callout getNewCallout() throws AxisFault {
	return new PEP_callout(this);
    }

    public PEP_callout getNewCallout(String session_id) throws AxisFault {
	return new PEP_callout(this, session_id);
    }

    protected String doATryaccess(PepState state, X509Certificate cert, List<PepRequestAttribute> otherAccessRequest) throws CertificateException,
	    AxisFault, XacmlSamlException {
	log.info("{} tryaccess called", logTag);
	//System.out.println("{KMcC;)} tryaccess called\n");

	// check the correct state
	String check = state.checkAction(PepAction.TRY);
	if (!check.equals(PepState.ok)) {
	    log.info("{} checking state... tryaccess not allowed\n{}", logTag, check);
	    throw new IllegalStateException(check);
	}
	log.debug("{} checking state... tryaccess allowed", logTag);

	// compose the message to send
	Element xmlMessage = composeTryaccessMessage(cert, otherAccessRequest);
	log.debug("{} composed tryaccess message", logTag);

	// perform tryaccess call
	Element accessResponse = communicator.sendATryaccess(xmlMessage);
	log.debug("{} tryaccess sent", logTag);
	System.out.println("[KMcC;)] tryaccess sent\n");

	String resp = utils.getSessionIdFromTryaccessResponse(accessResponse);

	return resp;
    }

    private Element composeTryaccessMessage(X509Certificate cert, List<PepRequestAttribute> otherAccessRequest) throws CertificateException {
	// compose the complete list of attributes
	List<PepRequestAttribute> accessRequest = null;
	if (cert != null) {
	    try {
		SecurityCommons sc = new SecurityCommons();
		String saml = sc.getSAMLAssertion(cert);
		accessRequest = this.utils.getPepAttributeFromSamlAssertion(saml, UconCategory.SUBJECT);
		accessRequest.addAll(otherAccessRequest);
	    } catch (IOException e) {
		log.info("{} error while parsing saml assertion in certificate {}", e.getMessage());
		throw new CertificateException(e);
	    } catch (XacmlSamlException e) {
		log.info("{} error while parsing saml assertion in certificate {}", e.getMessage());
		throw new CertificateException(e);
	    }
	} else {
	    accessRequest = otherAccessRequest;
	}

	// create xacml access request
	Element message;
	try {
	    message = utils.formTryaccessMessage(accessRequest, UconConstants.NO_SESSION_ID);
	} catch (XacmlSamlException e) {
	    // should never occur
	    throw new IllegalArgumentException(e.getMessage(), e);
	}
	return message;
    }

    protected String doAMapId(PepState state, String old_id, String new_id) throws AxisFault {
	log.info("[PEP_callout]: mapid called (session with id {} will have {})", old_id, new_id);
	state.checkAction(PepAction.MAP);

	communicator.sendAMapid(old_id, new_id);

	if (asynchSession.contains(old_id)) {
	    log.info("{}: changing session id in asynchronous session table", logTag);
	    asynchSession.remove(old_id);
	    asynchSession.add(new_id);
	}

	return new_id;
    }

    protected void doAnAynchStartaccess(PepState state, String session_id, PepCallback callback) throws AxisFault {
	log.info("{} startaccess called for session with id {}", logTag, session_id);
	state.checkAction(PepAction.START);
	log.debug("{} checking state... startaccess allowed", logTag);

	Element message = null;
	try {
	    message = utils.formStartaccessMessage(session_id, null);
	} catch (XacmlSamlException e) {
	    // should never occur
	    throw new RuntimeException(e);
	}
	log.debug("{} composed startaccess message", logTag);

	callback.setPep(this, session_id);
	communicator.sendAnAsynchStartaccess(message, callback);
	log.debug("{} startaccess sent", logTag);

	asynchSession.add(session_id);
	log.debug("{} record session {} as asynchronous", logTag, session_id);

    }

    // called by pep callback
    protected void doARevokeaccess(String session_id) throws AxisFault {
	if (asynchSession.contains(session_id)) {
	    log.info("{}: removing session id in asynchronous session table", logTag);
	    asynchSession.remove(session_id);
	    communicator.decrAsynchSessionCount();
	}
    }

    protected String doASynchStartaccess(PepState state, String session_id, URL replyTo) throws AxisFault {

	log.info("{} startaccess called for session with id {}", logTag, session_id);
	state.checkAction(PepAction.START);
	log.debug("{} checking state... startaccess allowed", logTag);

	Element message = null;
	try {
	    message = utils.formStartaccessMessage(session_id, replyTo);
	} catch (XacmlSamlException e) {
	    log.error("{} this exception should never occur", logTag);
	    throw new RuntimeException(e.getMessage(), e);
	}
	log.debug("{} composed startaccess message", logTag);

	Element response = communicator.sendAStartaccess(message);
	log.debug("{} startaccess sent", logTag);

	String resp = utils.getStartaccessResponse(response);

	if (resp.equals(UconConstants.RESPONSE_OK))
	    ;// incrCount(); //if it was a url startaccess?
	log.debug("{} received endaccess response: {}", logTag, resp);

	return resp;

    }

    protected String doAnEndaccess(PepState state, String session_id) throws AxisFault {
	log.info("{} endaccess called for session with id {}", logTag, session_id);
	state.checkAction(PepAction.END);
	log.debug("{} checking state... endaccess allowed", logTag);

	Element message = null;
	try {
	    message = utils.formEndaccessMessage(session_id);
	} catch (XacmlSamlException e) {
	    log.error("{} this exception should never occur", logTag);
	    throw new RuntimeException(e);
	}
	log.debug("{} composed endaccess message", logTag);

	Element response = communicator.sendAnEndaccess(message);
	log.debug("{} endaccess sent", logTag);

	String resp = utils.getEndaccessResponse(response);
	// this string response could be equals to UconConstants.SESSION_ALREADY_STOPPED_REVOKED, UconConstants.ERROR_INPUT_MESSAGE or
	// UconConstants.ERROR_GENERIC_MESSAGE, but I ignore it in case of error

	if (resp.equals(UconConstants.RESPONSE_OK)) {
	    // if it was a callback startaccess, I have to stop listener
	    if (asynchSession.contains(session_id)) {
		log.info("{}: removing session id in asynchronous session table", logTag);
		asynchSession.remove(session_id);
		communicator.decrAsynchSessionCount();
	    }
	}
	log.debug("{} received endaccess response: {}", logTag, resp);

	return resp;
    }

    // private String cleanResponse(String response, String method) {
    // // bad solution!!!
    // String clean = response.replace("&lt;", "<");
    // clean = clean.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
    // clean = clean.replace("<ns:" + method + "Response xmlns:ns=\"" + UconConstants.UCON_NAMESPACE + "\"><ns:return>", "");
    // clean = clean.replace("</ns:return></ns:" + method + "Response>", "");
    // return clean;
    // }

}
