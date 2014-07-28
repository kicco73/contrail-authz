package org.ow2.contrail.authorization.cnr.utils;

public class UconConstants {

    // for Axis2 support
    public static final String OPENSAML_UTILS = "opensamlutils";
    public static final String DB_TOOL = "access_db_tool";

    public static final String XML_STRING = "http://www.w3.org/2001/XMLSchema#string";
    public static final String XML_INT = "http://www.w3.org/2001/XMLSchema#integer";
    public static final String HOLDER_ELEMENT = "HOLDER_ELEMENT"; //CHECKME

    // protocol
    public static final String PIP_ATTRIBUTE_QUERY_Tag = "PIPAttributeQuery";
    public static final String PIP_ATTRIBUTE_SUBSCRIBE_Tag = "PIPAttributeSubscribe";
    public static final String PIP_ATTRIBUTE_UNSUBSCRIBE_Tag = "PIPAttributeUnsubscribe";
    public static final String PIP_ATTRIBUTE_RESPONSE_Tag = "PipResponse";
    public static final String PIP_ATTRIBUTE_UPDATE_Tag = "";
    public static final String PIP_ATTRIBUTE_RESPONSE_VALUE_ATTR = "value";
    public static final String PIP_REQUEST_SUBJECT_Tag = "PipSubject";
    public static final String PIP_REQUEST_RESOURCE_Tag = "PipResource";
    public static final String PIP_REQUEST_ACTION_Tag = "PipAction"; //unused
    public static final String PIP_REQUEST_ENVIRONMENT_Tag = "PipEnvironment"; //unused

    public static final String UCON_STARTACCESS_REPLYTO_ACTION_NAME = "reply_to";
    public static final String UCON_RESPONSE_Tag = "UCONResponse";    
    public static final String RESPONSE_OK = "done"; //CHECKME responses
    public static final String RESPONSE_NOT = "not_done";
    public static final String REVOKE_MESSAGE = "revoke";
    // public static final String SESSION_NEVER_START = "session never started"; //NO
    public static final String UCON_ERROR_Tag = "UCONError";
    public static final String SESSION_ALREADY_STARTED = "session already started";
    public static final String SESSION_ALREADY_STOPPED_REVOKED = "session already stopped or revoked";
    public static final String GENERIC_ERROR = "An error occur on Ucon service. Contact the administrator.";
    public static final String ID_INVALID_ERROR = "The request id is not valid";
    public static final String INPUT_MESSAGE_ERROR = "Input message is not valid";

    public static final String NO_SESSION_ID = "-1";
    
    // web service names (UCON)
    public static final String UCON_NAMESPACE = "http://ucon.core.cnr.authorization.contrail.ow2.org";
    public static final String TRYACCESS_METHOD_NAME = "tryaccess";
    public static final String TRYACCESS_PARAM_NAME = "request";
    public static final String STARTACCESS_METHOD_NAME = "startaccess";
    public static final String STARTACCESS_PARAM_NAME = "request";
    public static final String ASYNCH_STARTACCESS_METHOD_NAME = "startaccess"; //CHECKME actually is the same method
    public static final String ASYNCH_STARTACCESS_PARAM_NAME = "request";
    public static final String ENDACCESS_METHOD_NAME = "endaccess";
    public static final String ENDACCESS_PARAM_NAME = "request";
    public static final String MAPID_METHOD_NAME = "mapId";
    public static final String MAPID_PARAM1_NAME = "old_id";
    public static final String MAPID_PARAM2_NAME = "ovf_id";
    
    // web service names (PIP)
    public static final String PIP_NAMESPACE = "http://pip.cnr.authorization.contrail.ow2.org";
    public static final String ATTRIBUTE_QUERY_METHOD_NAME = "attributeQuery";
    public static final String ATTRIBUTE_QUERY_PARAM_NAME = "request";
    public static final String SUBSCRIBE_METHOD_NAME = "subscribe";
    public static final String SUBSCRIBE_PARAM_NAME = "request";
    public static final String UNSUBSCRIBE_METHOD_NAME = "unsubscribe";
    public static final String UNSUBSCRIBE_PARAM_NAME = "request";
    public static final String TRIGGER_UPDATE_METHOD_NAME = "triggerUpdate";

    
    @Deprecated
    public static final String END_MESSAGE = "end";
    @Deprecated
    public static final String PRINTLINE = "-------------------------------------------------------------------------------------------------------------------------------------------------------------";
    @Deprecated
    public static final String PRINTSTAR = "*************************************************************************************************************************************************************";
    @Deprecated
    public static final int VERBOSE_NONE = 0, VERBOSE_LOW = 1, VERBOSE_HIGH = 2;

}
