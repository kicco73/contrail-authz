package org.ow2.contrail.authorization.cnr.pep;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.ow2.contrail.authorization.cnr.utils.Communication;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.w3c.dom.Element;

public class PepCommunicator {

    private final Options tryaccessOptions;
    private final Options mapidOptions;
    private final Options startaccessOptions;
    private final Options endaccessOptions;
    
    private AtomicInteger asynchStartaccessNumber;
    private SimpleHTTPServer httpListener;
    
    private String port;
    private EndpointReference endpointPEP;
    private EndpointReference endpointPDP;

    private ConfigurationContext configContext; // Axis2 configuration
    
    public PepCommunicator(String eprPDP, String host, String port) throws AxisFault {

	// create configuration from the default axis2.xml
	this.port = port;
	this.endpointPEP = new EndpointReference("http://" + host + ":" + port);
	
	// ConfigurationContext
	configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
	
	asynchStartaccessNumber = new AtomicInteger(0);
	
	endpointPDP = new EndpointReference(eprPDP);

	// initialize tryaccess options
	tryaccessOptions = new Options();
	// setting target EPR
	tryaccessOptions.setTo(endpointPDP);
	// Setting action
	tryaccessOptions.setAction("urn:" + UconConstants.TRYACCESS_METHOD_NAME);
	tryaccessOptions.setCallTransportCleanup(true);

	// initialize mapid options
	mapidOptions = new Options();
	// setting target EPR
	mapidOptions.setTo(endpointPDP);
	// Setting action
	mapidOptions.setAction("urn:" + UconConstants.MAPID_METHOD_NAME);
	mapidOptions.setCallTransportCleanup(true);
	
	// initialize startaccess options
	startaccessOptions = new Options();
	// setting target EPR
	startaccessOptions.setTo(endpointPDP);
	// Setting action
	startaccessOptions.setAction("urn:" + UconConstants.STARTACCESS_METHOD_NAME);
	startaccessOptions.setCallTransportCleanup(true);
	
	// initialize tryaccess options
	endaccessOptions = new Options();
	// setting target EPR
	endaccessOptions.setTo(endpointPDP);
	// Setting action
	endaccessOptions.setAction("urn:" + UconConstants.ENDACCESS_METHOD_NAME);
	endaccessOptions.setCallTransportCleanup(true);
    }
    
    protected Element sendATryaccess(Element xmlMessage) throws AxisFault {
	    
        // compose OMElement message
        OMElement message = Communication.createPayload(UconConstants.UCON_NAMESPACE, UconConstants.TRYACCESS_METHOD_NAME,
        	UconConstants.TRYACCESS_PARAM_NAME, xmlMessage);
        
        // create axis service client // CHECKME: each time????
        ServiceClient serviceClient = new ServiceClient();
        
        System.out.println("mess:\n"+message);
        
        // do a send-receive
        Element resp = Communication.sendReceive(serviceClient, tryaccessOptions, message);

        return resp;
    }

    protected Element sendAStartaccess(Element xmlMessage) throws AxisFault {
        
	// String msg = message + "%" + url.toString();
	// compose OMElement message
	OMElement message = Communication.createPayload(UconConstants.UCON_NAMESPACE, UconConstants.STARTACCESS_METHOD_NAME, 
		UconConstants.STARTACCESS_PARAM_NAME, xmlMessage);
	
        // create axis service client // CHECKME: each time????
        ServiceClient serviceClient = new ServiceClient();
        
        // do a send-receive
        Element resp = Communication.sendReceive(serviceClient, startaccessOptions, message);
        
        // setting startaccess options
        serviceClient.setOptions(startaccessOptions);

        return resp;
    
    }
    
    protected void sendAnAsynchStartaccess(Element xmlMessage, PepCallback callback) throws AxisFault {
	// compose OMElement message
	OMElement message = Communication.createPayload(UconConstants.UCON_NAMESPACE, UconConstants.STARTACCESS_METHOD_NAME, 
		UconConstants.STARTACCESS_PARAM_NAME, xmlMessage);
		
	// if (count.get() == 0) {
	// configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
	// configContext.getAxisConfiguration().getTransportIn(serviceName).getParameter("port").setValue(port);
	//
	// TransportInDescription ti = new TransportInDescription(serviceName);
	// ti.addParameter(new Parameter("port",port));
	// configContext.getAxisConfiguration().addTransportIn(ti);
	// }
	
	// initialize asynchronous startaccess options
	Options startaccessAsynchOptions = new Options();
	// setting target EPR
	startaccessAsynchOptions.setTo(endpointPDP);
	// Setting action
	startaccessAsynchOptions.setAction("urn:" + UconConstants.STARTACCESS_METHOD_NAME);
	// setting asynchronous invocation
	startaccessAsynchOptions.setUseSeparateListener(true);
	startaccessAsynchOptions.setTransportInProtocol(Constants.TRANSPORT_HTTP);
	startaccessAsynchOptions.setReplyTo(endpointPEP);
	SimpleHTTPServer listener = incrAsynchSessionCount();
	startaccessAsynchOptions.setListener(listener);
	startaccessAsynchOptions.setCallTransportCleanup(true);
		
	ServiceClient serviceClient = new ServiceClient(configContext, null);
	serviceClient.engageModule("addressing");	
	serviceClient.setOptions(startaccessAsynchOptions);
	serviceClient.sendReceiveNonBlocking(message, callback);
    }
    
    protected Element sendAnEndaccess(Element xmlMessage) throws AxisFault {
        // compose OMElement message
        OMElement message = Communication.createPayload(UconConstants.UCON_NAMESPACE, UconConstants.ENDACCESS_METHOD_NAME, 
        	UconConstants.ENDACCESS_PARAM_NAME, xmlMessage);
        
        // create axis service client // CHECKME: each time????
        ServiceClient serviceClient = new ServiceClient();
        
        // do a send-receive
        Element resp = Communication.sendReceive(serviceClient, endaccessOptions, message);
    
        return resp;
    }

    protected void sendAMapid(String old_id, String new_id) throws AxisFault {
        // compose OMElement message
        OMElement mess = Communication.createPayload(UconConstants.UCON_NAMESPACE, UconConstants.MAPID_METHOD_NAME, UconConstants.MAPID_PARAM1_NAME, 
        	old_id, UconConstants.MAPID_PARAM2_NAME, new_id);
        // create axis service client // CHECKME: each time????
        ServiceClient serviceClient = new ServiceClient();
        // synchOpts.setCallTransportCleanup(true);
        serviceClient.setOptions(mapidOptions);
        serviceClient.fireAndForget(mess);
        serviceClient.cleanup();
	
    }
    
    private SimpleHTTPServer incrAsynchSessionCount() throws AxisFault {
	if (asynchStartaccessNumber.getAndIncrement() == 0) {
	    // transportIn.getReceiver().start();
	    // SimpleHTTPServer server = (SimpleHTTPServer) this.configContext.getAxisConfiguration().getTransportIn(serviceName).getReceiver();
	    httpListener = new SimpleHTTPServer(configContext, Integer.parseInt(port));
	    if (!httpListener.isRunning()) {
		httpListener.start();
		// System.out.println("started");
	    } else {
		// System.out.println("not started");
	    }

	    // this.configContext.getAxisConfiguration().getTransportIn(serviceName).getReceiver().start();
	}
	return httpListener;
	// System.out.println("incr to "+count.get());
    }

    protected void decrAsynchSessionCount() throws AxisFault {
	// System.out.println("decr from " + count.get());
	if (asynchStartaccessNumber.decrementAndGet() == 0) {

	    // /////////////**********************************************
	    // ///////////// PERFORMANCE TEST CODE //CHECKME
	    // /////////////**********************************************

	    // endTime = System.currentTimeMillis();
	    // System.out.println("end time " + endTime);

	    // /////////////**********************************************
	    try {
		// SimpleHTTPServer server = (SimpleHTTPServer) this.configContext.getAxisConfiguration().getTransportIn(serviceName)
		// .getReceiver();

		// System.out.println("check if httpListener is running");
		if (httpListener.isRunning()) {
		    // System.out.println("httpListener is running");
		    // this.configContext.getAxisConfiguration().getTransportIn(serviceName).getReceiver().stop();
		    // this.configContext.getAxisConfiguration().getTransportIn(serviceName).getReceiver().destroy();
		    // configContext.getAxisConfiguration().cleanup();
		    httpListener.stop();
		    httpListener.destroy();
		    configContext.cleanupContexts();		    
		}
		// Thread.sleep(1);
	    } catch (Exception e) {
		System.out.println("catched!!!\n"+e.getMessage());
	    }
	}
    }

}
