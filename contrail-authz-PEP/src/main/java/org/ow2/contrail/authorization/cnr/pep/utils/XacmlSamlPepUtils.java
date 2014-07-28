package org.ow2.contrail.authorization.cnr.pep.utils;

import java.net.URL;
import java.util.List;

import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.w3c.dom.Element;

public interface XacmlSamlPepUtils {	
	/**
	 * Form a XACML 2.0 request from PEP to CH
	 * 
	 * @param id
	 *            Session ID
	 * @param attributes
	 *            List of UconAttributes (action, environment, resource, subject)
	 * @return A XACMLAuthzDecisionQuery string
	 * @throws XacmlSamlException 
	 */
	public Element formTryaccessMessage(List<PepRequestAttribute> attributes, String session_id) throws XacmlSamlException;

	/**
	 * Form a XACML 3.0 request from PEP to CH
	 * 
	 * @param id
	 *            Session ID
	 * @param attributes
	 *            List of UconAttributes (action, environment, resource, subject)
	 * @return A XACMLAuthzDecisionQuery string
	 * @throws XacmlSamlException 
	 */
//	public String formXACML30AuthzDecisionQuery(List<PepRequestAttribute> attributes, String session_id) throws Exception;
	
	
	/**
	 * Parse the response got by pdp
	 * 
	 * @param response
	 *            session id if access decision is permit or UconConstants.NO_SESSION_ID otherwise
	 * @return
	 * @throws XacmlSamlException
	 */
	public String getSessionIdFromTryaccessResponse(Element response) throws XacmlSamlException;
	
	/**
	 * Form the start access message
	 * 
	 * @param id
	 * @param replyTo
	 * @return
	 * @throws XacmlSamlException 
	 */
	public Element formStartaccessMessage(String id, URL replyTo) throws XacmlSamlException;
	
	/**
	 * Form the end access message
	 *  
	 * @param id
	 * @return
	 * @throws XacmlSamlException 
	 */
	public Element formEndaccessMessage(String id) throws XacmlSamlException;
	
	public String getStartaccessResponse(Element response);

	public String getEndaccessResponse(Element response);
	
	/**
	 * Form the map id message
	 * @param id
	 * 			old id
	 * @param ovf_id
	 * 			new id
	 * @return
	 */
	public String formMapIdMessage(String id, String ovf_id);
	
	/**
	 * Extracts attributes from a saml assertion string (certificate)
	 * @param samlAssertionString
	 * @param subject 
	 * @return
	 * @throws XacmlSamlException
	 */	
	public List<PepRequestAttribute> getPepAttributeFromSamlAssertion(String samlAssertionString, UconCategory subject) throws XacmlSamlException;

}
