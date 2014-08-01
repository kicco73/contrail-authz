package org.ow2.contrail.authorization.cnr.pip;

// KMcC;) Fake Services Class

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.service.Lifecycle;
import org.ow2.contrail.authorization.cnr.utils.XMLConvert;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class Fake implements Lifecycle {

	private static Logger log = LoggerFactory.getLogger(Fake.class);
	private static final String logTag = "[FAKE]: ";

	@Override
	public void init(ServiceContext serviceContext) throws AxisFault {
		log.info("{} initialization completed", logTag);
	}

	@Override
	public void destroy(ServiceContext serviceContext) {
		// TODO Auto-generated method stub

	}
	
    private static void printSOAPResponse(SOAPMessage soapResponse, StreamResult result) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
//        System.out.print("body value:\n"+soapResponse.getSOAPBody().getValue());
        Source sourceContent = soapResponse.getSOAPPart().getContent();
        transformer.transform(sourceContent, result);
    }

	// [KMcC;]
	public OMElement fakeSaml(OMElement request) throws XacmlSamlException {

	   	String s = "<?xml version='1.0' encoding='utf-8'?>"
	   			   + "<env:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\">"
	   			   + "<env:Body>"
	   			   + "<saml:AttributeStatement xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
	   			   +"<saml:Attribute FriendlyName=\"fooAttrib\" Name=\"SFDC_USERNAME\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\">"
	   			   +"<saml:AttributeValue xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xs:string\">"
	   			   +"user101@salesforce.com"
	   			   +"</saml:AttributeValue>"
	   			   +"</saml:Attribute>"
	   			   +"</saml:AttributeStatement>"
	   			   +"</env:Body>"
	   			   +"</env:Envelope>";
	   	
		String xmlString2 = "<?xml version='1.0' encoding='utf-8'?>"
			   			   + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
+"<soapenv:Body>"
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
				+ "</saml2:Assertion>" 
				+ "</saml2p:Response>"
				+"</soapenv:Body>"
	   			   +"</soapenv:Envelope>";
	   	
	  
		String xmlString =

				"<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" "
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
				+ "</saml2:Assertion>" + "</saml2p:Response>";

		
		try {
			log.info("{} [KMcC;] fakeSaml() called!");

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(xmlString)));
			MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
			SOAPMessage soapMessage = (SOAPMessage) messageFactory.createMessage();
			SOAPEnvelope env = soapMessage.getSOAPPart().getEnvelope();
			env.addNamespaceDeclaration(SOAPConstants.SOAP_ENV_PREFIX, SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);//"http://schemas.xmlsoap.org/soap/envelope/");	
			SOAPBody body = soapMessage.getSOAPBody();
			soapMessage.getSOAPHeader().detachNode();
			body.addDocument(document);
			log.info("{} [KMcC;] fakeSaml() message...");
			//printSOAPResponse(soapMessage, new StreamResult(System.out));
		
			log.info("{} [KMcC;] fakeSaml() converting! "+soapMessage.getSOAPPart().getDocumentElement());
			return XMLConvert.toOM(soapMessage.getSOAPPart().getDocumentElement());
		} catch (Exception e) {
			log.error("{} [KMcC;] fakeSaml() EXCEPTION! " + e.getMessage());
			throw new XacmlSamlException(e.getMessage());
		}
	}
}
