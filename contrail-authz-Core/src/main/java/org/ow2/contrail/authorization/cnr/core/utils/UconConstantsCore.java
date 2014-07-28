package org.ow2.contrail.authorization.cnr.core.utils;

import org.ow2.contrail.authorization.cnr.utils.UconConstants;

public class UconConstantsCore extends UconConstants {

    // configuration file location for core
    public static final String configFile = "/etc/contrail/authz/core/config.properties";

    // for Axis2 support
    public static final String OPTIONS_ENTRY = "options_entry";
    public static final String PRE_PDP = "pre_pdp";
    public static final String ON_PDP = "on_pdp";
    public static final String POST_PDP = "post_pdp";
    public static final String UCON_COMMUNICATOR = "ucon_comunicator_module";
    public static final String UCON_OPTIONS = "ucon_options";

    public static final String SESSION_MANAGER = "sessionmanager";
    public static final String RESPONSE_ABORT_PHASE = "MessageOut";
    public static final String SOAP_STARTACCESS_ACTION = "urn:startaccessResponse";
    public static final String ABORT_MESSAGE = "f2153c9bVe3b5%4f19Aaaa5ZDafc2bcdb77dd";
    public static final String REVOKE_MESSAGE = "revoke";
    public static final String UCON_SESSION_ID_PREFIX = "UconSession_";

    // session status (use UconPhase instead)
    @Deprecated
    public static final String SESSION_PRE = "pre";
    @Deprecated
    public static final String SESSION_NEW = "new"; // new state between pre and post
    @Deprecated
    public static final String SESSION_ON = "on";
    @Deprecated
    public static final String SESSION_POST = "post";

    // properties name in configuration file
    public static final String DB_URL = "db_url";
    public static final String DB_USER = "db_user";
    public static final String DB_PASSWORD = "db_password";
    public static final String EPR_PIP = "epr_pip";
    public static final String ACCESS_DB_PARALLELISM = "access_db_parallelism";
    public static final String PARALLEL_THREAD_NUMBER = "parallel_thread_number";
    public static final String CYCLE_PAUSE_DURATION = "cycle_pause";


    // policy constants
    public static String POLICY_STORAGE_PRE = "policy-pre.xml";
    public static String POLICY_STORAGE_ON = "policy-on.xml";
    public static String POLICY_STORAGE_POST = "policy-post.xml";
    public static String POLICY_DIR_PRE = "pre/";
    public static String POLICY_DIR_ON = "on/";
    public static String POLICY_DIR_POST = "post/";

}
