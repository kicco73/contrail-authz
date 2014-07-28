package org.ow2.contrail.authorization.cnr.old.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.ow2.contrail.authorization.cnr.core.db.UconSession;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.ow2.contrail.authorization.cnr.old.core.utils.OpenSamlCoreOLD;
import org.ow2.contrail.authorization.cnr.old.core.utils.UconRequestContext;
import org.ow2.contrail.authorization.cnr.old.core.utils.UconXacmlRequest;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

public class AccessDBRead extends AccessDBConcurrent {

    public AccessDBRead(int numConn, boolean readOnly, String url, String user, String password) throws SQLException {
	super(numConn, readOnly, url, user, password);
    }

    public AccessDBRead(int numConn, boolean readOnly, String url, String user, String password, int maxAttempts) throws SQLException {
	super(numConn, readOnly, url, user, password, maxAttempts);
    }

    private final int getSession = 0, getSessionId = 1, getAllSessions = 2;

    protected PreparedStatement[] preparedStatementInit(Connection conn) throws SQLException {
	PreparedStatement[] pstList = new PreparedStatement[3];
	pstList[getSession] = conn.prepareStatement(SQLStatement.getSession);
	pstList[getSessionId] = conn.prepareStatement(SQLStatement.getSessionWithId);
	pstList[getAllSessions] = conn.prepareStatement(SQLStatement.getAllSessions);
	return pstList;
    }

    // *******startaccess endaccess*******
    /**
     * Get a session from database from its session key or session id
     * 
     * @param sessionId
     *            The session key or the session id
     * @param utils
     * @return The session from db or null if it doesn't exist
     * @throws XacmlSamlException
     *             Error parsing the session got (should not occur if session added using ucon system)
     * @throws InterruptedException
     *             Waiting for resource availability
     * @throws SQLException
     *             If the number of attempts to connect to database exceeded the limit
     */
    public UconSession getSession(String sessionId, OpenSamlCoreOLD utils) throws XacmlSamlException, InterruptedException, SQLException {

	UconSession uconSessionContext = null;
	ConnectionUtil conn = null;
	try {
	    conn = connections.take();
	} catch (InterruptedException e1) {
	    throw new InterruptedException(interruptedError);
	}
	try {
	    PreparedStatement pst = null;
	    if (isSessionKey(sessionId)) {
		pst = conn.pstArray[getSession];
		pst.setString(1, getSessionKey(sessionId));// currentSessionId);
	    } else {
		pst = conn.pstArray[getSessionId];
		pst.setString(1, sessionId);
	    }
	    // System.out.println("[AccessDB] get session query with id: " + uconReq.getSessionID()); //currentSessionId);
	    ResultSet ret = pst.executeQuery(); // do I have to check if ret contains more then one entry?
	    uconSessionContext = null;
	    if (ret.first()) { // ok in this manner (?)
		uconSessionContext = new UconSession();
		uconSessionContext.setSession_id_string(sessionId);
//		uconSessionContext.setSessionKey(ret.getString("session_key"));
//		uconSessionContext.setStatus(ret.getString("session_status"));
		uconSessionContext.setReplyTo(ret.getString("replyTo"));
		uconSessionContext.setMessageId(ret.getString("messageId"));
		String initialXacmlRequest = ret.getString("xacml_request");
		// the XacmlSamlException here should not occur because when I add xacml request I do the same computation
		UconXacmlRequest uconXacmlRequest = new UconXacmlRequest(initialXacmlRequest, utils);
		UconRequestContext uconRequest = utils.getRequestContextFromUconXacmlRequest(uconXacmlRequest);

//		uconSessionContext.setInitialRequestContext(uconRequest);
	    }
	    pst.clearParameters();
	    ret.close();
	} catch (SQLException e) {
	    if (attempts.incrementAndGet() < maxAttempts) {
		reconnect(conn);
		uconSessionContext = getSession(sessionId, utils);
		attempts.set(0);
	    } else {
		destroyAll();
		throw e;
	    }
	}
	connections.offer(conn);
	return uconSessionContext;
    }

    public List<UconSession> getAllSessions(OpenSamlCoreOLD utils) throws SQLException, InterruptedException, XacmlSamlException {
	ConnectionUtil conn = null;
	try {
	    conn = connections.take();
	} catch (InterruptedException e1) {
	    throw new InterruptedException(interruptedError);
	}
	List<UconSession> sessions = null;
	ResultSet result = null;
	try {
	    PreparedStatement pst = connection.pstArray[getAllSessions];
	    sessions = new ArrayList<UconSession>();
	    result = pst.executeQuery();
	    while (result.next()) {
		// System.out.println("[ACCESSDB] session to reevaluate: result set is not empty "+ result.first());
		UconSession session = new UconSession();
		UconRequestContext req = new UconRequestContext(CorePhase.PRE);

		try {
		    req.setXACMLRequest(new UconXacmlRequest(result.getString("xacml_request"), utils));
		} catch (XacmlSamlException e) {
		    result.close(); // should not fail
		    throw e;
		}

		session.setSession_id_string(result.getString("sessionId"));
		session.setReplyTo(result.getString("replyTo"));
		session.setMessageId(result.getString("messageId"));
//		session.setStatus(result.getString("session_status"));
//		session.setSessionKey(result.getString("session_key"));
//		session.setInitialRequestContext(req);
		sessions.add(session);
	    }
	    result.close();
	    pst.clearParameters();
	} catch (SQLException e) {
	    if (attempts.incrementAndGet() < maxAttempts) {
		reconnect(conn);
		sessions = getAllSessions(utils);
		attempts.set(0);
	    } else {
		destroyAll();
		throw e;
	    }
	}
	connections.offer(conn);
	return sessions;
    }

    private boolean isSessionKey(String str) {
	return str.startsWith(UconConstantsCore.UCON_SESSION_ID_PREFIX);
    }

}
