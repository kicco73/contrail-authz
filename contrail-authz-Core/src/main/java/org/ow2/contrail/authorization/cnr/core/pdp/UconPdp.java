package org.ow2.contrail.authorization.cnr.core.pdp;

import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

public abstract class UconPdp {

	protected CorePhase policyType;
	protected String policyPath;

	public UconPdp(CorePhase policyType, String policyPath) {
		this.policyType = policyType;
		this.policyPath = policyPath;
	}

	public abstract String evaluate(String xacmlRequest) throws XacmlSamlException;

}
