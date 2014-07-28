package org.ow2.contrail.authorization.cnr.core.pdp;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.core.ucon.UconOptions;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

public class PdpModule {

    private LinkedBlockingQueue<UconPdp> pdps;

    private PdpModule(ServiceContext serviceContext, CorePhase policy) {
	UconOptions options = UconOptions.getInstance(serviceContext);
	if (options != null) {
	    pdps = new LinkedBlockingQueue<UconPdp>(options.getConcurrency());
	    for(int i = 0; i < options.getConcurrency(); i++)
		pdps.add(new BalanaPdp(policy, options.getPolicy_path()));
	} else {
	    throw new RuntimeException("Options not initialized");
	}
    }

    public static void init(ServiceContext serviceContext, CorePhase policy) {
	String entry = "";
	switch (policy) {
	case PRE:
	    entry = UconConstantsCore.PRE_PDP;
	    break;
	case ON:
	    entry = UconConstantsCore.ON_PDP;
	    break;
	case POST:
	    entry = UconConstantsCore.POST_PDP;
	    break;
	}
	Object obj = serviceContext.getProperty(entry);
	if (obj == null) {
	    PdpModule module = new PdpModule(serviceContext, policy);
	    serviceContext.setProperty(entry, module);
	}
    }

    public static void initAllThree(ServiceContext serviceContext) {
	init(serviceContext, CorePhase.PRE);
	init(serviceContext, CorePhase.ON);
	init(serviceContext, CorePhase.POST);
    }

    public static String evaluate(ServiceContext serviceContext, CorePhase policy, String xacmlRequest) throws InterruptedException,
	    XacmlSamlException {
	String entry = "";
	switch (policy) {
	case PRE:
	    entry = UconConstantsCore.PRE_PDP;
	    break;
	case ON:
	    entry = UconConstantsCore.ON_PDP;
	    break;
	case POST:
	    entry = UconConstantsCore.POST_PDP;
	    break;
	}
	Object obj = serviceContext.getProperty(entry);
	PdpModule module;
	if (obj != null) {
	    module = (PdpModule) obj;
	} else {
	    module = new PdpModule(serviceContext, policy);
	    serviceContext.setProperty(entry, module);
	}

	UconPdp pdp = module.pdps.take();
	String eval;
	try {
	    eval = pdp.evaluate(xacmlRequest);
	} finally {
	    module.pdps.offer(pdp);
	}
	return eval;
    }
}
