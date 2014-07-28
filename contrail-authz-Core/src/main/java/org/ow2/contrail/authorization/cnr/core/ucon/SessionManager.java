package org.ow2.contrail.authorization.cnr.core.ucon;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ServiceContext;
import org.ow2.contrail.authorization.cnr.core.db.SessionDAO;
import org.ow2.contrail.authorization.cnr.core.pdp.PdpModule;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.ow2.contrail.authorization.cnr.utils.XMLConvert;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.w3c.dom.Element;

public class SessionManager {

    private ServiceContext serviceContext = null;
    private SessionDAO db = null;
    private UconCommunicator communicator = null;

    public SessionManager(ServiceContext serviceContext) throws SQLException {
	this.communicator = UconCommunicator.getInstance(serviceContext);
	this.serviceContext = serviceContext;
	this.db = new SessionDAO(serviceContext);
    }

    public UconResponse performEvaluationAndSave(UconDataEntity data) throws InterruptedException, XacmlSamlException, SQLException, AxisFault {

	// TODO: check if data exists and if it's already subscribed (if it is, but just one)?
	// boolean subscribed = sm.check;

	// ask for attributes to pip
	Element pipAttr = communicator.attributesQuery(data.getAttributeQueryRequest());

	System.out.println("pip response:\n"+XMLConvert.toString(pipAttr));
	
	// set attributes to data object
	data.setAttributesXml(pipAttr);
	
	// perform the evaluation
	String eval = PdpModule.evaluate(serviceContext, CorePhase.PRE, data.getFinalXacmlString());
	// create the response object
	UconResponse resp = UconResponse.getUconResponse(serviceContext, eval);
	if (resp.getBooleanResponse()) {
	    // if it's true, save data on database
	    boolean b = db.saveNewSessionAndHolders(data);
	}
	return resp;
    }

    /**
     * Start an existing session on DB (retrieved by id)
     * @param dataRequested
     * 		the session to start
     * @return
     * 		the holders to subscribe
     * @throws SQLException
     * @throws XacmlSamlException
     * @throws AxisFault 
     */
    public UconResponse startSessionFromDb(UconDataEntity dataRequested) throws SQLException, XacmlSamlException, AxisFault {
	
	// set new status on database
	UconDataEntity toSubscribe = db.setStatusById(dataRequested, CorePhase.ON);
	
	// send the subscription request
	communicator.subscribe(toSubscribe.getSubscriptionRequest());
	
	// set holders as subscribed
	//TODO
	
	// return true
	UconResponse resp = UconResponse.TRUE;
	return resp;
    }

    /**
     * End an existing session on DB (retrieved by id)
     * @param dataRequested
     * 		the session to end
     * @return
     * 		the holders to unsubscribe
     * @throws SQLException
     * @throws XacmlSamlException
     * @throws AxisFault 
     */
    public UconResponse endSessionFromDb(UconDataEntity dataRequested) throws SQLException, XacmlSamlException, AxisFault {
	
	// set new status on database
	UconDataEntity toUnsubscribe = db.setStatusById(dataRequested, CorePhase.POST);
	// send the subscription request
	communicator.unsubscribe(toUnsubscribe.getSubscriptionRequest());
	
	// set holders as subscribed
	//TODO
	
	// return true
	UconResponse resp = UconResponse.TRUE;
	return resp;
    }

    /**
     * Each UconDataEntity have at least a holder. For each holder, look into database for a involved session and re-evaluate it
     * @param in
     * 		a list of sessions with one holder
     * @return
     * 		a list of sessions to revoke 
     * @throws XacmlSamlException 
     * @throws InterruptedException 
     * @throws SQLException 
     * @throws AxisFault 
     */
    public List<UconDataEntity> performUpdateAndReevaluation(List<UconDataEntity> in) throws InterruptedException, XacmlSamlException, SQLException, AxisFault {
	// the set of session to re-evaluate
	Set<UconDataEntity> sessionsToReevaluate = new HashSet<UconDataEntity>();
	for(UconDataEntity holder: in) {
	    // update holder attributes and get from database all the sessions involved 
	    Set<UconDataEntity> involved_sessions = db.updateHolderAndGetInvolvedSession(holder);
	    // ad to set ( UconDataEntity "equals" method checks just the session id)
	    sessionsToReevaluate.addAll(involved_sessions);
	}
	
	// re-evaluate the sessions
	List<UconDataEntity> toUnsubscribe = new LinkedList<UconDataEntity>();
	for(UconDataEntity session: sessionsToReevaluate) {
	    String eval = PdpModule.evaluate(serviceContext, CorePhase.ON, session.getFinalXacmlString());
	    // create the response object
	    UconResponse resp = UconResponse.getUconResponse(serviceContext, eval);
	    if (!resp.getBooleanResponse()) {
		// end the session on db
		toUnsubscribe = db.updateSession(session); //TODO
		
		// send the revoke messages
		communicator.sendRevoke(session.getRevokeMessage()); //TODO I have to choose between axis2 asynch and rest
		
		
	    }
	}
	
	// TODO I have to send one unsubscribe messages for all data!!!!
	for(UconDataEntity data: toUnsubscribe) {
	    communicator.unsubscribe(data.getUnsubscriptionRequest());
	}
//	communicator.sendRevoke(session);

	return null;
    }

    public boolean sessionMapId(String old_id, String new_id) {
	// TODO Auto-generated method stub
	return false;
    }

}
