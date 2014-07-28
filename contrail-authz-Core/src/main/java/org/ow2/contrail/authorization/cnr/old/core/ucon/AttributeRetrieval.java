package org.ow2.contrail.authorization.cnr.old.core.ucon;

import java.net.ConnectException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.old.core.utils.XacmlSamlCoreUtilsOLD;
import org.ow2.contrail.authorization.cnr.utils.Communication;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
//import org.ow2.contrail.authorization.cnr.utils.pip.UconConstantsPip;

public class AttributeRetrieval implements Callable<List<UconAttribute>> {

	private String holder;
	private String object;
	private BlockingQueue<ServiceClient> pipServiceClients;
	private XacmlSamlCoreUtilsOLD utils;
	private List<UconAttribute> holderAttributes;
	
	public AttributeRetrieval(String holder, String object, BlockingQueue<ServiceClient> service,
			XacmlSamlCoreUtilsOLD utils, List<UconAttribute> holderAttributes) {
		this.holder = holder;
		this.object = object;
		this.pipServiceClients = service;
		this.utils = utils;
		this.holderAttributes = holderAttributes;
	}
	
	public AttributeRetrieval(String holder, BlockingQueue<ServiceClient> service, XacmlSamlCoreUtilsOLD utils,
			List<UconAttribute> holderAttributes) {
		this(holder, "", service, utils, holderAttributes);

	}
		
	@Override
	public List<UconAttribute> call() throws InterruptedException, ConnectException, XacmlSamlException {
		
		//take a service client from pool
		String response = "";
		ServiceClient sc = null;
		try {
			sc = pipServiceClients.take();
		} catch (InterruptedException e) {
			throw new InterruptedException("An interrupt occur while waiting for a PIP service client");
		}
		
		//do communication with PIP
		OMElement method = Communication.createPayload(UconConstantsCore.PIP_NAMESPACE, "attrQuery", "subject", holder, "object",
					object);		
		try {
			OMElement res = sc.sendReceive(method);
			response = "ATTRIBUTE RETRIEVAL " + res; //i need this
			sc.cleanupTransport();
			response = res.getFirstElement().getText();
//			if ((response).equals(UconConstantsPip.errorIP)) { throw new ConnectException(UconConstantsPip.errorIP); }
//			if ((response).equals(UconConstantsPip.errorIP_URL)) { throw new ConnectException(UconConstantsPip.errorIP); }
//			if ((response).equals(UconConstantsPip.errorMSG)) { throw new XacmlSamlException("Unable to have a response for ('" + holder
//					+ "','" + object + "') request from PIP"); }
			if ((response).equals("")) { throw new ConnectException(""); }
			if ((response).equals("")) { throw new ConnectException(""); }
			if ((response).equals("")) { throw new XacmlSamlException("Unable to have a response for ('" + holder
					+ "','" + object + "') request from PIP"); }
		} catch (ConnectException e) {
			throw e;
		} catch (AxisFault e) {
			throw new ConnectException(e.getMessage());
		} catch (XacmlSamlException e) {
			throw e;
		} finally {
			pipServiceClients.offer(sc);
		}

		//take the list from response
//		List<UconAttribute> list = utils.getAttributeFromPipResponse(response, holder);
		//look for a different attribute value 
		List<UconAttribute> resultList = new LinkedList<UconAttribute>();
//		for (UconAttribute pipAttr : list) {
			for (UconAttribute oldAttr : holderAttributes) {
//				String name = pipAttr.getXacmlId();
//				if (name.equals(oldAttr.getXacmlId())) { //it's the same attribute
//					String val = pipAttr.getValue();
//					if (!val.equals(oldAttr.getValue())) { //if has a different value add to list
						// System.out.println("[AR] Changed value of " + name + ": " + oldAttr.getAttributeKey());
//						UconAttribute newAttr = new UconAttribute(oldAttr.getAttributeKey(), name, oldAttr.getType(), val,
//								oldAttr.getIssuer(), holder, oldAttr.getCategory());
//						resultList.add(newAttr);
//					}
//					break; //skip other attribute
//				}
//			}
		}
		return resultList;
	}

}
