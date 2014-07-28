package org.ow2.contrail.authorization.cnr.pip.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.axis2.context.ServiceContext;
import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeQuery;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.impl.AttributeBuilder;
import org.opensaml.saml2.core.impl.AttributeQueryBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.opensaml.saml2.core.impl.ResponseBuilder;
import org.opensaml.saml2.core.impl.SubjectBuilder;
import org.opensaml.ws.soap.soap11.Body;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.xacml.ctx.AttributeType;
import org.opensaml.xacml.ctx.AttributeValueType;
import org.opensaml.xacml.ctx.ResourceType;
import org.opensaml.xacml.ctx.SubjectType;
import org.opensaml.xacml.ctx.impl.AttributeTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.AttributeValueTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.ResourceTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.SubjectTypeImplBuilder;
import org.opensaml.xml.XMLObject;
import org.ow2.contrail.authorization.cnr.pip.db.PipAttribute;
import org.ow2.contrail.authorization.cnr.pip.db.PipOwner;
import org.ow2.contrail.authorization.cnr.pip.db.PipSubscriber;
import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.OpenSamlUtils;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XMLConvert;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OpenSamlPip extends OpenSamlUtils implements XacmlSamlPipUtils {

    // builders //CHECKME: THESE AREN'T THREAD SAFE!!!!!!
    private SubjectTypeImplBuilder builderXacmlSubject;
    private ResourceTypeImplBuilder builderXacmlResource;
    private AttributeQueryBuilder builderAttrQuery;
    private IssuerBuilder builderIssuer;
    private SubjectBuilder builderSubject;
    private NameIDBuilder builderNameID;
    private AttributeBuilder builderAttribute;
    private ResponseBuilder builderResponse;
    private DocumentBuilder builderDocument;

    private OpenSamlPip(ServiceContext serviceContext) throws XacmlSamlException {
	super(serviceContext);
	// instantiate builders
	builderXacmlSubject = (SubjectTypeImplBuilder) builderFactory.getBuilder(SubjectType.DEFAULT_ELEMENT_NAME);
	builderXacmlResource = (ResourceTypeImplBuilder) builderFactory.getBuilder(ResourceType.DEFAULT_ELEMENT_NAME);
	builderAttrQuery = (AttributeQueryBuilder) builderFactory.getBuilder(AttributeQuery.DEFAULT_ELEMENT_NAME);
	builderIssuer = (IssuerBuilder) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
	builderSubject = (SubjectBuilder) builderFactory.getBuilder(Subject.DEFAULT_ELEMENT_NAME);
	builderNameID = (NameIDBuilder) builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME);
	builderAttribute = (AttributeBuilder) builderFactory.getBuilder(Attribute.DEFAULT_ELEMENT_NAME);
	builderResponse = (ResponseBuilder) builderFactory.getBuilder(Response.DEFAULT_ELEMENT_NAME);
	DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	try {
	    builderDocument = docFactory.newDocumentBuilder();
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
    public static XacmlSamlPipUtils getInstance(ServiceContext serviceContext) throws XacmlSamlException {

	try {
	    // get instance from superclass OpenSamlUtils. the current class will be instantiate by reflection.
	    return (OpenSamlPip) OpenSamlUtils.getInstanceBase(serviceContext, OpenSamlPip.class);
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

    /**
     * Separate the components (XML Element) of a request (AttributeQuery / Subscription / Unsubscription)
     */
    public List<Element> getUconGenericRequests(Element request) throws XacmlSamlException {
	
	if (request.getTagName().equals(UconConstants.PIP_ATTRIBUTE_QUERY_Tag)) {
	    return separeGenericRequests(request);
	} else {
	    if (request.getTagName().equals(UconConstants.PIP_ATTRIBUTE_SUBSCRIBE_Tag)) {
		return separeGenericRequests(request);
	    } else {
		if (request.getTagName().equals(UconConstants.PIP_ATTRIBUTE_UNSUBSCRIBE_Tag)) {
		    return separeGenericRequests(request);
		} else {
		    throw new XacmlSamlException("Unable to parse unknow request");
		}
	    }
	}
    }

    /**
     * {@inheritDoc}
     * 
     * @throws XacmlSamlException
     */
    public Element formSAMLAttributeQuery(String name) throws XacmlSamlException {
	return formSAMLAttributeQuery(name, new ArrayList<String>());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws XacmlSamlException
     */
    public Element formSAMLAttributeQuery(String name, List<String> attributes) throws XacmlSamlException {

	AttributeQuery attrQuery = builderAttrQuery.buildObject();
	attrQuery.setID("AttrQuery12345789");// CHECKME: ID of QUERY?
	attrQuery.setIssueInstant(new DateTime());
	attrQuery.setVersion(SAMLVersion.VERSION_20);

	Issuer issuer = builderIssuer.buildObject();
	// issuer.setFormat(Issuer.ENTITY); //CHECKME: what's it? it seems not necessary...
	issuer.setValue("http://Attribute authority address");
	attrQuery.setIssuer(issuer);

	Subject subject = builderSubject.buildObject();
	NameID nameID = builderNameID.buildObject();
	// nameID.setFormat(NameID.UNSPECIFIED); //CHECKME: typical use but again refer oasis saml doc
	nameID.setValue(name); // ID of principal subject of assertion
	subject.setNameID(nameID); // associate name id to subject
	attrQuery.setSubject(subject); // finally set main subject for attribute query

	// Add attribute to request spec:
	// "if no attributes are specified, it indicates that all attribute allowed by policy are requested"
	for (String attributeName : attributes) {
	    Attribute samlAttr = builderAttribute.buildObject();
	    samlAttr.setName(attributeName);
	    attrQuery.getAttributes().add(samlAttr);
	}
	return XMLConvert.toDOM(attrQuery);
    }

    public Element formPipResponse(List<Element> attributes) throws XacmlSamlException {

	// root elements
	Document doc = builderDocument.newDocument();
	Element response = doc.createElement(UconConstants.PIP_ATTRIBUTE_RESPONSE_Tag);

	for (Element e : attributes) {
	    response.appendChild(e);
	}
	return response;
    }

    public Element formUconUpdateMessage(List<Element> attributes) throws XacmlSamlException {
	// root elements
	Document doc = builderDocument.newDocument();
	Element response = doc.createElement(UconConstants.PIP_ATTRIBUTE_UPDATE_Tag);

	for (Element e : attributes) {
	    response.appendChild(e);
	}
	return response;
    }
    
    @Deprecated
    public Element formUconUpdateMessage(String updateMessage) throws XacmlSamlException { // TODO!
	
	Response samlResponse; 
	Envelope env = (Envelope) unmarshalling(updateMessage);
	Body body = env.getBody();
	if (body == null || body.getUnknownXMLObjects().size() < 0)
	    throw new XacmlSamlException("Unable to get SOAP content from this message:\n" + updateMessage);
	else
	    samlResponse = (Response) body.getUnknownXMLObjects().get(0);
	SubjectTypeImplBuilder subjectBuilder = (SubjectTypeImplBuilder) builderFactory.getBuilder(SubjectType.DEFAULT_ELEMENT_NAME);
	AttributeTypeImplBuilder attributeBuilder = (AttributeTypeImplBuilder) builderFactory.getBuilder(AttributeType.DEFAULT_ELEMENT_NAME);
	AttributeValueTypeImplBuilder xacmlAttrValueBuilder = (AttributeValueTypeImplBuilder) builderFactory
		.getBuilder(AttributeValueType.DEFAULT_ELEMENT_NAME);

	SubjectType xacmlSubject = subjectBuilder.buildObject();
	for (Assertion samlAssertion : samlResponse.getAssertions()) {

	    // create xacml attribute for holder
	    AttributeType xacmlAttributeHolder = attributeBuilder.buildObject();

	    xacmlAttributeHolder.setAttributeID("HOLDER_ATTRIBUTE");
	    xacmlAttributeHolder.setIssuer(samlAssertion.getIssuer().getValue());
	    xacmlAttributeHolder.setDataType(UconConstants.XML_STRING);
	    AttributeValueType xacmlAttributeHolderValue = xacmlAttrValueBuilder.buildObject();
	    xacmlAttributeHolderValue.setValue(samlAssertion.getSubject().getDOM().getTextContent());
	    xacmlAttributeHolder.getAttributeValues().add(xacmlAttributeHolderValue);
	    xacmlSubject.getAttributes().add(xacmlAttributeHolder);
	    // add the other attribute

	    for (AttributeStatement samlAttributeStatement : samlAssertion.getAttributeStatements()) {
		List<Attribute> samlAttributeList = samlAttributeStatement.getAttributes();
		for (Attribute samlAttribute : samlAttributeList) {

		    // create xacml attribute
		    AttributeType xacmlAttribute = attributeBuilder.buildObject();
		    // set xacml attribute id
		    xacmlAttribute.setAttributeID(samlAttribute.getName());
		    // set xacml attribute datatype

		    Element elem = samlAttribute.getDOM();
		    // System.out.println("\n******[UTILS PIP] "+samlAttribute.getName()+" value: " + elem.getAttribute("DataType") + "\n");
		    if (elem.hasAttribute("DataType")) // CHECKME: is datatype the attribute name that define the attribute type?
			xacmlAttribute.setDataType(elem.getAttribute("DataType"));
		    else
			xacmlAttribute.setDataType(UconConstants.XML_STRING);

		    xacmlAttribute.setIssuer(samlAssertion.getIssuer().getValue());
		    // set xacml attribute value
		    for (XMLObject samlAttributeValue : samlAttribute.getAttributeValues()) {

			AttributeValueType xacmlAttributeValue = xacmlAttrValueBuilder.buildObject();
			xacmlAttributeValue.setValue(samlAttributeValue.getDOM().getTextContent());
			xacmlAttribute.getAttributeValues().add(xacmlAttributeValue);
		    }
		    // set xacml issuer
		    xacmlAttribute.setIssuer(samlAssertion.getIssuer().getValue());
		    // add attribute to subject
		    xacmlSubject.getAttributes().add(xacmlAttribute);
		}
	    }
	}
	return XMLConvert.toDOM(xacmlSubject);
    }

    /**
     * {@inheritDoc}
     */
    public Element convertSAMLtoXACML(Element samlResponse, UconCategory category) throws XacmlSamlException {

	// // We could do directly, but at the moment we will do SAML -> SUBSCRIPTION -> XACML
	// Response samlResponse = (Response) unmarshalling(samlStringResponse);
	// List<AttributeType> attributes = getXacmlAttributeListFromString(samlResponse);
	// String response = convertXacmlAttributeListToString(attributes, category);

	PipOwner sub = convertSAMLtoSubscription(samlResponse, category, null, false);
	Element response = convertSubscriptionToXACML(sub);
	return response;
    }

    /**
     * {@inheritDoc}
     */
    public PipOwner convertSAMLtoSubscription(Element samlDomResponse, UconCategory category, String subscriber, boolean autoupdate)
	    throws XacmlSamlException {

	Response samlResponse = (Response) XMLConvert.toXMLObject(samlDomResponse);

	// CHECKME: I get just the first assertion
	if (samlResponse.getAssertions().size() > 0) {
	    Assertion samlAssertion = samlResponse.getAssertions().get(0);
	    String owner = samlAssertion.getSubject().getNameID().getValue();
	    String issuer = samlAssertion.getIssuer().getValue();
	    PipOwner result = new PipOwner();
	    result.setName(owner);
	    result.setIssuer(issuer);
	    result.setCategory(category);
	    PipSubscriber sub = new PipSubscriber();
	    sub.setSubscriber(subscriber);
	    result.addSubscriber(sub);
	    result.setAuto(autoupdate);
	    for (AttributeStatement samlAttributeStatement : samlAssertion.getAttributeStatements()) {
		for (Attribute samlAttribute : samlAttributeStatement.getAttributes()) {
		    if (!samlAttribute.getAttributeValues().isEmpty()) {
			// CHECKME: get just the first value
			if (samlAttribute.getAttributeValues().size() > 0) {
			    PipAttribute attr = new PipAttribute();
			    attr.setXacml_id(samlAttribute.getName());
			    attr.setValue(samlAttribute.getAttributeValues().get(0).getDOM().getTextContent());
			    attr.setType(UconConstants.XML_STRING); // TODO: set the correct type from a table
			    attr.setIssuer(issuer);
			    result.getAttributes().add(attr);
			}
		    }
		}
	    }
	    return result;
	}
	return null;
    }

    /**
     * {@inheritDoc}
     */
    public Element convertSubscriptionToXACML(PipOwner subscription) throws XacmlSamlException {
	// TODO: be sure if each subscriptions in list contains the same category
	List<AttributeType> attributes = getXacmlAttributeListFromSubscription(subscription);
	Element response = convertXacmlAttributeListToXml(attributes, subscription.getCategory());
	return response;
    }

    // DIRECT SAML -> XACML (CHECKME)
    public List<AttributeType> getXacmlAttributeListFromString(Response samlResponse) throws XacmlSamlException {
	List<AttributeType> list = new LinkedList<AttributeType>();

	AttributeTypeImplBuilder attributeBuilder = (AttributeTypeImplBuilder) builderFactory.getBuilder(AttributeType.DEFAULT_ELEMENT_NAME);
	AttributeValueTypeImplBuilder xacmlAttrValueBuilder = (AttributeValueTypeImplBuilder) builderFactory
		.getBuilder(AttributeValueType.DEFAULT_ELEMENT_NAME);
	for (Assertion samlAssertion : samlResponse.getAssertions()) {

	    // create first atttribute (subject/resource name)
	    AttributeType xacmlSubject = attributeBuilder.buildObject();
	    // set xacml attribute id name
	    xacmlSubject.setAttributeID(UconConstants.HOLDER_ELEMENT);
	    // set xacml attribute value
	    AttributeValueType xacmlHolderValue = xacmlAttrValueBuilder.buildObject();
	    xacmlHolderValue.setValue(samlAssertion.getSubject().getNameID().getValue());
	    xacmlSubject.getAttributeValues().add(xacmlHolderValue);
	    // add attribute to subject
	    list.add(xacmlSubject);

	    for (AttributeStatement samlAttributeStatement : samlAssertion.getAttributeStatements()) {

		List<Attribute> samlAttributeList = samlAttributeStatement.getAttributes();

		for (Attribute samlAttribute : samlAttributeList) {

		    // create xacml attribute
		    AttributeType xacmlAttribute = attributeBuilder.buildObject();
		    // set xacml attribute id
		    xacmlAttribute.setAttributeID(samlAttribute.getName());
		    // set xacml attribute datatype

		    Element elem = samlAttribute.getDOM();
		    // System.out.println("\n******[UTILS PIP] "+samlAttribute.getName()+" value: " + elem.getAttribute("DataType") + "\n");

		    // TODO: get type from a table

		    if (elem.hasAttribute("DataType")) // CHECKME: is datatype, the attribute name that define the attribute type?
			xacmlAttribute.setDataType(elem.getAttribute("DataType"));
		    else
			xacmlAttribute.setDataType(UconConstants.XML_STRING);

		    // set xacml issuer
		    xacmlAttribute.setIssuer(samlAssertion.getIssuer().getValue());
		    // set xacml attribute value
		    for (XMLObject samlAttributeValue : samlAttribute.getAttributeValues()) {

			AttributeValueType xacmlAttributeValue = xacmlAttrValueBuilder.buildObject();
			xacmlAttributeValue.setValue(samlAttributeValue.getDOM().getTextContent());
			xacmlAttribute.getAttributeValues().add(xacmlAttributeValue);
		    }

		    // add attribute to subject
		    list.add(xacmlAttribute);
		}
	    }
	}
	return list;
    }

    // SUBSCRIPTION -> XACML
    private List<AttributeType> getXacmlAttributeListFromSubscription(PipOwner subscription) throws XacmlSamlException {
	List<AttributeType> list = new ArrayList<AttributeType>();

	// include the attributes owner
	AttributeType xacmlOwner = convertXacmlAttributeToAttributeType(subscription);
	list.add(xacmlOwner);

	for (PipAttribute attr : subscription.getAttributes()) {
	    // convert to object
	    AttributeType xacmlAttribute = convertXacmlAttributeToAttributeType(attr);
	    // add attribute to subject
	    list.add(xacmlAttribute);
	}

	return list;
    }

    // List<XACML> -> XML
    private Element convertXacmlAttributeListToXml(List<AttributeType> attributes, UconCategory category) throws XacmlSamlException {
	Element response;
	switch (category) {
	case SUBJECT:
	    SubjectType xacmlSubject = builderXacmlSubject.buildObject();

	    for (AttributeType xacmlAttribute : attributes) {
		xacmlSubject.getAttributes().add(xacmlAttribute);
	    }

	    response = XMLConvert.toDOM(xacmlSubject);
	    break;
	case RESOURCE:
	    ResourceType xacmlResource = builderXacmlResource.buildObject();

	    for (AttributeType xacmlAttribute : attributes) {
		xacmlResource.getAttributes().add(xacmlAttribute);
	    }

	    response = XMLConvert.toDOM(xacmlResource);
	    break;
	default:
	    throw new XacmlSamlException("Invalid type " + category);
	}
	return response;
    }
}
