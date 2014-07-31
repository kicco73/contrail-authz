package org.ow2.contrail.authorization.cnr.utils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.util.XMLUtils;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.util.XMLHelper;
import org.opensaml.xml.util.XMLObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLConvert {
	private static Logger log = LoggerFactory.getLogger(XMLConvert.class);	// KMcC;)
    // TODO do better...

    public XMLConvert() {
	// TODO Auto-generated constructor stub
    }

    /**
     * String -> w3c (Document)
     */
    public static Document toDocument(String data) throws ParserConfigurationException {
	// Create a xml document from string
	Document document;
	try {
	    DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
	    document = documentBuilder.parse(new InputSource(new StringReader(data)));
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
	// DOMImplementationLS domImplLS = (DOMImplementationLS) document.getImplementation();
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
	    String message = "Marshalling error: unable to marshall the following obeject\n" + obj;
	    throw new XacmlSamlException(message);
	}
	// return XMLHelper.prettyPrintXML(obj.getDOM()); // DOESN?T WORK!!!!!
    }

    /**
     * String -> w3c (Element)
     */
    public static Element toDOM(String data) throws ParserConfigurationException {
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
		log.error("EEEEK ***** "+e.getMessage());
	    throw new XacmlSamlException("Unable to convert XML from Axiom OMElement to w3c Element: " + e.getMessage(), e);
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
	    throw new XacmlSamlException("Unmarshalling error: blank or null string");
	}
	try {

	    BasicParserPool parserPool = new BasicParserPool();
	    Reader reader = new StringReader(str.trim());
	    return XMLObjectHelper.unmarshallFromReader(parserPool, reader);

	} catch (Exception e) {
	    String message = "Unmarshalling error: unable to unmarshall the following input string\n" + str;
	    throw new XacmlSamlException(message, e);
	}
    }

    public static XMLObject toXMLObject(Element samlXacml) throws XacmlSamlException {
	try {
	    return Configuration.getUnmarshallerFactory().getUnmarshaller(samlXacml).unmarshall(samlXacml);
	} catch (UnmarshallingException e) {
	    throw new XacmlSamlException(e);
	} 
    }

}
