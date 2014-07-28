package org.ow2.contrail.authorization.cnr.pep;

import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.ws.WebServiceException;

import org.apache.axis2.AxisFault;
import org.ow2.contrail.authorization.cnr.pep.utils.PepAction;
import org.ow2.contrail.authorization.cnr.pep.utils.PepRequestAttribute;
import org.ow2.contrail.authorization.cnr.pep.utils.PepState;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PEP_callout {

    private static Logger log = LoggerFactory.getLogger(PEP_callout.class);
    private static final String logTag = "[PEP_callout]: ";
    
    private PEP pep;
    private String session_id;

    // actual state
    protected PepState state;
    /**
     * Create a pep callout object for a session already authorized. This object will accept only startaccess and endaccess method
     * 
     * @param pep
     * @param session_id
     * @throws AxisFault
     */
    protected PEP_callout(PEP pep, String session_id) throws AxisFault {
	this.pep = pep;
	this.session_id = session_id;

	this.state = PepState.VEP; // vep only
	
	log.info("{} a pep callout has been created with id: {}", logTag, session_id);
    }

    /**
     * Create a pep callout object to authorize a session.
     * 
     * @param pep
     * @throws AxisFault
     */
    protected PEP_callout(PEP pep) throws AxisFault {
	this(pep, UconConstants.NO_SESSION_ID);
	this.state = PepState.INIT;
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
     * Extract a list of user attributes from an user certificate and try to obtain an authorization for the access request from an ucon service. The
     * access request should contain at least three PepRequestAttribute of different type (one subject, one action, one object)
     * 
     * @param accessRequest
     *            The attribute request list
     * @param cert
     * @return True if the action is allowed, false otherwise
     * @throws WebServiceException
     * @throws AxisFault
     *             If contacting ucon service fail
     * @throws CertificateException
     * @throws IllegalStateException
     *             If you call two tryaccess on the same object or the PEP_callout have already a session id
     */
    public boolean tryaccess(X509Certificate cert, List<PepRequestAttribute> otherAccessRequest) throws AxisFault, CertificateException {

	try {
	    session_id = pep.doATryaccess(state, cert, otherAccessRequest);
	} catch (XacmlSamlException e) {
	    log.error("{} error while parsing tryaccess response:\n{}", logTag, e.getMessage());
	    session_id = UconConstants.NO_SESSION_ID;
	}
	
	boolean accessDecision = !session_id.equals(UconConstants.NO_SESSION_ID);
	log.info("{} tryaccess result id {}", logTag, session_id);
	
	if (accessDecision) {
	    state = state.changeState(PepAction.TRY);
	    log.debug("{} changing state after a successfull tryaccess", logTag);
	}

	return accessDecision;
    }

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
    public boolean tryaccess(List<PepRequestAttribute> accessRequest) throws AxisFault, IllegalStateException {
	log.debug("{} tryaccess called (without certificate)", logTag);
	try {
	    return tryaccess(null, accessRequest);
	} catch (CertificateException e) {
	    log.error("{} tryaccess without certificate, throws a CertificateException (THIS SHOULD NEVER OCCUR)", logTag);
	    throw new RuntimeException(e);
	}
    }

    /**
     * Alert the ucon service that the action (allowed before with a tryaccess), is going to start briefly. The revoke message is received on URL.
     * 
     * @param callback
     *            The object that will handle the revoke message
     * @throws AxisFault
     *             If contacting ucon service fail
     * @throws IllegalStateException
     *             If the current action was never allowed (tryaccess miss) or if it was already started (second startaccess)
     */
    public void startaccess(URL replyTo) throws AxisFault {

	pep.doASynchStartaccess(state, session_id, replyTo);
	
	state = state.changeState(PepAction.START);
	log.debug("{} changing state after an startaccess", logTag);

    }

    /**
     * Alert the ucon service that the action (allowed before with a tryaccess), is going to start briefly.
     * The revoke message is received by callback.
     * 
     * @param callback
     *            The object that will handle the revoke message
     * @throws AxisFault
     *             If contacting ucon service fail
     * @throws XacmlSamlException 
     * @throws IllegalStateException
     *             If the current action was never allowed (tryaccess miss) or if it was already started (second startaccess)
     */
    public void startaccess(PepCallback callback) throws AxisFault {
	
	pep.doAnAynchStartaccess(state, session_id, callback);

	state = state.changeState(PepAction.START);
	log.debug("{} changing state after an startaccess", logTag);
    }

    public void endaccess() throws AxisFault {

	// 
	pep.doAnEndaccess(state, session_id);

	//in every case I assume the access is terminated, so I change the state
	state.changeState(PepAction.END);
	log.debug("{} changing state after an endaccess", logTag);
    }

    public void mapId(String new_id) throws AxisFault {

	// notify the ucon service the change of id
	session_id = pep.doAMapId(state, session_id, new_id);	
	
	// move to an other state
	state = state.changeState(PepAction.MAP);
	log.debug("{} changing state after a mapid", logTag);
	return;
    }
}
