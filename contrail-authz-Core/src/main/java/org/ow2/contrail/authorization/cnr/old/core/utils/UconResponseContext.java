package org.ow2.contrail.authorization.cnr.old.core.utils;

@Deprecated
public class UconResponseContext {
	
	boolean accessDecision = false;	
	private String SAMLResponse = null;
	private String XACMLResponse = null;

	public boolean getAccessDecision() {
		return accessDecision;
	}
	
	public String getSAMLResponse() {
		return SAMLResponse;
	}		

	public String getXACMLResponse() {
		return XACMLResponse;
	}

	public void setSAMLResponse(String saml){
		SAMLResponse = saml;	
	}
	
	public void setXACMLResponse(String xacml){
		XACMLResponse = xacml;
	}	

	public void setAccessDecision(boolean decision){
		accessDecision = decision;
	}	
}
