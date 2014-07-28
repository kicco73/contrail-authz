package org.ow2.contrail.authorization.cnr.old.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;
import org.ow2.contrail.authorization.cnr.core.db.UconSession;

public class AccessDBUconWrite extends AccessDB {
	
	// enumerator???
//	private final int updateSessionStatus = 0, lookingForAttribute = 1, insertAttribute = 2, insertAttrPerSession = 3,
//			deleteAttrSessionPost = 4, deleteSessionPost = 5, deleteAttribute = 6, updateAttribute = 7, getAttributeKey = 8;

	private enum stm {
		updateSessionStatus(0),
		lookingForAttribute(1),
		insertAttribute(2), 
		insertAttrPerSession(3),
		deleteAttrSessionPost(4), 
		deleteSessionPost(5), 
		deleteAttribute(6), 
		updateAttribute(7), 
		getAttributeKey(8);
		
		int index;
		static int length = values().length;
		stm(int v) {
			index = v;
		}
	};
	
	public AccessDBUconWrite(boolean readOnly, String url, String user, String password) throws SQLException {
		super(readOnly, url, user, password);
	}

	public AccessDBUconWrite(boolean readOnly, String url, String user, String password, int maxAttempts) throws SQLException {
		super(readOnly, url, user, password, maxAttempts);
	}

	@Override
	protected PreparedStatement[] preparedStatementInit(Connection conn) throws SQLException {
		stm.values();
		PreparedStatement[] pstList = new PreparedStatement[stm.length];
		pstList[stm.updateSessionStatus.index] = conn.prepareStatement(SQLStatement.updateSessionStatus);
		pstList[stm.lookingForAttribute.index] = conn.prepareStatement(SQLStatement.lookingForAttribute);
		pstList[stm.insertAttribute.index] = conn.prepareStatement(SQLStatement.insertAttribute, Statement.RETURN_GENERATED_KEYS);
		pstList[stm.insertAttrPerSession.index] = conn.prepareStatement(SQLStatement.insertAttrPerSession);
		pstList[stm.deleteAttrSessionPost.index] = conn.prepareStatement(SQLStatement.deleteAttrSessionPost);
		pstList[stm.deleteSessionPost.index] = conn.prepareStatement(SQLStatement.deleteSessionPost);
		pstList[stm.deleteAttribute.index] = conn.prepareStatement(SQLStatement.deleteAttribute);
		pstList[stm.updateAttribute.index] = conn.prepareStatement(SQLStatement.updateAttribute);
		pstList[stm.getAttributeKey.index] = conn.prepareStatement(SQLStatement.getAttributeKey);
		return pstList;
	}
	
	// ***********************************
	// ***********usage control***********
	// ***********************************
	public boolean updateSession(UconSession sessionContext) throws SQLException {
		boolean resp = false;
		try {
//			String sessionKey = sessionContext.getSessionKey();
			PreparedStatement pst = connection.pstArray[stm.updateSessionStatus.index];
//			pst.setString(1, sessionContext.getStatus());
			pst.setString(2, sessionContext.getReplyTo());
			pst.setString(3, sessionContext.getMessageId());
//			pst.setString(4, sessionKey);
			int ret = pst.executeUpdate();
			pst.clearParameters();
			resp = ret == 1;
		} catch (SQLException e) {
			if (attempts.incrementAndGet() < maxAttempts) {
				reconnect(connection);
				resp = updateSession(sessionContext);
				attempts.set(0);
			} else {
				destroyAll();
				throw e;
			}
		}
		return resp;
	}

	public boolean insertAttributesForSession(List<UconAttribute> attrNeededbySession, String sessionKey) throws SQLException {
		boolean ok = true;
		try {
			connection.conn.setAutoCommit(false);
			for (UconAttribute uconAttr : attrNeededbySession) {

				PreparedStatement lookingAttr = null, insAttrSess = null, insAttr = null;

				// check if this attribute is already in database
				lookingAttr = connection.pstArray[stm.lookingForAttribute.index];
//				lookingAttr.setString(1, uconAttr.getCategory().toString());
				lookingAttr.setString(2, uconAttr.getType());
//				lookingAttr.setString(3, uconAttr.getXacmlId());
				lookingAttr.setString(4, uconAttr.getIssuer());
//				lookingAttr.setString(5, uconAttr.getHolderId());
				ResultSet result = lookingAttr.executeQuery();
				int key = -1;
				if (!result.next()) { // not found
					insAttr = connection.pstArray[stm.insertAttribute.index];
//					insAttr.setString(1, uconAttr.getCategory().toString());
					insAttr.setString(2, uconAttr.getType());
//					insAttr.setString(3, uconAttr.getXacmlId());
					insAttr.setString(4, uconAttr.getValue());
					insAttr.setString(5, uconAttr.getIssuer());
//					insAttr.setString(6, uconAttr.getHolderId());
					insAttr.setTimestamp(7, now());
					// attributes
					int ret = insAttr.executeUpdate();
					// System.out.println("risultato query: "+ret);
					if (ret == 1) {
						ResultSet rs = insAttr.getGeneratedKeys();
						if (rs.next()) {
							key = rs.getInt(1);
						}
					}
					insAttr.clearParameters();
					// System.out.println("[ACCESSDB] insertAttributesForSession: attribute inserted with key " + key);
				} else { // found it
					key = result.getInt("attribute_key");
				}
				result.close();
				insAttrSess = connection.pstArray[stm.insertAttrPerSession.index];
				// attribute per session
				insAttrSess.setString(1, sessionKey);
				insAttrSess.setInt(2, key);
				int ret2 = insAttrSess.executeUpdate();
				insAttrSess.clearParameters();
				// System.out.println("[ACCESSDB] insertAttributesForSession: attr_per_session inserted " + (ret2 == 1));
				ok = ok && ret2 == 1;
			}
			if (ok)
				connection.conn.commit();
			else
				connection.conn.rollback();
			connection.conn.setAutoCommit(true);
		} catch (SQLException e) {
			if (attempts.incrementAndGet() < maxAttempts) {
				reconnect(connection);
				ok = insertAttributesForSession(attrNeededbySession, sessionKey);
				attempts.set(0);
			} else {
				destroyAll();
				throw e;
			}
		}
		return ok;
	}

	public boolean deletePostSessions() throws SQLException {
		boolean resp = false;
		try {
			connection.conn.setAutoCommit(false);
			PreparedStatement pst = connection.pstArray[stm.deleteAttrSessionPost.index];
			boolean ret = pst.executeUpdate() > 0;
			PreparedStatement pst2 = connection.pstArray[stm.deleteSessionPost.index];
			boolean ret2 = pst2.executeUpdate() > 0;
			resp = ret && ret2;
			if (resp) // CHECKME: I'm not sure about this part :/
				connection.conn.commit();
			else
				connection.conn.rollback();
			connection.conn.setAutoCommit(true);
		} catch (SQLException e) {
			if (attempts.incrementAndGet() < maxAttempts) {
				reconnect(connection);
				resp = deletePostSessions();
				attempts.set(0);
			} else {
				destroyAll();
				throw e;
			}
		}
		return resp;
	}

	public boolean deleteUnreferredAttributes() throws SQLException {
		boolean resp = false;
		try {
			PreparedStatement pst = connection.pstArray[stm.deleteAttribute.index];
			resp = pst.executeUpdate() > 0;
		} catch (SQLException e) {
			if (attempts.incrementAndGet() < maxAttempts) {
				reconnect(connection);
				resp = deleteUnreferredAttributes();
				attempts.set(0);
			} else {
				destroyAll();
				throw e;
			}
		}
		return resp;
	}

	public boolean updateAttributes(List<UconAttribute> updatedAttributes) throws SQLException {
		boolean ok = true;
		try {
			connection.conn.setAutoCommit(false);
			PreparedStatement updt = connection.pstArray[stm.updateAttribute.index];
			PreparedStatement getKey = connection.pstArray[stm.getAttributeKey.index];
			for (UconAttribute attr : updatedAttributes) {
		
//				int key = attr.getAttributeKey();
//				if(attr.getAttributeKey() == -1) {
//					//get key
//					getKey.setString(1, attr.getXacmlId());
//					getKey.setString(2, attr.getHolderId());
//					System.out.println("Query "+getKey);
//					ResultSet result = getKey.executeQuery();					
//					if (result.next()) {
//						System.out.println("QUI");
//						key = result.getInt("attribute_key");
//						attr.setAttributeKey(key);						
//					}
//					result.close();
//				}
//				System.out.println(key);
				updt.setString(1, attr.getValue());
				updt.setTimestamp(2, now());
//				updt.setInt(3, key);
				
				int ret = updt.executeUpdate();
				ok = ok && ret == 1;
			}
			updt.clearParameters();
			if (ok)
				connection.conn.commit();
			else
				connection.conn.rollback();
			connection.conn.setAutoCommit(true);
		} catch (SQLException e) {
			if (attempts.incrementAndGet() < maxAttempts) {
				reconnect(connection);
				ok = updateAttributes(updatedAttributes);
				attempts.set(0);
			} else {
				destroyAll();
				throw e;
			}
		}
		return ok;
	}
}
