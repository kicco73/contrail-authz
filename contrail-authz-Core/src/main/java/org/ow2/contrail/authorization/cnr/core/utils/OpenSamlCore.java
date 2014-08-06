package org.ow2.contrail.authorization.cnr.core.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.axis2.context.ServiceContext;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthzDecisionStatement;
import org.opensaml.xacml.ctx.ActionType;
import org.opensaml.xacml.ctx.AttributeType;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResourceType;
import org.opensaml.xacml.ctx.SubjectType;
import org.opensaml.xacml.ctx.impl.ActionTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.RequestTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.ResourceTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.SubjectTypeImplBuilder;
import org.opensaml.xacml.profile.saml.XACMLAuthzDecisionQueryType;
import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;
import org.ow2.contrail.authorization.cnr.core.db.UconHolder;
import org.ow2.contrail.authorization.cnr.core.db.UconSession;
import org.ow2.contrail.authorization.cnr.core.ucon.UconOptions;
import org.ow2.contrail.authorization.cnr.core.ucon.UconWs;
import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.OpenSamlUtils;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XMLConvert;
import org.ow2.contrail.authorization.cnr.utils.XacmlAttribute;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OpenSamlCore extends OpenSamlUtils implements XacmlSamlCoreUtils {
	private static Logger log = LoggerFactory.getLogger(UconWs.class);	// KMcC;)
    private DocumentBuilder builderDocument;
    // private AssertionBuilder builderAssertion;
    private RequestTypeImplBuilder builderRequest;
    private ActionTypeImplBuilder builderAction;
    private ResourceTypeImplBuilder builderResource;
    private SubjectTypeImplBuilder builderSubject;

    // private XACMLAuthzDecisionQueryTypeImplBuilder builderAuthzDecisionQuery;

    private OpenSamlCore(ServiceContext serviceContext) throws XacmlSamlException {
	super(serviceContext);
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	try {
	    builderDocument = factory.newDocumentBuilder();
	    // builderAssertion = (AssertionBuilder) builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
	    builderRequest = (RequestTypeImplBuilder) builderFactory.getBuilder(RequestType.DEFAULT_ELEMENT_NAME);
	    builderAction = (ActionTypeImplBuilder) builderFactory.getBuilder(ActionType.DEFAULT_ELEMENT_NAME);
	    builderResource = (ResourceTypeImplBuilder) builderFactory.getBuilder(ResourceType.DEFAULT_ELEMENT_NAME);
	    builderSubject = (SubjectTypeImplBuilder) builderFactory.getBuilder(SubjectType.DEFAULT_ELEMENT_NAME);
	    // builderAuthzDecisionQuery = (XACMLAuthzDecisionQueryTypeImplBuilder) builderFactory
	    // .getBuilder(XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_NAME_XACML20);
	} catch (ParserConfigurationException e) {
	    throw new XacmlSamlException(e);
	}

    }

    public static void init(ServiceContext serviceContext) throws XacmlSamlException {
    	getInstance(serviceContext);
    }

    /**
     * Constructor by singleton pattern (adapted for web service)
     * 
     * @param serviceContext
     *            The service context in which save/get the singleton instance
     * @return
     * @throws XacmlSamlException
     * @throws NullPointerException
     */
    public static XacmlSamlCoreUtils getInstance(ServiceContext serviceContext) throws XacmlSamlException {

	try {
	    // get instance from superclass OpenSamlUtils. the current class will be instantiate by reflection.
		log.debug("{} ServiceContext: "+serviceContext, "[KMcC;]");
	    return (OpenSamlCore) OpenSamlUtils.getInstanceBase(serviceContext, OpenSamlCore.class);
	} catch (NoSuchMethodException e) {
	    throw new RuntimeException("This should not be possible", e);
	} catch (InstantiationException e) {
	    throw new RuntimeException("This should not be possible", e);
	} catch (IllegalArgumentException e) {
	    throw new RuntimeException("This should not be possible", e);
	} catch (InvocationTargetException e) {
	    throw new RuntimeException("This should not be possible", e);
	} catch (IllegalAccessException e) {
	    e.printStackTrace();
	    throw new RuntimeException("Reflection security problem", e);
	}
    }

    public UconSession getUconSessionFromXacmlRetrieve(Element retrieve) throws XacmlSamlException {

	Assertion assertion = (Assertion) XMLConvert.toXMLObject(retrieve);
	UconSession session = new UconSession();
	session.setSession_id_string(assertion.getID());
	// CHECKME
	// Other attributes (like reply to address)
	for (AuthzDecisionStatement stat : assertion.getAuthzDecisionStatements()) {
	    if (!stat.getActions().isEmpty()) {
		if (stat.getActions().get(0).getAction().equals(UconConstants.UCON_STARTACCESS_REPLYTO_ACTION_NAME)) {
		    session.setReplyTo(stat.getResource());
		}
	    }
	}
	return session;
    }

    @Override
    public Element formAttributeQueryRequest(UconSession session) {
	// root elements
	Document doc = builderDocument.newDocument();
	Element request = doc.createElement(UconConstants.PIP_ATTRIBUTE_QUERY_Tag);

	// for each holder except from action and environment
	for (UconHolder holder : session.getHolders()) {
	    if (holder.getCategory() != UconCategory.ACTION && holder.getCategory() != UconCategory.ENVIRONMENT) {
		// create element
		Element holderElement = doc.createElement(holder.getCategory().getTagFromCategory());
		holderElement.setTextContent(holder.getValue());
		request.appendChild(holderElement);
	    }
	}

	return request;
    }

    @Override
    public Element formSubscriptionRequest(UconSession session) {
	// root elements
	Document doc = builderDocument.newDocument();
	Element request = doc.createElement(UconConstants.PIP_ATTRIBUTE_SUBSCRIBE_Tag);

	for (UconHolder holder : session.getHolders()) {
	    // add to request only if is not subscribed
	    if (!holder.isSubscribed()) {
		if (holder.getCategory() != UconCategory.ACTION || holder.getCategory() != UconCategory.ENVIRONMENT) {
		    // create element
		    Element holderElement = doc.createElement(holder.getCategory().getTagFromCategory());
		    holderElement.setTextContent(holder.getXacml_id());
		    request.appendChild(holderElement);
		}
	    }
	}

	return request;
    }

    public Element formUnubscriptionRequest(UconSession session) {
	// root elements
	Document doc = builderDocument.newDocument();
	Element request = doc.createElement(UconConstants.PIP_ATTRIBUTE_UNSUBSCRIBE_Tag);

	for (UconHolder holder : session.getHolders()) {
	    // add to request only if is not subscribed
	    if (holder.isSubscribed()) { // CHECKME
		if (holder.getCategory() != UconCategory.ACTION || holder.getCategory() != UconCategory.ENVIRONMENT) {
		    // create element
		    Element holderElement = doc.createElement(holder.getCategory().getTagFromCategory());
		    holderElement.setTextContent(holder.getXacml_id());
		    request.appendChild(holderElement);
		}
	    }
	}

	return request;
    }

    public String formXacmlPDPRequest(UconSession session) throws XacmlSamlException {
	RequestType request = builderRequest.buildObject();
	ActionType action = builderAction.buildObject();
	ResourceType resource = builderResource.buildObject();
	SubjectType subject = builderSubject.buildObject();

	for (UconHolder holder : session.getHolders()) {
	    // get holder attributes
	    List<AttributeType> attributes = new LinkedList<AttributeType>();
	    for (XacmlAttribute attr : holder.getAttributes()) {
		attributes.add(convertXacmlAttributeToAttributeType(attr));
	    }
	    switch (holder.getCategory()) {
	    case ACTION:
		// set the first action (holder)
		action.getAttributes().add(convertXacmlAttributeToAttributeType(holder));
		// set other actions
		action.getAttributes().addAll(attributes);
		break;
	    case RESOURCE:
		// set the first resource (holder)
		resource.getAttributes().add(convertXacmlAttributeToAttributeType(holder));
		// set other resource
		resource.getAttributes().addAll(attributes);
		break;
	    case SUBJECT:
		// set the first subject (holder)
		subject.getAttributes().add(convertXacmlAttributeToAttributeType(holder));
		// set other subject
		subject.getAttributes().addAll(attributes);
		break;
	    case ENVIRONMENT:
		break;
	    default:
		break;

	    }
	}
	// set the holders (and their attributes) in request
	request.setAction(action);
	request.getResources().add(resource);
	request.getSubjects().add(subject);

	return XMLConvert.toString(request);
    }

    public UconSession getUconSessionFromSamlXacmlRequest(Element samlXacml) throws XacmlSamlException {

	XACMLAuthzDecisionQueryType authz = (XACMLAuthzDecisionQueryType) XMLConvert.toXMLObject(samlXacml);

	// create UconSession object
	UconSession session = new UconSession();
	// set session id
	session.setSession_id_string(authz.getID());
	// set status id
	session.setStatus(CorePhase.PRE);
	// get request
	RequestType request = authz.getRequest();

	// get (from UconOptions) the xacml id that identify the holder
	UconOptions opts = UconOptions.getInstance(serviceContext);
	String sub_uuid = opts.getSub_uuid();
	String res_uuid = opts.getRes_uuid();
	String act_uuid = opts.getAct_uuid();

	// get the subject attributes
	UconHolder subjectHolder = new UconHolder();
	subjectHolder.setCategory(UconCategory.SUBJECT);
	for (SubjectType sub : request.getSubjects()) {
	    for (AttributeType attr : sub.getAttributes()) {
		if (sub_uuid.equals(attr.getAttributeID())) {
		    // this is the holder
		    convertAttributeTypeToXacmlAttribute(subjectHolder, attr);
		} else {
		    // other attributes
		    UconAttribute ucon_attr = new UconAttribute();
		    convertAttributeTypeToXacmlAttribute(ucon_attr, attr);
		    subjectHolder.addAttribute(ucon_attr);
		}
	    }
	}
	subjectHolder.addSession(session);

	// get the resource attributes
	UconHolder resourceHolder = new UconHolder();
	resourceHolder.setCategory(UconCategory.RESOURCE);

	for (ResourceType res : request.getResources()) {
	    for (AttributeType attr : res.getAttributes()) {
		if (res_uuid.equals(attr.getAttributeID())) {
		    // this is the holder
		    convertAttributeTypeToXacmlAttribute(resourceHolder, attr);
		} else {
		    // other attributes
		    UconAttribute ucon_attr = new UconAttribute();
		    convertAttributeTypeToXacmlAttribute(ucon_attr, attr);
		    resourceHolder.addAttribute(ucon_attr);
		}
	    }
	}
	resourceHolder.addSession(session);

	// get the action attributes
	UconHolder actionHolder = new UconHolder();
	actionHolder.setCategory(UconCategory.ACTION);
	ActionType act = request.getAction();
	for (AttributeType attr : act.getAttributes()) {
	    if (act_uuid.equals(attr.getAttributeID())) {
		// this is the holder
		convertAttributeTypeToXacmlAttribute(actionHolder, attr);
	    } else {
		// other attributes
		UconAttribute ucon_attr = new UconAttribute();
		convertAttributeTypeToXacmlAttribute(ucon_attr, attr);
		actionHolder.addAttribute(ucon_attr);
	    }
	}
	actionHolder.addSession(session);

	return session;
    }

    @Override
    public UconSession getAttributeFromPipResponse(Element attributesXml) throws XacmlSamlException {
	UconSession attr = new UconSession();
	
	//List<Element> elements = separeGenericRequests(attributesXml);
	List<Element> elements = separeGenericRequests((Element) attributesXml.getElementsByTagName("PipResponse").item(0));
	for (Element elem : elements) {
		log.info("[OPENSAMLCORE] [KMcC;)] getAttributeFromPipResponse(): {}", elem.getLocalName());
	    // identify category
	    String cat = elem.getLocalName().toUpperCase(); // [KMcC;)] getTagName() returns qualified name! this was a bug
	    UconCategory category = UconCategory.valueOf(cat);

	    switch (category) {
	    case RESOURCE:
		// convert XML -> XACML
		ResourceType xacmlResource = (ResourceType) XMLConvert.toXMLObject(elem);
		// get the attribute list
		List<AttributeType> resAttrList = xacmlResource.getAttributes();
		// the first resource is the holder
		UconHolder resourceHolder = new UconHolder();
		super.convertAttributeTypeToXacmlAttribute(resourceHolder, resAttrList.get(0));
		// get the other attribute
		for (int i = 1; i < resAttrList.size(); i++) {
		    UconAttribute resourceAttribute = new UconAttribute();
		    super.convertAttributeTypeToXacmlAttribute(resourceAttribute, resAttrList.get(i));
		    resourceHolder.addAttribute(resourceAttribute);
		}
		resourceHolder.addSession(attr);
		break;
	    case SUBJECT:
		// convert XML -> XACML
		SubjectType xacmlSubject = (SubjectType) XMLConvert.toXMLObject(elem);
		// get the attribute list
		List<AttributeType> subAttrList = xacmlSubject.getAttributes();
		// the first subject is the holder
		UconHolder subjectHolder = new UconHolder();
		super.convertAttributeTypeToXacmlAttribute(subjectHolder, subAttrList.get(0));
		// get the other attribute
		for (int i = 1; i < subAttrList.size(); i++) {
		    UconAttribute subjectAttribute = new UconAttribute();
		    super.convertAttributeTypeToXacmlAttribute(subjectAttribute, subAttrList.get(i));
		    subjectHolder.addAttribute(subjectAttribute);
		}
		subjectHolder.addSession(attr);
		break;
	    default:
		break;
	    }
	}
	log.info("[OPENSAMLCORE] [KMcC;)] getAttributeFromPipResponse(): attr {}", attr.toString());

	return attr;
    }

    @Override
    // TODO One UconSession could contain all the holders
    public List<UconSession> getHolderUpdate(Element updates) throws XacmlSamlException {
	List<UconSession> updatedAttribute = new LinkedList<UconSession>();
	List<Element> elements = separeGenericRequests(updates);

	for (Element elem : elements) {
	    // identify category
	    UconCategory category = UconCategory.getCategoryFromTag(elem.getTagName());

	    UconSession attr = new UconSession();

	    switch (category) {
	    case RESOURCE:
		// convert XML -> XACML
		ResourceType xacmlResource = (ResourceType) XMLConvert.toXMLObject(elem);
		// get the attribute list
		List<AttributeType> resAttrList = xacmlResource.getAttributes();
		// the first resource is the holder //TODO: use uuid
		UconHolder resourceHolder = new UconHolder();
		super.convertAttributeTypeToXacmlAttribute(resourceHolder, resAttrList.get(0));
		// get the other attribute
		for (int i = 1; i < resAttrList.size(); i++) {
		    UconAttribute resourceAttribute = new UconAttribute();
		    super.convertAttributeTypeToXacmlAttribute(resourceAttribute, resAttrList.get(i));
		    resourceHolder.addAttribute(resourceAttribute);
		}
		resourceHolder.addSession(attr);
		break;
	    case SUBJECT:
		// convert XML -> XACML
		SubjectType xacmlSubject = (SubjectType) XMLConvert.toXMLObject(elem);
		// get the attribute list
		List<AttributeType> subAttrList = xacmlSubject.getAttributes();
		// the first subject is the holder //TODO: use uuid
		UconHolder subjectHolder = new UconHolder();
		super.convertAttributeTypeToXacmlAttribute(subjectHolder, subAttrList.get(0));
		// get the other attribute
		for (int i = 1; i < subAttrList.size(); i++) {
		    UconAttribute subjectAttribute = new UconAttribute();
		    super.convertAttributeTypeToXacmlAttribute(subjectAttribute, subAttrList.get(i));
		    subjectHolder.addAttribute(subjectAttribute);
		}
		subjectHolder.addSession(attr);
		break;
	    default:
		break;
	    }
	    updatedAttribute.add(attr);
	}
	return updatedAttribute;
    }
}
