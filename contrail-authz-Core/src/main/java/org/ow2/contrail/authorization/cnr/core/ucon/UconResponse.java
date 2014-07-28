package org.ow2.contrail.authorization.cnr.core.ucon;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.old.core.utils.OpenSamlCoreOLD;
import org.ow2.contrail.authorization.cnr.old.core.utils.XacmlSamlCoreUtilsOLD;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XMLConvert;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.w3c.dom.Element;

public class UconResponse {

    // TODO: return message? xml?
    public static final UconResponse TRUE = new UconResponse(UconConstants.RESPONSE_OK, true);
    public static final UconResponse FALSE = new UconResponse(UconConstants.RESPONSE_NOT, false);

    private String stringResp;
    private Element xmlResp = null;
    private boolean booleanResp;

    private UconResponse(String xacmlResp, boolean booleanResp) {
	this.setStringResponse(xacmlResp);
	this.setBooleanResponse(booleanResp);
    }

    public static UconResponse getUconResponse(ServiceContext serviceContext, String xacml) throws XacmlSamlException {
	XacmlSamlCoreUtilsOLD utils = OpenSamlCoreOLD.getInstance(serviceContext);
	boolean b = utils.getAccessDecision(xacml);
	return new UconResponse(xacml, b);
    }

    public String getStringResponse() {
	return stringResp;
    }

    public Element getXmlResponse() throws XacmlSamlException {
	if (xmlResp == null) {
	    if (stringResp != null) {
		try {
		    xmlResp = XMLConvert.toDOM(stringResp);
		} catch (ParserConfigurationException e) {
		    throw new XacmlSamlException(e);
		}
	    } else {
		throw new IllegalStateException();
	    }
	}
	return xmlResp;
    }

    private void setStringResponse(String xacmlResp) {
	this.stringResp = xacmlResp;
    }

    public boolean getBooleanResponse() {
	return booleanResp;
    }

    private void setBooleanResponse(boolean booleanResp) {
	this.booleanResp = booleanResp;
    }

}
