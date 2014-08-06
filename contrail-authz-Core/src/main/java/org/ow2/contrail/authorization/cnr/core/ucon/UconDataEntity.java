package org.ow2.contrail.authorization.cnr.core.ucon;

import java.util.LinkedList;
import java.util.List;

import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.core.db.UconSession;
import org.ow2.contrail.authorization.cnr.core.utils.OpenSamlCore;
import org.ow2.contrail.authorization.cnr.core.utils.XacmlSamlCoreUtils;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class UconDataEntity {
	private static Logger log = LoggerFactory.getLogger(UconWs.class);	// KMcC;)
	private static String logTag = "[UCONDATAENTITY]";
    private XacmlSamlCoreUtils utils;
    // initial request (Xacml wrapped in Saml)
    private Element samlXacml;
    // session object
    private UconSession session;
    // attributes from PIP
    private Element attributesXml;
    private UconSession attributes;
    // initial request + attributes from PIP
    private String finalXacmlString;
    private UconSession finalSession;

    private UconDataEntity(ServiceContext serviceContext) throws XacmlSamlException {
	this.utils = OpenSamlCore.getInstance(serviceContext);
	this.session = null;
	this.attributesXml = null;
	this.samlXacml = null;
	this.finalXacmlString = null;
	this.finalSession = null;
    }

    /**
     * Create a data from a DOM saml-xacml request (e.g. tryaccess message)
     * 
     * @param serviceContext
     * @param samlXacml
     * @return
     * @throws XacmlSamlException
     */
    public static UconDataEntity getInstanceFromSamlXacml(ServiceContext serviceContext, Element samlXacml) throws XacmlSamlException {
	return new UconDataEntity(serviceContext).setSamlXacml(samlXacml);
    }

    public static UconDataEntity getInstanceFromXacmlRetrieveRequest(ServiceContext serviceContext, Element request) throws XacmlSamlException {
	return new UconDataEntity(serviceContext).setSessionByRetrieveRequest(request);
    }

    /**
     * Create a data from a UconSession (e.g. database access)
     * @param serviceContext
     * @param session
     * @return
     * @throws XacmlSamlException
     */
    public static UconDataEntity getInstanceFromUconSession(ServiceContext serviceContext, UconSession session) throws XacmlSamlException {
	return new UconDataEntity(serviceContext).setSession(session);
    }

    public static List<UconDataEntity> getInstanceFromUpdate(ServiceContext serviceContext, Element updates) throws XacmlSamlException {
	// get attributes (a set of UconSession) from update message 
	List<UconSession> updated = new UconDataEntity(serviceContext).utils.getHolderUpdate(updates);
	List<UconDataEntity> result = new LinkedList<UconDataEntity>();
	for(UconSession sess: updated) {
	    result.add(UconDataEntity.getInstanceFromUconSession(serviceContext, sess));
	}
	return null;
    }

    public Element getAttributeQueryRequest() throws XacmlSamlException {
	return utils.formAttributeQueryRequest(getSession());
    }

    public String getFinalXacmlString() throws XacmlSamlException {
	if (finalXacmlString == null) {
	    finalXacmlString = utils.formXacmlPDPRequest(getFinalSession()); //CHECKME the method is unimplemented
	}
	return finalXacmlString;
    }

    public UconSession getFinalSession() throws XacmlSamlException {
	if (finalSession == null) {
	    UconSession attributes = getAttributes();
	    if (attributes == null) {
	    	finalSession = session;
	    	log.debug("{} [KMcC;)] getFinalSession(): attributes == null, {}", logTag, finalSession);

	    } else {
		// add attributes (if they exist) to final session
	    	finalSession = UconSession.formCompleteSession(session, attributes);
	    	//finalSession = attributes;// XXX FIXME [KMcC;)] REMOVE!!! THIS WAS NOT IN THE ORIGINAL CODE 
	    	log.debug("{} [KMcC;)] getFinalSession(): attributes != null, {}", logTag, session);
	    }
	}
	log.debug("{} [KMcC;)] getFinalSession(): {}", logTag, finalSession);
	return finalSession;
    }

    private UconDataEntity setSamlXacml(Element samlXacml) {
	this.samlXacml = samlXacml;
	return this;
    }

    private UconDataEntity setSession(UconSession session) {
	this.session = session;
	return this;
    }

    private UconSession getSession() throws XacmlSamlException {
	if (session == null) {
	    setSession(utils.getUconSessionFromSamlXacmlRequest(samlXacml));
	}
	return session;
    }

    private UconDataEntity setSessionByRetrieveRequest(Element retrieve) throws XacmlSamlException {
	this.session = utils.getUconSessionFromXacmlRetrieve(retrieve);
	return this;
    }

    /**
     * Attach the attributes got from PIP attributes query
     * 
     * @param attributesXml
     * @return
     */
    public UconDataEntity setAttributesXml(Element attributesXml) {
	this.attributesXml = attributesXml;
	return this;
    }

    private UconSession getAttributes() throws XacmlSamlException {
	if (attributes == null) {
	    if (attributesXml == null) {
		return null; // CHECKME: when I get this session from database, this values are null
		// throw new IllegalStateException("No attributes exist. Use UconDataEntity.setAttributeXml(org.w3c.dom.Element) first");
	    }
	    attributes = utils.getAttributeFromPipResponse(attributesXml);
	}
	return attributes;
    }

    public void setStatus(String status) {
	// TODO:
	// session.setStatus(status);
    }

    public Element getSubscriptionRequest() throws XacmlSamlException {
	return utils.formSubscriptionRequest(getSession());
    }

    public String getSessionId() throws XacmlSamlException {
	return getSession().getSession_id_string();
    }
    
    @Override
    public boolean equals(Object o) {
	try {
	    
	    String id = getSessionId();
	    
	    return id.equals(((UconDataEntity) o).getSessionId());		
	    
	} catch (XacmlSamlException e) {
	    return false;
	}
	
    }

    public Element getUnsubscriptionRequest() {
	// TODO no! I need a static method to compose the unsubscribe message between more UconDataEntity
	return null;
    }

    public UconDataEntity getRevokeMessage() {
	// TODO I have to choose which kind of message I have to send
	return null;
    }

}
