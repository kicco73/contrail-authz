package org.ow2.contrail.authorization.cnr.old.core.utils;

import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;

@Deprecated
public class UconRequestContext {

	private CorePhase type;
	private String subjectId = null;
	private String objectId = null;
	private String actionId = null;

	private String SAMLRequest = null;
	private UconXacmlRequest XACMLRequest = null;
	private String issuer = null;
	private String sessionID = null;

	public UconRequestContext(CorePhase request) {
		type = request;
	}

	public String getSubjectId() {
		return subjectId;
	}

	public String getObjectId() {
		return objectId;
	}

	public String getActionId() {
		return actionId;
	}

	public String getSAMLRequest() {
		return SAMLRequest;
	}

	public UconXacmlRequest getXACMLRequest() {
		return XACMLRequest;
	}

	public void setXACMLRequest(UconXacmlRequest xacml) {
		XACMLRequest = xacml;
	}
	
	public String getSessionId() {
		return sessionID;
	}

	public CorePhase getRequestType() {
		return type;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setSubjectId(String id) {
		subjectId = id;
	}

	public void setObjectId(String id) {
		objectId = id;
	}

	public void setActionId(String id) {
		actionId = id;
	}

	public void setSAMLRequest(String saml) {
		SAMLRequest = saml;
	}

	public void setRequestType(CorePhase requestType) {
		type = requestType;
	}

	public void setSessionId(String sessionId) {
		this.sessionID = sessionId;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}
}
