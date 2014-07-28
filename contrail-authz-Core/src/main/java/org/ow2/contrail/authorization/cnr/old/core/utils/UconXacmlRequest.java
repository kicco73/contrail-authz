package org.ow2.contrail.authorization.cnr.old.core.utils;

import org.opensaml.xacml.ctx.RequestType;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

@Deprecated
public class UconXacmlRequest {
	private RequestType req = null;
	private OpenSamlCoreOLD utils = null;

	public UconXacmlRequest(String req, OpenSamlCoreOLD utils) throws XacmlSamlException {
		this(utils.convertXacmlRequestToObject(req), utils);
	}
	
	
	public UconXacmlRequest(RequestType req, OpenSamlCoreOLD utils) {
		this.req = req;
		this.utils = utils;
	}
	
	public RequestType getObject() {
		return req;
	}
	
	public void setObject(RequestType req) {
		this.req = req;
	}
	
	public String getString() throws XacmlSamlException {
		return utils.convertXacmlRequestToString(req);
	}
	
	public void setString(String str) throws XacmlSamlException {
		this.req = utils.convertXacmlRequestToObject(str);
	}
}
