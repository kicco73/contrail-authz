package org.ow2.contrail.authorization.cnr.old.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;
import org.ow2.contrail.authorization.cnr.core.db.UconSession;
import org.ow2.contrail.authorization.cnr.core.utils.CorePhase;
import org.ow2.contrail.authorization.cnr.old.core.utils.OpenSamlCoreOLD;
import org.ow2.contrail.authorization.cnr.old.core.utils.UconRequestContext;
import org.ow2.contrail.authorization.cnr.old.core.utils.UconXacmlRequest;
import org.ow2.contrail.authorization.cnr.utils.XacmlSamlException;

public class AccessDBUconRead extends AccessDB {

	private final int getActiveSession = 0, attributeToBePulled = 1, sessionsToReevaluate = 2;

	public AccessDBUconRead(boolean readOnly, String url, String user, String password) throws SQLException {
		super(readOnly, url, user, password);
	}

	public AccessDBUconRead(boolean readOnly, String url, String user, String password, int maxAttempts) throws SQLException {
		super(readOnly, url, user, password, maxAttempts);
	}

	@Override
	protected PreparedStatement[] preparedStatementInit(Connection conn) throws SQLException {
		PreparedStatement[] pstList = new PreparedStatement[3];
		pstList[getActiveSession] = conn.prepareStatement(SQLStatement.getActiveSession);
		pstList[attributeToBePulled] = conn.prepareStatement(SQLStatement.attributeToBePulled);
		pstList[sessionsToReevaluate] = conn.prepareStatement(SQLStatement.sessionsToReevaluate);
		return pstList;
	}
	
	//actually I take ALL attributes from database!!
	public List<UconAttribute> attributeToBePulled() throws SQLException {
		List<UconAttribute> attributeList = null;
		try {
			PreparedStatement pst = connection.pstArray[attributeToBePulled];
			ResultSet result = pst.executeQuery();
			attributeList = new ArrayList<UconAttribute>();
			while (result.next()) {
				UconAttribute attribute = createUconAttribute(result);
				attributeList.add(attribute);
			}
			result.close();
		} catch (SQLException e) {
			if (attempts.incrementAndGet() < maxAttempts) {
				reconnect(connection);
				attributeList = attributeToBePulled();
				attempts.set(0);
			} else {
				destroyAll();
				throw e;
			}
		}
		return attributeList;
	}

	public List<UconSession> sessionsToReevaluate(List<UconAttribute> updatedAttributes, OpenSamlCoreOLD utils) throws SQLException, XacmlSamlException {
		List<UconSession> sessions = null;
		ResultSet result = null;
		try {
			PreparedStatement pst = connection.pstArray[sessionsToReevaluate];
			sessions = new ArrayList<UconSession>();
			for (UconAttribute attr : updatedAttributes) {
				// System.out.println("[DB] sessionToReevaluate: updatading value of attribute " + attr.getAttributeKey());
//				pst.setInt(1, attr.getAttributeKey());
				result = pst.executeQuery();
				while (result.next()) {
					// System.out.println("[ACCESSDB] session to reevaluate: result set is not empty "+ result.first());
					UconSession session = new UconSession();
					UconRequestContext req = new UconRequestContext(CorePhase.PRE);
				
					try {
						req.setXACMLRequest(new UconXacmlRequest(result.getString("xacml_request"), utils));
					} catch (XacmlSamlException e) {
						result.close(); //should not fail
						throw e;
					} 
					
					session.setSession_id_string(result.getString("sessionId"));
					session.setReplyTo(result.getString("replyTo"));
					session.setMessageId(result.getString("messageId"));
//					session.setStatus(result.getString("session_status"));
//					session.setSessionKey(result.getString("session_key"));
//					session.setInitialRequestContext(req);
					sessions.add(session);
				}
				result.close();
			}
			pst.clearParameters();
		} catch (SQLException e) {
			if (attempts.incrementAndGet() < maxAttempts) {
				reconnect(connection);
				sessions = sessionsToReevaluate(updatedAttributes, utils);
				attempts.set(0);
			} else {
				destroyAll();
				throw e;
			}
		}
		return sessions;
	}

	public int activeSessions() throws SQLException {
		int resp = -1;

		ResultSet result = null;
		try {
			PreparedStatement pst = connection.pstArray[getActiveSession];
			result = pst.executeQuery();
			result.next(); // it's needed!!! (tested)
			resp = result.getInt(1);
			result.close();
		} catch (SQLException e) {
			if (attempts.incrementAndGet() < maxAttempts) {
				reconnect(connection);
				resp = activeSessions();
				attempts.set(0);
			} else {
				destroyAll();
				throw e;
			}
		}
		return resp;
	}

}
