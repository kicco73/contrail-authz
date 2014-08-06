package org.ow2.contrail.authorization.cnr.utils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.util.XMLUtils;
import org.opensaml.DefaultBootstrap;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.util.XMLHelper;
import org.opensaml.xml.util.XMLObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLConvert {
	private static Logger log = LoggerFactory.getLogger(XMLConvert.class); // KMcC;)
	private static String logTag = "[XMLConvert]";
	// TODO do better...

	public XMLConvert() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * String -> w3c (Document)
	 */
	public static Document toDocument(String data)
			throws ParserConfigurationException {
		// Create a xml document from string
		Document document;
		try {
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder documentBuilder = documentFactory
					.newDocumentBuilder();
			document = documentBuilder.parse(new InputSource(new StringReader(
					data)));
			return document;
		} catch (SAXException e) {
			throw new ParserConfigurationException(e.getMessage());
		} catch (IOException e) {
			throw new ParserConfigurationException(e.getMessage());
		}
	}

	/**
	 * w3c (Element) -> String
	 */
	public static String toString(Element element) {
		// Document document = element.getOwnerDocument();
		// DOMImplementationLS domImplLS = (DOMImplementationLS)
		// document.getImplementation();
		// LSSerializer serializer = domImplLS.createLSSerializer();
		// String string = serializer.writeToString(element);
		// return string;
		return XMLHelper.prettyPrintXML(element);
	}

	/**
	 * w3c (Document) -> String
	 */
	public static String toString(Document document) {
		return toString(document.getDocumentElement());
	}

	/**
	 * XMLObject -> String Return a string representation of a XMLObject
	 * 
	 * @param obj
	 * @return
	 * @throws XacmlSamlException
	 *             If an error occur (getMessage for more informations)
	 */
	public static String toString(XMLObject obj) throws XacmlSamlException {
		try {
			return XMLHelper.prettyPrintXML(XMLObjectHelper.marshall(obj));
		} catch (MarshallingException e) {
			String message = "Marshalling error: unable to marshall the following obeject\n"
					+ obj;
			throw new XacmlSamlException(message);
		}
		// return XMLHelper.prettyPrintXML(obj.getDOM()); // DOESN?T WORK!!!!!
	}

	/**
	 * String -> w3c (Element)
	 */
	public static Element toDOM(String data)
			throws ParserConfigurationException {
		return toDocument(data).getDocumentElement();
	}

	public static Element toDOM(XMLObject obj) throws XacmlSamlException {
		try {
			return XMLObjectHelper.marshall(obj);
		} catch (MarshallingException e) {
			throw new XacmlSamlException(e);
		}
	}

	/**
	 * Axiom -> w3c
	 */
	public static Element toDOM(OMElement element) throws XacmlSamlException {
		try {
			return XMLUtils.toDOM(element);
		} catch (Exception e) {
			log.error("EEEEK ***** " + e.getMessage());
			throw new XacmlSamlException(
					"Unable to convert XML from Axiom OMElement to w3c Element: "
							+ e.getMessage(), e);
		}
	}

	/**
	 * w3c -> Axiom
	 */
	public static OMElement toOM(Element element) throws XacmlSamlException {
		try {
			return XMLUtils.toOM(element);
		} catch (Exception e) {
			throw new XacmlSamlException(e);
		}
	}

	/**
	 * string -> Axiom
	 */
	public static OMElement toOM(String string) throws XacmlSamlException {
		try {
			return AXIOMUtil.stringToOM(string);
		} catch (XMLStreamException e) {
			throw new XacmlSamlException(e);
		}
	}

	/**
	 * String -> XMLObject Return a XMLObject representation of a XML string
	 * 
	 * @param str
	 * @return
	 * @throws XacmlSamlException
	 *             If an error occur (getMessage for more informations)
	 */
	public static XMLObject toXMLObject(String str) throws XacmlSamlException {
		if (str == null || str.trim().equals("")) {
			throw new XacmlSamlException(
					"Unmarshalling error: blank or null string");
		}
		try {

			BasicParserPool parserPool = new BasicParserPool();
			Reader reader = new StringReader(str.trim());
			parserPool.setNamespaceAware(true);

			return XMLObjectHelper.unmarshallFromReader(parserPool, reader);

		} catch (Exception e) {
			String message = "Unmarshalling error: unable to unmarshall the following input string\n"
					+ str;
			throw new XacmlSamlException(message, e);
		}
	}

	public static XMLObject FIXMEtoXMLObject(Element samlXacml)
			throws XacmlSamlException {
		// FIXME rough implementation: convert to string then back to XMLObject since unmarshalling in
		// opensaml + axis + jax-ws does not work properly.
		log.info("{} ************ [KMcC;)] toXMLObjectFromSOAP(): UGLY CONVERTER CALLED!", logTag);
		XMLObject o = null;
		Source sourceContent = new DOMSource(samlXacml.getOwnerDocument());
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        //PipedReader reader = new PipedReader();
		try {
		    DefaultBootstrap.bootstrap();
	        //PipedWriter writer = new PipedWriter(reader);
			StringWriter writer = new StringWriter();
			transformer = transformerFactory.newTransformer();
			BasicParserPool parserPool = new BasicParserPool();
			parserPool.setNamespaceAware(true);
	        StreamResult result = new StreamResult(writer);
			log.info("{} [KMcC;)] ******* toXMLObjectFromSOAP(): transforming", logTag);
	        transformer.transform(sourceContent, result);
			writer.flush();
	        Reader reader = new StringReader(writer.toString());	       
			o = XMLObjectHelper.unmarshallFromReader(parserPool, reader);
		} catch (TransformerConfigurationException e) {
			String message = "toXmlObject(): can't create transformer: " + e.getMessage();
			log.error("{} [KMcC;)] ******* toXMLObjectFromSOAP(): {}", logTag, message);
			throw new XacmlSamlException(message, e);
		} catch (TransformerException e) {
			String message = "toXmlObject(): can't transform: " + e.getMessage();
			log.error("{} [KMcC;)] ******* toXMLObjectFromSOAP(): {}", logTag, message);
			throw new XacmlSamlException(message, e);
		} catch (XMLParserException e) {
			String message = "toXmlObject(): xml parser exception: " + e.getMessage();
			log.error("{} [KMcC;)] ******* toXMLObjectFromSOAP(): {}", logTag, message);
			throw new XacmlSamlException(message, e);
		} catch (UnmarshallingException e) {
			String message = "toXmlObject(): can't unmarshall: " + e.getMessage();
			log.error("{} [KMcC;)] ******* toXMLObjectFromSOAP(): {}", logTag, message);
			throw new XacmlSamlException(message, e);
		}
		catch(Exception e) {
			String message = "toXmlObject(): unexpected exception: " + e;
			log.error("{} [KMcC;)] ******* toXMLObjectFromSOAP(): {}", logTag, message);
			throw new XacmlSamlException(message, e);		
		}
		log.info("{} [KMcC;)] ******* toXMLObjectFromSOAP(): DONE", logTag);
		return o;
	}

	public static XMLObject toXMLObject(Element samlXacml)
			throws XacmlSamlException {

		// The following is a good implementation, but does not work since axis.saaj + axiom have TODOs 
		log.info("{} [KMcC;)] toXMLObject(): called", logTag);
		try {
			UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
			log.info("{} [KMcC;)] toXMLObject(): unmarshallerFactory {}", logTag, unmarshallerFactory);
			Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(samlXacml);
			log.info("{} [KMcC;)] toXMLObject(): unmarshaller {}", logTag, unmarshaller);
			XMLObject xmlObject = unmarshaller.unmarshall(samlXacml);
			log.info("{} [KMcC;)] toXMLObject(): done {}", logTag, xmlObject);
			return xmlObject;
		} catch  (UnmarshallingException e) {
			log.error("{} [KMcC;)] toXMLObject(): EXCEPTION {}", logTag, e.getMessage());
			throw new XacmlSamlException(e);
		}
	}
}
