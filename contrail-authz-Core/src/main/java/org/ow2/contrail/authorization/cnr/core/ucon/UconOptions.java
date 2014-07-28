package org.ow2.contrail.authorization.cnr.core.ucon;

import java.io.File;

import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UconOptions {

    private static Logger log = LoggerFactory.getLogger(UconWs.class);
    private static final String logTag = "[UCON_options]: ";

    private String policy_path;
    private int concurrency;
    private EndpointReference endpointPIP;
    private String sub_uuid, res_uuid, act_uuid;

    public static final String DEFAULT_POLICY_PATH = "/etc/contrail/contrail-authz-core/policies/";
    public static final int DEFAULT_CONCURRENCY = 32;
    public static final String DEFAULT_PIP_ENDPOINT = "http://localhost:8080/axis2/services/Pip";

    public static final String DEFAULT_SUB_UUID = "urn:contrail:names:federation:subject:uuid";
    public static final String DEFAULT_RES_UUID = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";
    public static final String DEFAULT_ACT_UUID = "urn:contrail:fed:action:id";

    private UconOptions(File optionsFile) {
	if (optionsFile == null || !optionsFile.exists()) {
	    setPolicy_path(DEFAULT_POLICY_PATH);
	    setConcurrency(DEFAULT_CONCURRENCY);
	    setEndpointPIP(new EndpointReference(DEFAULT_PIP_ENDPOINT));
	    setSub_uuid(DEFAULT_SUB_UUID);
	    setAct_uuid(DEFAULT_ACT_UUID);
	    setRes_uuid(DEFAULT_RES_UUID);
	} else {
	    //TODO read each options from a property file or set default values
	    setPolicy_path(DEFAULT_POLICY_PATH);
	    setConcurrency(DEFAULT_CONCURRENCY);
	    setEndpointPIP(new EndpointReference(DEFAULT_PIP_ENDPOINT));
	    setSub_uuid(DEFAULT_SUB_UUID);
	    setAct_uuid(DEFAULT_ACT_UUID);
	    setRes_uuid(DEFAULT_RES_UUID);
	}
    }

    public static void init(ServiceContext serviceContext, File optionsFile) {
	Object obj = serviceContext.getProperty(UconConstantsCore.UCON_OPTIONS);
	if (obj == null) {
	    UconOptions opt = new UconOptions(optionsFile);
	    serviceContext.setProperty(UconConstantsCore.UCON_OPTIONS, opt);
	}
    }

    public static UconOptions getInstance(ServiceContext serviceContext) {
	Object obj = serviceContext.getProperty(UconConstantsCore.UCON_OPTIONS);
	if (obj != null) {
	    return (UconOptions) obj;
	} else {
	    UconOptions opt = new UconOptions(null);
	    serviceContext.setProperty(UconConstantsCore.UCON_OPTIONS, opt);
	    return opt;
	}
    }

    public String getPolicy_path() {
	return policy_path;
    }

    public UconOptions setPolicy_path(String policy_path) {
	this.policy_path = policy_path;
	log.debug("{} policy path set to: {}", logTag, policy_path);
	return this;
    }

    public int getConcurrency() {
	return concurrency;
    }

    public UconOptions setConcurrency(int concurrency) {
	this.concurrency = concurrency;
	log.debug("{} concurrency level set to: {}", logTag, concurrency);
	return this;
    }

    public EndpointReference getEndpointPIP() {
	return endpointPIP;
    }

    public UconOptions setEndpointPIP(EndpointReference endpointPIP) {
	this.endpointPIP = endpointPIP;
	log.debug("{} PIP endpoint set to: {}", logTag, endpointPIP);
	return this;
    }

    public String getSub_uuid() {
	return sub_uuid;
    }

    public void setSub_uuid(String sub_uuid) {
	this.sub_uuid = sub_uuid;
    }

    public String getRes_uuid() {
	return res_uuid;
    }

    public void setRes_uuid(String res_uuid) {
	this.res_uuid = res_uuid;
    }

    public String getAct_uuid() {
	return act_uuid;
    }

    public void setAct_uuid(String act_uuid) {
	this.act_uuid = act_uuid;
    }

}
