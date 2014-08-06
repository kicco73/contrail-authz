package org.ow2.contrail.authorization.cnr.pip;

// KMcC;) Fake Services Class

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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

	   	String s = ""
	   			 +"<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
				 + "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
				 	+ "ID=\"_6296dc07137f221a500be6ab19511fa0\" IssueInstant=\"2012-03-02T13:33:18.864Z\" "
				 	+ "Version=\"2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchemaInstance\">\n"
					+ "<saml:Issuer>cccccccc-1674-bbbb-aaaa-1544f346baaa</saml:Issuer>\n"
					+ "<saml:Subject>\n"
						+"<saml:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">3f7b3dcf-1674-4ecd-92c8-1544f346baf8</saml:NameID>\n"
					+ "</saml:Subject>\n"
					+ "<saml:AttributeStatement>\n"
	   			   		+"<saml:Attribute FriendlyName=\"fooAttrib\" Name=\"issuer\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\">\n"
	   			   			+"<saml:AttributeValue>user101@salesforce.com</saml:AttributeValue>\n"
	   			   		+"</saml:Attribute>\n"
	   			   +"</saml:AttributeStatement>\n"
	   			 +"</saml:Assertion>\n"
	   			 +"</saml2p:Response>";
		
		try {
			log.info("{} [KMcC;] fakeSaml() called!");

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(s)));
			log.info("{} [KMcC;] fakeSaml() message...");
			log.info("{} [KMcC;] fakeSaml() converting! "+document.getDocumentElement());
			return XMLConvert.toOM(document.getDocumentElement());
		} catch (Exception e) {
			log.error("{} [KMcC;] fakeSaml() EXCEPTION! " + e.getMessage());
			throw new XacmlSamlException(e.getMessage());
		}
	}
}
