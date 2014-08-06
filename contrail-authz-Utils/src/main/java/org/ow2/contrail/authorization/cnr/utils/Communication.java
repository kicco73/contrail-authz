package org.ow2.contrail.authorization.cnr.utils;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.w3c.dom.Element;

public class Communication {

    /**
     * Compose a message for a web service communication
     * 
     * @param namespace
     *            Destination namespace
     * @param actionName
     *            Name of the remote method to call
     * @param param
     *            Couple (parameter name - parameter value). The number of String must be zero or even.
     * @return The message created
     * @throws IllegalArgumentException
     *             If the parameter number is odd
     */
    public static OMElement createPayload(String namespace, String actionName, String... param) {
	if (param.length > 0 && param.length % 2 != 0) {
	    throw new IllegalArgumentException("Parameter number must be even");
	}
	OMFactory fac = OMAbstractFactory.getOMFactory();
	OMNamespace omNs = fac.createOMNamespace(namespace, "ns");
	OMElement method = fac.createOMElement(actionName, omNs);
	for (int i = 0; i < param.length; i += 2) {
	    OMElement value = fac.createOMElement(param[i], omNs);
	    value.setText(param[i + 1]);
	    method.addChild(value);
	}
	return method;
    }
    
    /**
     * Compose a message for a web service communication
     * 
     * @param namespace
     *            Destination namespace
     * @param actionName
     *            Name of the remote method to call
     * @param paramName
     * 		  The name of parameter
     * @param xmlValue
     *            XML value to send
     * @return The message created
     * @throws AxisFault 
     * @throws IllegalArgumentException
     *             If the parameter number is odd
     */
    public static OMElement createPayload(String namespace, String actionName, String paramName, Element xmlValue) throws AxisFault {
	OMFactory fac = OMAbstractFactory.getOMFactory();
	OMNamespace omNs = fac.createOMNamespace(namespace, "ns");
	OMElement method = fac.createOMElement(actionName, omNs);
	OMElement value = fac.createOMElement(paramName, omNs);
	
	try {
	    value.addChild(XMLConvert.toOM(xmlValue));
	} catch (XacmlSamlException e) {
	    throw new AxisFault(e.getMessage());
	}
	
	method.addChild(value);	
	
	return method;
    }

    /**
     * Send and receive a message on a service client
     * 
     * @param sc
     * @param endpoint
     * @param namespace
     * @param actionName
     * @param param
     * @return
     * @throws AxisFault
     */
    @Deprecated
    public static String sendReceive(ServiceClient sc, String endpoint, String namespace, String actionName, String... param) throws AxisFault {
	OMElement method = Communication.createPayload(namespace, actionName, param);
	Options opts = new Options();
	opts.setTo(new EndpointReference(endpoint));
	opts.setAction("urn:" + actionName);
	opts.setUseSeparateListener(false);
	opts.setCallTransportCleanup(true);
	sc.setOptions(opts);
	OMElement res = sc.sendReceive(method);
	String response = "smt " + res; // i need this, i don't know why
	sc.cleanupTransport();
	response = res.getFirstElement().getText();
	return response;
    }
    
    /**
     * Send and receive a message on a service client
     * 
     * @param sc
     * @param endpoint
     * @param namespace
     * @param actionName
     * @param param
     * @return
     * @throws AxisFault
     * @throws XacmlSamlException 
     */
    @Deprecated
    public static Element sendReceive(ServiceClient sc, String endpoint, String namespace, String actionName, String paramName, Element xmlValue) throws AxisFault, XacmlSamlException {
	OMElement method = Communication.createPayload(namespace, actionName, paramName, xmlValue);
	Options opts = new Options();
	opts.setTo(new EndpointReference(endpoint));
	opts.setAction("urn:" + actionName);
	opts.setUseSeparateListener(false);
	opts.setCallTransportCleanup(true);
	sc.setOptions(opts);
	OMElement res = sc.sendReceive(method);
	res.build();
	return XMLConvert.toDOM(res);
    }

    public static Element sendReceive(ServiceClient serviceClient, Options options, OMElement message) throws AxisFault {
	// set options
	serviceClient.setOptions(options);
        // send and receive
        OMElement resp = serviceClient.sendReceive(message);
        resp.build();
        serviceClient.cleanup();
        
        try {
	    return XMLConvert.toDOM(resp);
	} catch (XacmlSamlException e) {
	    throw new AxisFault(e.getMessage());
	}
    }
    
    // used by ongoing evaluator
    public static void sendStartAccessResponseSoap(String msg, String messageId, String replyTo, ConfigurationContext configContext,
	    ServiceClient sender) throws AxisFault {

	OMElement method = Communication.createPayload(UconConstants.UCON_NAMESPACE, "startaccessResponse", "return", msg);

	EndpointReference targetEPR = new EndpointReference(replyTo);
	Options options = new Options();
	options.setTo(targetEPR);
	options.setAction("urn:startaccessResponse");
	options.setRelationships(new RelatesTo[] { new RelatesTo(messageId) });
	// options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
	// options.activate(configContext);
	// ServiceClient sender = new ServiceClient(configContext, null);
	sender.setOptions(options);
	sender.fireAndForget(method);
	// System.out.println("[COM]: "+method);
	sender.cleanupTransport();
	sender.cleanup();
	// System.out.println("[COM]: response to startaccess was sent");
    }
}
