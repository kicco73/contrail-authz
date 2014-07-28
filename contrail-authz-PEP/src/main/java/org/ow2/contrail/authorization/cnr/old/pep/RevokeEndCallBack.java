package org.ow2.contrail.authorization.cnr.old.pep;

import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.MessageContext;

@Deprecated
public class RevokeEndCallBack implements AxisCallback {

    private Thread sessionExecutorThread;

    RevokeEndCallBack(Thread sessionThread) {
	sessionExecutorThread = sessionThread;
    }

    public void onMessage(MessageContext messageContext) {
	SOAPBody msg = messageContext.getEnvelope().getBody();
	String resp = msg.toString();
	System.out.println("[RevokeEndCallBack] received: " + resp);

	//should process the received message and call the correct one	
	if (resp.indexOf("revoke") > 0)
	    onRevokeAccess();
	if (resp.indexOf("end") > 0)
	    onEndAccess();
    }

    public void onFault(MessageContext messageContext) {
	System.err.println("onFault");
	messageContext.getFailureReason().printStackTrace();
    }

    public void onError(Exception e) {
	System.err.println("onError:\n" + e.getMessage());
    }

    public void onComplete() {
	//when the MEP is over... 
    }

    public void onRevokeAccess() {
	//In final release this should be abstract
	System.out.println("UCON STARTACCESS RESPONSE: access should be revoked");
	//send interrupt message to the thread which performs the action execution
	sessionExecutorThread.interrupt();
    }

    public void onEndAccess() {
	//In final release this should be abstract
	System.out.println("UCON STARTACCESS RESPONSE: session can be now ended");
	//interrupt or could be smth else...
	sessionExecutorThread.interrupt();
    }

}
