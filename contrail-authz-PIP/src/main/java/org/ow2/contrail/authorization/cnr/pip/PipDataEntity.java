package org.ow2.contrail.authorization.cnr.pip;

import java.util.LinkedList;
import java.util.List;

import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.pip.db.PipOwner;
import org.ow2.contrail.authorization.cnr.pip.db.PipSubscriber;
import org.ow2.contrail.authorization.cnr.pip.utils.OpenSamlPip;
import org.ow2.contrail.authorization.cnr.pip.utils.XacmlSamlPipUtils;
import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class PipDataEntity {
    
    private static Logger log = LoggerFactory.getLogger(PipDataEntity.class);
    private static final String logTag = "[PIPDataEntity]: ";
    
    private XacmlSamlPipUtils utils;
    private PipOwner dao;
    private Element saml;
    private Element xacml;
    private UconCategory category;
    private String subscriber;
    private boolean autoupdate;

    private PipDataEntity(ServiceContext serviceContext) throws XacmlSamlException {
	this.utils = OpenSamlPip.getInstance(serviceContext);
	this.dao = null;
	this.saml = null;
	this.xacml = null;
	this.category = null;
	this.subscriber = null;
	this.autoupdate = false;
    }

    public static List<PipDataEntity> getInstanceFromUconRequest(ServiceContext serviceContext, Element request, String subscriber)
	    throws XacmlSamlException {

	if (serviceContext == null || request == null) {
		// KMcC;) 
		log.error("{} [KMcC;)] getInstanceFromUconRequest(): NULL POINTER EXCEPTION!", logTag);
	    throw new NullPointerException();
	}

	XacmlSamlPipUtils utils = OpenSamlPip.getInstance(serviceContext);
	// separate different owner in request
	List<Element> list = utils.getUconGenericRequests(request);
	List<PipDataEntity> requested = new LinkedList<PipDataEntity>();
	for (Element elem : list) {
	    // for each owner form attribute query request
	    String owner = elem.getTextContent();
	    log.debug("{} attribute holder name {}", logTag, owner);
	    UconCategory category = UconCategory.getCategoryFromTag(elem.getTagName());
	    log.debug("{} attribute holder category {}", logTag, category);
	    PipOwner dao = new PipOwner();
	    dao.setName(owner);
	    dao.setCategory(category);
	    PipDataEntity data = new PipDataEntity(serviceContext);
	    data.setDao(dao);
	    data.setSubscriber(subscriber);
	    data.category = category; // [KMcC;)] fixup
	    data.subscriber = subscriber;// [KMcC;)] fixup
	    log.debug("{} complete data: {}", logTag, dao);
	    log.debug("{} [KMcC;)] getInstanceFromUconRequest() category: {}", logTag, data.getCategory());
	    log.debug("{} [KMcC;)] getInstanceFromUconRequest() subscribers: {} {}", logTag, data.getSubscriber(), subscriber);
	    requested.add(data);
	    // requests.add(data.setSaml(utils.formSAMLAttributeQuery(owner), category, null, false));
	}
	return requested;
    }

    public static PipDataEntity getInstanceFromSaml(ServiceContext serviceContext, Element samlString, UconCategory category, String subscriber,
	    boolean autoupdate) throws XacmlSamlException {

	if (serviceContext == null || samlString == null || category == null) {
		// KMcC;) 
		log.error("{} [KMcC;)] getInstanceFromSaml(): NULL POINTER EXCEPTION!", logTag);
	    throw new NullPointerException();
	}
	return (new PipDataEntity(serviceContext)).setSaml(samlString, category, subscriber, autoupdate);

    }

    public static PipDataEntity getInstanceFromXacml(ServiceContext serviceContext, Element xacmlString, String subscriber, boolean autoupdate)
	    throws XacmlSamlException {
	if (serviceContext == null || xacmlString == null) {
		// KMcC;) 
		log.error("{} [KMcC;)] getInstanceFromXacml(): NULL POINTER EXCEPTION!", logTag);
	    throw new NullPointerException();
	}
	return (new PipDataEntity(serviceContext)).setXacml(xacmlString, subscriber, autoupdate);
    }

    public static PipDataEntity getInstanceFromDao(ServiceContext serviceContext, PipOwner dao) throws XacmlSamlException {
	return (new PipDataEntity(serviceContext)).setDao(dao);
    }

    // private setters
    private PipDataEntity setXacml(Element xacmlString, String subscriber, boolean autoupdate) {
	this.xacml = xacmlString;
	this.subscriber = subscriber;
	this.autoupdate = autoupdate;
	return this;
    }

    private PipDataEntity setSaml(Element samlXml, UconCategory category, String subscriber, boolean autoupdate) {
	this.saml = samlXml;
	this.category = category;
	this.subscriber = subscriber;
	this.autoupdate = autoupdate;
	return this;
    }

    private PipDataEntity setDao(PipOwner dao) {
	this.dao = dao;
	return this;
    }

    // public getters
    public PipOwner getDao() throws XacmlSamlException {
	if (dao == null) {
	    if (saml != null) {
		dao = utils.convertSAMLtoSubscription(getSaml(), category, subscriber, autoupdate);
	    } else {
		if (xacml != null) {
		    // TODO ?
		    throw new RuntimeException("Not implemented yet");
		} else {
		    throw new RuntimeException("This should not be possible (all datas in DataEntity are null)");
		}
	    }
	}
	return dao;
    }

    public Element getXacml() throws XacmlSamlException {
	if (xacml == null) {
	    xacml = utils.convertSubscriptionToXACML(getDao());
	}
	return xacml;
    }

    public Element getSaml() {
	if (saml == null) {
	    // TODO ?
	    throw new RuntimeException("Not implemented yet");
	} else {
	    return saml;
	}
    }

    public Element getSamlAttributeQuery() throws XacmlSamlException {
	PipOwner dao = getDao();
	return utils.formSAMLAttributeQuery(dao.getName());
    }

    public boolean equals(Object o) {
	if (o != null && o.getClass() == this.getClass()) {
	    PipDataEntity data = (PipDataEntity) o;
	    try {
		return this.getDao().isTheSameEntity(data.getDao());
	    } catch (XacmlSamlException e) {
		return false;
	    }
	}
	return false;
    }

    public UconCategory getCategory() {
	return category;
    }

    private PipDataEntity setSubscriber(String subscriber) {
	this.subscriber = subscriber;
	return this;
    }

    public String getSubscriber() {
	return subscriber;
    }

    public void addSubscriber(String subscriber) throws XacmlSamlException {
	// create subscriber
	PipSubscriber sub = new PipSubscriber();
	sub.setSubscriber(subscriber);
	// set database link reference
	getDao().addSubscriber(sub);
    }

    /**
     * Group data by category and create a XACML string by concatenation
     * 
     * @param data
     * @param serviceContext
     * @return
     * @throws XacmlSamlException
     */
    public static Element composeXacmlSet(ServiceContext serviceContext, boolean attr_query_response, PipDataEntity... data)
	    throws XacmlSamlException {
	XacmlSamlPipUtils utils = OpenSamlPip.getInstance(serviceContext);
	LinkedList<Element> elem = new LinkedList<Element>();
	// add the subject at the top of list and the resource at the end
	// change this part if you want to include action or environment
	for (PipDataEntity d : data) {
		log.debug("{} [KMcC:)] composeXacmlSet() {}", logTag, d);
	    switch (d.category) {
	    case SUBJECT:
		elem.addFirst(d.getXacml());
		break;
	    case RESOURCE:
		elem.addLast(d.getXacml());
		break;
	    case ACTION:
	    case ENVIRONMENT:
	    default:
		break;
	    }
	}
	if (attr_query_response)
	    return utils.formPipResponse(elem);
	else
	    return utils.formUconUpdateMessage(elem);
    }

}
