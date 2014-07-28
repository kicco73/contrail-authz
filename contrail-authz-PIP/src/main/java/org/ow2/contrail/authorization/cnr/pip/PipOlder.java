package org.ow2.contrail.authorization.cnr.pip;

import static org.ow2.contrail.authorization.cnr.utils.UconConstants.VERBOSE_HIGH;
import static org.ow2.contrail.authorization.cnr.utils.UconConstants.VERBOSE_LOW;
import static org.ow2.contrail.authorization.cnr.utils.UconConstants.VERBOSE_NONE;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.service.Lifecycle;
import org.ow2.contrail.authorization.cnr.pip.utils.OpenSamlPip;
import org.ow2.contrail.authorization.cnr.pip.utils.UconConstantsPip;
import org.ow2.contrail.authorization.cnr.pip.utils.XacmlSamlPipUtils;
import org.ow2.contrail.authorization.cnr.utils.Communication;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
@Deprecated
public class PipOlder implements Lifecycle {

	
	private class SubscriptionSet {
		ConcurrentMap<String, ConcurrentMap<String, Set<String>>> values;

		public SubscriptionSet() {
			values = new ConcurrentHashMap<String, ConcurrentMap<String, Set<String>>>();
		}
		
		public void subscribe(String subject, String attribute, String subscriber) {
			ConcurrentMap<String, Set<String>> attributes = values.get(subject);
			if(attributes == null) {
				attributes = new ConcurrentHashMap<String, Set<String>>();
			}
			Set<String> subscribers = attributes.get(attribute);
			if(subscribers == null) {
				subscribers = Collections.synchronizedSet(new HashSet<String>());
			}
			subscribers.add(subscriber);
			attributes.put(attribute, subscribers);
			values.put(subject, attributes);
		}
		
		public void unSubscribe(String subject, String attribute, String subscriber) {
			if(values.containsKey(subject)) {
				ConcurrentMap<String, Set<String>> attributes = values.get(subject);
				if(attributes.containsKey(attribute)) {
					attributes.get(attribute).remove(subscriber);					
				}
			}
		}
		
		public void empty() {
			for(String sub: values.keySet()) {
				values.get(sub).clear();
			}
			values.clear();
		}
		
		public Set<String> getSubscriber(String subject, String attribute) {
			if(values.containsKey(subject)) {
				ConcurrentMap<String, Set<String>> attributes = values.get(subject);
				if(attributes.containsKey(attribute)) {
					return attributes.get(attribute);					
				}
			}
			return null;
		}
	}
	
	
	
	// private OneResponse rc;
	// VirtualMachinePool vmPool;

	private static String verboseTableEntry = "VERBOSITY";
	private static String SUBSCRIBERS_SET = "nnn";
	@Override
	public void destroy(ServiceContext serviceContext) {
		int verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);
		LOG(verbosity, "service destroy", VERBOSE_NONE);
	}

	@Override
	public void init(ServiceContext serviceContext) throws AxisFault {
		System.out.println("[PIP]: service init");

		readConfigurationFile(serviceContext);
		
		boolean debug = debug(serviceContext);
		int verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);

		if(debug)
			LOG(0, "Debug mode on: "+debug, VERBOSE_NONE);
		
		LOG(0, "Identity Provider address: " + serviceContext.getProperty(UconConstantsPip.IDENTITY_PROVIDER_URL), VERBOSE_NONE);
		// init OpenSaml
		try {
			XacmlSamlPipUtils utils = OpenSamlPip.getInstance(serviceContext);
			LOG(verbosity, "OpenSaml initialization", VERBOSE_LOW);
			serviceContext.setProperty(UconConstants.OPENSAML_UTILS, utils);
		} catch (XacmlSamlException e) {
			LOG(verbosity, " "+e.getMessage(), VERBOSE_NONE);
			LOG(0, "Initialization failed (Unable to initialize OpenSaml library)", VERBOSE_NONE);
			return;
		}
		
		SubscriptionSet subscribers = new SubscriptionSet();
		serviceContext.setProperty(SUBSCRIBERS_SET, subscribers);
				
	}

	public String restart() {
		MessageContext messageContext = MessageContext.getCurrentMessageContext();
		ServiceContext serviceContext = messageContext.getServiceContext();
		serviceContext.setProperty(UconConstantsPip.IDENTITY_PROVIDER_URL, null);
		serviceContext.setProperty(UconConstants.OPENSAML_UTILS, null);
		
		SubscriptionSet sub = (SubscriptionSet) serviceContext.getProperty(SUBSCRIBERS_SET);
		if (sub != null)
			sub.empty();
		serviceContext.setProperty(SUBSCRIBERS_SET, null);
		
		try {
			init(serviceContext);
		} catch (AxisFault e) {
			e.printStackTrace();
		}
		System.gc();
		return "done";
	}

	// //read config file from jar (aar)
	// String configFile = "pipconfig.properties";
	// ClassLoader classLoader = getClass().getClassLoader();
	// Properties properties = new Properties();
	// try {
	// properties.load(classLoader.getResourceAsStream(configFile));
	// } catch (Exception e) {
	// e.printStackTrace();
	// int verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);
	// LOG(verbosity, "doesn't find config file in " + configFile, VERBOSE_NONE);
	// return;
	// }
	private void readConfigurationFile(ServiceContext serviceContext) {
		Properties properties = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(UconConstantsPip.configFile);
			properties.load(in);
		} catch(FileNotFoundException e) {
			LOG(0, " ERROR: unable to find configuration file in " + UconConstantsPip.configFile, VERBOSE_NONE);
			return;
		} catch (IOException e) {
			e.printStackTrace();
			LOG(0, " ERROR: while reading configuration file in " + UconConstantsPip.configFile, VERBOSE_NONE);
			return;
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
				}
		}
		String urlString = properties.getProperty(UconConstantsPip.IDENTITY_PROVIDER_URL);
		if (urlString != null) {
			serviceContext.setProperty(UconConstantsPip.IDENTITY_PROVIDER_URL, urlString);			
		} else
			LOG(0, " ERROR: Identity Provider address not set in configuration file in " + UconConstantsPip.configFile, VERBOSE_NONE);
	}

	private boolean debug(ServiceContext serviceContext) {
		String configFile = System.getProperty("user.home") + "/testmanagerconfig.properties";
		int verbosity = 0;
		String idpAddr = (String) serviceContext.getProperty(UconConstantsPip.IDENTITY_PROVIDER_URL);
		Properties properties = new Properties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(configFile);
			properties.load(in);
			verbosity = Integer.parseInt(properties.getProperty("verbosity"));
			String temp = properties.getProperty(UconConstantsPip.IDENTITY_PROVIDER_URL);
			if(temp != null)
				idpAddr = temp;
		} catch(FileNotFoundException e) {
			return false;
			//It means no debug mode 
		} catch(NullPointerException e) {
			LOG(0, " ERROR: while reading debug configuration file in " + configFile+" (verbosity property isn't set)", VERBOSE_NONE);
			return false;
		} catch(NumberFormatException e) {
			LOG(0, " ERROR: while reading debug configuration file in " + configFile+" (verbosity property is set to "+properties.getProperty("verbosity")+")", VERBOSE_NONE);
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			LOG(0, " ERROR: while reading debug configuration file in " + configFile, VERBOSE_NONE);
			return false;
		}finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
				}
			LOG(verbosity, "verbose set to: " + verbosity, VERBOSE_LOW);
			serviceContext.setProperty(verboseTableEntry, verbosity);
			serviceContext.setProperty(UconConstantsPip.IDENTITY_PROVIDER_URL, idpAddr);
		}
		return true;
	}

	private void LOG(int verbosity, String text, int mode) {
		if (mode <= verbosity) {
			System.out.println("[PIP]: " + text);
		}
	}

	public String attrQuery(String subject, String object) throws XacmlSamlException {

		String result = "";

		MessageContext messageContext = MessageContext.getCurrentMessageContext();
		ServiceContext serviceContext = messageContext.getServiceContext();
		int verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);
		LOG(verbosity, "request received: " + subject + ", " + object, VERBOSE_LOW);

//		LOG(verbosity, "transp addr "+ messageContext.getProperty(MessageContext.TRANSPORT_ADDR), VERBOSE_NONE);
		
		XacmlSamlPipUtils utils = (XacmlSamlPipUtils) serviceContext.getProperty(UconConstants.OPENSAML_UTILS);

		Object attrQuery = utils.formSAMLAttributeQuery(subject);
		String request = null;
//		try {
//			request = utils.formSOAPMessage(attrQuery);
//		} catch (XacmlSamlException e) {
//			// It should never occur!
//			e.printStackTrace();
//		}

		String queryResponse = "";
		int attempt = 0, maxAttempt = 2;
		while (attempt < maxAttempt) {
			try {
				queryResponse = queryIdentityProvider(request);
				break;
			} catch (MalformedURLException e) {
				if (++attempt >= maxAttempt) {
					LOG(verbosity, "ERROR: Malformed URL for Identity Provider: " + e.getMessage() + " - check the configuration file located at " +
							UconConstantsPip.configFile, VERBOSE_NONE);
					return "";//UconConstantsPip.errorIP_URL;
				}
			} catch (IOException e) {
				if (++attempt >= maxAttempt) { 
					LOG(verbosity, "ERROR: A connection error occurs "+e.getMessage(), VERBOSE_NONE);
					return "";//UconConstantsPip.errorIP;
				}
			}
		}
		
		if(queryResponse == null || queryResponse.trim().equals("")) {
			LOG(verbosity, "ERROR: Invalid response from Identity Provider for the following request:\n"+request, VERBOSE_NONE);
			return "";//UconConstantsPip.errorIP;
		}
		
		if (queryResponse.equals(""))//UconConstantsPip.errorMSG))
			return queryResponse;

//		try {
			result = "";//utils.formXACMLResponseMessage(queryResponse);//, "Subject");
//		} catch (XacmlSamlException e) {
//			LOG(verbosity, "ERROR: An error occur during response creation on string:\n"+queryResponse, VERBOSE_NONE);
//			return "";//UconConstantsPip.errorMSG;
//		}
		// TODO: another call for "Object"

		// Client oneClient = new
		// Client("sasha:contrail","http://localhost:2633/RPC2");
		// vmPool = new VirtualMachinePool(oneClient, -2);
		// rc = vmPool.info();
		result += getObjectAttribute(object);
		// System.out.println("[PIP] response forwaded, ok: " + result);
		
		
		//subscription //TODO: take all attributes
		SubscriptionSet sub = (SubscriptionSet) serviceContext.getProperty(SUBSCRIBERS_SET);
		String subscriber = (String) messageContext.getProperty(MessageContext.REMOTE_ADDR); //the actual client addr
		subscriber = "http://"+subscriber+":8080/axis2/services/UconWs"; //TODO: how to get the port?
//		try {
			for(String attribute: new LinkedList<String>()){//utils.getAttributeNameList(queryResponse)) {
				sub.subscribe(subject, attribute, subscriber);
			}
//		} catch (XacmlSamlException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		return result;
	}

	private String getObjectAttribute(String objectID) {
		String issuer = "CNR-PIP";
		boolean fake = false;
		if (fake) {
			String result = "";
			String num_vm_active = "0";
			String attr_id = "urn:contrail:names:vep:resource:num-vm-running";
			result = "<xacml-context:Resource>\n"
					+ create_xacml_attribute(attr_id, UconConstants.XML_INT, num_vm_active, issuer)
					+ "\n</xacml-context:Resource>";
			return result;
		} else {
			// FIXME: what about object?
			String result = "";
			if (!objectID.equals("")) {
				String cloud_num_vm = "0";// Integer.toString(vmPool.getLength());

				int num_vm_running = 0;
				// 3 - if a VM is active
				/*
				 * for ( VirtualMachine vmachine : vmPool){ if (vmachine.state() == 3) num_vm_running++; }
				 */
				String num_vm_active = Integer.toString(num_vm_running);

				String integer = UconConstants.XML_INT;
				
				String attr_id_num_vm = "urn:contrail:names:provider:resource:cloud:num-vm";
				String attr_id_num_vm_run = "urn:contrail:names:provider:resource:cloud:num-vm-running";
				// TODO: use opensaml library, ResourceType object
				// XacmlSamlUtils utils = new XacmlSamlUtils();
				// utils.createXACMLAttribute(uconAttribute)
				result = "<xacml-context:Resource>\n"
						+ create_xacml_attribute(attr_id_num_vm, integer, cloud_num_vm, issuer)
						+ create_xacml_attribute(attr_id_num_vm_run, integer, num_vm_active, issuer) 
						+ "\n</xacml-context:Resource>";
			}
			return result;
		}
	}

	private String create_xacml_attribute(String attributeID, String type, String value, String issuer) {
		String attribute = "";
		attribute = "<xacml-context:Attribute\n" + "AttributeId=\"" + attributeID + "\"\n" + "DataType=\"" + type + "\"\n" + "Issuer=\""
				+ issuer + "\">\n" + "<xacml-context:AttributeValue>" + value + "</xacml-context:AttributeValue>\n"
				+ "</xacml-context:Attribute>";
		return attribute;
	}

	// private String queryIdentityProvider222(String soapRequest) {
	// return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
	// + "<soap11:Envelope xmlns:soap11=\"http://schemas.xmlsoap.org/soap/envelope/\">" + "<soap11:Body>"
	// + "<saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" "
	// + "ID=\"_946d879f37a82f98c54c75495a9a682f\" InResponseTo=\"AttrQuery12345789\" "
	// + "IssueInstant=\"2012-03-02T13:33:18.866Z\" Version=\"2.0\">"
	// + "<saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">CNR-PIP</saml2:Issuer>" + "<saml2p:Status>"
	// + "<saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\" />" + "</saml2p:Status>"
	// + "<saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" "
	// + "ID=\"_6296dc07137f221a500be6ab19511fa0\" IssueInstant=\"2012-03-02T13:33:18.864Z\" " + "Version=\"2.0\">"
	// + "<saml2:Issuer>CNR-PIP</saml2:Issuer>" + "<saml2:Subject>"
	// + "<saml2:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">usr</saml2:NameID>"
	// + "</saml2:Subject>" + "<saml2:Conditions NotBefore=\"2012-03-02T13:33:08.864Z\" "
	// + "NotOnOrAfter=\"2012-03-02T14:03:18.864Z\" />" + "<saml2:AttributeStatement>" +
	// "<saml2:Attribute Name=\"" + "urn:contrail:names:federation:subject:reputation0\" " +
	// "DataType=\"http://www.w3.org/2001/XMLSchema#integer\">" + // ???
	// "<saml2:AttributeValue>7</saml2:AttributeValue>" + "</saml2:Attribute>"
	// + "</saml2:AttributeStatement>" + "</saml2:Assertion>" + "</saml2p:Response>" + "</soap11:Body>" + "</soap11:Envelope>";
	// }

	private String queryIdentityProvider(String soapRequest) throws MalformedURLException, IOException {
		if (soapRequest == null)
			return "";//UconConstantsPip.errorMSG;

		String response = "";

		MessageContext messageContext = MessageContext.getCurrentMessageContext();
		ServiceContext serviceContext = messageContext.getServiceContext();
		int verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);

		URL url = null;
		String urlString = (String) serviceContext.getProperty(UconConstantsPip.IDENTITY_PROVIDER_URL);
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
				throw new IOException("opening connection with " + urlString + " ("+e.getMessage()+")");
			}

			LOG(verbosity, "Connected to Identity Provider", VERBOSE_HIGH);

			String data = soapRequest;
			connection.setUseCaches(false);
			connection.setDoOutput(true);
			connection.setDoInput(true);
			try {
				connection.setRequestMethod("POST");
			} catch (ProtocolException e) {
			}
			connection.setRequestProperty("Content-Type", "application/soap+xml");

			DataOutputStream wr = null;
			try {
				wr = new DataOutputStream(connection.getOutputStream());
				wr.writeBytes(data + "\r\n");
				wr.flush();
			} catch (IOException e) {				
				throw new IOException("writing to " + urlString + " ("+e.getMessage()+")", e);
			}

			if(connection.getResponseCode() == 200) {
				BufferedReader read = null;
				try {
					read = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					String line = read.readLine();
					//LOG(verbosity, "line:" + line, VERBOSE_HIGH);
					while (line != null) {
						response += line + "\n";
						line = read.readLine();
						//LOG(verbosity, "line:" + line, VERBOSE_HIGH);
					}
				} catch (IOException e) {
					e.printStackTrace();
					throw new IOException("reading from " + urlString + " (" + e.getMessage() + ")", e);
				}
			} else {
				throw new IOException("due response code: "+connection.getResponseCode() + " " + connection.getResponseMessage()+
						".\nThis is the request sent to Identity Provider:\n"+soapRequest);
				
			}
		} catch (IOException e) {
			throw e;
		} finally {
			connection.disconnect();
		}
		LOG(verbosity, "respose content:\n" + response, VERBOSE_HIGH);
		return response;
	}
	
	/**
	 * Receive a notification of updating
	 * @param subject
	 * 		The attribute owner
	 * @param atribute
	 * 		The attribute updated
	 * @param value
	 * 		The new value
	 */
	public void notifyUpdate(String subject, String attribute, String value) {
		MessageContext messageContext = MessageContext.getCurrentMessageContext();
		ServiceContext serviceContext = messageContext.getServiceContext();
		int verbosity = (Integer) serviceContext.getProperty(verboseTableEntry);
		
		//form the message		
		String message = 
				"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + 
				"  <soap11:Envelope xmlns:soap11=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" + 
				"    <soap11:Body>\n"	+ 
				"      <saml2p:Response xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" \n" + 
				"        ID=\"_946d879f37a82f98c54c75495a9a682f\" InResponseTo=\"AttrQuery12345789\" \n" +
				"        IssueInstant=\"2012-03-02T13:33:18.866Z\" Version=\"2.0\">\n" +
				"      <saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">CNR-PIP</saml2:Issuer>\n" + 
				"      <saml2p:Status>\n" +
				"        <saml2p:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\" />\n" + 
				"      </saml2p:Status>\n" + 
				"      <saml2:Assertion xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" \n" +
				"        ID=\"_6296dc07137f221a500be6ab19511fa0\" IssueInstant=\"2012-03-02T13:33:18.864Z\" Version=\"2.0\">\n" +
				"        <saml2:Issuer>CNR-PIP</saml2:Issuer>\n" + 
				"        <saml2:Subject>\n" +
				"          <saml2:NameID Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">" + subject + "</saml2:NameID>\n" +
				"        </saml2:Subject>\n" + 
				"        <saml2:Conditions NotBefore=\"2012-03-02T13:33:08.864Z\" NotOnOrAfter=\"2012-03-02T14:03:18.864Z\" />\n" + 
				"        <saml2:AttributeStatement>\n" +
				"          <saml2:Attribute Name=\"" + attribute + "\" " + "DataType=\"http://www.w3.org/2001/XMLSchema#integer\">\n" + // ???
				"            <saml2:AttributeValue>" + value + "</saml2:AttributeValue>\n" + 
				"          </saml2:Attribute>\n" +
				"        </saml2:AttributeStatement>\n" +
				"      </saml2:Assertion>\n" + 
				"    </saml2p:Response>\n" + 
				"  </soap11:Body>\n" + 
				"</soap11:Envelope>\n";
	
		System.out.println("notify upadate for "+subject+" "+attribute+" "+value);
		
		XacmlSamlPipUtils utils = (XacmlSamlPipUtils) serviceContext.getProperty(UconConstants.OPENSAML_UTILS);
		String xacmlMessage = "";
//		try {
//			//xacmlMessage = utils.formXACMLResponseMessage(message);
//			xacmlMessage = utils.formXACMLUpdateMessage(message);
//		} catch (XacmlSamlException e) {
//			LOG(verbosity, "ERROR: An error occur during notify update message creation on string:\n"+message, VERBOSE_NONE);
//			return;
//		}
		
//		System.out.println(message);
//		//send to all subscribers found in table		
		SubscriptionSet sub = (SubscriptionSet) serviceContext.getProperty(SUBSCRIBERS_SET);
		
		Set<String> subscribers = sub.getSubscriber(subject, attribute);
		try {
			ServiceClient sc = new ServiceClient();
			for(String subscriber: subscribers) {
				System.out.println("notify upadate to subscriber "+subscriber);
				Communication.sendReceive(sc, subscriber, UconConstants.UCON_NAMESPACE, "subscription", "message", xacmlMessage);
			}
		} catch (AxisFault e) {
			e.printStackTrace();
		}

	}
	
}
