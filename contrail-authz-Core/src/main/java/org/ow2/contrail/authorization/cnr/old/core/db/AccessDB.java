package org.ow2.contrail.authorization.cnr.old.core.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;

import org.ow2.contrail.authorization.cnr.core.db.UconAttribute;
import org.ow2.contrail.authorization.cnr.core.utils.UconConstantsCore;

public abstract class AccessDB {

    protected static final int DEFAULT_MAX_ATTEMPTS = 2;
    private String url, user, password;
    protected int maxAttempts = 2;
    protected AtomicInteger attempts = new AtomicInteger(0);
    protected ConnectionUtil connection = null;
    protected boolean readOnly = false;

    public String interruptedError = "An interrupt occur while waiting an available connection.";

    protected AccessDB(boolean readOnly, String url, String user, String password) throws SQLException {
	this(readOnly, url, user, password, 2);
    }

    protected AccessDB(boolean readOnly, String url, String user, String password, int maxAttempts) throws SQLException {
	this.url = url;
	this.user = user;
	this.password = password;
	this.maxAttempts = maxAttempts;
	this.readOnly = readOnly;
	connection = initConnection();
    }

    // create a java.sql.Connection using url, user, password
    private Connection getConnection() throws SQLException {
	Connection conn = DriverManager.getConnection(url, user, password);
	if (readOnly) {
	    conn.setReadOnly(true);
	}
	return conn;
    }

    // destroy and create a ConnectionUtil
    protected void reconnect(ConnectionUtil conUtil) throws SQLException {
	destroyConnection(conUtil);
	connection = initConnection();
    }

    // create all the ConnectionUtil requested
    protected void initAll() throws SQLException {
	connection = initConnection();
    }

    // create a single ConnectionUtil
    protected ConnectionUtil initConnection() throws SQLException {
	Connection conn = getConnection();
	return new ConnectionUtil(conn, preparedStatementInit(conn));
    }

    // create all the PreparedStatement (it depends on kind of access to database)
    protected abstract PreparedStatement[] preparedStatementInit(Connection conn) throws SQLException;

    /**
     * destroy all the ConnectionUtil created (sort of class destructor)
     */
    public void destroyAll() {
	destroyConnection(connection);
    }

    // destroy a single ConnectionUtil
    protected void destroyConnection(ConnectionUtil conUtil) {
	for (PreparedStatement pst : conUtil.pstArray) {
	    try {
		pst.close(); // from java API: When a Statement object is closed, its current ResultSet object, if one exists, is also closed
	    } catch (SQLException e) {
	    }
	}
	try {
	    conUtil.conn.close();
	} catch (SQLException e) {
	}
    }

    protected class ConnectionUtil {
	public Connection conn;
	public PreparedStatement[] pstArray;

	public ConnectionUtil(Connection conn, PreparedStatement[] pstArray) {
	    this.conn = conn;
	    this.pstArray = pstArray;
	}
    }

    protected Timestamp now() {
	return new Timestamp(System.currentTimeMillis());
    }

    protected String getSessionKey(String str) {
	return str.substring(UconConstantsCore.UCON_SESSION_ID_PREFIX.length());
    }

    protected UconAttribute createUconAttribute(ResultSet result) throws SQLException {
	// return new UconAttribute(result.getInt("attribute_key"), result.getString("xacml_attribute_id"), result.getString("type"),
	// result.getString("value"), result.getString("issuer"), result.getString("holder"), Category.valueOf(result
	// .getString("category")) // FIXME: it could throws a runtime exception!
	// );
	return null;
    }

}

/*
 * public boolean emptyDB() { try { sem.acquire(); } catch (InterruptedException e) { e.printStackTrace(); } PreparedStatement pst = null; int ret =
 * -1; boolean resp = false; try { pst = connDb.prepareStatement("TRUNCATE TABLE attr_per_session"); ret = pst.executeUpdate(); resp = ret == 1; }
 * catch (Exception e) { e.printStackTrace(); } try { pst = connDb.prepareStatement("TRUNCATE TABLE sessions"); ret = pst.executeUpdate(); resp = resp
 * && ret == 1;
 * 
 * } catch (Exception e) { e.printStackTrace(); } try { pst = connDb.prepareStatement("TRUNCATE TABLE attribute"); ret = pst.executeUpdate(); resp =
 * resp && ret == 1; } catch (Exception e) { e.printStackTrace(); } try { pst = connDb.prepareStatement("TRUNCATE TABLE retrieval_policy"); ret =
 * pst.executeUpdate(); resp = resp && ret == 1; } catch (Exception e) { e.printStackTrace(); } finally { try { pst.close(); } catch (SQLException e)
 * { } } sem.release(); return resp; }
 */

// ***********************************
