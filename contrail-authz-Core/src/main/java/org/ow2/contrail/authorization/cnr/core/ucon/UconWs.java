package org.ow2.contrail.authorization.cnr.core.ucon;

import java.sql.SQLException;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.service.Lifecycle;
import org.ow2.contrail.authorization.cnr.core.db.HibernateUtil;
import org.ow2.contrail.authorization.cnr.core.pdp.PdpModule;
import org.ow2.contrail.authorization.cnr.core.utils.OpenSamlCore;
import org.ow2.contrail.authorization.cnr.utils.XMLConvert;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UconWs implements Lifecycle {

    private static Logger log = LoggerFactory.getLogger(UconWs.class);
    private static final String logTag = "[UCONX]: ";

    @Override
    public void init(ServiceContext serviceContext) throws AxisFault {
	
	    // CHECKME: INIT OPTIONS (what should these options contain? I should read values from a property file) 
	    UconOptions.init(serviceContext, null);
	    PdpModule.initAllThree(serviceContext);
	    try {
		OpenSamlCore.init(serviceContext);
	    } catch (XacmlSamlException e) {
		    throw new AxisFault(e.getMessage());
	    }
    	// KMcC;)
	    catch (NoSuchMethodError e) {
		    log.error("{} [KMcC;)] cannot call OpenSamlCore.init(): no such method "+e.getMessage(), logTag);
		    throw e;
		}
	    try {
		HibernateUtil.init(serviceContext);
	    } catch (SQLException e) {
		throw new AxisFault(e.getMessage());
	    }
	    log.info("{} complete initialization", logTag);
	
    }

    @Override
    public void destroy(ServiceContext serviceContext) {

    }
    
    private OMElement getMessage(OMElement message) {
	message.build();
	message.detach();
	return message.getFirstElement();
    }

    public OMElement tryaccess(OMElement request) throws InterruptedException, XacmlSamlException, SQLException, AxisFault, XMLStreamException {
	
	log.info("{} tryaccess message received info", logTag);
	log.debug("{} tryaccess message received debug\n {}", logTag, request);
	
	// get message context
	MessageContext messageContext = MessageContext.getCurrentMessageContext();
	// create context handler
	ContextHandler ch = new ContextHandler(messageContext);
	// perform the action
	UconResponse resp = ch.performeTryaccess(XMLConvert.toDOM(getMessage(request)));
	// retrieve response
	return XMLConvert.toOM(resp.getStringResponse());
    }

    public OMElement startaccess(OMElement request) throws SQLException, XacmlSamlException, AxisFault {
	
	log.debug("{} startaccess message received\n {}", logTag, request);
	// get message context
	MessageContext messageContext = MessageContext.getCurrentMessageContext();
	// create context handler
	ContextHandler ch = new ContextHandler(messageContext);
	// perform the action
	UconResponse resp = ch.performStartaccess(XMLConvert.toDOM(getMessage(request)));
	// retrieve response
	return XMLConvert.toOM(resp.getXmlResponse());

    }

    public OMElement endaccess(OMElement request) throws SQLException, XacmlSamlException, AxisFault {
	
	log.debug("{} endaccess message received\n {}", logTag, request);
	// get message context
	MessageContext messageContext = MessageContext.getCurrentMessageContext();
	// create context handler
	ContextHandler ch = new ContextHandler(messageContext);
	// perform the action
	UconResponse resp = ch.performEndaccess(XMLConvert.toDOM(getMessage(request)));
	// retrieve response
	return XMLConvert.toOM(resp.getXmlResponse());

    }

    public void updateNotification(OMElement updates) throws SQLException, XacmlSamlException, InterruptedException, AxisFault {
	
	log.debug("{} updates message received\n {}", logTag, updates);
	// get message context
	MessageContext messageContext = MessageContext.getCurrentMessageContext();
	// create context handler
	ContextHandler ch = new ContextHandler(messageContext);
	// perform the action
	ch.handleUpdate(XMLConvert.toDOM(getMessage(updates)));

    }

    public void mapid(String old_id, String new_id) throws SQLException {
        // get message context
        MessageContext messageContext = MessageContext.getCurrentMessageContext();
        // create context handler
        ContextHandler ch = new ContextHandler(messageContext);
        // perform the action
        boolean resp = ch.performMapId(old_id, new_id);
    }

}