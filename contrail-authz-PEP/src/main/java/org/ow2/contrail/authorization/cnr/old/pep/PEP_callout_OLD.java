package org.ow2.contrail.authorization.cnr.old.pep;

import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.ws.WebServiceException;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.ow2.contrail.authorization.cnr.old.utils.pep.PepRequestAttribute_OLD;
import org.ow2.contrail.authorization.cnr.old.utils.pep.XacmlSamlPepUtils_OLD;
import org.ow2.contrail.authorization.cnr.utils.Communication;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

import eu.contrail.security.SecurityCommons;

public class PEP_callout_OLD {

    private PEP_OLD pep;
    private String session_id;
    private XacmlSamlPepUtils_OLD utils;

    // default configuration
    private EndpointReference eprPDP;

    // USAGE CONTROL
    private ServiceClient synchSc;
    private Options synchOpts;

    // state
    protected State state;

    private enum State {
	VEP, INIT, PERMITTED, RUNNING, ENDED
    }

    /**
     * Create a pep callout object for a session already authorized. This object will accept only startaccess and endaccess method
     * 
     * @param pep
     * @param session_id
     * @throws AxisFault
     */
    public PEP_callout_OLD(PEP_OLD pep, String session_id) throws AxisFault {
	this.pep = pep;
	this.session_id = session_id;
	this.utils = pep.getXacmlSamlPepUtils();
	this.eprPDP = new EndpointReference(pep.getPDPEndpoint());

	// this.state = state_undef; //vep only
	this.state = State.VEP; // vep only
	// ConfigurationContext configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);

	// add to configuration
	// configContext.getAxisConfiguration().addTransportIn(pep.getListener());
	// ConfigurationContext configContext = pep.getConfigurationContext();
	// // 1 - create asynchronous client for "startaccess" invocation
	// asynchSc = new ServiceClient(configContext, null);
	// asynchSc.engageModule("addressing");
	// asynchOpts = new Options();
	// // setting target EPR
	// asynchOpts.setTo(eprPDP);

	// 2 - create synchronous client for "tryaccess" and "endaccess" invocations
	synchSc = new ServiceClient();
	// synchSc.engageModule("addressing");
	synchOpts = new Options();
	// setting target EPR
	synchOpts.setTo(eprPDP);
    }

    /**
     * Create a pep callout object to authorize a session.
     * 
     * @param pep
     * @throws AxisFault
     */
    public PEP_callout_OLD(PEP_OLD pep) throws AxisFault {
	this(pep, UconConstants.NO_SESSION_ID);
	this.state = State.INIT;
    }

    /**
     * Extract a list of user attributes from an user certificate
     * 
     * @param cert
     * @return a list of PepRequestAttribute or null if an error occur
     */
    public List<PepRequestAttribute_OLD> getAttributesFromCertificate(X509Certificate cert) {
	SecurityCommons sc = new SecurityCommons();
	try {
	    String saml = sc.getSAMLAssertion(cert);
	    List<PepRequestAttribute_OLD> list = this.utils.getPepAttributeFromSamlAssertion(saml);
	    return list;
	} catch (IOException e) {
	    System.err.println("PEP error looking for saml assertion in certificate " + e.getMessage());
	} catch (XacmlSamlException e) {
	    System.err.println("PEP error while parsing saml assertion in certificate " + e.getMessage());
	}
	return null;
    }

    // // VERIFY CERTIFICATE
    // //1) Signature X.509
    // //2) Date condition on X509 and SAML
    // //3) Issuer of X509 (type: x.509 DN) = Issuer of SAML (type: url?)
    // //4) Subject of X.509 = Subject of SAML
    // try { FileInputStream in = new FileInputStream("");
    // CertificateFactory cf =CertificateFactory.getInstance("X.509");
    // Certificate c = (Certificate) cf.generateCertificate(in);
    // in.close(); X509Certificate t = X509Certificate) c;
    // t.gets
    // System.out.println(t.getVersion());
    // System.out.println(t.getSerialNumber().toString(16));
    // System.out.println(t.getSubjectDN());
    // System.out.println(t.getIssuerDN());
    // System.out.println(t.getNotBefore());
    // System.out.println(t.getNotAfter());
    // System.out.println(t.getSigAlgName());
    // byte[] sig = t.getSignature();
    // System.out.println(new
    // BigInteger(sig).toString(16));
    // PublicKey pk = t.getPublicKey();
    // byte[] pkenc = pk.getEncoded();
    // for (int i = 0; i < pkenc.length;i++) {
    // System.out.print(pkenc[i] + ",");
    // }
    // } catch (Exception e) { e.printStackTrace(); }

    /**
     * Try to obtain an authorization for the access request from an ucon service. The access request should contain at least three
     * PepRequestAttribute of different type (one subject, one action, one object)
     * 
     * @param accessRequest
     *            The attribute request list
     * @return True if the action is allowed, false otherwise
     * @throws WebServiceException
     * @throws AxisFault
     *             If contacting ucon service fail
     * @throws IllegalStateException
     *             If you call two tryaccess on the same object or the PEP_callout have already a session id
     */
    public boolean tryaccess(List<PepRequestAttribute_OLD> accessRequest) throws AxisFault {

	String response = null;
	try {
	    response = tryaccessCall(accessRequest);
	} catch (WebServiceException e) {
	    throw e;
	}

	try {
	    session_id = utils.getSessionIdFromResponse(response);
	} catch (XacmlSamlException e) {
	    System.err.println(e.getMessage());
	    session_id = UconConstants.NO_SESSION_ID;
	}
	// System.out.println("[PEP] session id: "+session_id);
	boolean accessDecision = !session_id.equals(UconConstants.NO_SESSION_ID);
	state = accessDecision ? State.PERMITTED : State.INIT;

	return accessDecision;
    }

    protected String tryaccessCall(List<PepRequestAttribute_OLD> accessRequest) throws AxisFault {
	if (state != State.INIT) {
	    // state = State.INVALID; //why?
	    if (state != State.VEP)
		throw new IllegalStateException("[PEP] You can do only one tryaccess for each pep_callout object");
	    else
		throw new IllegalStateException("[PEP] A pep_callout object created with a session id can't do a tryacces");
	}

	// create xacml access request
	String message = "";
	try {
	    message = utils.formXACMLAuthzDecisionQuery(accessRequest, session_id);
	} catch (XacmlSamlException e) {
	    // should never occur
	    throw new IllegalArgumentException(e.getMessage());
	}

	OMElement res = sendSynchInOutRequest("tryaccess", "request", message);
	String response = "" + res;

	response = cleanResponse(response, "tryaccess");

	if (response.equals(UconConstants.GENERIC_ERROR)) {
	    throw new WebServiceException("Ucon Service answer: " + UconConstants.GENERIC_ERROR);
	}
	if (response.equals(UconConstants.ID_INVALID_ERROR)) {
	    // why should it happen?
	    throw new IllegalArgumentException("Ucon Service answer: " + UconConstants.ID_INVALID_ERROR + " id: " + session_id);
	}
	if (response.equals(UconConstants.INPUT_MESSAGE_ERROR)) {
	    throw new IllegalArgumentException("Ucon Service answer: " + UconConstants.INPUT_MESSAGE_ERROR + "\nInput message:\n" + message);
	}

	return response;
    }

    public void startaccess(URL replyTo) throws AxisFault {
	if (state != State.PERMITTED || state != State.VEP) {
	    switch (state) {
	    case INIT:
		throw new IllegalStateException("[PEP] You must have the authorization before starting an action (call tryaccess first)");
	    case RUNNING:
		throw new IllegalStateException("[PEP] You already start an action on this object");
	    case ENDED:
		throw new IllegalStateException("[PEP] The action started is already ended or revoked by application");
	    }
	}
	// System.out.println("[PEP]: sending startaccess");

	String message = null;
	try {
	    message = utils.formStartMessage(session_id);
	} catch (XacmlSamlException e) {
	    // should never occur
	    System.err.println(e.getMessage());
	    return;
	}

	pep.doAStartAccess(message, replyTo);

	state = State.RUNNING;

    }

    /**
     * Alert the ucon service that the action (allowed before with a tryaccess), is going to start briefly.
     * 
     * @param callback
     *            The object that will handle the revoke message
     * @throws AxisFault
     *             If contacting ucon service fail
     * @throws IllegalStateException
     *             If the current action was never allowed (tryaccess miss) or if it was already started (second startaccess)
     */
    public void startaccess(PepCallback callback) throws AxisFault {

	if (state != State.PERMITTED || state != State.VEP) {
	    switch (state) {
	    case INIT:
		throw new IllegalStateException("[PEP] You must have the authorization before starting an action (call tryaccess first)");
	    case RUNNING:
		throw new IllegalStateException("[PEP] You already start an action on this object");
	    case ENDED:
		throw new IllegalStateException("[PEP] The action started is already ended or revoked by application");
	    }
	}

	// System.out.println("[PEP]: sending startaccess");

	String message = null;
	try {
	    message = utils.formStartMessage(session_id);
	} catch (XacmlSamlException e) {
	    // should never occur
	    System.err.println(e.getMessage());
	    return;
	}

	pep.doAStartAccess(message, callback);

	state = State.RUNNING;
    }

    public void endaccess() throws AxisFault {

	if (state != State.RUNNING || state != State.VEP) {
	    switch (state) {
	    case INIT:
		throw new IllegalStateException("[PEP] You must have the authorization before starting an action (call tryaccess first)");
	    case PERMITTED:
		throw new IllegalStateException("[PEP] You must start an action before ending it (call startaccess first)");
	    case ENDED:
		throw new IllegalStateException("[PEP] The action started is already ended or revoked by application");
	    }
	}

	// System.out.println("[PEP]:sending endaccess");
	String message = null;
	try {
	    message = utils.formEndMessage(session_id);
	} catch (XacmlSamlException e) {
	    // should never occur
	    System.err.println(e.getMessage());
	    return;
	}

	String resp = sendSynchInOutRequest("endaccess", "endAssertion", message) + "";

	resp = cleanResponse(resp, "endaccess");

	if (resp.equals(UconConstants.SESSION_ALREADY_STOPPED_REVOKED)) {
	    state = State.ENDED;
	}

	if (resp.equals(UconConstants.INPUT_MESSAGE_ERROR)) {
	    state = State.ENDED; // Probably the action is already ended or revoked (doesn't exist on ucon server)
	}

	if (resp.equals(UconConstants.END_MESSAGE))
	    pep.decrCount();
	state = State.ENDED;
    }

    public void mapId(String ovf_id) throws AxisFault {
	if (state != State.PERMITTED || state != State.RUNNING) {
	    switch (state) {
	    case INIT:
		throw new IllegalStateException("[PEP] You must have the authorization before map another id (call tryaccess first)");
	    case VEP:
		throw new IllegalStateException("[PEP] The id is already mapped");
	    case ENDED:
		throw new IllegalStateException("[PEP] The action is already ended or revoked by application");
	    }
	}

	String actionName = "mapId";
	// Setting action
	synchOpts.setAction("urn:" + actionName);
	// setting synchronous invocation
	synchOpts.setUseSeparateListener(false);
	// synchOpts.setCallTransportCleanup(true);
	synchSc.setOptions(synchOpts);
	synchSc.fireAndForget(Communication.createPayload(UconConstants.UCON_NAMESPACE, actionName, "old_id", session_id, "ovf_id", ovf_id));
	// synchSc.cleanup();
	session_id = ovf_id;
	this.state = State.VEP;
	return;
    }

    private String cleanResponse(String response, String method) {
	// bad solution!!!
	String clean = response.replace("&lt;", "<");
	clean = clean.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
	clean = clean.replace("<ns:" + method + "Response xmlns:ns=\"" + UconConstants.UCON_NAMESPACE + "\"><ns:return>", "");
	clean = clean.replace("</ns:return></ns:" + method + "Response>", "");
	return clean;
    }

    // UCON supplementary
    private OMElement sendSynchInOutRequest(String actionName, String paramName, String paramValue) throws AxisFault {
	// Setting action
	synchOpts.setAction("urn:" + actionName);
	// setting synchronous invocation
	synchOpts.setUseSeparateListener(false);
	synchOpts.setCallTransportCleanup(true); // ?
	// setting created option into service client
	// IF NO WS-ADDRESSING SYPPORT - synchOpts.setReplyTo(null);
	// synchOpts.setReplyTo(new EndpointReference(eprPEP));

	synchSc.setOptions(synchOpts);
	OMElement res = synchSc.sendReceive(Communication.createPayload(UconConstants.UCON_NAMESPACE, actionName, paramName, paramValue));
	return res;
    }
}
