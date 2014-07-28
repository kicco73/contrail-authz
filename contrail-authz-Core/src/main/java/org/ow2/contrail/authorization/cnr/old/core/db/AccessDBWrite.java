package org.ow2.contrail.authorization.cnr.old.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.ow2.contrail.authorization.cnr.core.db.UconSession;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;
import org.ow2.contrail.authorization.cnr.utils.UconConstants;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

public class AccessDBWrite extends AccessDBConcurrent {

	private final int insertSession = 0, insertSessionId = 1, mapId = 2;

	public AccessDBWrite(int numConn, boolean readOnly, String url, String user, String password) throws SQLException {
		super(numConn, readOnly, url, user, password);
	}

	public AccessDBWrite(int numConn, boolean readOnly, String url, String user, String password, int maxAttempts) throws SQLException {
		super(numConn, readOnly, url, user, password, maxAttempts);
	}

	protected PreparedStatement[] preparedStatementInit(Connection conn) throws SQLException {
		PreparedStatement[] pstList = new PreparedStatement[3];
		pstList[insertSession] = conn.prepareStatement(SQLStatement.insertSession, Statement.RETURN_GENERATED_KEYS);
		pstList[insertSessionId] = conn.prepareStatement(SQLStatement.insertSessionWithId);
		pstList[mapId] = conn.prepareStatement(SQLStatement.mapId);
		return pstList;
	}

	// *************tryaccess*************
	public String insertSession(UconSession newSessionContext) throws SQLException, XacmlSamlException, InterruptedException {
		String sessionId = newSessionContext.getSession_id_string();
		PreparedStatement pst = null;
		ConnectionUtil conn = null;
		try {
			conn = connections.take();
		} catch (InterruptedException e) {
			throw new InterruptedException(interruptedError);
		}
		try {
//			String xacmlRequest = newSessionContext.getInitialRequestContext().getXACMLRequest().getString();			
			if (sessionId.equals(UconConstants.NO_SESSION_ID)) {
				pst = conn.pstArray[insertSession];
//				pst.setString(1, xacmlRequest);
				// TODO (written by sasha): xacml request stored in DB should be VERY short, i.e.
				// mostly (s,o,r), no other static attributes
				pst.setString(2, newSessionContext.getReplyTo());
//				pst.setString(3, newSessionContext.getStatus());
				pst.setTimestamp(4, now()); // reevaluation
				int ret = pst.executeUpdate();
				if (ret == 1) {
					ResultSet rs = pst.getGeneratedKeys();
					if (rs.next()) {
						sessionId = UconConstantsCore.UCON_SESSION_ID_PREFIX + rs.getInt(1);
					}
				}
				// mutable attributes will be added on fly before the access
			} else {
				// vep version
				pst = conn.pstArray[insertSessionId];
//				pst.setString(1, xacmlRequest);
				pst.setString(2, newSessionContext.getReplyTo());
//				pst.setString(3, newSessionContext.getStatus());
				pst.setTimestamp(4, now()); // reevaluation
				pst.setString(5, sessionId);
				int ret = pst.executeUpdate();
				if (ret != 1) {
					sessionId = UconConstants.NO_SESSION_ID;
				}
			}
			pst.clearParameters();
		} catch (SQLException e) {
			if (attempts.incrementAndGet() < maxAttempts) {
				reconnect(conn);
				sessionId = insertSession(newSessionContext);
				attempts.set(0);
			} else {
				destroyAll();
				throw e;
			}
		}
		connections.offer(conn);
		return sessionId;
	}

	/**
	 * Map a session key with a new session id
	 * @param old_id
	 * 		session key
	 * @param ovf_id
	 * 		session id
	 * @return
	 * 		true if always goes fine
	 * @throws InterruptedException
	 * 		Waiting for resource availability
	 * @throws SQLException
	 * 		If the number of attempts to connect to database exceeded the limit
	 */
	public boolean mapId(String old_id, String ovf_id) throws InterruptedException, SQLException {
		boolean resp = false;
		ConnectionUtil conn = null;
		try {
			conn = connections.take();
		} catch (InterruptedException e) {
			throw new InterruptedException(interruptedError);
		}
		try {
			PreparedStatement pst = conn.pstArray[mapId];
			pst.setString(1, ovf_id);
			pst.setString(2, getSessionKey(old_id));
			int ret = pst.executeUpdate();
			pst.clearParameters();
			resp = ret == 1;
		} catch (SQLException e) {
			if (attempts.incrementAndGet() < maxAttempts) {
				reconnect(conn);
				resp = mapId(old_id, ovf_id);
				attempts.set(0);
			} else {
				destroyAll();
				throw e;
			}
		}
		connections.offer(conn);
		return resp;
	}
}
