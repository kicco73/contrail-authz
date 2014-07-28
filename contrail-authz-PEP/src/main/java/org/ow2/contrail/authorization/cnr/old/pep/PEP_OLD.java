package org.ow2.contrail.authorization.cnr.old.pep;

import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.http.SimpleHTTPServer;
import org.ow2.contrail.authorization.cnr.old.utils.pep.OpenSamlPep_OLD;
import org.ow2.contrail.authorization.cnr.old.utils.pep.XacmlSamlPepUtils_OLD;
import org.ow2.contrail.authorization.cnr.utils.Communication;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

public class PEP_OLD {

    // /////////////**********************************************
    // ///////////// PERFORMANCE TEST CODE //CHECKME
    // /////////////**********************************************
    private long endTime = 0;

    public long getTime() {
	return endTime;
    }

    // /////////////**********************************************

    // default configuration
    private String eprPDP;
    private String eprPEP;
    private String port;

    private XacmlSamlPepUtils_OLD utils;

    private ConfigurationContext configContext;

    // private int count = 0;
    private AtomicInteger count;

    // private class IncomeMessagePrinter extends AbstractHandler {
    //
    // public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
    // System.out.println("HANDLER PEP message: "+msgContext.getEnvelope().toString());
    // return InvocationResponse.CONTINUE;
    // }
    //
    // }

    SimpleHTTPServer server;

    public PEP_OLD(String endpointAddress, String port, String host) throws AxisFault {
	// create configuration from the default axis2.xml
	this.port = port;
	eprPDP = endpointAddress;
	eprPEP = "http://" + host + ":" + port;
	// ConfigurationContext
	configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);

	// AxisConfiguration config = null;
	// config = configContext.getAxisConfiguration();
	// List<Phase> phasesIn = null;
	// phasesIn = config.getInFlowPhases();
	// phasesIn.get(0).setPhaseFirst(new IncomeMessagePrinter());
	// phasesIn.get(phasesIn.size()-1).setPhaseLast(new IncomeMessagePrinter());

	// server = new SimpleHTTPServer(configContext,Integer.parseInt(port));
	// configContext.getAxisConfiguration().getTransportIn("http").setReceiver(server);
	//

	// for(Parameter p : configContext.getAxisConfiguration().getTransportIn("http").getParameters()) {
	// System.out.println("parameter name " +p.getName()+ " "+ p.getValue());
	// }
	try {
	    utils = OpenSamlPep_OLD.getInstance();
	} catch (XacmlSamlException e) {
	    System.out.println("Initialization failed (Unable to initialize OpenSaml library)");
	}

	count = new AtomicInteger(0);
    }

    protected XacmlSamlPepUtils_OLD getXacmlSamlPepUtils() {
	return utils;
    }

    protected ConfigurationContext getConfigurationContext() {
	return this.configContext;
    }

    ServiceClient serviceClient;

    protected synchronized void doAStartAccess(String message, PepCallback callback) throws AxisFault {
	SimpleHTTPServer server = incrCount();

	// if (count.get() == 0) {
	// configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
	// configContext.getAxisConfiguration().getTransportIn(serviceName).getParameter("port").setValue(port);
	//
	// TransportInDescription ti = new TransportInDescription(serviceName);
	// ti.addParameter(new Parameter("port",port));
	// configContext.getAxisConfiguration().addTransportIn(ti);
	// }

	serviceClient = new ServiceClient(configContext, null);
	serviceClient.engageModule("addressing");
	Options opts = new Options();
	// setting target EPR
	opts.setTo(new EndpointReference(eprPDP));
	String actionName = "startaccessAsynch";
	// Setting action
	opts.setListener(server);
	opts.setAction("urn:" + actionName);
	// setting asynchronous invocation
	opts.setUseSeparateListener(true);
	opts.setTransportInProtocol(Constants.TRANSPORT_HTTP);
	opts.setReplyTo(new EndpointReference(eprPEP));
	// opts.setReplyTo(new EndpointReference("http://146.48.99.126:4500"));
	// opts.setReplyTo(new EndpointReference("http://146.48.82.58:15000"));
	opts.setCallTransportCleanup(true);
	// setting created option into service client
	serviceClient.setOptions(opts);
	serviceClient
		.sendReceiveNonBlocking(Communication.createPayload(UconConstants.UCON_NAMESPACE, actionName, "ackAssertion", message), callback);
    }

    protected synchronized void doAStartAccess(String message, URL url) throws AxisFault {

	serviceClient = new ServiceClient();
	Options opts = new Options();
	// setting target EPR
	opts.setTo(new EndpointReference(eprPDP));
	String actionName = "startaccess";
	// Setting action
	opts.setAction("urn:" + actionName);
	// setting asynchronous invocation
	// opts.setReplyTo(new EndpointReference(url.toString()));
	opts.setCallTransportCleanup(true);
	// setting created option into service client
	serviceClient.setOptions(opts);
	String msg = message + "%" + url.toString();
	OMElement res = serviceClient.sendReceive(Communication.createPayload(UconConstants.UCON_NAMESPACE, actionName, "message", msg));
	// System.out.println("startacces "+res);
    }

    private SimpleHTTPServer incrCount() throws AxisFault {
	if (count.getAndIncrement() == 0) {
	    // transportIn.getReceiver().start();
	    // SimpleHTTPServer server = (SimpleHTTPServer) this.configContext.getAxisConfiguration().getTransportIn(serviceName).getReceiver();
	    server = new SimpleHTTPServer(configContext, Integer.parseInt(port));
	    if (!server.isRunning()) {
		server.start();
		// System.out.println("started");
	    } else {
		// System.out.println("not started");
	    }

	    // this.configContext.getAxisConfiguration().getTransportIn(serviceName).getReceiver().start();
	}
	return server;
	// System.out.println("buongiorno "+count.get());
    }

    protected void decrCount() throws AxisFault {
	// System.out.println("buonasera " + count.get());
	if (count.decrementAndGet() == 0) {

	    // /////////////**********************************************
	    // ///////////// PERFORMANCE TEST CODE //CHECKME
	    // /////////////**********************************************

	    endTime = System.currentTimeMillis();
	    // System.out.println("end time " + endTime);

	    // /////////////**********************************************
	    try {
		// SimpleHTTPServer server = (SimpleHTTPServer) this.configContext.getAxisConfiguration().getTransportIn(serviceName)
		// .getReceiver();

		// System.out.println("prima di if");
		if (server.isRunning()) {
		    // System.out.println("prima di stop");
		    // this.configContext.getAxisConfiguration().getTransportIn(serviceName).getReceiver().stop();
		    // this.configContext.getAxisConfiguration().getTransportIn(serviceName).getReceiver().destroy();
		    // configContext.getAxisConfiguration().cleanup();
		    server.stop();
		    server.destroy();
		    configContext.cleanupContexts();
		    // System.out.println("dopo stop");
		}
		// Thread.sleep(1);
	    } catch (Exception e) {
		System.out.println("catched!!!");
	    }
	}
    }

    protected String getPDPEndpoint() {
	return eprPDP;
    }

    // protected String getPEPEndpoint() {
    // return eprPEP;
    // }
}
