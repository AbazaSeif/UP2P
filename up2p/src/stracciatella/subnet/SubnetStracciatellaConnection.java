package stracciatella.subnet;


	import java.io.IOException;

	import stracciatella.ConnectionData;
	import stracciatella.SearchSession;
import stracciatella.StracciatellaConnection;
	import stracciatella.message.MessageReceiver;
import stracciatella.routing.Router;
	import stracciatella.subnet.SubNetRouter;
import stracciatella.subnet.SubNetSearchSession;

	public class SubnetStracciatellaConnection extends StracciatellaConnection {		
		/**
		 * Constructs an empty connection, the application must add a host cache or
		 * servant to generate activity
		 */
		public SubnetStracciatellaConnection() throws IOException {
			this(null);
		}

		/**
		 * Construct the connection specifying connection data. The connection will
		 * not have access to a host cache unless specified later.
		 *
		 * @param connData Local connection data
		 **/
		public SubnetStracciatellaConnection(ConnectionData connData) throws IOException {
			super(connData);
		}
		
		/**
		 * Generates and returns the router thread for the Gnutella connection. This produces
		 * a SUBNETQUERY capable router (standard Gnutella message handling is unchanged).
		 * @return The router thread for the Gnutella connection.
		 */
		protected Router initializeRouter() {
			return new SubNetRouter(getConnections(), getConnectionData(), getHostCache());
		}
		
		/** 
		 * Creates a sub-network capable session to conduct network searches
		 *
		 * @param query Search criteria
		 * @param queryType The type of query (see SubNetSearchMessage)
		 * @param subNetId The sub-network identifier to search on
		 * @param maxResults Maximum result set size
		 * @param minSpeed Minimum speed for responding servants
		 * @param receiver Receiver for search responses
		 * @return session The resulting SubNetSearchSession
		 */
		public SearchSession createSubNetSearchSession(
			String query,
			int queryType,
			String subNetId,
			int maxResults,
			int minSpeed,
			MessageReceiver receiver) {
				return new SubNetSearchSession(
					query,
					queryType,
					subNetId,
					maxResults,
					minSpeed,
					this,
					getRouter(),
					receiver);
		}

	}