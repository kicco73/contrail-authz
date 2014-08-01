package org.ow2.contrail.authorization.cnr.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import org.apache.axis2.context.ServiceContext;
import org.apache.log4j.Logger;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.xacml.XACMLConstants;
import org.opensaml.xacml.ctx.AttributeType;
import org.opensaml.xacml.ctx.AttributeValueType;
import org.opensaml.xacml.ctx.DecisionType;
import org.opensaml.xacml.ctx.ResponseType;
import org.opensaml.xacml.ctx.ResultType;
import org.opensaml.xacml.ctx.impl.AttributeTypeImplBuilder;
import org.opensaml.xacml.ctx.impl.AttributeValueTypeImplBuilder;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.XMLParserException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class OpenSamlUtils {

    private static Logger logger = Logger.getLogger(OpenSamlUtils.class);
    private static OpenSamlUtils utils = null;

    protected XMLObjectBuilderFactory builderFactory = null;
    protected AttributeTypeImplBuilder attributeBuilder = null;
    protected AttributeValueTypeImplBuilder attributeValueBuilder = null;
    protected ServiceContext serviceContext = null;

    /**
     * Constructor for singleton pattern
     * 
     * @throws XacmlSamlException
     *             If an error occurs during bootstrap (getMessage for more informations)
     */
    protected OpenSamlUtils(ServiceContext serviceContext) throws XacmlSamlException {
	this.builderFactory = createBuilderFactory();
	this.attributeBuilder = (AttributeTypeImplBuilder) builderFactory.getBuilder(AttributeType.DEFAULT_ELEMENT_NAME);
	this.attributeValueBuilder = (AttributeValueTypeImplBuilder) builderFactory.getBuilder(AttributeValueType.DEFAULT_ELEMENT_NAME);
	this.serviceContext = serviceContext;
    }

    /**
     * Method for singleton pattern (adapted for web service), callable only from a sub class
     * 
     * @param serviceContext
     *            The service context in which save/get the singleton instance
     * @param classType
     *            The subclass type to instantiate
     * @return An instance of subclass of a OpenSamlUtils
     * @throws XacmlSamlException
     *             If an error occurs during bootstrap (getMessage for more informations)
     * @throws NoSuchMethodException
     *             If a constructor is not found.
     * @throws IllegalArgumentException
     *             If the no parameters constructors doesn't exist
     * @throws IllegalAccessException
     *             If a security manager is present and deny the operation
     * @throws InstantiationException
     *             If the class that declares the underlying constructor represents an abstract class.
     * @throws InvocationTargetException
     *             If an exception occur in classType constructor
     */
    protected static OpenSamlUtils getInstanceBase(ServiceContext serviceContext, Class<? extends OpenSamlUtils> classType)
	    throws XacmlSamlException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException,
	    InvocationTargetException {
	if (classType == null) {
		// KMcC;) 
		logger.error("[OpenSamlUtils] [KMcC;)] getInstanceBase(): NULL POINTER EXCEPTION!");
		throw new NullPointerException();
	}

	if (serviceContext == null) {
	    if (utils == null) {
		logger.debug("Creating a new OpenSamlUtils (static)");
		utils = newSuperInstance(serviceContext, classType);
	    } else {
		logger.debug("Getting an existing OpenSamlUtils (static)");
	    }
	    return utils;
	} else {
	    Object obj = serviceContext.getProperty(UconConstants.OPENSAML_UTILS);
	    if (obj != null) {
		logger.debug("Getting an existing OpenSamlUtils (from Axis2 Service Context)");
		return (OpenSamlUtils) obj;
	    } else {
		OpenSamlUtils utils = newSuperInstance(serviceContext, classType);
		serviceContext.setProperty(UconConstants.OPENSAML_UTILS, utils);
		return utils;
	    }
	}
    }

    // Instantiate an OpenSamlUtils extending object by reflection
    private static OpenSamlUtils newSuperInstance(ServiceContext serviceContext, Class<? extends OpenSamlUtils> classType)
	    throws InstantiationException, IllegalAccessException, IllegalArgumentException, NoSuchMethodException, SecurityException,
	    XacmlSamlException, InvocationTargetException {
	Constructor<? extends OpenSamlUtils> constr = classType.getDeclaredConstructor(ServiceContext.class);
	constr.setAccessible(true);
	try {
	    return constr.newInstance(serviceContext);
	} catch (InvocationTargetException e) {
	    if (e.getCause() instanceof XacmlSamlException) {
		throw (XacmlSamlException) e.getCause();
	    }
	    throw e;
	}
    }

    // private method to initialize the class
    private XMLObjectBuilderFactory createBuilderFactory() throws XacmlSamlException {
	try {
	    DefaultBootstrap.bootstrap();
	} catch (ConfigurationException e) {
	    throw new XacmlSamlException("There is a problem initializating OpenSaml library (message: " + e.getMessage() + ")");
	}
	return Configuration.getBuilderFactory();
    }

    /**
     * the method has been moved to XMLConvert
     */
    @Deprecated
    protected String marshalling(XMLObject obj) throws XacmlSamlException {
	// the method has been moved to XMLConvert
	return null;
    }

    /**
     * the method has been moved to XMLConvert
     */
    @Deprecated
    protected XMLObject unmarshalling(String str) throws XacmlSamlException {
	// the method has been moved to XMLConvert
	return null;
    }

    /**
     * Parse a response got by PDP
     * 
     * @param response
     *            XACML response
     * @return
     * @throws UnmarshallingException
     * @throws XMLParserException
     */
    protected boolean getAccessDecision(ResponseType response) {
	ResultType result = response.getResult();
	DecisionType decision = result.getDecision();
	boolean value = false;
	switch (decision.getDecision()) { // don't we care about the other decisions?
	case Deny:
	case Indeterminate:
	case NotApplicable:
	    value = false;
	    break;
	case Permit:
	    value = true;
	    break;
	}
	return value;
    }

    // convert OBJECT -> XACML
    protected AttributeType convertXacmlAttributeToAttributeType(XacmlAttribute attribute) {
	AttributeType xacmlAttribute = attributeBuilder.buildObject(XACMLConstants.XACML20CTX_NS, AttributeType.DEFAULT_ELEMENT_LOCAL_NAME,
		XACMLConstants.XACMLCONTEXT_PREFIX);

	xacmlAttribute.setIssuer(attribute.getIssuer());
	xacmlAttribute.setAttributeID(attribute.getXacml_id());
	xacmlAttribute.setDataType(attribute.getType());

	AttributeValueType subjectValue = attributeValueBuilder.buildObject(AttributeValueType.DEFAULT_ELEMENT_NAME);
	subjectValue.setValue(attribute.getValue());
	xacmlAttribute.getAttributeValues().add(subjectValue);
	return xacmlAttribute;
    }

    // convert XACML -> OBJECT
    protected XacmlAttribute convertAttributeTypeToXacmlAttribute(XacmlAttribute dest, AttributeType source) {
	dest.setXacml_id(source.getAttributeID());
	dest.setIssuer(source.getIssuer());
	dest.setType(source.getDataType());
	if (!source.getAttributeValues().isEmpty()) {
	    dest.setValue(source.getAttributeValues().get(0).getValue());
	} else {
	    dest.setValue(null);
	}
	return dest;
    }

    // convert XML -> List<XML>
    protected List<Element> separeGenericRequests(Element request) throws XacmlSamlException {
	List<Element> requestes = new LinkedList<Element>();
	NodeList list = request.getChildNodes();
	for (int i = 0; i < list.getLength(); i++) {
	    Node n = list.item(i);
	    if (n.getNodeType() == Node.ELEMENT_NODE) {
		requestes.add((Element) n);
	    }
	}
	return requestes;
    }
}
