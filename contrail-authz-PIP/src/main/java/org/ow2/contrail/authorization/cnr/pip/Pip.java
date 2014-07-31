package org.ow2.contrail.authorization.cnr.pip;

import java.io.StringReader;
import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.SOAPException;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.service.Lifecycle;
import org.ow2.contrail.authorization.cnr.pip.db.HibernateUtil;
import org.ow2.contrail.authorization.cnr.pip.utils.OpenSamlPip;
import org.ow2.contrail.authorization.cnr.pip.utils.UconConstantsPip;
import org.ow2.contrail.authorization.cnr.utils.XMLConvert;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class Pip implements Lifecycle {

	private static Logger log = LoggerFactory.getLogger(Pip.class);
	private static final String logTag = "[PIP]: ";

	@Override
	public void init(ServiceContext serviceContext) throws AxisFault {
		try {
			log.info("{} initialization started", logTag);
			// TODO once we have a table of identity provider, change this
			serviceContext.setProperty(UconConstantsPip.IDENTITY_PROVIDER_URL,
					"http://146.48.81.249:8085/federation-api/usersutils/saml");
			serviceContext.setProperty(UconConstantsPip.IDENTITY_PROVIDER_URL,
					"http://localhost:8080/axis2/services/Pip/fakeSaml");
			log.info(
					"{} [KMcC;)] identity provider URL set to: {}",
					logTag,
					serviceContext
							.getProperty(UconConstantsPip.IDENTITY_PROVIDER_URL));
			PipCommunicator.init(serviceContext);
			HibernateUtil.init(serviceContext);
			OpenSamlPip.init(serviceContext);
			log.info("{} initialization completed", logTag);
		} catch (SOAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XacmlSamlException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void destroy(ServiceContext serviceContext) {
		// TODO Auto-generated method stub

	}

	private OMElement getMessage(OMElement message) {
		message.build();
		if (message.getParent() != null) // [KMcC;)] bug fix
			message.detach();
		return message.getFirstElement();
	}

	public OMElement attributeQuery(OMElement request)
			throws XacmlSamlException {

		log.info("{} attribute query received", logTag);
		log.debug("{} attribute query received\n{}", logTag, request);
		System.out.println("[KMcC;] ECCO");

		// get message context
		MessageContext messageContext = MessageContext
				.getCurrentMessageContext();

		// create the PIP executor module
		PipExecutor exec = new PipExecutor(messageContext);
		log.info("{} [KMcC;] ECCO22", logTag);

		// execute the attributes retrieval
		log.info("[KMcC;] {} ECCO 3!: {}", logTag, getMessage(request));
		log.info("[KMcC;] {} ECCO 4!: {}", logTag,
				XMLConvert.toDOM(getMessage(request)));
		Element response = exec.attributeQuery(XMLConvert
				.toDOM(getMessage(request)));

		log.info("[KMcC;] ECCO RESPONSE", logTag, response);
		return XMLConvert.toOM(response);
	}

	public OMElement subscribe(OMElement request) throws XacmlSamlException,
			SQLException {

		log.info("{} subscribe recived", logTag);
		log.debug("{} subscribe recived\n{}", logTag, request);

		// get message context
		MessageContext messageContext = MessageContext
				.getCurrentMessageContext();

		// create the PIP executor module
		PipExecutor exec = new PipExecutor(messageContext);

		// execute the subscription
		Element response = exec.addSubscription(XMLConvert
				.toDOM(getMessage(request)));

		return XMLConvert.toOM(response);

	}

	public OMElement unsubscribe(OMElement request) throws XacmlSamlException,
			SQLException {

		log.info("{} unsubscribe recived", logTag);
		log.debug("{} unsubscribe recived\n{}", logTag, request);

		// get message context
		MessageContext messageContext = MessageContext
				.getCurrentMessageContext();

		// create the PIP executor module
		PipExecutor exec = new PipExecutor(messageContext);

		// communicate to system to stop update for this entity
		Element response = exec.removeSubscription(XMLConvert
				.toDOM(getMessage(request)));

		return XMLConvert.toOM(response);

	}

	public OMElement triggerUpdate() throws XacmlSamlException, AxisFault,
			SQLException {

		log.info("{} triggered update recived", logTag);

		// get message context
		MessageContext messageContext = MessageContext
				.getCurrentMessageContext();

		// create the PIP executor module
		PipExecutor exec = new PipExecutor(messageContext);

		// perform the update
		Element response = exec.triggeredUpdate();

		return XMLConvert.toOM(response);

	}

	public void update(String msg) {
		// TODO
		// try {
		// // get message context
		// MessageContext messageContext =
		// MessageContext.getCurrentMessageContext();
		//
		// // create the PIP executor module
		// PipExecutor exec = new PipExecutor(messageContext);
		//
		// // extract information
		// exec.receivedUpdate(msg);
		//
		//
		//
		// } catch(Exception e) {
		// e.printStackTrace();
		// }
	}

	// [KMcC;]
	public OMElement fakeSaml(OMElement request) throws XacmlSamlException {
		String xmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
				+ "<soap11:Envelope xmlns:soap11=\"http://schemas.xmlsoap.org/soap/envelope/\">"
				+ "<soap11:Body>"
				+ "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" "
				+ "ID=\"_946d879f37a82f98c54c75495a9a682f\" InResponseTo=\"AttrQuery12345789\" "
				+ "IssueInstant=\"2012-03-02T13:33:18.866Z\" Version=\"2.0\">"
				+ "<saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">CNR-PIP</saml2:Issuer>"
				+ "<saml2p:Status>"
				+ "<saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\" />"
				+ "</saml2p:Status>"
				+ "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" "
				+ "ID=\"_6296dc07137f221a500be6ab19511fa0\" IssueInstant=\"2012-03-02T13:33:18.864Z\" "
				+ "Version=\"2.0\">"
				+ "<saml2:Issuer>CNR-PIP</saml2:Issuer>"
				+ "<saml2:Subject>"
				+ "<saml2:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">usr</saml2:NameID>"
				+ "</saml2:Subject>"
				+ "<saml2:Conditions NotBefore=\"2012-03-02T13:33:08.864Z\" "
				+ "NotOnOrAfter=\"2012-03-02T14:03:18.864Z\" />"
				+ "<saml2:AttributeStatement>"
				+ "<saml2:Attribute Name=\""
				+ "urn:contrail:names:federation:subject:reputation0\" "
				+ "DataType=\"http://www.w3.org/2001/XMLSchema#integer\">"
				+ // ???
				"<saml2:AttributeValue>7</saml2:AttributeValue>"
				+ "</saml2:Attribute>" + "</saml2:AttributeStatement>"
				+ "</saml2:Assertion>" + "</saml2p:Response>"
				+ "</soap11:Body>" + "</soap11:Envelope>";

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		log.info("{} [KMcC;] fakeSaml() called!");
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(
					xmlString)));
			log.info("{} [KMcC;] fakeSaml() converting!");
			return XMLConvert.toOM(document.getDocumentElement());
		} catch (Exception e) {
			log.error("{} [KMcC;] fakeSaml() EXCEPTION! " + e.getMessage());
			throw new XacmlSamlException(e.getMessage());
		}
	}
}
