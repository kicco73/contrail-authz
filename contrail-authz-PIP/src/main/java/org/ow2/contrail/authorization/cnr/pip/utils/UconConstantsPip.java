package org.ow2.contrail.authorization.cnr.pip.utils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class UconConstantsPip {

    // configuration file location for pip
    public static final String configFile = "/etc/contrail/authz/pip/pipconfig.properties";

    // Axis2 table entry for PIP
    public static final String IDENTITY_PROVIDER_URL = "identity_provider_url";
    public static final String PIP_COMMUNICATOR = "pip_communicator";
    // public static final String SUBSCRIBERS_SET = "SUBSCRIBERS";

    // error message
    public static final String ERROR_GENERIC_MESSAGE = "PIP ERROR: An error occur on PIP service. Contact the administrator.";
    public static final String ERROR_IP_URL = "PIP ERROR: Malformed URL for Identity Provider ";
    public static final String ERROR_IP = "PIP ERROR: Unable to reach Identity Provider";
    public static final String ERROR_MSG = "PIP ERROR: Malformed request";

    public static final Element PIP_ATTRIBUTE_SUBSCRIBE_OK;
    public static final Element PIP_ATTRIBUTE_SUBSCRIBE_NOT_OK;
    // initialize PIP responses
    static {
	DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder docBuilder;
	try {
	    docBuilder = docFactory.newDocumentBuilder();
	} catch (ParserConfigurationException e) {
	    throw new RuntimeException(e);
	}
	Document doc = docBuilder.newDocument();
	Element element = doc.createElement(UconConstants.PIP_ATTRIBUTE_RESPONSE_Tag);
	element.setAttribute(UconConstants.PIP_ATTRIBUTE_RESPONSE_VALUE_ATTR, "ok");
	PIP_ATTRIBUTE_SUBSCRIBE_OK = element;
	Element element2 = doc.createElement(UconConstants.PIP_ATTRIBUTE_RESPONSE_Tag);
	element2.setAttribute(UconConstants.PIP_ATTRIBUTE_RESPONSE_VALUE_ATTR, "ok");
	PIP_ATTRIBUTE_SUBSCRIBE_NOT_OK = element2;
    }

}
