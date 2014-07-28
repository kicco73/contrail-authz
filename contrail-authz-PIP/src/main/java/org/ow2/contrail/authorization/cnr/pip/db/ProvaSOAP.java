package org.ow2.contrail.authorization.cnr.pip.db;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class ProvaSOAP {

	private static Logger logger = Logger.getLogger(ProvaSOAP.class);
	
	public static void main(String[] args) throws SOAPException, IOException {
        try {        
            // Create SOAP Connection
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();
		MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            
            
            
        	String prefix = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        	String presoap = "<soap11:Envelope xmlns:soap11=\"http://schemas.xmlsoap.org/soap/envelope/\">\n<soap11:Body>";
        	String postsoap = "</soap11:Body>\n</soap11:Envelope>";
        	String presoap2 = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n<SOAP-ENV:Body>";
        	String postsoap2 = "</SOAP-ENV:Body>\n</SOAP-ENV:Envelope>";
		
        	String msg = "<samlp:AttributeQuery xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"AttrQuery12345789\" " +
				"IssueInstant=\"2009-07-15T15:48:16.421Z\" Version=\"2.0\">\n" +
				"<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">http://somecom.com/SomeJavaRelyingParty</saml:Issuer>\n" +
				"<saml:Subject xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" >\n" +
				"<saml:NameID>contrailuser</saml:NameID>\n" +
				"</saml:Subject>\n</samlp:AttributeQuery>";
		
//		String str = prefix + presoap2 + msg + postsoap2;
		String str = msg;
		
//		OpenSamlPip util = new OpenSamlPip();
//		AttributeQuery a = (AttributeQuery) util.formSAMLAttributeQuery("contrailuser");

		
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
       Document document = builder.parse(new InputSource(new StringReader(str)));
		
        SOAPMessage soapMessage = (SOAPMessage) messageFactory.createMessage();
        SOAPEnvelope env = soapMessage.getSOAPPart().getEnvelope();
        env.addNamespaceDeclaration(SOAPConstants.SOAP_ENV_PREFIX, SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);//"http://schemas.xmlsoap.org/soap/envelope/");
//        System.out.println(env.removeNamespaceDeclaration("env"));
//        soapMessage.saveChanges();
 SOAPBody body = soapMessage.getSOAPBody();
 soapMessage.getSOAPHeader().detachNode();

        
//        MimeHeaders mh = new MimeHeaders();
//        mh.addHeader("Content-Type", "application/soap+xml");
//        SOAPMessage soapMessage2 = (SOAPMessage) messageFactory.createMessage(null, new ByteArrayInputStream(str.getBytes()));
        
//      SOAPBody body2 = soapMessage2.getSOAPPart().getEnvelope().getBody();

 body.addDocument(document);

 
        SOAPMessage toSend = soapMessage;
        
        
        
        
            // Send SOAP Message to SOAP Server
            String url = "http://146.48.81.248:8080/federation-id-prov/saml";
            System.out.print("\n\nSOAP Message to send= \n");
            printSOAPResponse(toSend, new StreamResult(System.out));
            SOAPMessage soapResponse = soapConnection.call(toSend, url);
            // Process the SOAP Response
            System.out.print("\n\nSOAP Message received= \n");
            
            StringWriter stringWriter = new StringWriter();
            printSOAPResponse(soapResponse, new StreamResult(stringWriter));
            
            System.out.println(stringWriter.toString());
            
            String res = stringWriter.toString();
            res = res.replaceAll(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE);
            SOAPMessage soapMessage3 = (SOAPMessage) messageFactory.createMessage(null, new ByteArrayInputStream(res.getBytes()));
//            soapMessage3.getSOAPPart().getEnvelope().addNamespaceDeclaration("env", "http://www.w3.org/2003/05/soap-envelope");
           
            
//            System.out.println("\ndopo:\n"+soapMessage3.getSOAPBody().getNodeValue());
        
//            soapResponse.getSOAPPart().getEnvelope().addNamespaceDeclaration("env", "http://www.w3.org/2003/05/soap-envelope");
//            soapResponse.removeAllAttachments();
            SOAPBody b = soapMessage3.getSOAPBody();
            Document resd = b.extractContentAsDocument();
            Source source = new DOMSource(resd);
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
//            System.out.print("body value:\n"+soapResponse.getSOAPBody().getValue());
           
                       
            StringWriter strres = new StringWriter();
            
            transformer.transform(source, new StreamResult(strres));
            
          System.out.println("\ndopo:\n"+strres);
            
//            
//            Result result = new StreamResult(stringWriter);
//            TransformerFactory tfactory = TransformerFactory.newInstance();
//       Transformer transformer = tfactory.newTransformer();
//       transformer.transform(source, result);
//       String strBody=stringWriter.getBuffer().toString();
//       System.out.println("\nprinted:\n"+b);
            
//        SOAPPart soapPart2 =  soapMessage2.getSOAPPart();
 
//        SOAPPart soapPart =  soapMessage.getSOAPPart();
//        SOAPBody body = soapPart.getEnvelope().getBody();
        
//        body.addDocument(document);
//        body.addTextNode("aaa");
//        Node node = ((Document) soapPart).importNode(document, true);
//        body.appendChild(node);
//        
//        OpenSamlPip util = new OpenSamlPip();
//        Element ele = util.formSAMLAttributeQuery("contrailuser", new ArrayList<String>()).getDOM();
//body.appendChild(ele);
        
        
//       Source sourceContent = document.;
//       System.out.print("\nResponse SOAP Message = ");
//       StreamResult result = new StreamResult(System.out);
//       TransformerFactory transformerFactory = TransformerFactory.newInstance();
//       Transformer transformer = transformerFactory.newTransformer();
//       transformer.transform(sourceContent, result);


            soapConnection.close();
        } catch (Exception e) {
            System.err.println("Error occurred while sending SOAP Request to Server");
            e.printStackTrace();
        }
	}
        
        private static void printSOAPResponse(SOAPMessage soapResponse, StreamResult result) throws Exception {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
//            System.out.print("body value:\n"+soapResponse.getSOAPBody().getValue());
            Source sourceContent = soapResponse.getSOAPPart().getContent();
                       
            
            
            transformer.transform(sourceContent, result);
        }
        
        
	}
	
	
//	public static void main(String[] args) {
//		
//		SubscriptionsDAO sDAO = new SubscriptionsDAO();
//		
//		Subscriptions s = new Subscriptions();
//		s.setName("na");
//		s.setOwner("owner13");
//		s.setValue("value1666");
//		s.setSubscriber("subscriber3");
//		s.setAuto(false);
//		System.out.println("******Adding**************\n"+s);
//		long start = System.currentTimeMillis();
//		int id = sDAO.add(s);
//		
//		System.out.println("******Added "+ id+ " in: "+(System.currentTimeMillis()-start)+" ms");
//		System.out.println("******Finished**************");
//		
//		
////		System.out.println("UPDATE");
////		int i = sDAO.update("owner1", "name4", "newvalue");
////		System.out.println("DONE "+i);
//		
////		System.out.println("READ");
////		Subscriptions s = sDAO.read("owner1", "name4");
////		System.out.println("DONE "+s);
//		
////		System.out.println("LIST");
////		Collection<Subscriptions> list = sDAO.getNotAuto();
////		System.out.println("DONE");
////		for(Subscriptions sub: list)
////			System.out.println(sub);
//		
//		System.out.println("REMOVE");
//		int i = sDAO.remove("owner1", "subscriber1");
//		System.out.println("DONE "+s);
//		
//		logger.fatal("fatal message");
//		logger.info("info message");
//		
//		HibernateUtil.getSessionFactory().close();
//		logger.warn("closed: "+HibernateUtil.getSessionFactory().isClosed());
//	}


