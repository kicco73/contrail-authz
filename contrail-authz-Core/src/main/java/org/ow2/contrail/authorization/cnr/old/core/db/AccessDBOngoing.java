package org.ow2.contrail.authorization.cnr.old.core.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;

public class AccessDBOngoing extends AccessDBConcurrent {

	private int getAttributeForSession = 0;

	public AccessDBOngoing(int numConn, boolean readOnly, String url, String user, String password) throws SQLException {
		super(numConn, readOnly, url, user, password);
	}

	public AccessDBOngoing(int numConn, boolean readOnly, String url, String user, String password, int maxAttempts) throws SQLException {
		super(numConn, readOnly, url, user, password, maxAttempts);
	}

	@Override
	protected PreparedStatement[] preparedStatementInit(Connection conn) throws SQLException {
		PreparedStatement[] psts = new PreparedStatement[1];
		psts[getAttributeForSession] = conn.prepareStatement(SQLStatement.getAttributeForSession);
		return psts;
	}
		
	// usage control parallel
	public List<UconAttribute> getAttributeForSession(String session_key) throws SQLException, InterruptedException {
		List<UconAttribute> attributeList = null;
		ConnectionUtil conn = null;
		try {
			conn = connections.take();
		} catch (InterruptedException e) {
			throw new InterruptedException(interruptedError);
		}
		try {
			PreparedStatement pst = conn.pstArray[getAttributeForSession];
			pst.setString(1, session_key);
			ResultSet result = pst.executeQuery();
			attributeList = new ArrayList<UconAttribute>();
			while (result.next()) {
				UconAttribute attribute = createUconAttribute(result);
				attributeList.add(attribute);
			}
			result.close();
			pst.clearParameters();
		} catch (SQLException e) {
			if (attempts.incrementAndGet() < maxAttempts) {
				reconnect(conn);
				attributeList = getAttributeForSession(session_key);
				attempts.set(0);
			} else {
				destroyAll();
				throw e;
			}
		}
		connections.offer(conn);
		return attributeList;
	}

}
