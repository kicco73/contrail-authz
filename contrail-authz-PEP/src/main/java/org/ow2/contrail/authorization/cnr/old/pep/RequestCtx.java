package org.ow2.contrail.authorization.cnr.old.pep;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.security.utils.XMLUtils;
import org.joda.time.DateTime;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.impl.IssuerBuilder;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.profile.saml.SAMLProfileConstants;
import org.opensaml.xacml.profile.saml.XACMLAuthzDecisionQueryType;
import org.opensaml.xacml.profile.saml.impl.XACMLAuthzDecisionQueryTypeImplBuilder;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.parse.BasicParserPool;
import org.w3c.dom.Document;

@Deprecated
public class RequestCtx {

    private List<String> attrsSubject = new ArrayList<String>();
    private List<String> attrsResource = new ArrayList<String>();
    private List<String> attrsAction = new ArrayList<String>();
    private List<String> attrsEnvironment = new ArrayList<String>();

    // old code

    private static Logger logger = Logger.getLogger("cnr.contrail.pep.requestCtx");

    private String pepID = new String("anonymous");

    private static UnmarshallerFactory unMarshallerFactory = null;
    private static MarshallerFactory marshallerFactory = null;
    private static XMLObjectBuilderFactory builderFactory = null;
    private static javax.xml.parsers.DocumentBuilderFactory dbf = null;
    private static javax.xml.parsers.DocumentBuilder db = null;

    public RequestCtx(List<Attribute> attributes, String requestorID) {
	try {
	    Iterator<Attribute> it = attributes.iterator();

	    pepID = requestorID;

	    // Separate received attributes into appropriate categories and convert them to XACML format.
	    while (it.hasNext()) {

		Attribute attr = (Attribute) it.next();

		switch (attr.getHolder()) {
		case Attribute.SUBJECT:
		    attrsSubject.add(attr.createXACMLAttr());
		    break;
		case Attribute.RESOURCE:
		    attrsResource.add(attr.createXACMLAttr());
		    break;
		case Attribute.ACTION:
		    attrsAction.add(attr.createXACMLAttr());
		    break;
		case Attribute.ENVIRONMENT:
		    attrsEnvironment.add(attr.createXACMLAttr());
		    break;
		}
	    }

	    // SAML init
	    init();

	} catch (Exception e) {
	    // DO SOMETHING
	    // System.out.println("RequestCtx->constructor:" + e.toString());
	    logger.log(Level.WARNING, "RequestCtx->constructor:", e);
	}
    }

    private static void init() throws ConfigurationException, ParserConfigurationException {

	// Should be used in new versions: org.apache.xml.security.Init.init();

	org.opensaml.DefaultBootstrap.bootstrap();

	unMarshallerFactory = org.opensaml.xml.Configuration.getUnmarshallerFactory();
	marshallerFactory = org.opensaml.xml.Configuration.getMarshallerFactory();

	dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
	dbf.setNamespaceAware(true);

	db = dbf.newDocumentBuilder();

	builderFactory = org.opensaml.xml.Configuration.getBuilderFactory();
    }

    // Link together all attributes of the same category
    private String combineAttributes(List<String> attributes) {
	String result = new String("");

	try {
	    Iterator<String> it = attributes.iterator();

	    while (it.hasNext()) {
		String attr = (String) it.next();
		result = result + attr;
	    }
	} catch (Exception e) {
	    // DO SOMETHING
	    // System.out.println("RequestCtx->combineAttributes:" + e.toString());
	    logger.log(Level.WARNING, "RequestCtx->combineAttributes:", e);
	}

	return result;
    }

    /**
     * Creates a new XACMLAuthzQuery from a XAML request
     * 
     * @param XAML
     *            request
     * @return the created XACMLAuthzQuery
     * @throws Exception
     */
    private XACMLAuthzDecisionQueryType makeXACMLAuthzQuery(String xacml_request) throws Exception {

	// XACML Request
	InputStream in = new ByteArrayInputStream(xacml_request.getBytes());

	// SAML-XACML enhancements
	IssuerBuilder issuer = (IssuerBuilder) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);

	Issuer objectissuer = issuer.buildObject();
	objectissuer.setValue(pepID);

	BasicParserPool pool = new BasicParserPool();
	Document documentXACMLRequest = pool.parse(in);

	Unmarshaller requestUnmarshaller = unMarshallerFactory.getUnmarshaller(RequestType.DEFAULT_ELEMENT_NAME);

	RequestType request = (RequestType) requestUnmarshaller.unmarshall(documentXACMLRequest.getDocumentElement());

	// should be deprecated and trusted source of time used
	DateTime issueinstantQ = new DateTime(new DateTime());

	XACMLAuthzDecisionQueryTypeImplBuilder xacmlDecisionQueryBuilder = (XACMLAuthzDecisionQueryTypeImplBuilder) builderFactory
		.getBuilder(XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_NAME_XACML20);

	XACMLAuthzDecisionQueryType xacmlQuery = xacmlDecisionQueryBuilder.buildObject(SAMLProfileConstants.SAML20XACML20P_NS,
		XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_LOCAL_NAME, SAMLProfileConstants.SAML20XACMLPROTOCOL_PREFIX);

	// 1 - for usage control; 0 - for access control
	xacmlQuery.setID("0"); // "1234"
	xacmlQuery.setDestination("contrail-pdp"); // "localhost"
	xacmlQuery.setIssuer(objectissuer);
	xacmlQuery.setVersion(org.opensaml.common.SAMLVersion.VERSION_20);
	xacmlQuery.setRequest(request);
	xacmlQuery.setIssueInstant(issueinstantQ);

	return xacmlQuery;
    }

    private String marshallXACMLAuthzDecisionQueryType(XACMLAuthzDecisionQueryType authzQuery) {

	String samlxacml_request = new String("");

	Document docAuthzDecisionQuery = db.newDocument();

	Marshaller marshallerAuthzDecisionQuery = marshallerFactory.getMarshaller(XACMLAuthzDecisionQueryType.DEFAULT_ELEMENT_NAME_XACML20);

	try {
	    marshallerAuthzDecisionQuery.marshall(authzQuery, docAuthzDecisionQuery);

	    ByteArrayOutputStream sos = new ByteArrayOutputStream();
	    XMLUtils.outputDOMc14nWithComments(docAuthzDecisionQuery, sos);

	    samlxacml_request = sos.toString();

	} catch (Exception e) {
	    logger.log(Level.WARNING, "RequestCtx->marshallXACMLAuthzDecisionQueryType:", e);
	}
	return samlxacml_request;
    }

    /** Create SAML/XACML request. **/
    public String createSAMLXACMLRequest() {
	String samlxacml_request = new String("");
	try {

	    // should be deprecated in further versions
	    String xacml_request = createXACMLRequest();

	    // make SAML-XACML Decision Query as DOM object
	    XACMLAuthzDecisionQueryType authzQuery = makeXACMLAuthzQuery(xacml_request);

	    // marshall the query to be sent in SOAP envelope
	    samlxacml_request = marshallXACMLAuthzDecisionQueryType(authzQuery);

	} catch (Exception e) {
	    // DO SOMETHING
	    // System.out.println("RequestCtx->createXACMLRequest:" + e.toString());
	    logger.log(Level.WARNING, "RequestCtx->createSAMLXACMLRequest:", e);
	}
	return samlxacml_request;
    }

    // should be deprecated
    private String createXACMLRequest() {
	String xacml_request = new String("");
	try {
	    xacml_request = "<xacml-context:Request xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">";

	    // Add subject attributes
	    xacml_request = xacml_request + "<xacml-context:Subject>" + combineAttributes(attrsSubject) + "</xacml-context:Subject>";

	    // Add resource attributes
	    xacml_request = xacml_request + "<xacml-context:Resource>" + combineAttributes(attrsResource) + "</xacml-context:Resource>";

	    // Add action attributes
	    xacml_request = xacml_request + "<xacml-context:Action>" + combineAttributes(attrsAction) + "</xacml-context:Action>";

	    // Add environment attributes
	    xacml_request = xacml_request + "<xacml-context:Environment>" + combineAttributes(attrsEnvironment) + "</xacml-context:Environment>";

	    // Finish.
	    xacml_request = xacml_request + "</xacml-context:Request>";
	} catch (Exception e) {
	    // DO SOMETHING
	    // System.out.println("RequestCtx->createXACMLRequest:" + e.toString());
	    logger.log(Level.WARNING, "RequestCtx->createXACMLRequest:", e);
	}
	return xacml_request;
    }
}
