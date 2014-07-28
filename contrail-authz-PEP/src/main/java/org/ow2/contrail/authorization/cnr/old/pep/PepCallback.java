package org.ow2.contrail.authorization.cnr.old.pep;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.async.AxisCallback;
import org.apache.axis2.context.MessageContext;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;

public abstract class PepCallback implements AxisCallback {

	private boolean isRevoke = false;
	private PEP_OLD pep;

	public PepCallback(PEP_OLD pep) {
		this.pep = pep;
	}

	public void onComplete() {

	}

	public void onError(Exception e) {
		System.err.println("An error occour: "+e.getMessage());
	}

	public void onFault(MessageContext messageContext) {
		System.err.println(messageContext.getEnvelope().getBody().getFirstElement().getFirstElement().getText()+"\n");
	}

	public boolean isRevoked() throws AxisFault {
		return isRevoke;
	}

	public void onMessage(MessageContext messageContext) {
		// System.out.println("[PEPCALLBACK] received "+messageContext.getEnvelope().getBody().toString());
		if(messageContext.getEnvelope() == null || messageContext.getEnvelope().getBody() == null) {
			onError(new NullPointerException());
			return;
		}		
		OMElement ele = messageContext.getEnvelope().getBody().getFirstElement();
		if(ele == null) {
			onError(new NullPointerException());
		}
		ele = ele.getFirstElement();
		if(ele == null) {
			onError(new NullPointerException());
		}
		String resp = ele.getText();		 

		// process the received message
		if (resp.equals(UconConstants.REVOKE_MESSAGE)) {
//		if (resp.indexOf(UconConstants.REVOKE_MESSAGE) > 0) {
			isRevoke = true;
			onRevokeAccess();
			try {
				pep.decrCount();
			} catch (AxisFault e) {
				e.printStackTrace();
			}//
		} else {
			if (resp.equals(UconConstants.GENERIC_ERROR) || resp.equals(UconConstants.INPUT_MESSAGE_ERROR)) {
//			if (resp.indexOf(UconConstants.ERROR_GENERIC_MESSAGE) > 0 || 
//					resp.indexOf(UconConstants.ERROR_INPUT_MESSAGE) > 0) {
				try {
					pep.decrCount();
				} catch (AxisFault e) {
					e.printStackTrace();
				}
				onFault(messageContext);
			}
		}
	}

	public abstract void onRevokeAccess();

}
