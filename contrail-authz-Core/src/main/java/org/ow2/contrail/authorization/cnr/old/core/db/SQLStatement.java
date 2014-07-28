package org.ow2.contrail.authorization.cnr.old.core.db;

import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;

public class SQLStatement {
	
	// SQL queries string
	public static String insertSession = 
			"INSERT INTO sessions(xacml_request, replyTo, session_status, lastReevaluation) "
			+ "VALUES(?, ?, ?, ?)";
	
	public static String insertSessionWithId = 
			"INSERT INTO sessions(xacml_request, replyTo, session_status, lastReevaluation, sessionId) "
			+ "VALUES(?, ?, ?, ?, ?)";
	
	public static String getAllSessions = 
			"SELECT * FROM sessions " +
			"WHERE session_status = '" + UconConstantsCore.SESSION_NEW + "'";;
	
	public static String getSession = 
			"SELECT * FROM sessions " +
			"WHERE session_key = ?";
	
	public static String getSessionWithId = 
			"SELECT * FROM sessions " +
			"WHERE sessionId = ?";
	
	public static String updateSessionStatus = 
			"UPDATE sessions SET session_status = ?, replyTo = ?, messageId = ? " +
			"WHERE session_key = ?";
	
	public static String insertAttrPerSession = 
			"INSERT INTO attr_per_session(session_key, attribute_key) " +
			"VALUES(?, ?)";

	public static String insertAttribute = 
			"INSERT INTO attribute(category, type, xacml_attribute_id, value, issuer, holder, lastChange) "
			+ "VALUES(?, ?, ?, ?, ?, ?, ?)";
	
	public static String deleteSession = 
			"DELETE FROM sessions WHERE session_key = ?";
	
	public static String deleteAttrSession = 
			"DELETE FROM attr_per_session WHERE session_key = ?";
	
	public static String deleteAttrSessionPost = 
			"DELETE FROM attr_per_session " +
			"WHERE EXISTS " +
			"(SELECT * FROM sessions " +
			"WHERE sessions.session_status = '" + UconConstantsCore.SESSION_POST + "' AND " +
					"sessions.session_key = attr_per_session.session_key)";

	public static String deleteSessionPost = 
			"DELETE FROM sessions " +
			"WHERE session_status = '" + UconConstantsCore.SESSION_POST + "'";
	
	public static String getActiveSession = 
			"SELECT COUNT(*) FROM sessions " +
			"WHERE session_status = '" + UconConstantsCore.SESSION_ON + "'";
	
	public static String getAttributeKey = 
			"SELECT attribute_key " +
			"FROM attribute " +
			"WHERE xacml_attribute_id = ? AND holder = ? ";
		
	public static String updateAttribute = 
			"UPDATE attribute SET value = ?, lastChange = ? " +
			"WHERE attribute_key = ?";
	
//	public static String sessionsToReevaluate = 
//			"SELECT DISTINCT sessions.session_key, sessionId, xacml_request, replyTo, messageId, session_status " + 
//			"FROM sessions, attr_per_session " +
//			"WHERE sessions.session_status = '" + UconConstantsCore.SESSION_ZOMBIE + "' OR " +
//			" (attribute_key = ? AND sessions.session_key = attr_per_session.session_key AND" +
//			" sessions.session_status = '" + UconConstantsCore.SESSION_ON + "')";
	
	public static String sessionsToReevaluate = 
			"SELECT DISTINCT sessions.session_key, sessionId, xacml_request, replyTo, messageId, session_status " + 
			"FROM sessions, attr_per_session " +
			"WHERE (attribute_key = ? AND sessions.session_key = attr_per_session.session_key AND" +
			" sessions.session_status = '" + UconConstantsCore.SESSION_ON + "') OR " +
			" sessions.session_status = '" + UconConstantsCore.SESSION_NEW + "'";
	
	public static String getAttributeForSession =
			"SELECT * FROM attribute, attr_per_session " +
			"WHERE session_key = ? AND attr_per_session.attribute_key = attribute.attribute_key";
	
	public static String attributeToBePulled = 
			"SELECT * FROM attribute ORDER BY holder"; // FIXME: Currently I take all attributes!
	
	public static String lookingForAttribute = 
			"SELECT * FROM attribute " +
			"WHERE category = ? AND type = ? AND xacml_attribute_id = ? AND issuer = ? AND holder = ?";
	
	public static String deleteAttribute = 
			"DELETE FROM attribute " +
			"WHERE NOT EXISTS " +
			"( SELECT * FROM attr_per_session " +
			"WHERE attr_per_session.attribute_key = attribute.attribute_key)";
	
	public static String mapId = 
			"UPDATE sessions SET sessionId = ?" +
			"WHERE session_key = ?";
}
