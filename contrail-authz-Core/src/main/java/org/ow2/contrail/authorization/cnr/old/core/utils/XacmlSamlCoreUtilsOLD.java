package org.ow2.contrail.authorization.cnr.old.core.utils;

import java.util.List;

import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;
import org.ow2.contrail.authorization.cnr.core.db.UconHolder;
import org.ow2.contrail.authorization.cnr.core.db.UconSession;
import org.ow2.contrail.authorization.cnr.core.ucon.UconDataEntity;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.w3c.dom.Element;

public interface XacmlSamlCoreUtilsOLD {

	
	
	public UconSession getUconSessionFromXacmlRetrieve(Element retrieve);
		

	/**
	 * Add the PIP attributes to xacmlAuthzQuery
	 * 
	 * @param xacmlAuthzQuery
	 *            The xacmlAuthzQuery
	 * @param attributePIP
	 *            List of UconAttributes
	 * @return The new xacmlAuthzQuery
	 */
	public String formXacmlAuthzQueryAttribute(UconXacmlRequest xacmlAuthzQuery, List<UconAttribute> attribute) throws XacmlSamlException;
	public String convertXacmlAuthzQuery20to30(UconXacmlRequest xacmlAuthzQuery, List<UconAttribute> attributes) throws XacmlSamlException;
	/**
	 * Parse the response got by pdp
	 * 
	 * @param response
	 *            XACML response
	 * @return
	 */
	public boolean getAccessDecision(String xacmlResponse) throws XacmlSamlException;
	public boolean getAccessDecision30(String xacmlResponse) throws XacmlSamlException;
		
	/**
	 * Incapsulate a xacmlresponse (by a pdp) and sessionId in a saml response
	 * 
	 * @param xacmlResponse
	 * @param sessionId
	 * @return
	 * @throws XacmlSamlException 
	 */
	public String formResponse(String xacmlResponse, String sessionId) throws XacmlSamlException;
	
	/**
	 * Used in session manager with the pip response
	 * 
	 * @param attributesXml
	 * @param holder
	 * @return
	 * @throws XacmlSamlException
	 */
	public UconSession getAttributeFromPipResponse(Element attributesXml) throws XacmlSamlException;
	
	/**
	 * Used in session manager with pip update
	 * @param updateMessage
	 * @return
	 * @throws XacmlSamlException
	 */
	public List<UconAttribute> getAttributeFromPipUpdate(String updateMessage) throws XacmlSamlException;
	
	/**
	 * Extract every information from request context
	 * @param request
	 * @return
	 * @throws XacmlSamlException 
	 */
	public UconRequestContext getRequestContextFromAccessRequest(String request) throws XacmlSamlException;
	
	//questi son buoni
	
	public UconSession getUconSessionFromSamlXacmlRequest(String authzQuery) throws XacmlSamlException;
	public String getXacmlRequestFromUconSession(UconSession session) throws XacmlSamlException;

	public Element formAttributeQueryRequest(UconSession session);
	public Element formSubscriptionRequest(UconSession session);
	public Element formUnubscriptionRequest(UconSession session);
}
