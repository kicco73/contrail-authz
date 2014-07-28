package org.ow2.contrail.authorization.cnr.core.ucon;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.utils.Communication;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.w3c.dom.Element;

public class UconCommunicator {

    private final Options attributeQueryOptions;
    private final Options subscribeOptions;
    private final Options unsubscribeOptions;
    
    private UconCommunicator(EndpointReference endpointPIP) {
	// initialize attribute query options
	attributeQueryOptions = new Options();
	// setting target EPR
	attributeQueryOptions.setTo(endpointPIP);
	// Setting action
	attributeQueryOptions.setAction("urn:" + UconConstants.ATTRIBUTE_QUERY_METHOD_NAME);
	attributeQueryOptions.setCallTransportCleanup(true);
	
	// initialize subscribe options
	subscribeOptions = new Options();
	// setting target EPR
	subscribeOptions.setTo(endpointPIP);
	// Setting action
	subscribeOptions.setAction("urn:" + UconConstants.SUBSCRIBE_METHOD_NAME);
	subscribeOptions.setCallTransportCleanup(true);
	
	// initialize unsubscribe options
	unsubscribeOptions = new Options();
	// setting target EPR
	unsubscribeOptions.setTo(endpointPIP);
	// Setting action
	unsubscribeOptions.setAction("urn:" + UconConstants.UNSUBSCRIBE_METHOD_NAME);
	unsubscribeOptions.setCallTransportCleanup(true);
    }

    public static void init(ServiceContext serviceContext) {
	Object obj = serviceContext.getProperty(UconConstantsCore.UCON_COMMUNICATOR);
	if (obj == null) {
	    // get pip endpoint from ucon options
	    UconOptions uconOptions = UconOptions.getInstance(serviceContext);
	    
	    UconCommunicator uc = new UconCommunicator(uconOptions.getEndpointPIP());
	    
	    serviceContext.setProperty(UconConstantsCore.UCON_COMMUNICATOR, uc);
	}
    }

    public static UconCommunicator getInstance(ServiceContext serviceContext) {
	Object obj = serviceContext.getProperty(UconConstantsCore.UCON_COMMUNICATOR);
	if (obj != null) {
	    return (UconCommunicator) obj;
	} else {
	    // get pip endpoint from ucon options
	    UconOptions uconOptions = UconOptions.getInstance(serviceContext);
	    
	    UconCommunicator uc = new UconCommunicator(uconOptions.getEndpointPIP());
	    
	    serviceContext.setProperty(UconConstantsCore.UCON_COMMUNICATOR, uc);
	    return uc;
	}
    }

    public Element attributesQuery(Element xmlMessage) throws AxisFault {
        // compose OMElement message
        OMElement message = Communication.createPayload(UconConstants.PIP_NAMESPACE, UconConstants.ATTRIBUTE_QUERY_METHOD_NAME,
        	UconConstants.ATTRIBUTE_QUERY_PARAM_NAME, xmlMessage);
        
        // create axis service client // CHECKME: each time????
        ServiceClient serviceClient = new ServiceClient();
        
        // do a send-receive
        Element resp = Communication.sendReceive(serviceClient, attributeQueryOptions, message);
        
	return resp;
    }

    public Element subscribe(Element xmlMessage) throws XacmlSamlException, AxisFault {
        // compose OMElement message
        OMElement message = Communication.createPayload(UconConstants.PIP_NAMESPACE, UconConstants.SUBSCRIBE_METHOD_NAME,
        	UconConstants.SUBSCRIBE_PARAM_NAME, xmlMessage);
        
        // create axis service client // CHECKME: each time????
        ServiceClient serviceClient = new ServiceClient();
        
        // do a send-receive
        Element resp = Communication.sendReceive(serviceClient, subscribeOptions, message);
        
	return resp;

    }

    public Element unsubscribe(Element xmlMessage) throws AxisFault {
	
        // compose OMElement message
        OMElement message = Communication.createPayload(UconConstants.PIP_NAMESPACE, UconConstants.UNSUBSCRIBE_METHOD_NAME,
        	UconConstants.UNSUBSCRIBE_PARAM_NAME, xmlMessage);
        
        // create axis service client // CHECKME: each time????
        ServiceClient serviceClient = new ServiceClient();
        
        // do a send-receive
        Element resp = Communication.sendReceive(serviceClient, unsubscribeOptions, message);
        
	return resp;

    }

    public void sendRevoke(UconDataEntity session) {
	// TODO Auto-generated method stub

    }

}
