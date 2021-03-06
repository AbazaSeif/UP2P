/*
 * Copyright (C) 2000-2001  Ken McCrary
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Email: jkmccrary@yahoo.com
 */
package stracciatella;

import java.util.List;
import java.util.ListIterator;
import java.io.IOException;

import org.apache.log4j.Logger;

import protocol.com.kenmccrary.jtella.util.Log;
import stracciatella.message.PingMessage;

/**
 *  Maintains activity on live connections
 *
 */
class KeepAliveThread extends Thread {
	/** Name of Logger used by this adapter. */
    public static final String LOGGER = "protocol.pingpong.jtella";

    /** Logger used by this adapter. */
    protected static Logger LOG = Logger.getLogger(LOGGER);
	
	/** 
	 * Determines the number of milliseconds between pings sent to find
	 * new nodes.
	 * 
	 * 1 800 000 ms = 30 minutes
	 */
	public static final int LOOKUP_PING_INTERVAL_MS = 1800000;
	
	private static final int SLEEP_TIME = 5000;
	private boolean shutdownFlag = false;
	private ConnectionList connectionList;
	private long lastLookupPing;
	
	/** Flag which determines if lookup pings should be periodically generated */
	private boolean lookupPingEnabled;

	/**
	 *  Constructor
	 *
	 *  @param connectionList connection list
	 */
	KeepAliveThread(ConnectionList connectionList, boolean lookupPingEnabled) {
		super("KeepAliveThread");
		this.connectionList = connectionList;
		this.lookupPingEnabled = lookupPingEnabled;
		lastLookupPing = 0;
	}

	/**
	 *  Stops the thread
	 *
	 */
	void shutdown() {
		shutdownFlag = true;
		interrupt();
	}

	public void run() {
		while (!shutdownFlag) {
			try {
				List<Connection> connections = connectionList.getActiveConnections();
				ListIterator<Connection> i = connections.listIterator();
				
				boolean sentLookup = false;
				long currentTime = System.currentTimeMillis();
				long lookupElapsed = currentTime - lastLookupPing;
				
				PingMessage lookupPing = null;
				if(lookupPingEnabled) {
					if (lookupElapsed > LOOKUP_PING_INTERVAL_MS) {
						LOG.info("Sending LOOKUP PING (TTL = 7)");
						// Node lookup ping
						lookupPing = new PingMessage();
						lookupPing.setTTL((byte)7);
					}
				}

				while (i.hasNext()) {
					Connection connection = i.next();

					try {
						if (connection.getStatus() == Connection.STATUS_OK) {
							long elapsedTime = currentTime - connection.getSendTime();

							if (lookupPing != null) {
								connection.prioritySend(lookupPing);
								sentLookup = true;
							} else if (elapsedTime >= SLEEP_TIME) {
									Log.getLog().logDebug(
										"Sending keep alive ping to: "
											+ connection.getConnectedServentIdAsString());
	
									PingMessage keepAlivePing = new PingMessage();
									keepAlivePing.setTTL((byte)1);
									connection.prioritySend(keepAlivePing);
							}
							
						}						
					}
					catch (IOException io) {
						Log.getLog().log(io);
					}
				}

				if(sentLookup) {
					lastLookupPing = currentTime;
				}
				
				sleep(SLEEP_TIME);
			}
			catch (Exception e) {
				Log.getLog().log(e);
			}
		}
	}
}
