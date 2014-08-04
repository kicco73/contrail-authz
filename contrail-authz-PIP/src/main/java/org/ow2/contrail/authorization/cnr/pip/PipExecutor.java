package org.ow2.contrail.authorization.cnr.pip;

import java.sql.SQLException;
import java.util.List;

import javax.xml.soap.SOAPException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.pip.db.SubscriptionsDAO;
import org.ow2.contrail.authorization.cnr.pip.utils.UconConstantsPip;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class PipExecutor {

    private static Logger log = LoggerFactory.getLogger(PipExecutor.class);
    private static final String logTag = "[PIP]: ";

    private MessageContext messageContext = null;
    private ServiceContext serviceContext = null;
    private PipCommunicator communicator = null;

    public PipExecutor(MessageContext messageContext) throws XacmlSamlException {
	this.messageContext = messageContext;
	this.serviceContext = messageContext.getServiceContext();
	try {
	    this.communicator = PipCommunicator.getInstance(serviceContext);
	} catch (SOAPException e) {
	    throw new XacmlSamlException(e); // TODO: error message?
	}

    }

    public Element attributeQuery(Element xmlRequest) throws XacmlSamlException {
	// get the requests
    	log.debug("{} [KMcC;)] attributeQuery() - xmlRequest = {}", logTag, xmlRequest);
	List<PipDataEntity> requests = PipDataEntity.getInstanceFromUconRequest(serviceContext, xmlRequest, null);

	// ask for attributes //CHECKME ALWAYS????? TODO it could be optimized (if I had some attributes with auto-update)
	log.debug("{} [KMcC;)] attributeQuery() - calling contactIdentityProvider()", logTag);
	PipDataEntity[] data = contactIdentityProvider(requests);
	// compose the response
	Element response = PipDataEntity.composeXacmlSet(serviceContext, true, data);

	return response;

    }

    public Element addSubscription(Element request) throws XacmlSamlException, SQLException {

	String subscriber = getSenderAddress();
	log.debug("{} subscriber address: {}", logTag, subscriber);

	// get data from requests
	List<PipDataEntity> dataRequests = PipDataEntity.getInstanceFromUconRequest(serviceContext, request, subscriber);

	// get the database access
	SubscriptionsDAO db = new SubscriptionsDAO(serviceContext);

	// update them in database (return the list of not found data)
	List<PipDataEntity> notUpdated = db.insertSubscriptionIfOwnerExist(dataRequests, subscriber);
	// ask them to identity provider
	log.debug("{} [KMcC;)] addSubscription() - calling contactIdentityProvider()", logTag);

	PipDataEntity[] dataGot = contactIdentityProvider(notUpdated);
	// add the new data
	boolean res = db.add(dataGot);

	return getBooleanResponse(res);
    }

    public Element removeSubscription(Element request) throws SQLException, XacmlSamlException {

	// add data to database
	SubscriptionsDAO db = new SubscriptionsDAO(serviceContext);

	String subscriber = getSenderAddress();
	log.debug("{} subscriber address: {}", logTag, subscriber);

	// get data from requests
	List<PipDataEntity> dataRequests = PipDataEntity.getInstanceFromUconRequest(serviceContext, request, subscriber);

	// remove subscription from database ( <owner, subscriber> )
	boolean res = db.remove(dataRequests, subscriber);

	return getBooleanResponse(res);
    }

    public Element triggeredUpdate() throws SQLException, XacmlSamlException, AxisFault {
	// retrieve the list of updated data
	// List<PipDataEntity> updated = manualUpdate();
	log.info("{} performing a manual update", logTag);
	// prepare database
	SubscriptionsDAO db = new SubscriptionsDAO(serviceContext);
	// retrieve the subscription without auto-update
	List<PipDataEntity> toUpdate = db.getNotAuto();

	// get the updated values
	log.debug("{} [KMcC;)] triggeredUpdate() - calling contactIdentityProvider()", logTag);
	PipDataEntity[] latestData = contactIdentityProvider(toUpdate);
	// check if values are equal
	List<PipDataEntity> updated = db.update(toUpdate, latestData);

	// perform the update notification
	performUpdateNotification(updated);

	boolean res = true;

	return getBooleanResponse(res);

    }

    public List<PipDataEntity> receivedUpdate(String msg) {
	// TODO Auto-generated method stub
	// how is this message?
	// how much entities could be updated in the same message?
	// perform the update notification
	// performUpdateNotification(updated);
	return null;
    }

    private Element getBooleanResponse(boolean b) {
	return (b) ? UconConstantsPip.PIP_ATTRIBUTE_SUBSCRIBE_OK : UconConstantsPip.PIP_ATTRIBUTE_SUBSCRIBE_NOT_OK;
    }

    // TODO: parallelize it!!! (preserving the order)
    
    private PipDataEntity[] contactIdentityProvider(List<PipDataEntity> requests) throws XacmlSamlException {
	PipDataEntity[] resultArray = new PipDataEntity[requests.size()];
	int i = 0;
	// for each data ask the value to identity provider
	for (PipDataEntity query : requests) {
		log.debug("{} [KMcC;)] contactIdentityProvider([]) - calling contactIdentityProvider()", logTag);
	    resultArray[i++] = contactIdentityProvider(query);
	}
	// return the data object list
	return resultArray;
    }

    private PipDataEntity contactIdentityProvider(PipDataEntity query) throws XacmlSamlException {
	try {
	    String url = getIdentityProviderUrl(query);
	    Element response = communicator.queryIdentityProvider(url, query.getSamlAttributeQuery());
	    // TODO: auto update: where I can get this value? I know it statically, or Identity Provider could give me?
	    log.debug("{} [KMCc;)] contactIdentityProvider() got response: {}", logTag, response);
	    log.debug("{} [KMCc;)] contactIdentityProvider() serviceContext: {}", logTag, serviceContext);
	    log.debug("{} [KMCc;)] contactIdentityProvider() category: {}", logTag, query.getCategory());
	    log.debug("{} [KMCc;)] contactIdentityProvider() subscriber: {}", logTag, query.getSubscriber());
	    PipDataEntity result = PipDataEntity.getInstanceFromSaml(serviceContext, response, query.getCategory(), query.getSubscriber(), false);
	    return result;
	} catch (SOAPException e) {
	    throw new XacmlSamlException(e); // TODO: error message?
	} catch (Exception e) {
	    throw new XacmlSamlException(e);
	}
    }

    private void performUpdateNotification(List<PipDataEntity> toUpdate) throws XacmlSamlException, AxisFault {
	log.info("{} perform the update notification to the ucon services");

	// compose the message
	Element msg = PipDataEntity.composeXacmlSet(serviceContext, false, (PipDataEntity[]) toUpdate.toArray());

	// send the notification
	communicator.notifyUpdate(msg);
    }

    private String getSenderAddress() {
	// get requester address
	String address = (String) messageContext.getProperty(MessageContext.REMOTE_ADDR); // the actual client address
	return String.format("http://%s:8080/axis2/services/UconWs", address); // TODO: how to get the port?
    }
    
    private String getIdentityProviderUrl(PipDataEntity data) {
	// TODO read the data content and retrieve from a table the url
	return (String) serviceContext.getProperty(UconConstantsPip.IDENTITY_PROVIDER_URL);
    }

}
