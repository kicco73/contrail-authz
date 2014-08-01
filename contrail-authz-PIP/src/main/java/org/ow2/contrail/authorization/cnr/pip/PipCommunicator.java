package org.ow2.contrail.authorization.cnr.pip;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.pip.utils.UconConstantsPip;
import org.ow2.contrail.authorization.cnr.utils.XMLConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class PipCommunicator {

    private static Logger log = LoggerFactory.getLogger(PipCommunicator.class);
    private static final String logTag = "[PIP_communicator]: ";
    
    private static MessageFactory messageFactory;
    private static DocumentBuilderFactory documentFactory;
    private static SOAPConnectionFactory soapConnectionFactory;
    private static TransformerFactory transformerFactory;

    private PipCommunicator() throws SOAPException {
	messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
	documentFactory = DocumentBuilderFactory.newInstance();
	documentFactory.setNamespaceAware(true);
	soapConnectionFactory = SOAPConnectionFactory.newInstance();
	transformerFactory = TransformerFactory.newInstance();
    }

    public static void init(ServiceContext serviceContext) throws SOAPException {
	Object obj = serviceContext.getProperty(UconConstantsPip.PIP_COMMUNICATOR);
	if (obj == null) {
	    PipCommunicator pc = new PipCommunicator();
	    serviceContext.setProperty(UconConstantsPip.PIP_COMMUNICATOR, pc);
	}
    }

    public static PipCommunicator getInstance(ServiceContext serviceContext) throws SOAPException {
	Object obj = serviceContext.getProperty(UconConstantsPip.PIP_COMMUNICATOR);
	if (obj != null) {
	    return (PipCommunicator) obj;
	} else {
	    PipCommunicator pc = new PipCommunicator();
	    serviceContext.setProperty(UconConstantsPip.PIP_COMMUNICATOR, pc);
	    return pc;
	}
    }

    public void notifyUpdate(Element msg) throws AxisFault {
	// TODO
	// ServiceClient sc = new ServiceClient();
	// // synchSc.engageModule("addressing");
	// Options synchOpts = new Options();
	// // setting target EPR
	// synchOpts.setTo(new EndpointReference("http://localhost:8080/axis2/services/UconWs"));
	// // synchOpts.setTo(new EndpointReference("http://146.48.96.76:8080/axis2/services/UconWs"));
	// String actionName = "reevaluation";
	// synchOpts.setAction("urn:" + actionName);
	// // setting synchronous invocation
	// synchOpts.setUseSeparateListener(false);
	// synchOpts.setCallTransportCleanup(true); // ?
	// // setting created option into service client
	// sc.setOptions(synchOpts);
	// sc.fireAndForget(Communication.createPayload(UconConstants.UCON_NAMESPACE, actionName));
	// sc.cleanupTransport();

    }

    /**
     * 
     * @param urlString
     * @param data
     * @return
     * @throws SOAPException
     * @throws TransformerException
     */
    public Element queryIdentityProvider(String urlString, Element data) throws SOAPException {

	if(log.isDebugEnabled()) {
	    log.debug("{} query to identity provider {}:\n{}", logTag, urlString, XMLConvert.toString(data));
	}
	
	// Create SOAP message
	log.debug("{} CREATE SOAP MESSAGE {}:\n{}", logTag, "[KMcC;)]");
	SOAPMessage msg = createSOAPMessage(data);
	log.debug("{} compose SOAP message", logTag);
	// Create SOAP connection
	SOAPConnection soapConnection = soapConnectionFactory.createConnection();
	log.debug("{} connection SOAP created", logTag);
	// Send to SOAP server
	SOAPMessage soapResponse = soapConnection.call(msg, urlString);
	log.debug("{} SOAP message sent", logTag);
	// Extract SOAP part from response
	Element response = extractContentFromSOAPMessage(soapResponse);
	// Close SOAP connection
	soapConnection.close();
	
	if(log.isDebugEnabled()) {
	    log.debug("{} query to identity provider response:\n{}", logTag, XMLConvert.toString(response));
	}
	
	return response;
    }

    private SOAPMessage createSOAPMessage(Element data) throws SOAPException {

	// Create SOAP message
   	log.debug("[KMcC;)] createSOAPMessage(): creating" );
	SOAPMessage soapMessage = (SOAPMessage) messageFactory.createMessage();
	// TODO: Identity Provider uses Soap 1.1 but it requests Soap 1.2 Content Type ("application/soap+xml")
	SOAPEnvelope env = soapMessage.getSOAPPart().getEnvelope();
	env.addNamespaceDeclaration(SOAPConstants.SOAP_ENV_PREFIX, SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);
	// remove header from soap message
   	log.debug("[KMcC;)] createSOAPMessage(): detaching node" );
	soapMessage.getSOAPHeader().detachNode();

	// add xml document to soap body
   	log.debug("[KMcC;)] createSOAPMessage(): adding XML: "+data.toString());
   	Node n = soapMessage.getSOAPPart().importNode(data, true);	// WrongDocument bug fixed -- KMcC;)
	soapMessage.getSOAPBody().appendChild(n);
   	log.debug("[KMcC;)] createSOAPMessage(): DONE" );
	return soapMessage;
    }

    
    private String messageToString(SOAPMessage soapMessage) throws SOAPException {
    	// Transform SOAP message in string format
    	StringWriter stringWriter = new StringWriter();
    	try {
        	Source sourceContent = soapMessage.getSOAPPart().getContent();
    		Transformer transformer = transformerFactory.newTransformer();
    	    transformer.transform(sourceContent, new StreamResult(stringWriter));
    	} catch (TransformerException e) {
    	   	log.error("{} [KMcC;)] extractContentFromSOAPMessage() exception 1! {}", logTag, e.getMessage());
    	    throw new SOAPException(e);
    	}
    	return stringWriter.toString();
    }
    
    private Element extractContentFromSOAPMessage(SOAPMessage soapMessage) throws SOAPException {

	// TODO: Identity Provider replies using Soap 1.1 namespace even if it use Soap 1.2 protocol
	// Replacing namespace only in string format
	// Where to put the temporary string
   	log.info("{} [KMcC;)] extractContentFromSOAPMessage() called", logTag);
   	log.info("{} [KMcC;)] extractContentFromSOAPMessage() received: {}", logTag, messageToString(soapMessage));

	Document resd = soapMessage.getSOAPBody().extractContentAsDocument();

	log.info("{} [KMcC;)] extractContentFromSOAPMessage(): extractContentAsDocument: {}", logTag, resd);

	Element rv =  (Element)resd.getFirstChild();
	// // Where to put result
	// StringWriter msg = new StringWriter();
	// // Transform SOAP message in string format (again)
	// try {
	// transformer.transform(new DOMSource(resd), new StreamResult(msg));
	// } catch (TransformerException e) {
	// throw new SOAPException(e);
	// }
	// // Return the string
	// return msg.toString();
   	log.info("{} [KMcC;)] extractContentFromSOAPMessage() finished. rv = {}", logTag, rv);

	return rv;
    }
 
    @Deprecated
    public String queryIdentityProviderSocket(String urlString, String data) throws IOException {
	if (data == null)
	    return UconConstantsPip.ERROR_MSG;

	String response = "";

	log.debug("{} try to connect to {}", logTag, urlString);

	URL url = null;
	try {
	    url = new URL(urlString);
	} catch (MalformedURLException e) {
	    throw new MalformedURLException(urlString);
	}

	HttpURLConnection connection = null;
	try {
	    try {
		connection = (HttpURLConnection) url.openConnection();
	    } catch (IOException e) {
		throw new IOException("opening connection with " + urlString);
	    }

	    log.debug("{} Connected to Identity Provider", logTag);

	    connection.setUseCaches(false);
	    connection.setDoOutput(true);
	    connection.setDoInput(true);
	    try {
		connection.setRequestMethod("POST");
	    } catch (ProtocolException e) {
		throw new IOException("setting request method to POST (" + e.getMessage() + ")", e);
	    }
	    connection.setRequestProperty("Content-Type", "application/soap+xml");

	    DataOutputStream wr = null;
	    try {
		wr = new DataOutputStream(connection.getOutputStream());
		wr.writeBytes(data + "\r\n");
		wr.flush();
	    } catch (IOException e) {
		throw new IOException("writing to " + urlString + " (" + e.getMessage() + ")", e);
	    }

	    if (connection.getResponseCode() == 200) {
		BufferedReader read = null;
		try {
		    read = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		    String line = read.readLine();
		    // log.trace(loggerTag+"line: " + line);
		    while (line != null) {
			response += line + "\n";
			line = read.readLine();
			// log.trace(loggerTag+"line: " + line);
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		    throw new IOException("reading from " + urlString + " (" + e.getMessage() + ")", e);
		}
	    } else {
		throw new IOException("due response code: " + connection.getResponseCode() + " " + connection.getResponseMessage()
			+ ".\nThis is the request sent to Identity Provider:\n" + data);
	    }
	} catch (IOException e) {
	    throw e;
	} finally {
	    connection.disconnect();
	}
	log.debug("{} respose content:\n" + response);
	return response;
    }

}
