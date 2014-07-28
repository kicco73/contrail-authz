package org.ow2.contrail.authorization.cnr.old.utils.pep;

import java.util.List;

import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

public interface XacmlSamlPepUtils_OLD {	
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
	public String formXACMLAuthzDecisionQuery(List<PepRequestAttribute_OLD> attributes, String session_id) throws XacmlSamlException;

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
	public String getSessionIdFromResponse(String response) throws XacmlSamlException;
	
	/**
	 * Form the start access message
	 * 
	 * @param id
	 * @return
	 * @throws XacmlSamlException 
	 */
	public String formStartMessage(String id) throws XacmlSamlException;
	
	/**
	 * Form the end access message
	 *  
	 * @param id
	 * @return
	 * @throws XacmlSamlException 
	 */
	public String formEndMessage(String id) throws XacmlSamlException;
	
	
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
	 * @return
	 * @throws XacmlSamlException
	 */	
	public List<PepRequestAttribute_OLD> getPepAttributeFromSamlAssertion(String samlAssertionString) throws XacmlSamlException;
}
