package org.ow2.contrail.authorization.cnr.old.core.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.axis2.context.ServiceContext;
import org.joda.time.DateTime;
import org.opensaml.Configuration;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Statement;
import org.opensaml.saml2.core.Subject;
import org.opensaml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.saml2.core.impl.ResponseBuilder;
import org.opensaml.saml2.core.impl.SubjectBuilder;
import org.opensaml.xacml.XACMLConstants;
import org.opensaml.xacml.ctx.ActionType;
import org.opensaml.xacml.ctx.AttributeType;
import org.opensaml.xacml.ctx.AttributeValueType;
import org.opensaml.xacml.ctx.EnvironmentType;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResourceType;
import org.opensaml.xacml.ctx.ResponseType;
import org.opensaml.xacml.ctx.SubjectType;
import org.opensaml.xacml.ctx.impl.ActionTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.AttributeTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.AttributeValueTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.EnvironmentTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.RequestTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.ResourceTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.SubjectTypeImplBuilder;
import org.opensaml.xacml.profile.saml.XACMLAuthzDecisionQueryType;
import org.opensaml.xacml.profile.saml.XACMLAuthzDecisionStatementType;
import org.opensaml.xacml.profile.saml.impl.XACMLAuthzDecisionStatementTypeImplBuilder;
import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;
import org.ow2.contrail.authorization.cnr.core.db.UconHolder;
import org.ow2.contrail.authorization.cnr.core.db.UconSession;
import org.ow2.contrail.authorization.cnr.core.ucon.UconDataEntity;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.ow2.contrail.authorization.cnr.utils.UconCategory;
import org.ow2.contrail.authorization.cnr.utils.OpenSamlUtils;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlAttribute;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.balana.ParsingException;
import org.wso2.balana.ctx.AbstractResult;
import org.wso2.balana.ctx.ResponseCtx;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class OpenSamlCoreOLD extends OpenSamlUtils implements XacmlSamlCoreUtilsOLD {
	
	private DocumentBuilder builderDocument;
	private AssertionBuilder builderAssertion;
	private DocumentBuilder docBuilder;
	
	private OpenSamlCoreOLD(ServiceContext serviceContext) throws XacmlSamlException {
		super(serviceContext);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			builderDocument = factory.newDocumentBuilder();
			builderAssertion = (AssertionBuilder) builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			docBuilder = docFactory.newDocumentBuilder();
			
		} catch (ParserConfigurationException e) {
			throw new XacmlSamlException(e);
		}
		
	}

	public static void init(ServiceContext serviceContext) throws XacmlSamlException {
		getInstance(serviceContext);
	}
	
	/**
	 * Constructor by singleton pattern (adapted for web service)
	 * @param serviceContext The service context in which save/get the singleton instance
	 * @return
	 * @throws XacmlSamlException
	 * @throws NullPointerException
	 */
	public static XacmlSamlCoreUtilsOLD getInstance(ServiceContext serviceContext) throws XacmlSamlException {
//		OpenSamlUtils obj = OpenSamlUtils.getInstanceBase(serviceContext, Class.class);
//		if(obj == null) {
//			OpenSamlPip utils = new OpenSamlPip();
//			serviceContext.setProperty(UconConstants.OPENSAML_UTILS, utils); //added by super class
//			return utils;
//		}
//		return (OpenSamlPip) obj;
		
			try {
				// WHAT THE F??? [KMcC;)]
				return (OpenSamlCoreOLD) OpenSamlUtils.getInstanceBase(serviceContext, OpenSamlCoreOLD.class);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("This should not be possible",e);
			} catch (InstantiationException e) {
				throw new RuntimeException("This should not be possible",e);
			} catch (IllegalArgumentException e) {
				throw new RuntimeException("This should not be possible",e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException("This should not be possible",e);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new RuntimeException("Reflection security problem",e);
			}
	}	
	
	public UconSession getUconSessionFromXacmlRetrieve(Element retrieve) {
		Assertion assertion = builderAssertion.buildObject();
		assertion.setDOM(retrieve);
		UconSession session = new UconSession();
		session.setSession_id_string(assertion.getID());
//		Other attribute (like reply to address)
//		assertion.get
		
		return session;		
	}
	
	public Element formAttributeQueryRequest(UconSession session) {
		// root elements
		Document doc = docBuilder.newDocument();
		Element request = doc.createElement(UconConstants.PIP_ATTRIBUTE_QUERY_Tag);
		// create subject element
		Element subject = doc.createElement(UconConstants.PIP_REQUEST_SUBJECT_Tag);
//		subject.setTextContent(session.getSubject().getXacml_id());
		// append subject element
		request.appendChild(subject);
		// create resource element
		Element resource = doc.createElement(UconConstants.PIP_REQUEST_RESOURCE_Tag);
//		resource.setTextContent(session.getObject().getXacml_id());
		// append resource element
		request.appendChild(resource);
		
		return request;
	}
	
	public Element formSubscriptionRequest(UconSession session) {
		// root elements
		Document doc = docBuilder.newDocument();
		Element request = doc.createElement(UconConstants.PIP_ATTRIBUTE_SUBSCRIBE_Tag);
		// create subject element if is not subscribed
//		UconHolder sub = session.getSubject();
//		if(!sub.isSubscribed()) {
			Element subject = doc.createElement(UconConstants.PIP_REQUEST_SUBJECT_Tag);
//			subject.setTextContent(session.getSubject().getXacml_id());
			// append subject element
			request.appendChild(subject);
//		}
		// create resource element if is not subscribed
//		UconHolder res = session.getSubject();
//		if(!res.isSubscribed()) {
			Element resource = doc.createElement(UconConstants.PIP_REQUEST_RESOURCE_Tag);
//			resource.setTextContent(res.getXacml_id());
			// append resource element
			request.appendChild(resource);
//		}
		
		return request;
	}
	
	public Element formUnubscriptionRequest(UconSession session) {
		// TODO ....
		return null;
//		// root elements
//		Document doc = docBuilder.newDocument();
//		Element request = doc.createElement(UconConstants.PIP_ATTRIBUTE_QUERY_Tag);
//		
//		Element subject = doc.createElement(UconConstants.PIP_REQUEST_SUBJECT_Tag);
//		subject.setTextContent(session.getSubject().getXacml_id());
//		
//		request.appendChild(subject);
//		
//		Element resource = doc.createElement(UconConstants.PIP_REQUEST_RESOURCE_Tag);
//		resource.setTextContent(session.getObject().getXacml_id());
//		
//		request.appendChild(resource);
//		
//		return request;
	}
	
	
///////////////////////////////////////////////////////////////////////////////////////
	//////DA RICONTROLLARE/ELIMINARE
	
	private AttributeType copyAttributeType(AttributeType attr) {
		AttributeTypeImplBuilder builderAttr = (AttributeTypeImplBuilder) builderFactory.getBuilder(AttributeType.DEFAULT_ELEMENT_NAME);
		AttributeValueTypeImplBuilder builderAttrValue = (AttributeValueTypeImplBuilder) builderFactory
				.getBuilder(AttributeValueType.DEFAULT_ELEMENT_NAME);

		AttributeType newAttr = builderAttr.buildObject();
		newAttr.setAttributeID(attr.getAttributeID());
		newAttr.setDataType(attr.getDataType());
		newAttr.setIssuer(attr.getIssuer());
		for (AttributeValueType value : attr.getAttributeValues()) {
			AttributeValueType newAttrValue = builderAttrValue.buildObject();
			newAttrValue.setValue(value.getValue());
			newAttr.getAttributeValues().add(newAttrValue);
		}
		return newAttr;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean getAccessDecision(String xacmlResponse) throws XacmlSamlException { // not used (xacml2.0)
		ResponseType response;
		response = (ResponseType) unmarshalling(xacmlResponse);
		return getAccessDecision(response);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean getAccessDecision30(String xacmlResponse) throws XacmlSamlException {
		ByteArrayInputStream inputStream;
		DocumentBuilderFactory dbf;
		Document doc;

		inputStream = new ByteArrayInputStream(xacmlResponse.getBytes());
		dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);

		try {
			doc = dbf.newDocumentBuilder().parse(inputStream);
		} catch (Exception e) { // SAXException IOException ParserConfigurationException
			throw new XacmlSamlException("DOM of request element can not be created from the following string:\n" + xacmlResponse);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) { // it should never happen
				System.err.println("Error in closing input stream of XACML response");
			}
		}
		Element elem = doc.getDocumentElement();
		ResponseCtx responseCtx = null;
		try {
			responseCtx = ResponseCtx.getInstance(elem);
		} catch (ParsingException e) {
			throw new XacmlSamlException("Unable to convert elemet in Balana response instance (" + e.getMessage() + ")\n"
					+ elem.getTextContent());
		}
		AbstractResult result = responseCtx.getResults().iterator().next();
		return (AbstractResult.DECISION_PERMIT == result.getDecision());

	}

	/**
	 * {@inheritDoc}
	 */
	public String formResponse(String xacmlResponse, String sessionId) throws XacmlSamlException {
		ResponseBuilder builderResponse = (ResponseBuilder) builderFactory.getBuilder(Response.DEFAULT_ELEMENT_NAME);
		Response response = builderResponse.buildObject();
		response.setVersion(SAMLVersion.VERSION_20);
		AssertionBuilder builderAssertion = (AssertionBuilder) builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
		Assertion assertion = builderAssertion.buildObject();
		IssuerBuilder builderIssuer = (IssuerBuilder) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
		Issuer issuer = builderIssuer.buildObject();
		issuer.setValue("contrail-pdp");
		issuer.setSPProvidedID("cnr");
		SubjectBuilder builderSubject = (SubjectBuilder) builderFactory.getBuilder(Subject.DEFAULT_ELEMENT_NAME);
		Subject subject = builderSubject.buildObject();
		XACMLAuthzDecisionStatementTypeImplBuilder builderXacmlAuthz = (XACMLAuthzDecisionStatementTypeImplBuilder) builderFactory
				.getBuilder(XACMLAuthzDecisionStatementType.TYPE_NAME_XACML20);
		XACMLAuthzDecisionStatementType xacmlAuthzStatement = builderXacmlAuthz.buildObject(Statement.DEFAULT_ELEMENT_NAME,
				XACMLAuthzDecisionStatementType.TYPE_NAME_XACML20);
		ResponseType responseXacml = (ResponseType) unmarshalling(xacmlResponse);
		xacmlAuthzStatement.setResponse(responseXacml);
		assertion.setVersion(org.opensaml.common.SAMLVersion.VERSION_20);
		// assertion.setID(Integer.toString(sessionId));
		assertion.setID(sessionId);
		DateTime issueinstant = new DateTime(); // CHECKME: Should be set to now?
		assertion.setIssueInstant(issueinstant);
		assertion.setSubject(subject);
		assertion.setIssuer(issuer);
		assertion.getStatements().add(xacmlAuthzStatement);
		response.getAssertions().add(assertion);
		return marshalling(response);
	}
	
	public List<UconAttribute> getAttributeFromPipUpdate(String updateMessage) throws XacmlSamlException {
		List<UconAttribute> attributes = getAttributeFromPipResponse(updateMessage, "");
		List<UconAttribute> actualAttr = new LinkedList<UconAttribute>();
		String holder = "";
		for(UconAttribute attr : attributes) {
			if(attr.getXacml_id().equals("HOLDER_ATTRIBUTE")) {
				holder = attr.getValue();
			} else {
				actualAttr.add(attr);
			}
		}
		for(UconAttribute attr : attributes) {
//			attr.setHolder(holder);
		}
		return actualAttr;
	}
	
//	public List<UconAttribute> getAttributeFromPipResponse(String response) throws XacmlSamlException {
//		Document dom;
//		try {
//			dom = builderDocument.parse(new InputSource(response));
//		} catch (Exception e) {
//			throw new XacmlSamlException(e);
//		}
//		
//	}

	@Deprecated
	public List<UconAttribute> getAttributeFromPipResponse(String response, String holder2) throws XacmlSamlException {

		List<UconAttribute> attributes = new ArrayList<UconAttribute>();
		String prolog = "", postprolog = "";
		try {
			prolog = response.substring(0, response.indexOf(">") + 1);
			// System.out.println("[UCON] prolog:\n" + prolog);
			postprolog = response.substring(response.indexOf("<x"), response.length());
			// System.out.println("[UCON] prolog:\n" + postprolog);
		} catch (IndexOutOfBoundsException e) {
			throw new XacmlSamlException("Unable to parse the following PIP response:\n" + response);
		}

		// in order to use opensaml library I create a false <Request>
		response = prolog + "<xacml-context:Request xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">" + postprolog
				+ "</xacml-context:Request>";
		// response = prolog + "<Request xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">" + postprolog +
		// "</Request>";
		response = response.replace("&lt;", "<");

		// response = "<" + response.substring(1).replace("<", "<xacml-context:").replace("<xacml-context:/", "</xacml-context:"); //
		// OMG!		
		// CHECKME: bad programming :D

		RequestType request = (RequestType) unmarshalling(response);

		// try { System.out.println("[UTILS] qui ci siamo:\n"+marshalling(request)); } catch (MarshallingException e1) {
		// e1.printStackTrace(); }
		List<SubjectType> subjects = request.getSubjects();
		// System.out.println("[UTILS] Numero di subjects: " + subjects.size());
		// System.out.println("[UTILS] Holder: " + holder);
		for (SubjectType subject : subjects) {
			// try { System.out.println("[UTILS] secondo me questo è vuoto:\n"+marshalling(subject)); } catch (MarshallingException e) {
			// e.printStackTrace(); }
			String holder = "";
			for (AttributeType attr : subject.getAttributes()) {
				if(attr.getAttributeID().equals(UconConstants.HOLDER_ELEMENT)) {
					holder = attr.getAttributeValues().get(0).getValue();
					break;
				}
			}			
			for (AttributeType attr : subject.getAttributes())
				attributes.add(formUconAttribute(attr, holder, UconCategory.SUBJECT));
		}
		List<ResourceType> resources = request.getResources();
		// System.out.println("Numero di resource: " + resources.size());
		for (ResourceType resource : resources) {
			// try { System.out.println("[UTILS] secondo me questo è vuoto:\n"+marshalling(resource)); } catch (MarshallingException e)
			// { e.printStackTrace(); }
			String holder = "";
			for (AttributeType attr : resource.getAttributes()) {
				if(attr.getAttributeID().equals(UconConstants.HOLDER_ELEMENT)) {
					holder = attr.getAttributeValues().get(0).getValue();
					break;
				}
			}
			for (AttributeType attr : resource.getAttributes())
				attributes.add(formUconAttribute(attr, holder, UconCategory.RESOURCE));
		}
		return attributes;
	}

	/**
	 * @param attribute
	 * @param category
	 * @return
	 * @throws XacmlSamlException
	 */
	private UconAttribute formUconAttribute(AttributeType attribute, String holder, UconCategory category) throws XacmlSamlException {
		try {
			int key = -1; // key is generated by inserting attributes to database
			// CHECKME: I get only the first attribute value
			UconAttribute attr = new UconAttribute();
			attr.setAttribute_id(key);
			attr.setXacml_id(attribute.getAttributeID());
			attr.setType(attribute.getDataType());
			attr.setValue(attribute.getAttributeValues().get(0).getValue());
			attr.setIssuer(attribute.getIssuer());
//			attr.setHolder(holder);
//			attr.setCategory(category);
			return attr;
			
		} catch (IndexOutOfBoundsException e) {
			throw new XacmlSamlException("Invalid attribute parsing");
		}
	}

	/**
	 * used in accessdb //TODO: should be removed, when the db will be changed! (will it?)
	 * 
	 * @param xacmlRequest
	 * @return
	 * @throws XacmlSamlException
	 */

	public String convertXacmlRequestToString(RequestType xacmlRequest) throws XacmlSamlException {
		return marshalling(xacmlRequest);
	}

	public RequestType convertXacmlRequestToObject(String xacmlRequest) throws XacmlSamlException {
		return (RequestType) unmarshalling(xacmlRequest);
	}

	@Deprecated
	public UconRequestContext getRequestContextFromUconXacmlRequest(UconXacmlRequest uconRequest) throws XacmlSamlException {
		UconRequestContext requestContext = new UconRequestContext(CorePhase.PRE);
		RequestType request = uconRequest.getObject();
		requestContext.setXACMLRequest(uconRequest);
		// CHECKME: in following code I take the first element for each type list!!!

		return requestContext;
	}

	@Deprecated
	public UconRequestContext getRequestContextFromAccessRequest(String authzQuery) throws XacmlSamlException {
		XACMLAuthzDecisionQueryType authz = (XACMLAuthzDecisionQueryType) unmarshalling(authzQuery);
		RequestType request = authz.getRequest();
		UconRequestContext requestContext = getRequestContextFromUconXacmlRequest(new UconXacmlRequest(request, this));
		requestContext.setSessionId(authz.getID());
		return requestContext;
	}
	
	private UconAttribute getUconAttribute(AttributeType xacmlAttr) throws XacmlSamlException {
		if(!xacmlAttr.getAttributeValues().isEmpty()) {
			UconAttribute attr = new UconAttribute();
			attr.setIssuer(xacmlAttr.getIssuer());
			attr.setType(xacmlAttr.getDataType());
			attr.setXacml_id(xacmlAttr.getAttributeID());
			attr.setValue(xacmlAttr.getAttributeValues().get(0).getValue()); //CHECKME: just the first
			return attr;
		} else {
			throw new XacmlSamlException("Unable to parse xacml request");
		}
	}
	
	public UconSession getUconSessionFromSamlXacmlRequest(String authzQuery) throws XacmlSamlException {
		// create destination object
		UconSession session = new UconSession();
		
		// convert to OpenSaml representation 
		XACMLAuthzDecisionQueryType authz = (XACMLAuthzDecisionQueryType) unmarshalling(authzQuery);
		// set id
		session.setSession_id_string(authz.getID());
		// get the request
		RequestType request = authz.getRequest();
		
		// SUBJECT:
		List<SubjectType> subjects = request.getSubjects();
		// check the existence of at least a subject
		if(subjects.isEmpty() || subjects.get(0).getAttributes().isEmpty()) {
			throw new XacmlSamlException("Unable to parse xacml request");
		}
		// get the first subject (holder)
		AttributeType firstSubject = request.getSubjects().get(0).getAttributes().remove(0); //CHECKME
		UconHolder subjectHolder = UconHolder.makeHolderFromAttribute(getUconAttribute(firstSubject), UconCategory.SUBJECT);
		// get the other subjects
		for(SubjectType subjectType: request.getSubjects()) {
			for(AttributeType attributeType: subjectType.getAttributes()) {
				subjectHolder.addAttribute(getUconAttribute(attributeType));
			}
		}
		// add to UconSession
//		session.setSubject(subjectHolder);
		
		// OBJECT:
		List<ResourceType> objects = request.getResources();
		// check the existence of at least a resource
		if(objects.isEmpty() || objects.get(0).getAttributes().isEmpty()) {
			throw new XacmlSamlException("Unable to parse xacml request");
		}
		// get the first object
		AttributeType firstObject = request.getResources().get(0).getAttributes().remove(0); //CHECKME
		UconHolder objectHolder = UconHolder.makeHolderFromAttribute(getUconAttribute(firstObject), UconCategory.RESOURCE);
		// get the other object
		for(ResourceType objectType: request.getResources()) {
			for(AttributeType attributeType: objectType.getAttributes()) {
				objectHolder.addAttribute(getUconAttribute(attributeType));
			}
		}
		// add to UconSession
//		session.setObject(objectHolder);
		
		// ACTION:
		ActionType action = request.getAction();
		// check the existence of at least an action
		if(action.getAttributes().isEmpty()) {
			throw new XacmlSamlException("Unable to parse xacml request");
		}
		// get the first action
		AttributeType firstAction = action.getAttributes().remove(0); //CHECKME
		UconHolder actionHolder = UconHolder.makeHolderFromAttribute(getUconAttribute(firstAction), UconCategory.ACTION);
		// get the other action
		for(AttributeType attributeType: action.getAttributes()) {
			actionHolder.addAttribute(getUconAttribute(attributeType));
		}
		// add to UconSession
//		session.setAction(actionHolder);
		
		return session;
	}
	
	public String getXacmlRequestFromUconSession(UconSession session) throws XacmlSamlException {
		// create the builders
		RequestTypeImplBuilder requestBuilder = (RequestTypeImplBuilder) builderFactory.getBuilder(RequestType.DEFAULT_ELEMENT_NAME);
		RequestType request = requestBuilder.buildObject();
		ActionTypeImplBuilder actionBuilder = (ActionTypeImplBuilder) builderFactory.getBuilder(ActionType.DEFAULT_ELEMENT_NAME);
		EnvironmentTypeImplBuilder environmentBuilder = (EnvironmentTypeImplBuilder) builderFactory.getBuilder(EnvironmentType.DEFAULT_ELEMENT_NAME);
		ResourceTypeImplBuilder resourceBuilder = (ResourceTypeImplBuilder) builderFactory.getBuilder(ResourceType.DEFAULT_ELEMENT_NAME);
		SubjectTypeImplBuilder subjectBuilder = (SubjectTypeImplBuilder) builderFactory.getBuilder(SubjectType.DEFAULT_ELEMENT_NAME);
		
		// set the first action (holder)
		ActionType action = actionBuilder.buildObject();
//		action.getAttributes().add(convertXacmlAttributeToAttributeType(session.getAction()));
		//set other actions
//		for(XacmlAttribute attr : session.getAction().getAttributes()) {
//			action.getAttributes().add(convertXacmlAttributeToAttributeType(attr));
//		}
		request.setAction(action);
		
		// set the first object	(holder)
		ResourceType resource = resourceBuilder.buildObject();
//		resource.getAttributes().add(convertXacmlAttributeToAttributeType(session.getObject()));
		//set other objects
//		for(XacmlAttribute attr : session.getObject().getAttributes()) {
//			resource.getAttributes().add(convertXacmlAttributeToAttributeType(attr));
//		}
		request.getResources().add(resource);
		
		// set the first subject (holder)
		SubjectType subject = subjectBuilder.buildObject();
//		subject.getAttributes().add(convertXacmlAttributeToAttributeType(session.getSubject()));		
		//set other subject
//		for(XacmlAttribute attr : session.getSubject().getAttributes()) {
//			subject.getAttributes().add(convertXacmlAttributeToAttributeType(attr));
//		}
		request.getSubjects().add(subject);
		
		return marshalling(request);
	}

	/**
	 * {@inheritDoc}
	 */
	@Deprecated
	public String getIdFromStart(String message) throws XacmlSamlException {
		String sessionId = ((Assertion) unmarshalling(message)).getID();		
		return sessionId;
	}

	/**
	 * {@inheritDoc}
	 */
	@Deprecated
	public String getIdFromEnd(String message) throws XacmlSamlException {
		String sessionId = ((Assertion) unmarshalling(message)).getID();		
		return sessionId;
	}

	/*	public String convertXacmlAuthzQuery20to30(UconXacmlRequest xacmlAuthzQuery, List<UconAttribute> attributes) throws XacmlSamlException {
		String conv = "";
		try {
			RequestType initialRequest = xacmlAuthzQuery.getObject();

			RequestTypeImplBuilder builderRequest = (RequestTypeImplBuilder) builderFactory.getBuilder(RequestType.DEFAULT_ELEMENT_NAME);
			RequestType request = builderRequest.buildObject();
			// copying actions
			ActionTypeImplBuilder builderAction = (ActionTypeImplBuilder) builderFactory.getBuilder(ActionType.DEFAULT_ELEMENT_NAME);
			ActionType newAction = builderAction.buildObject();
			for (AttributeType attr : initialRequest.getAction().getAttributes()) {
				AttributeType newAttr = copyAttributeType(attr);
				newAction.getAttributes().add(newAttr);
			}
			request.setAction(newAction);
			// copying environments
			EnvironmentTypeImplBuilder builderEnvironment = (EnvironmentTypeImplBuilder) builderFactory
					.getBuilder(EnvironmentType.DEFAULT_ELEMENT_NAME);
			EnvironmentType newEnvironment = builderEnvironment.buildObject();
			EnvironmentType oldEnvironment = initialRequest.getEnvironment();
			if (oldEnvironment != null)
				for (AttributeType attr : oldEnvironment.getAttributes()) {
					AttributeType newAttr = copyAttributeType(attr);
					newEnvironment.getAttributes().add(newAttr);
				}
			request.setEnvironment(newEnvironment);
			// copying Resources
			ResourceTypeImplBuilder builderResource = (ResourceTypeImplBuilder) builderFactory
					.getBuilder(ResourceType.DEFAULT_ELEMENT_NAME);
			for (ResourceType resource : initialRequest.getResources()) {
				ResourceType newResource = builderResource.buildObject();
				for (AttributeType attr : resource.getAttributes()) {
					AttributeType newAttr = copyAttributeType(attr);
					newResource.getAttributes().add(newAttr);
				}
				request.getResources().add(newResource);
			}
			// copying Subjects
			SubjectTypeImplBuilder builderSubject = (SubjectTypeImplBuilder) builderFactory.getBuilder(SubjectType.DEFAULT_ELEMENT_NAME);
			for (SubjectType Subject : initialRequest.getSubjects()) {
				SubjectType newSubject = builderSubject.buildObject();
				for (AttributeType attr : Subject.getAttributes()) {
					AttributeType newAttr = copyAttributeType(attr);
					newSubject.getAttributes().add(newAttr);
				}
				request.getSubjects().add(newSubject);
			}

			for (UconAttribute uconAttr : attributes) {
				AttributeType xacmlAttribute = createXacmlAttribute(uconAttr);
				switch (uconAttr.getCategory()) {
				case ACTION:
					break;// ?
				case ENVIRONMENT:
					break;// ?
				case RESOURCE:
					// if there are more then one resource, how do i have to insert these attributes?
					request.getResources().get(0).getAttributes().add(xacmlAttribute);
					break;
				case SUBJECT:
					// if there are more then one subject, how do i have to insert these attributes?
					request.getSubjects().get(0).getAttributes().add(xacmlAttribute);
					break;
				}
			}

			conv = "<Request xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\" CombinedDecision=\"false\" ReturnPolicyIdList=\"false\">\n";
			conv += "<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:action\">\n";
			for (AttributeType attr : request.getAction().getAttributes()) {
				conv = conv + "<Attribute AttributeId=\"" + attr.getAttributeID() + "\" IncludeInResult=\"false\" Issuer=\""
						+ attr.getIssuer() + "\">\n" + "<AttributeValue DataType=\"" + attr.getDataType() + "\">"
						+ attr.getAttributeValues().get(0).getValue() + "</AttributeValue>\n" + "</Attribute>\n";
			}
			conv += "</Attributes>\n<Attributes Category=\"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject\">\n";
			for (SubjectType sub : request.getSubjects()) {
				for (AttributeType attr : sub.getAttributes()) {
					conv = conv + "<Attribute AttributeId=\"" + attr.getAttributeID() + "\" IncludeInResult=\"false\" Issuer=\""
							+ attr.getIssuer() + "\">\n" + "<AttributeValue DataType=\"" + attr.getDataType() + "\">"
							+ attr.getAttributeValues().get(0).getValue() + "</AttributeValue>\n" + "</Attribute>\n";
				}
			}
			conv += "</Attributes>\n<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\">\n";
			for (ResourceType res : request.getResources()) {
				for (AttributeType attr : res.getAttributes()) {
					conv = conv + "<Attribute AttributeId=\"" + attr.getAttributeID() + "\" IncludeInResult=\"false\" Issuer=\""
							+ attr.getIssuer() + "\">\n" + "<AttributeValue DataType=\"" + attr.getDataType() + "\">"
							+ attr.getAttributeValues().get(0).getValue() + "</AttributeValue>\n" + "</Attribute>\n";
				}
			}
			conv += "</Attributes>\n<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:environment\">\n";
			for (AttributeType attr : request.getEnvironment().getAttributes()) {
				conv = conv + "<Attribute AttributeId=\"" + attr.getAttributeID() + "\" IncludeInResult=\"false\" Issuer=\""
						+ attr.getIssuer() + "\">\n" + "<AttributeValue DataType=\"" + attr.getDataType() + "\">"
						+ attr.getAttributeValues().get(0).getValue() + "</AttributeValue>\n" + "</Attribute>\n";
			}
			conv += "</Attributes>\n</Request>";

			// System.out.println("[UTIL] convert authz query from 2.0 to 3.0\n" + conv);
		} catch (Exception e) { //IndexOutOfBoundsException OR NullPointerException
			throw new XacmlSamlException("Unable to convert a XacmlAuthzQuery from xacml version 2.0 to version 3.0");
		}
		return conv;
	}

	public String formXacmlAuthzQueryAttribute(UconXacmlRequest xacmlAuthzQuery, List<UconAttribute> attributes) throws XacmlSamlException {
		RequestType request = null;
		try {
			RequestType initialRequest = xacmlAuthzQuery.getObject();

			RequestTypeImplBuilder builderRequest = (RequestTypeImplBuilder) builderFactory.getBuilder(RequestType.DEFAULT_ELEMENT_NAME);
			request = builderRequest.buildObject();
			// copying actions
			ActionTypeImplBuilder builderAction = (ActionTypeImplBuilder) builderFactory.getBuilder(ActionType.DEFAULT_ELEMENT_NAME);
			ActionType newAction = builderAction.buildObject();
			for (AttributeType attr : initialRequest.getAction().getAttributes()) {
				AttributeType newAttr = copyAttributeType(attr);
				newAction.getAttributes().add(newAttr);
			}
			request.setAction(newAction);
			// copying environments
			EnvironmentTypeImplBuilder builderEnvironment = (EnvironmentTypeImplBuilder) builderFactory
					.getBuilder(EnvironmentType.DEFAULT_ELEMENT_NAME);
			EnvironmentType newEnvironment = builderEnvironment.buildObject();
			EnvironmentType oldEnvironment = initialRequest.getEnvironment();
			if (oldEnvironment != null)
				for (AttributeType attr : oldEnvironment.getAttributes()) {
					AttributeType newAttr = copyAttributeType(attr);
					newEnvironment.getAttributes().add(newAttr);
				}
			request.setEnvironment(newEnvironment);
			// copying Resources
			ResourceTypeImplBuilder builderResource = (ResourceTypeImplBuilder) builderFactory
					.getBuilder(ResourceType.DEFAULT_ELEMENT_NAME);
			for (ResourceType resource : initialRequest.getResources()) {
				ResourceType newResource = builderResource.buildObject();
				for (AttributeType attr : resource.getAttributes()) {
					AttributeType newAttr = copyAttributeType(attr);
					newResource.getAttributes().add(newAttr);
				}
				request.getResources().add(newResource);
			}
			// copying Subjects
			SubjectTypeImplBuilder builderSubject = (SubjectTypeImplBuilder) builderFactory.getBuilder(SubjectType.DEFAULT_ELEMENT_NAME);
			for (SubjectType Subject : initialRequest.getSubjects()) {
				SubjectType newSubject = builderSubject.buildObject();
				for (AttributeType attr : Subject.getAttributes()) {
					AttributeType newAttr = copyAttributeType(attr);
					newSubject.getAttributes().add(newAttr);
				}
				request.getSubjects().add(newSubject);
			}

			for (UconAttribute uconAttr : attributes) {
				AttributeType xacmlAttribute = createXacmlAttribute(uconAttr);
				switch (uconAttr.getCategory()) {
				case ACTION:
					break;// ?
				case ENVIRONMENT:
					break;// ?
				case RESOURCE:
					// if there are more then one resource, how do i have to insert these attributes?
					request.getResources().get(0).getAttributes().add(xacmlAttribute);
					break;
				case SUBJECT:
					// if there are more then one subject, how do i have to insert these attributes?
					request.getSubjects().get(0).getAttributes().add(xacmlAttribute);
					break;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			throw new XacmlSamlException("Unable to convert a XacmlAuthzQuery from xacml version 2.0 to version 3.0");
		} catch (NullPointerException e) {
			throw new XacmlSamlException("Unable to convert a XacmlAuthzQuery from xacml version 2.0 to version 3.0");
		}
		return marshalling(request);
	}
*/
}
