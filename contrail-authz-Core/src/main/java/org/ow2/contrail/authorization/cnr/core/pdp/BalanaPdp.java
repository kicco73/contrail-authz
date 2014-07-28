package org.ow2.contrail.authorization.cnr.core.pdp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.impl.CurrentEnvModule;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;
import org.wso2.balana.finder.impl.SelectorModule;

public class BalanaPdp extends UconPdp {

	private PDP pdp;
	
	public BalanaPdp(CorePhase policyType, String policyPath) {
		super(policyType, policyPath);
		String policyLocation = policyPath;
		switch (policyType) {
		case PRE:
			policyLocation += UconConstantsCore.POLICY_DIR_PRE;
			break;
		case ON:
			policyLocation += UconConstantsCore.POLICY_DIR_ON;
			break;
		case POST:
			policyLocation += UconConstantsCore.POLICY_DIR_POST;
			break;
		}
		//doesn't work changing system environment variable
		// System.setProperty(FileBasedPolicyFinderModule.POLICY_DIR_PROPERTY, policyLocation);
		// Balana balana = Balana.getInstance();
		// PDPConfig pdpConfig = balana.getPdpConfig();
		// System.out.println("[BALANAPDP] directory: "+System.getProperty(FileBasedPolicyFinderModule.POLICY_DIR_PROPERTY));
				
		PolicyFinder policyFinder = new PolicyFinder();
		Set<PolicyFinderModule> policyFinderModules = new HashSet<PolicyFinderModule>();
		Set<String> locationSet = new HashSet<String>();
		locationSet.add(policyLocation); //set the correct policy location
		FileBasedPolicyFinderModule fileBasedPolicyFinderModule = new FileBasedPolicyFinderModule(locationSet);
		policyFinderModules.add(fileBasedPolicyFinderModule);
		policyFinder.setModules(policyFinderModules);

		AttributeFinder attributeFinder = new AttributeFinder();
		List<AttributeFinderModule> attributeFinderModules = new ArrayList<AttributeFinderModule>();
		SelectorModule selectorModule = new SelectorModule();
		CurrentEnvModule currentEnvModule = new CurrentEnvModule();
		attributeFinderModules.add(selectorModule);
		attributeFinderModules.add(currentEnvModule);
		attributeFinder.setModules(attributeFinderModules);

		PDPConfig pdpConfig = new PDPConfig(attributeFinder, policyFinder, null, false);

		pdp = new PDP(pdpConfig);

	}

	public String evaluate(String xacmlRequest) {
		String xacmlRes = pdp.evaluate(xacmlRequest);
		//xacmlRes = xacmlRes.replaceFirst("<Response", "<Response xmlns=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\"");
		return xacmlRes;
	}
}
