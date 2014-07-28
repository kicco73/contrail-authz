package org.ow2.contrail.authorization.cnr.pep;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.MessageContext;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PepCallback implements AxisCallback {

    private static Logger log = LoggerFactory.getLogger(PepCallback.class);
    private static final String logTag = "[PEP_callback]: ";
    
    private boolean isRevoke = false;
    private PEP pep;
    private String session_id;
    
    public PepCallback() {
    }

    protected void setPep(PEP pep, String session_id) {
	this.pep = pep;
	this.session_id = session_id;
    }
    
    public void onComplete() {

    }

    public void onError(Exception e) {
	log.error("{} (onError): {}", logTag, e.getMessage());
    }

    public void onFault(MessageContext messageContext) {
	log.error("{} (onFault): {}", logTag, messageContext.getEnvelope().getBody().getFirstElement().getFirstElement().getText());
    }

    public boolean isRevoked() throws AxisFault {
	log.debug("{} session is revoke: {}", logTag, isRevoke);
	return isRevoke;
    }

    public void onMessage(MessageContext messageContext) {
	// System.out.println("[PEPCALLBACK] received "+messageContext.getEnvelope().getBody().toString());
	if (messageContext.getEnvelope() == null || messageContext.getEnvelope().getBody() == null) {
	    onError(new NullPointerException());
	    return;
	}
	OMElement ele = messageContext.getEnvelope().getBody().getFirstElement();
	if (ele == null) {
	    onError(new NullPointerException());
	}
	ele = ele.getFirstElement();
	if (ele == null) {
	    onError(new NullPointerException());
	}
	String resp = ele.getText();
	log.debug("{} callback receive: {}", logTag, resp);
	
	// process the received message
	if (resp.equals(UconConstants.REVOKE_MESSAGE)) {
	    isRevoke = true;
	    onRevokeAccess();
	    try {
		pep.doARevokeaccess(session_id);
	    } catch (AxisFault e) {
		e.printStackTrace();
	    }//
	} else {
	    if (resp.equals(UconConstants.GENERIC_ERROR) || resp.equals(UconConstants.INPUT_MESSAGE_ERROR)) {
		
		try {
		    pep.doARevokeaccess(session_id);
		} catch (AxisFault e) {
		    e.printStackTrace();
		}
		onFault(messageContext);
	    }
	}
    }

    public abstract void onRevokeAccess();

}
