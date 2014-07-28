package org.ow2.contrail.authorization.cnr.old.core.db;

import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AccessDBConcurrent extends AccessDB {
	
	protected int numConn = 10;
	protected BlockingQueue<ConnectionUtil> connections = null;

	public String interruptedError = "An interrupt occur while waiting an available connection.";
	
	protected AccessDBConcurrent(int numConn, boolean readOnly, String url, String user, String password) throws SQLException {
		this(numConn, readOnly, url, user, password, DEFAULT_MAX_ATTEMPTS);
	}

	protected AccessDBConcurrent(int numConn, boolean readOnly, String url, String user, String password, int maxAttempts) throws SQLException {
		super(readOnly, url, user, password, maxAttempts);
		this.numConn = numConn;
		initAll();
	}

	@Override
	protected void reconnect(ConnectionUtil conUtil) throws SQLException {		
		destroyConnection(conUtil);
		connections.offer(initConnection());
	}

	@Override
	public void initAll() throws SQLException {
		connections = new LinkedBlockingQueue<ConnectionUtil>(numConn);
		for(int i = 0; i < numConn; i++) {
			connections.offer(initConnection());
		}
	}

	@Override
	public void destroyAll() {
		for (ConnectionUtil conUtil : connections) {
			destroyConnection(conUtil);
		}
	}	
}