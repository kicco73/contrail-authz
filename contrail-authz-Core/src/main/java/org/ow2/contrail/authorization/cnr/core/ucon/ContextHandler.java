package org.ow2.contrail.authorization.cnr.core.ucon;

import java.sql.SQLException;
import java.util.List;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class ContextHandler {

    private static Logger log = LoggerFactory.getLogger(ContextHandler.class);
    private static final String logTag = "[UCON]: ";

    private MessageContext messageContext = null;
    private ServiceContext serviceContext = null;
    private SessionManager sessionManager = null;

    // private UconPhase phase;

    public ContextHandler(MessageContext messageContext) throws SQLException {
	this.messageContext = messageContext;
	this.serviceContext = messageContext.getServiceContext();
	// create session manager
	this.sessionManager = new SessionManager(serviceContext);
	// this.phase = phase;
    }

    public UconResponse performeTryaccess(Element request) throws InterruptedException, XacmlSamlException, SQLException, AxisFault {
	// create a data object
	UconDataEntity data = UconDataEntity.getInstanceFromSamlXacml(serviceContext, request);

	// perform session evaluation e save on database
	UconResponse response = sessionManager.performEvaluationAndSave(data);

	return response;
    }

    public UconResponse performStartaccess(Element request) throws XacmlSamlException, SQLException, AxisFault {

	// create data reading the request
	UconDataEntity dataRequested = UconDataEntity.getInstanceFromXacmlRetrieveRequest(serviceContext, request);
	// get response
	UconResponse resp = sessionManager.startSessionFromDb(dataRequested);

	return resp;

    }

    public UconResponse performEndaccess(Element request) throws XacmlSamlException, SQLException, AxisFault {

	// create data from read the request
	UconDataEntity dataRequested = UconDataEntity.getInstanceFromXacmlRetrieveRequest(serviceContext, request);
	// get data from database
	UconResponse resp = sessionManager.endSessionFromDb(dataRequested);

	return resp;

    }

    public void handleUpdate(Element updates) throws XacmlSamlException, InterruptedException, SQLException, AxisFault {
	
	// get the updates (as UconDataEntity)
	List<UconDataEntity> in = UconDataEntity.getInstanceFromUpdate(serviceContext, updates);
	// perform the re-evaluation on sessions involved 
	sessionManager.performUpdateAndReevaluation(in);
    }

    public boolean performMapId(String old_id, String new_id) {
	return sessionManager.sessionMapId(old_id, new_id);
    }

}
