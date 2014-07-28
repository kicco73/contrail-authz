package org.ow2.contrail.authorization.cnr.old.core.ucon;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.PhaseRule;
import org.apache.axis2.engine.Handler;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;

public class ResponseAbortHandler implements Handler {

	// ucon service will reply to startaccess inside the service implementation and use a ServiceClient for this
	// thus, all messages for the startaccess operation will be aborted
	public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
		//System.out.println("[UCON-HANDLER]: try to send this message:\n" + msgContext.getEnvelope().toString());

		String soapAction = msgContext.getSoapAction().toString();
		if (UconConstantsCore.SOAP_STARTACCESS_ACTION.equals(soapAction)) {
			if (msgContext.getEnvelope().toString().indexOf(UconConstantsCore.ABORT_MESSAGE) > 0) {
				//if (TestUtils.PRINT)
					//System.out.println("[UCON-HANDLER]: response to " + soapAction + " was aborted");
				return InvocationResponse.ABORT;
			}
		}
		//if (TestUtils.PRINT)
			//System.out.println("[UCON-HANDLER]: response to " + soapAction + " was forwarded");
		return InvocationResponse.CONTINUE;
	}

	/**
	 * {@inheritDoc}
	 */
	public HandlerDescription getHandlerDesc() {
		HandlerDescription handlerDesc = null;
		PhaseRule rule = null;

		rule = new PhaseRule(UconConstantsCore.RESPONSE_ABORT_PHASE);
		rule.setPhaseLast(true);

		handlerDesc = new HandlerDescription();
		handlerDesc.setHandler(this);
		handlerDesc.setName(this.getName());
		handlerDesc.setRules(rule);

		return handlerDesc;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "SimpleHandler";
	}

	/**
	 * {@inheritDoc}
	 */
	public void cleanup() {
	}

	/**
	 * {@inheritDoc}
	 */
	public void flowComplete(MessageContext msgContext) {
	}

	/**
	 * {@inheritDoc}
	 */
	public Parameter getParameter(String name) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void init(HandlerDescription handlerDesc) {
	}

}