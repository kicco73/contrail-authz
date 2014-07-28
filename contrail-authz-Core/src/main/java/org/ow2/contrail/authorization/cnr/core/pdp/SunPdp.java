package org.ow2.contrail.authorization.cnr.core.pdp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import oasis.names.tc.xacml._2_0.context.schema.os.RequestType;

import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

import com.sun.xacml.BindingUtility;
import com.sun.xacml.Indenter;
import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.AttributeFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.impl.CurrentEnvModule;
import com.sun.xacml.finder.impl.SelectorModule;
import com.sun.xacml.support.finder.FilePolicyModule;

public class SunPdp extends UconPdp {

	// FIXME: policy storage?
	//private String POLICY_DIR = "/srv/cloud/one/newworkspace/contrail-uconauthz/policies/";
	//private String POLICY_DIR = "/etc/contrail/uconws/policies/";
	//private String POLICY_STORAGE_PRE = "policy-pre.xml";
	//private String POLICY_STORAGE_ON = "policy-on.xml";
	//private String POLICY_STORAGE_POST = "policy-post.xml";
	
	private PDP pdp = null;

	public SunPdp(CorePhase policyType, String policyPath) {
		super(policyType, policyPath);
		//if (TestUtils.PRINT) System.out.println("[PDP] Initialization");

		// 1 - attribute finder (currently is an empty set)
		
		List<AttributeFinderModule> attributeModules = new ArrayList<AttributeFinderModule>();
		CurrentEnvModule envAttributeModule = new CurrentEnvModule();
		attributeModules.add(envAttributeModule);
		SelectorModule selectorAttributeModule = new SelectorModule();
		attributeModules.add(selectorAttributeModule);
		
		AttributeFinder attributeFinder = new AttributeFinder();
		attributeFinder.setModules(attributeModules);

		// 2 - policy finder;
		//if (!TestUtils.VEPTEST) {
			switch (policyType) {
			case PRE:
				policyPath += UconConstantsCore.POLICY_DIR_PRE + UconConstantsCore.POLICY_STORAGE_PRE;
				break;
			case ON:
				policyPath += UconConstantsCore.POLICY_DIR_ON + UconConstantsCore.POLICY_STORAGE_ON;
				break;
			case POST:
				policyPath += UconConstantsCore.POLICY_DIR_POST + UconConstantsCore.POLICY_STORAGE_POST;
				break;
			}
		
		// TODO: CAN WE VALIDATE THE POLICY?
		Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();		
		FilePolicyModule filePolicyModule = new FilePolicyModule();
		filePolicyModule.addPolicy(policyPath);
		policyModules.add(filePolicyModule);
		PolicyFinder policyFinder = new PolicyFinder();
		policyFinder.setModules(policyModules);

		// 3 - resource finder - is null
		PDPConfig config = new PDPConfig(attributeFinder, policyFinder, null);

		// init PDP
		pdp = new PDP(config);
		// System.out.println("[PDP] Initialized");
	}

	@SuppressWarnings("unchecked")
	// type safety warning
	public String evaluate(String xacmlRequest) throws XacmlSamlException {
		// org.opensaml.xacml.ctx.RequestType xacmlRequest
		//if (TestUtils.PRINT) System.out.println("[PDP] Starting evaluation");
		RequestType request = null;
		
		//System.out.println("[PDP utils] request:\n"+xacmlRequest);
		byte[] bytes = (xacmlRequest).getBytes();
		// byte[] bytes = (prova).getBytes();
		// System.out.println("com'era:\n"+srequest);
		// System.out.println("come dev'essere:\n"+prova2);
		ByteArrayInputStream iorequest = new ByteArrayInputStream(bytes);	
		try {
			Unmarshaller u = BindingUtility.getUnmarshaller();			
			request = ((JAXBElement<oasis.names.tc.xacml._2_0.context.schema.os.RequestType>) u.unmarshal(iorequest)).getValue();
		} catch (JAXBException e) {
			throw new XacmlSamlException("Unable to unmarshall the following xacmlRequest:\n"+xacmlRequest);
		}
		//request = utils.convertRequestForPDP(xacmlRequest);
		
		ResponseCtx response = pdp.evaluate(request);
		ByteArrayOutputStream byte1 = new ByteArrayOutputStream();

		// NOTE: for Ruby next line is better to change to: response.encode(byte1);
		response.encode(byte1, new Indenter());
		String xacmlRes = byte1.toString();
		// insert xmlns="urn:oasis:names:tc:xacml:2.0:context:schema:os" should be deprecated later
		String res = xacmlRes.replaceFirst("<Response", "<Response xmlns=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\"");

		return res;

	}
}
