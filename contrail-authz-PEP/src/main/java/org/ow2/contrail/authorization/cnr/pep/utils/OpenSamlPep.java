package org.ow2.contrail.authorization.cnr.pep.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.WebServiceException;

import org.joda.time.DateTime;
import org.opensaml.saml2.core.Action;
import org.opensaml.saml2.core.impl.ActionBuilder;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AuthzDecisionStatement;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml2.core.impl.AuthzDecisionStatementBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.ResponseBuilder;
import org.opensaml.xacml.ctx.ActionType;
import org.opensaml.xacml.ctx.AttributeType;
import org.opensaml.xacml.ctx.EnvironmentType;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResourceType;
import org.opensaml.xacml.ctx.SubjectType;
import org.opensaml.xacml.ctx.impl.ActionTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.EnvironmentTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.RequestTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.ResourceTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.SubjectTypeImplBuilder;
import org.opensaml.xacml.profile.saml.SAMLProfileConstants;
import org.opensaml.xacml.profile.saml.XACMLAuthzDecisionQueryType;
import org.opensaml.xacml.profile.saml.impl.XACMLAuthzDecisionQueryTypeImplBuilder;
import org.opensaml.xml.io.MarshallingException;
import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.XMLConvert;
import org.ow2.contrail.authorization.cnr.utils.OpenSamlUtils;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class OpenSamlPep extends OpenSamlUtils implements XacmlSamlPepUtils {
	private static Logger log = LoggerFactory.getLogger(OpenSamlPep.class);	// KMcC;)
	private static String logTag = "[OpenSamlPep]";

    private OpenSamlPep() throws XacmlSamlException {
	super(null);
	xacml3AuthzDecisionQueryBuilder = (XACMLAuthzDecisionQueryTypeImplBuilder) builderFactory
		.getBuilder(XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_NAME_XACML30);
	xacml2AuthzDecisionQueryBuilder = (XACMLAuthzDecisionQueryTypeImplBuilder) builderFactory
		.getBuilder(XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_NAME_XACML20);
	requestBuilder = (RequestTypeImplBuilder) builderFactory.getBuilder(RequestType.DEFAULT_ELEMENT_NAME);
	actionBuilder = (ActionTypeImplBuilder) builderFactory.getBuilder(ActionType.DEFAULT_ELEMENT_NAME);
	environmentBuilder = (EnvironmentTypeImplBuilder) builderFactory.getBuilder(EnvironmentType.DEFAULT_ELEMENT_NAME);
	resourceBuilder = (ResourceTypeImplBuilder) builderFactory.getBuilder(ResourceType.DEFAULT_ELEMENT_NAME);
	subjectBuilder = (SubjectTypeImplBuilder) builderFactory.getBuilder(SubjectType.DEFAULT_ELEMENT_NAME);
	builderAuthzDecisionStatement = (AuthzDecisionStatementBuilder) builderFactory.getBuilder(AuthzDecisionStatement.DEFAULT_ELEMENT_NAME);
	builderAuthzStatementAction = (ActionBuilder) builderFactory.getBuilder(Action.DEFAULT_ELEMENT_NAME);
	builderAssertion = (AssertionBuilder) builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
	builderResponse = (ResponseBuilder) builderFactory.getBuilder(Response.DEFAULT_ELEMENT_NAME);
    }

    protected static OpenSamlPep instance;
    private XACMLAuthzDecisionQueryTypeImplBuilder xacml3AuthzDecisionQueryBuilder;
    private XACMLAuthzDecisionQueryTypeImplBuilder xacml2AuthzDecisionQueryBuilder;
    private RequestTypeImplBuilder requestBuilder;
    private ActionTypeImplBuilder actionBuilder;
    private EnvironmentTypeImplBuilder environmentBuilder;
    private ResourceTypeImplBuilder resourceBuilder;
    private SubjectTypeImplBuilder subjectBuilder;
    private AuthzDecisionStatementBuilder builderAuthzDecisionStatement;
    private ActionBuilder builderAuthzStatementAction;
    private AssertionBuilder builderAssertion;
    private ResponseBuilder builderResponse;

    public static OpenSamlPep getInstance() throws XacmlSamlException {
	if (instance == null) {
	    instance = new OpenSamlPep();
	}
	return instance;
    }

    // unused //CHECKME
    public String formXACML30AuthzDecisionQuery(List<PepRequestAttribute> attributes, String session_id) throws XacmlSamlException {
	
	XACMLAuthzDecisionQueryType authz = xacml3AuthzDecisionQueryBuilder.buildObject(SAMLProfileConstants.SAML20XACML30P_NS,
		XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_LOCAL_NAME, SAMLProfileConstants.SAML20XACMLPROTOCOL_PREFIX);

	authz.setID(session_id);
	authz.setDestination("localhost"); // FIXME
	IssuerBuilder issuerBuilder = (IssuerBuilder) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
	Issuer issuer = issuerBuilder.buildObject();
	issuer.setValue("anonymous"); // FIXME
	authz.setIssuer(issuer);
	authz.setVersion(org.opensaml.common.SAMLVersion.VERSION_20);
	// authz.setRequest(request);
	authz.setIssueInstant(new DateTime(System.currentTimeMillis()));
	String result = XMLConvert.toString(authz);

	String xacmlRequest = formRequest30(attributes);

	result = result.replace("</saml2:Issuer>", "</saml2:Issuer>" + xacmlRequest);

	return result;
    }

    /**
     * Form the Xacml 3.0 Request from a list of attribute
     * 
     * @param attributes
     * @return
     */
    private String formRequest30(List<PepRequestAttribute> attributes) {
	String request = "<Request xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\" CombinedDecision=\"false\" ReturnPolicyIdList=\"false\">\n";

	String actions = "<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\">\n";
	String environments = "<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:environment\">\n";
	String subjects = "<Attributes Category=\"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject\">\n";
	String resources = "<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\">\n";

	for (PepRequestAttribute pepAttr : attributes) {

	    String attribute = "<Attribute IncludeInResult=\"false\"\n" + "AttributeId=\"" + pepAttr.getXacml_id() + "\"\n" + "Issuer=\""
		    + pepAttr.getIssuer() + "\">\n" + "<AttributeValue DataType=\"" + pepAttr.getType() + "\">\n" + pepAttr.getValue()
		    + "\n</AttributeValue>\n</Attribute>";

	    switch (pepAttr.getCategory()) {
	    case ACTION:
		actions += attribute;
		break;
	    case ENVIRONMENT:
		environments += attribute;
		break;
	    case RESOURCE:
		resources += attribute;
		break;
	    case SUBJECT:
		subjects += attribute;
		break;
	    }
	}

	String closetag = "</Attributes>";
	request = request + subjects + closetag + resources + closetag + actions + closetag + environments + closetag + "</Request>\n";

	return request;
    }

    /**
     * {@inheritDoc}
     */
    public Element formTryaccessMessage(List<PepRequestAttribute> attributes, String session_id) throws XacmlSamlException {
	
	// compose XACML request
	RequestType request = formRequestCtx(attributes);
	// compose SAML wrapper
	XACMLAuthzDecisionQueryType authz = xacml2AuthzDecisionQueryBuilder.buildObject(SAMLProfileConstants.SAML20XACML20P_NS,
		XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_LOCAL_NAME, SAMLProfileConstants.SAML20XACMLPROTOCOL_PREFIX);

	authz.setID(session_id);
	authz.setDestination("localhost"); // FIXME
	IssuerBuilder issuerBuilder = (IssuerBuilder) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
	Issuer issuer = issuerBuilder.buildObject();
	issuer.setValue("anonymous"); // FIXME
	authz.setIssuer(issuer);
	authz.setVersion(org.opensaml.common.SAMLVersion.VERSION_20);
	authz.setRequest(request);
	authz.setIssueInstant(new DateTime());
	
	return XMLConvert.toDOM(authz);
    }

    /**
     * Form the Request from a list of attribute
     * 
     * @param attributes
     * @return
     */
    private RequestType formRequestCtx(List<PepRequestAttribute> attributes) {
	
	RequestType request = requestBuilder.buildObject();
	
	for (PepRequestAttribute pepAttr : attributes) {
	    AttributeType xacmlAttribute = convertXacmlAttributeToAttributeType(pepAttr);
	    switch (pepAttr.getCategory()) {
	    case ACTION:
		ActionType action = actionBuilder.buildObject();
		action.getAttributes().add(xacmlAttribute);
		request.setAction(action);
		break;
	    case ENVIRONMENT:
		EnvironmentType environment = environmentBuilder.buildObject();
		environment.getAttributes().add(xacmlAttribute);
		request.setEnvironment(environment);
		break;
	    case RESOURCE:
		ResourceType resource = resourceBuilder.buildObject();
		resource.getAttributes().add(xacmlAttribute);
		request.getResources().add(resource);
		break;
	    case SUBJECT:
		SubjectType subject = subjectBuilder.buildObject();
		subject.getAttributes().add(xacmlAttribute);
		request.getSubjects().add(subject);
		break;
	    }
	}
	return request;
    }

    /**
     * Form the start access message
     * 
     * @param id
     * @param replyTo
     * @return
     * @throws MarshallingException
     */
    public Element formStartaccessMessage(String id, URL replyTo) throws XacmlSamlException {
	Assertion assertion = builderAssertion.buildObject();
	// set ID
	assertion.setID(id);
	if(replyTo != null) {
	    // set reply_to address as AuthzDecisionStatement action 
	    AuthzDecisionStatement stat = builderAuthzDecisionStatement.buildObject();
	    Action action = builderAuthzStatementAction.buildObject();
	    // name of action
	    action.setAction(UconConstants.UCON_STARTACCESS_REPLYTO_ACTION_NAME);
	    stat.getActions().add(action);
	    assertion.getAuthzDecisionStatements().add(stat);
	}
	return assertion.getDOM();
    }

    public String getStartaccessResponse(Element response) {
	// take the message content
	String content = response.getTextContent();
	// try to recognize an error message first
	if (response.getTagName().equals(UconConstants.UCON_ERROR_Tag)) {
	    content =  response.getTextContent();
	    // if I don't know the error message, set a generic error message
	    if (!(content.equals(UconConstants.SESSION_ALREADY_STARTED) || content.equals(UconConstants.INPUT_MESSAGE_ERROR))) {
		content = UconConstants.GENERIC_ERROR;
	    }
	}
	if (!response.getTagName().equals(UconConstants.UCON_RESPONSE_Tag)) {
	    // CHECKME: May I throw a protocol exception?
	    content = "ERROR (start access response):\ntag name: "+response.getTagName()+ "\nmessage: "+response.getTextContent();
	}
	return content;
    }
    
    /**
     * Form the end access message
     * 
     * @param id
     * @return
     * @throws MarshallingException
     */
    public Element formEndaccessMessage(String id) throws XacmlSamlException {
	Assertion assertion = builderAssertion.buildObject();
	assertion.setID(id);
	return assertion.getDOM();
    }
    
    public String getEndaccessResponse(Element response) {
	// take the message content
	String content = response.getTextContent();
	// try to recognize an error message first
	if (response.getTagName().equals(UconConstants.UCON_ERROR_Tag)) {
	    content =  response.getTextContent();
	    // if I don't know the error message, set a generic error message
	    if (!(content.equals(UconConstants.SESSION_ALREADY_STOPPED_REVOKED) || content.equals(UconConstants.INPUT_MESSAGE_ERROR))) {
		content = UconConstants.GENERIC_ERROR;
	    }
	}
	if (!response.getTagName().equals(UconConstants.UCON_RESPONSE_Tag)) {
	    // CHECKME: May I throw a protocol exception?
	    content = "ERROR (end access response):\ntag name: "+response.getTagName()+ "\nmessage: "+response.getTextContent();
	}
	return content;
    }

    public String getSessionIdFromTryaccessResponse(Element response) throws XacmlSamlException {
	String sessionId = UconConstants.NO_SESSION_ID;

	// try to recognize an error message first
	if (response.getTagName().equals(UconConstants.UCON_ERROR_Tag)) {
	    // get the error message
	    String errorMessage = response.getTextContent();
	    if (errorMessage.equals(UconConstants.GENERIC_ERROR)) {
		throw new WebServiceException("Ucon Service answer: " + UconConstants.GENERIC_ERROR);
	    }
	    if (errorMessage.equals(UconConstants.ID_INVALID_ERROR)) {
		// why should it happen?
		throw new IllegalArgumentException("Ucon Service answer: " + UconConstants.ID_INVALID_ERROR);
	    }
	    if (errorMessage.equals(UconConstants.INPUT_MESSAGE_ERROR)) {
		throw new IllegalArgumentException("Ucon Service answer: " + UconConstants.INPUT_MESSAGE_ERROR);
	    }
	}

	log.debug("{} [KMcC;)] getSessionIdFromTryaccessResponse(): got {}", logTag, response);
	Response samlResponse = (Response) XMLConvert.FIXMEtoXMLObject(response);
	try {
	    sessionId = samlResponse.getAssertions().get(0).getID();
	} catch (IndexOutOfBoundsException e) {
	    throw new XacmlSamlException("Unabel to take id from the following response:\n" + response);
	}
	// if the access is denied, session id is equals to UconConstants.NO_SESSION_ID
	return sessionId;
    }

    /**
     * {@inheritDoc}
     */
    // WHO NEED THIS? (actually it isn't used in UCON, but in ISTI pep test)
    public List<PepRequestAttribute> getPepAttributeFromXacmlRequest(String xacmlRequestString) throws XacmlSamlException {
	RequestType request = (RequestType) XMLConvert.toXMLObject(xacmlRequestString);

	List<PepRequestAttribute> list = new ArrayList<PepRequestAttribute>();
	for (SubjectType subject : request.getSubjects()) {
	    for (AttributeType attr : subject.getAttributes())
		list.add(new PepRequestAttribute(attr.getAttributeID(), attr.getDataType(), attr.getAttributeValues().get(0).getValue(), "issuer",
			UconCategory.SUBJECT));
	}
	for (ResourceType resource : request.getResources()) {
	    for (AttributeType attr : resource.getAttributes())
		list.add(new PepRequestAttribute(attr.getAttributeID(), attr.getDataType(), attr.getAttributeValues().get(0).getValue(), "issuer",
			UconCategory.RESOURCE));
	}
	for (AttributeType attr : request.getAction().getAttributes()) {
	    list.add(new PepRequestAttribute(attr.getAttributeID(), attr.getDataType(), attr.getAttributeValues().get(0).getValue(), "issuer",
		    UconCategory.ACTION));
	}
	for (AttributeType attr : request.getEnvironment().getAttributes()) {
	    list.add(new PepRequestAttribute(attr.getAttributeID(), attr.getDataType(), attr.getAttributeValues().get(0).getValue(), "issuer",
		    UconCategory.ENVIRONMENT));
	}
	return list;
    }

    /**
     * {@inheritDoc}
     */
    public List<PepRequestAttribute> getPepAttributeFromSamlAssertion(String samlAssertionString, UconCategory category) throws XacmlSamlException {

	List<PepRequestAttribute> list = new ArrayList<PepRequestAttribute>();
	Assertion samlAss = (Assertion) XMLConvert.toXMLObject(samlAssertionString);
	// String holder = samlAss.getSubject().getDOM().getTextContent();
	String issuer = "issuer";
	if(samlAss.getIssuer() != null) {
	    issuer = samlAss.getIssuer().getValue(); // CHECKME! in certificate is null
	}
	PepRequestAttribute pepAttrSubject = new PepRequestAttribute(samlAss.getSubject().getNameID().getFormat(), UconConstants.XML_STRING, samlAss
		.getSubject().getDOM().getTextContent(), issuer, category);
	list.add(pepAttrSubject);
	for (AttributeStatement attrStat : samlAss.getAttributeStatements()) {
	    for (Attribute samlAttribute : attrStat.getAttributes()) {
		String xacmlid = samlAttribute.getName();
		String dataType = "";
		Element elem = samlAttribute.getDOM();
		if (elem.hasAttribute("DataType")) // CHECKME: is datatype the attribute name that define the attribute type?
		    dataType = elem.getAttribute("DataType");
		else
		    dataType = UconConstants.XML_STRING;
		String value = "";
		try {
		    value = samlAttribute.getAttributeValues().get(0).getDOM().getTextContent();
		} catch (IndexOutOfBoundsException e) {
		    throw new XacmlSamlException("Unable to get value for the attribute:\n" + XMLConvert.toString(samlAttribute));
		}
		PepRequestAttribute pepAttr = new PepRequestAttribute(xacmlid, dataType, value, issuer, category);
		list.add(pepAttr);
	    }
	}
	return list;
    }

    /**
     * {@inheritDoc}
     */
    public String formMapIdMessage(String id, String ovf_id) {
	return null;
    }
}
