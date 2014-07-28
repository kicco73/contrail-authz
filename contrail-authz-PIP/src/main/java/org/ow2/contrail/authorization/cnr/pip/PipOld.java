package org.ow2.contrail.authorization.cnr.pip;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.service.Lifecycle;
//import org.apache.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ow2.contrail.authorization.cnr.pip.db.HibernateUtil;
import org.ow2.contrail.authorization.cnr.pip.db.PipOwner;
import org.ow2.contrail.authorization.cnr.pip.db.SubscriptionsDAO;
import org.ow2.contrail.authorization.cnr.pip.utils.OpenSamlPip;
import org.ow2.contrail.authorization.cnr.pip.utils.UconConstantsPip;
import org.ow2.contrail.authorization.cnr.pip.utils.XacmlSamlPipUtils;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
@Deprecated
public class PipOld implements Lifecycle {
	
	//private static final Logger logger = Logger.getLogger(Pip.class);
    private static Logger log = LoggerFactory.getLogger(PipOld.class);
	private static final String logTag = "[PIP]: ";

	public void init(ServiceContext serviceContext) throws AxisFault {
		log.info("{} service init", logTag);
		
		//these initialization aren't needed (could be done during the first use)
		
		// OpenSaml initialization (axis2 web service singleton)
		try {
			OpenSamlPip.getInstance(serviceContext);
			log.info("{} OpenSaml initialization", logTag);
		} catch (XacmlSamlException e) {
			log.error("{} Initialization failed: Unable to initialize OpenSaml library ({})", logTag, e.getCause().getMessage());
			return;
		}
		// Database initialization (axis2 web service singleton)
		try {
			HibernateUtil.getSessionFactory(serviceContext);
			log.info("{} ");
			
		} catch (SQLException e) {
			log.error("{} Initialization failed: Unable to get a database connection ({})", logTag, e.getCause().getMessage());
			return;
		}

	}

	public void destroy(ServiceContext serviceContext) {
		log.info("{} service destroy", logTag);
	}

	// ///////////////////WEB SERVICE METHOD/////////////////////////////////////
	/**
	 * Ask for subject and object value
	 * 
	 * @param subject
	 * @param object
	 * @return Two XACML concatenated with attribute values (subject and
	 *         resource)
	 */
	public String attributeQuery(String subject, String object) {
		log.info("{} Attribute query received: {}, {}", logTag, subject, object);

		ServiceContext serviceContext = MessageContext.getCurrentMessageContext().getServiceContext();

		// get OpenSaml utils
		XacmlSamlPipUtils utils;
		try {
			utils = OpenSamlPip.getInstance(serviceContext);
		} catch (XacmlSamlException e) {
			log.error("{} attrQuery: open saml utils is null ({})", logTag, e.getCause().getMessage());
			return UconConstantsPip.ERROR_GENERIC_MESSAGE;
		}

		// contact Identity Provider for subject and object (if they are not empty)
		String samlResponseSubject;
		try {
			samlResponseSubject = singleAttributeQuery(utils, subject);
		} catch (XacmlSamlException e) {
			// It should never occur!
			e.printStackTrace();
			log.error("{} error while forming message for {}", logTag, subject);
			return UconConstantsPip.ERROR_GENERIC_MESSAGE;
		} catch (IOException e) {
			if(e instanceof MalformedURLException) {
				log.error("{} ERROR: Malformed URL for Identity Provider: {} - check the configuration file located at {}", 
						logTag, e.getMessage(), UconConstantsPip.configFile);
				return UconConstantsPip.ERROR_IP_URL;				
			} else {
				log.error("{} ERROR: A connection error occurs " + e.getMessage(), logTag);
				return UconConstantsPip.ERROR_IP;
			}
		}

		if (samlResponseSubject.equals(UconConstantsPip.ERROR_MSG))
			return samlResponseSubject;

		log.debug("{} Values for subject {}\n{}", logTag, subject, samlResponseSubject);

		String samlResponseObject = "";//singleAttributeQuery(utils, object);

		if (samlResponseObject.equals(UconConstantsPip.ERROR_MSG))
			return samlResponseObject;

		log.debug("{} Values for object {}\n{}", logTag, object, samlResponseObject);

		// convert the two saml response in a concatenation of xacml
		String response = "";
//		try {
//			response += "";//utils.convertSamlToXacml(samlResponseSubject, Category.SUBJECT);
//		} catch (XacmlSamlException e) {
//			log.error("{} ERROR: An error occur during response creation on string:\n{}", logTag, samlResponseSubject);
//			log.debug(e.getMessage());
//			return UconConstantsPip.ERROR_MSG;
//		}	
			
//		try {
//			response += utils.convertSamlToXacml(samlResponseObject, Category.RESOURCE);
//		} catch (XacmlSamlException e) {
//			log.error("{} ERROR: An error occur during response creation on string:\n{}", logTag, samlResponseObject);
//			log.debug(e.getMessage());
//			return UconConstantsPip.ERROR_MSG;
//		}

		log.debug("{} Values for subject {} and {}", logTag, subject, object);		
		log.debug("{} Respose content:\n{}", logTag, response);
		return response;
	}

	/**
	 * Ask for subject and object value and alert me for future changes
	 * 
	 * @param subject
	 * @param object
	 * @return Two XACML concatenated with attribute values (subject and
	 *         resource)
	 */
	public String attributeQuerySubscribe(String subject, String object) {
		log.info("{} Attribute query received: {}, {}", logTag, subject, object);

		MessageContext messageContext = MessageContext.getCurrentMessageContext();
		ServiceContext serviceContext = messageContext.getServiceContext();

		// get OpenSaml utils
		XacmlSamlPipUtils utils;
		try {
			utils = OpenSamlPip.getInstance(serviceContext);
		} catch (XacmlSamlException e) {
			log.error("{} attrQuery: open saml utils is null ({})", logTag, e.getCause().getMessage());
			return UconConstantsPip.ERROR_GENERIC_MESSAGE;
		}

		// contact Identity Provider for subject and object (if they are not empty)
		String samlResponseSubject = "";//singleAttributeQuery(utils, subject);

		if (samlResponseSubject.equals(UconConstantsPip.ERROR_MSG))
			return samlResponseSubject;

		log.debug("{} Values for subject {}\n{}", logTag, subject, samlResponseSubject);

		String samlResponseObject = "";//singleAttributeQuery(utils, object);

		if (samlResponseObject.equals(UconConstantsPip.ERROR_MSG))
			return samlResponseObject;

		log.debug("{} Values for object {}\n{}", logTag, object, samlResponseObject);

		
		// convert the two saml response in a concatenation of xacml and take a
		// list of (attribute, value) for database
		String response = "";
		List<String[]> attrSub, attrObj;
//		try {
//			response += utils.convertSamlToXacml(samlResponseSubject, Category.SUBJECT);
//			attrSub = null;//utils.getAttributeNameValueList(samlResponseSubject);
//		} catch (XacmlSamlException e) {
//			log.error("{} ERROR: An error occur during response creation on string:\n{}", logTag, samlResponseSubject);
//			log.debug(e.getMessage());
//			return UconConstantsPip.ERROR_MSG;
//		}	
			
//		try {
//			response += utils.convertSamlToXacml(samlResponseObject, Category.RESOURCE);
//			attrObj = null;//utils.getAttributeNameValueList(samlResponseObject);
//		} catch (XacmlSamlException e) {
//			log.error("{} ERROR: An error occur during response creation on string:\n{}", logTag, samlResponseObject);
//			log.debug(e.getMessage());
//			return UconConstantsPip.ERROR_MSG;
//		}


		log.debug("{} Values for subject {} and {}", logTag, subject, object);		
		log.debug("{} Respose content:\n{}", logTag, response);


		// get requester address
		String subscriber = (String) messageContext.getProperty(MessageContext.REMOTE_ADDR); // the actual client addr
		subscriber = "http://" + subscriber + ":8080/axis2/services/UconWs"; // TODO: how to get the port?
		log.debug("{} subscriber address: {}", logTag, subscriber);


		// add to database
		try {
			SubscriptionsDAO db = new SubscriptionsDAO(serviceContext);
//			db.add(PipOwner.newList(subject, attrSub, subscriber, false));
//			db.add(PipOwner.newList(object,  attrObj, subscriber, false));
		} catch (SQLException e) {
			log.error("{} Attibuter query (Subscribe): access db tool is null", logTag);
			log.debug("{} {}", logTag, e.getMessage());
			return UconConstantsPip.ERROR_GENERIC_MESSAGE;
		}

		log.info("{} Attributes added to database", logTag);

		return response;
	}

	//TODO CHECKME FIXME
	
	/**
	 * Stop to alert me for element's attribute changes
	 * 
	 * @param attribute
	 */
	public void attributeUnsubscribe(String element) {
		log.info("{} Attribute query (Unsubscribe) received: "
				+ element);

		MessageContext messageContext = MessageContext
				.getCurrentMessageContext();
		ServiceContext serviceContext = messageContext.getServiceContext();

		// get requester address
		String subscriber = (String) messageContext
				.getProperty(MessageContext.REMOTE_ADDR); // the actual client addr
		subscriber = "http://" + subscriber + ":8080/axis2/services/UconWs"; // TODO:
																				// how  to get the port?
		log.debug("{} subscriber address: " + subscriber);

		// get database access tool
//		AccessDB dbTool = (AccessDB) serviceContext .getProperty(UconConstantsPip.DB_TOOL);
//		if (dbTool == null) {
//			log.error("{} Attibuter query (Subscribe): access db tool is null",logTag);
//		}
//
//		// update database
//		dbTool.remove(element, subscriber);
	}

	/**
	 * New value for attribute
	 * 
	 * @param attribute
	 */
	public void notifyUpdate(String attribute) {// param?

	}

	/**
	 * Update the subscribed attribute that not support automatic refresh
	 * 
	 * @return
	 */
	public String triggeredUpdate() {
		log.info("{} Triggered update received", logTag);
		
		MessageContext messageContext = MessageContext.getCurrentMessageContext();
		ServiceContext serviceContext = messageContext.getServiceContext();
		
		// get OpenSaml utils
		XacmlSamlPipUtils utils;
		try {
			utils = OpenSamlPip.getInstance(serviceContext);
		} catch (XacmlSamlException e) {
			log.error("{} attrQuery: open saml utils is null ({})", logTag, e.getCause().getMessage());
			return UconConstantsPip.ERROR_GENERIC_MESSAGE;
		}
		
		//get database access tool
//		AccessDB dbTool = (AccessDB) serviceContext.getProperty(UconConstantsPip.DB_TOOL);
//		if(dbTool == null) {
//			log.error("{}  Attibuter query (Subscribe): access db tool is null", logTag);	
//		}
		
		//retrieve from database the list of attributes don't support subscription 
//		Collection<ElementAttributesValue> attrToCheck = dbTool.getNotAuto();
		//list of changed attributes

		//for each attribute 
//		for(ElementAttributesValue attr: attrToCheck) {
//			//ask for refresh attributes value
//			String samlResponse = singleAttributeQuery(utils, attr.getElementName());
//			try {
//				List<String[]> attrValue = utils.getAttributeNameValueList(samlResponse);
//			} catch (XacmlSamlException e) {
//				e.printStackTrace();
//			}
//			//
//		}
		
		return null;
	}

	// ///////////////////INTERNAL METHOD/////////////////////////////////////
	private String singleAttributeQuery(XacmlSamlPipUtils utils, String element) throws MalformedURLException, IOException, XacmlSamlException {

		if (element.equals(""))
			return "";

		// create SAML attribute query 
		Object attrQuery = utils.formSAMLAttributeQuery(element);
		String request = "";//utils.formSOAPMessage(attrQuery);

		log.info("{} asking for {}", logTag, element);

		String queryResponse = "";
		int attempt = 0, maxAttempt = 2;
		while (attempt < maxAttempt) {
//			try {
				queryResponse = null;//queryIdentityProvider(request);
				break;
//			} catch (IOException e) {
//				if (++attempt >= maxAttempt) 
//					throw e;
//			}
		}

		if (queryResponse == null || queryResponse.trim().equals("")) {
			log.error("{} ERROR: Invalid response from Identity Provider for the following request:\n{}", logTag, request);
			return UconConstantsPip.ERROR_IP;
		}

		return queryResponse;
	}

}
