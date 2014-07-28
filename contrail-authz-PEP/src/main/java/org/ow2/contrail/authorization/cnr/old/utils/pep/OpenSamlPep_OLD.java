package org.ow2.contrail.authorization.cnr.old.utils.pep;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
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
import org.ow2.contrail.authorization.cnr.utils.OpenSamlUtils;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.w3c.dom.Element;

public class OpenSamlPep_OLD extends OpenSamlUtils implements XacmlSamlPepUtils_OLD {

    private OpenSamlPep_OLD() throws XacmlSamlException {
	super(null);
    }

    protected static OpenSamlPep_OLD instance;

    public static OpenSamlPep_OLD getInstance() throws XacmlSamlException {
	if (instance == null) {
	    instance = new OpenSamlPep_OLD();
	}
	return instance;
    }

    // unused
    public String formXACML30AuthzDecisionQuery(List<PepRequestAttribute_OLD> attributes, String session_id) throws XacmlSamlException {
	XACMLAuthzDecisionQueryTypeImplBuilder authzBuilder = (XACMLAuthzDecisionQueryTypeImplBuilder) builderFactory
		.getBuilder(XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_NAME_XACML30);
	XACMLAuthzDecisionQueryType authz = authzBuilder.buildObject(SAMLProfileConstants.SAML20XACML30P_NS,
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
	String result = marshalling(authz);

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
    private String formRequest30(List<PepRequestAttribute_OLD> attributes) {
	String request = "<Request xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\" CombinedDecision=\"false\" ReturnPolicyIdList=\"false\">\n";

	String actions = "<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\">\n";
	String environments = "<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:environment\">\n";
	String subjects = "<Attributes Category=\"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject\">\n";
	String resources = "<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\">\n";

	for (PepRequestAttribute_OLD pepAttr : attributes) {

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
    public String formXACMLAuthzDecisionQuery(List<PepRequestAttribute_OLD> attributes, String session_id) throws XacmlSamlException {
	RequestType request = formRequestCtx(attributes);

	XACMLAuthzDecisionQueryTypeImplBuilder authzBuilder = (XACMLAuthzDecisionQueryTypeImplBuilder) builderFactory
		.getBuilder(XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_NAME_XACML20);
	XACMLAuthzDecisionQueryType authz = authzBuilder.buildObject(SAMLProfileConstants.SAML20XACML20P_NS,
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
	String result = marshalling(authz);
	return result;
    }

    /**
     * Form the Request from a list of attribute
     * 
     * @param attributes
     * @return
     */
    private RequestType formRequestCtx(List<PepRequestAttribute_OLD> attributes) {
	RequestTypeImplBuilder requestBuilder = (RequestTypeImplBuilder) builderFactory.getBuilder(RequestType.DEFAULT_ELEMENT_NAME);
	RequestType request = requestBuilder.buildObject();
	ActionTypeImplBuilder actionBuilder = (ActionTypeImplBuilder) builderFactory.getBuilder(ActionType.DEFAULT_ELEMENT_NAME);
	EnvironmentTypeImplBuilder environmentBuilder = (EnvironmentTypeImplBuilder) builderFactory.getBuilder(EnvironmentType.DEFAULT_ELEMENT_NAME);
	ResourceTypeImplBuilder resourceBuilder = (ResourceTypeImplBuilder) builderFactory.getBuilder(ResourceType.DEFAULT_ELEMENT_NAME);
	SubjectTypeImplBuilder subjectBuilder = (SubjectTypeImplBuilder) builderFactory.getBuilder(SubjectType.DEFAULT_ELEMENT_NAME);

	for (PepRequestAttribute_OLD pepAttr : attributes) {
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
     * Extract from a saml response a xacml response
     * 
     * @param samlResponse
     * @return
     */
    // I need id only
    // private ResponseType getResponseFromSAML(Response samlResponse) {
    // Assertion assertion = samlResponse.getAssertions().get(0); // should be one assertion
    // XACMLAuthzDecisionStatementType xacmlAuthz = (XACMLAuthzDecisionStatementType) assertion.getStatements().get(0); // should be one
    // // statement
    // return xacmlAuthz.getResponse();
    // }

    /**
     * Form the start access message
     * 
     * @param id
     * @return
     * @throws MarshallingException
     */
    public String formStartMessage(String id) throws XacmlSamlException {
	AssertionBuilder builderAssertion = (AssertionBuilder) builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
	Assertion assertion = builderAssertion.buildObject();
	assertion.setID(id);
	String message = marshalling(assertion);
	return message;
    }

    /**
     * Form the end access message
     * 
     * @param id
     * @return
     * @throws MarshallingException
     */
    public String formEndMessage(String id) throws XacmlSamlException {
	AssertionBuilder builderAssertion = (AssertionBuilder) builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
	Assertion assertion = builderAssertion.buildObject();
	assertion.setID(id);
	String message = marshalling(assertion);
	return message;
    }

    public String getSessionIdFromResponse(String response) throws XacmlSamlException {
	String sessionId = UconConstants.NO_SESSION_ID;
	Response samlResponse = (Response) unmarshalling(response);
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
    public List<PepRequestAttribute_OLD> getPepAttributeFromXacmlRequest(String xacmlRequestString) throws XacmlSamlException {
	RequestType request = (RequestType) unmarshalling(xacmlRequestString);

	List<PepRequestAttribute_OLD> list = new ArrayList<PepRequestAttribute_OLD>();
	for (SubjectType subject : request.getSubjects()) {
	    for (AttributeType attr : subject.getAttributes())
		list.add(new PepRequestAttribute_OLD(attr.getAttributeID(), attr.getDataType(), attr.getAttributeValues().get(0).getValue(), "issuer",
			UconCategory.SUBJECT));
	}
	for (ResourceType resource : request.getResources()) {
	    for (AttributeType attr : resource.getAttributes())
		list.add(new PepRequestAttribute_OLD(attr.getAttributeID(), attr.getDataType(), attr.getAttributeValues().get(0).getValue(), "issuer",
			UconCategory.RESOURCE));
	}
	for (AttributeType attr : request.getAction().getAttributes()) {
	    list.add(new PepRequestAttribute_OLD(attr.getAttributeID(), attr.getDataType(), attr.getAttributeValues().get(0).getValue(), "issuer",
		    UconCategory.ACTION));
	}
	for (AttributeType attr : request.getEnvironment().getAttributes()) {
	    list.add(new PepRequestAttribute_OLD(attr.getAttributeID(), attr.getDataType(), attr.getAttributeValues().get(0).getValue(), "issuer",
		    UconCategory.ENVIRONMENT));
	}
	return list;
    }

    /**
     * {@inheritDoc}
     */
    public List<PepRequestAttribute_OLD> getPepAttributeFromSamlAssertion(String samlAssertionString) throws XacmlSamlException {

	List<PepRequestAttribute_OLD> list = new ArrayList<PepRequestAttribute_OLD>();
	Assertion samlAss = (Assertion) unmarshalling(samlAssertionString);
	// String holder = samlAss.getSubject().getDOM().getTextContent();
	PepRequestAttribute_OLD pepAttrSubject = new PepRequestAttribute_OLD(samlAss.getSubject().getNameID().getFormat(), UconConstants.XML_STRING, samlAss
		.getSubject().getDOM().getTextContent(), "issuer", UconCategory.SUBJECT);
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
		    throw new XacmlSamlException("Unable to get value for the attribute:\n" + marshalling(samlAttribute));
		}
		PepRequestAttribute_OLD pepAttr = new PepRequestAttribute_OLD(xacmlid, dataType, value, "issuer", UconCategory.SUBJECT);
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
