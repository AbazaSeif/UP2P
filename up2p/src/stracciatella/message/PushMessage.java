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
package stracciatella.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import stracciatella.Connection;
import stracciatella.GUID;
import stracciatella.Utilities;

/**
 *  Push message, represents a request to push a file to a receiving node
 *
 *
 */
public class PushMessage extends Message {
	public static String UP2P_GGEP_ID = "up2pext";
	private GUID targetClientIdentifier;
	private int fileIndex;
	private String ipAddress;
	private short port;
	
	/** 
	 * The url prefix of the peer serving these results. If specified
	 * this will be included in a GGEP extension block with the ID
	 * "up2purl" 
	 **/
    private String urlPrefix;

	/**
	 *  Construct a PUSH message, indicates a node wants this servant
	 *  to connect and transfer a file
	 *
	 */
	PushMessage() {
		super(Message.PUSH);
	}

	/**
	 *  Construct a PushMessage from network data
	 *
	 *  @param rawMessage binary data from a connection
	 *  @param originatingConnection Connection creating this message
	 */
	public PushMessage(byte[] rawMessage, Connection originatingConnection) {
		super(rawMessage, originatingConnection);
	}

	/**
	 *  Construct a PushMessage using a previously received SearchReplyMessage.
	 *
	 *  @param searchReplyMessage search reply containing file to push
	 *  @param fileIndex index of file (from FileRecord) to push
	 *  @param ipAddress for push transfer
	 *  @param port port for push transfer
	 */
	public PushMessage(
		SearchReplyMessage searchReplyMessage,
		int fileIndex,
		String ipAddress,
		short port) {
		super(Message.PUSH);
		this.targetClientIdentifier = searchReplyMessage.getClientIdentifier();
		this.fileIndex = fileIndex;

		try {
			this.ipAddress = InetAddress.getByName(ipAddress).getHostAddress();
		} catch (UnknownHostException e) {
			this.ipAddress = ipAddress;
		}
		
		this.port = port;
		this.urlPrefix = null;

		buildPayload();
	}
	
	/**
	 *  Construct a PushMessage by specifying a client identifier.
	 *
	 *  @param clientIdentifier	The GUID of the client this message should
	 *  						be routed to.
	 *  @param fileIndex index of file (from FileRecord) to push
	 *  @param ipAddress for push transfer
	 *  @param port port for push transfer
	 */
	public PushMessage(
		GUID clientIdentifier,
		int fileIndex,
		String ipAddress,
		short port) {
		super(Message.PUSH);
		this.targetClientIdentifier = clientIdentifier;
		this.fileIndex = fileIndex;
		
		try {
			this.ipAddress = InetAddress.getByName(ipAddress).getHostAddress();
		} catch (UnknownHostException e) {
			this.ipAddress = ipAddress;
		}
		
		this.port = port;
		this.urlPrefix = null;

		buildPayload();
	}
	
	/**
	 *  Construct a PushMessage by specifying a client identifier.
	 *
	 *  @param clientIdentifier	The GUID of the client this message should
	 *  						be routed to.
	 *  @param fileIndex index of file (from FileRecord) to push
	 *  @param ipAddress for push transfer
	 *  @param port port for push transfer
	 *  @param urlPrefix	A url prefix which should be included in the GGEP
	 *  					extension block of the message
	 */
	public PushMessage(
		GUID clientIdentifier,
		int fileIndex,
		String ipAddress,
		short port,
		String urlPrefix) {
		super(Message.PUSH);
		this.targetClientIdentifier = clientIdentifier;
		this.fileIndex = fileIndex;

		try {
			this.ipAddress = InetAddress.getByName(ipAddress).getHostAddress();
		} catch (UnknownHostException e) {
			this.ipAddress = ipAddress;
		}
		
		this.port = port;
		this.urlPrefix = urlPrefix;

		buildPayload();
	}

	/**
	 *  Retrieve the client GUID targeted by this push request
	 *
	 *  @return client GUID
	 */
	public GUID getTargetIdentifier() {
		byte[] guidData = Arrays.copyOfRange(payload, 0, 16);
		
		return GUID.getGUID(guidData);
	}
	
	/**
	 * @return	The client GUID of the source of this push request
	 * 			Returns null if the push message does not carry a U-P2P
	 * 			extension block.
	 */
	public GUID getSourceIdentifier() {
		if(payload.length <= 26 ||
				payload[26] != 0x0087) {
			// Either no GGEP extension exists, or the flags were not recognized
			return null;
		}
		
		int payloadIndex = 27;
		for(char c : UP2P_GGEP_ID.toCharArray()) {
			if(c != (char)payload[payloadIndex]) {
				// GGEP extension ID did not match
				return null;
			}
			payloadIndex++;
		}
		
		// Skip the data length field (not terribly important since we know the first
		// 16 bytes will always be the servent ID)
		while(payloadIndex < payload.length) {
			if((payload[payloadIndex] & 0x0040) == 0x0040) {
				payloadIndex++;
				break;
			}
			payloadIndex++;
		}
		
		// payloadIndex should now point at the first byte of the servent ID
		byte[] guidData = new byte[16];
		System.arraycopy(payload, payloadIndex, guidData, 0, 16);
		return GUID.getGUID(guidData);
	}

	/**
	 *  Retrieve the index of the file to push
	 *
	 *  @return file index
	 */
	public int getFileIndex() {
		int byte1 = payload[16];
		int byte2 = payload[17];
		int byte3 = payload[18];
		int byte4 = payload[19];

		return byte1 | (byte2 << 8) | (byte3 << 16) | (byte4 << 24);
	}

	/**
	 *  Get the IP Address to push to 
	 *
	 *  @return IP address
	 */
	public String getIPAddress() {
		int byte1 = payload[20];
		int byte2 = payload[21];
		int byte3 = payload[22];
		int byte4 = payload[23];

		return (new Integer(byte1)).toString()
			+ "."
			+ (new Integer(byte2)).toString()
			+ "."
			+ (new Integer(byte3)).toString()
			+ "."
			+ (new Integer(byte4)).toString();
	}

	/**
	 *  Get the port the connection should use
	 *
	 *  @return port
	 */
	public short getPort() {
		int byte1 = payload[24];
		int byte2 = payload[25];

		return (short) (byte1 | (byte2 << 8));
	}
	
	/**
	 * Get the url prefix specified in the GGEP extension block
	 * (if one exists)
	 * 
	 * @return	The url prefix that should be used to push to the remote
	 * 			peer, or null if none was provided
	 */
	public String getUrlPrefix() {
		if(payload.length <= 26 ||
				payload[26] != 0x0087) {
			// Either no GGEP extension exists, or the flags were not recognized
			return null;
		}
		
		int payloadIndex = 27;
		for(char c : UP2P_GGEP_ID.toCharArray()) {
			if(c != (char)payload[payloadIndex]) {
				// GGEP extension ID did not match
				return null;
			}
			payloadIndex++;
		}
		
		// GGEP extension was found with valid flags and extension ID, read the data
		// length
		
		// Note: payloadIndex should now point to the first byte of the data
		// length field
		
		StringBuffer dataLengthString = new StringBuffer();
		while(payloadIndex < payload.length) {
			dataLengthString.append(Integer.toBinaryString(payload[payloadIndex] & 0x003F));
			if((payload[payloadIndex] & 0x0040) == 0x0040) {
				payloadIndex++;
				break;
			}
			payloadIndex++;
		}
		
		int dataLength = Integer.parseInt(dataLengthString.toString(), 2);
		// Skip the 16 byte servent ID, and reduce the data length accordingly
		payloadIndex = payloadIndex + 16;
		dataLength = dataLength - 16;
		
		// Got the data length, now read the url prefix
		StringBuffer urlPrefix = new StringBuffer();
		for(int i = 0; i < dataLength; i++) {	
			urlPrefix.append((char)payload[payloadIndex]);
			payloadIndex++;
		}
		return urlPrefix.toString();
	}

	/**
	 *   Builds the PUSH message payload
	 *
	 */
	void buildPayload() {
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream dataStream = new DataOutputStream(byteStream);

			// Target Servant Identifier
			byte[] targetGuidData =
				targetClientIdentifier.getData();
			for (int i = 0; i < targetGuidData.length; i++) {
				dataStream.writeByte((byte)targetGuidData[i]);
			}

			// File Index
			int indexByte1 = 0x000000FF & fileIndex;
			int indexByte2 = (0x0000FF00 & fileIndex) >> 8;
			int indexByte3 = (0x00FF0000 & fileIndex) >> 16;
			int indexByte4 = (0xFF000000 & fileIndex) >> 24;
			dataStream.writeByte(indexByte1);
			dataStream.writeByte(indexByte2);
			dataStream.writeByte(indexByte3);
			dataStream.writeByte(indexByte4);

			// IP Address TODO: update for IPV6
			int beginIndex = 0;
			int endIndex = ipAddress.indexOf('.');

			int ip1 =
				Integer.parseInt(ipAddress.substring(beginIndex, endIndex));

			beginIndex = endIndex + 1;
			endIndex = ipAddress.indexOf('.', beginIndex);

			int ip2 =
				Integer.parseInt(ipAddress.substring(beginIndex, endIndex));

			beginIndex = endIndex + 1;
			endIndex = ipAddress.indexOf('.', beginIndex);

			int ip3 =
				Integer.parseInt(ipAddress.substring(beginIndex, endIndex));

			beginIndex = endIndex + 1;

			int ip4 =
				Integer.parseInt(
					ipAddress.substring(beginIndex, ipAddress.length()));

			dataStream.write(ip1);
			dataStream.write(ip2);
			dataStream.write(ip3);
			dataStream.write(ip4);

			// Port (little endian)
			int byte1 = 0x00FF & port;
			int byte2 = (0xFF00 & port) >> 8;
			dataStream.write(byte1);
			dataStream.write(byte2);
			
			if(urlPrefix != null) {
				// GGEP extension - URL prefix
				
				// Flags specify:
				// Bit 7 set - Last GGEP extension in this message
				// Bit 6 clear - No Encoding
				// Bit 5 clear - No compression
				// Bit 4 clear - Reserved, always clear
				// Bits 0 - 3 - Length of ID field
				int GgepFlags = 0x0087;
				dataStream.write(GgepFlags);
				
				// Write the extension ID
				for(char c : UP2P_GGEP_ID.toCharArray()) {
					dataStream.write(c);
				}
				
				// Write the data length
				// (see http://rfc-gnutella.sourceforge.net/src/rfc-0_6-draft.html
				//  for how this is encoded)
				int ggepLength = urlPrefix.length() + 16; // Servent ID is always 16 bytes
				String binaryLength = Integer.toBinaryString(ggepLength);
				int numBytes = binaryLength.length() / 6;
				if(binaryLength.length() % 6 != 0) {
					numBytes++;
				}
				
				int leadingZeroes = 0;
				for(int i = 0; i < numBytes; i++) {
					StringBuffer writeBinary = new StringBuffer();
					if(i == numBytes - 1) {
						// Last byte
						writeBinary.append("01");
					} else {
						writeBinary.append("10");
					}
					
					// Pack with leading zeros on the first byte if the bit length of 
					// the length field is not a clean multiple of 6
					if(i == 0 && binaryLength.length() % 6 != 0) {
						for(int j = 0; j < 6 - (binaryLength.length() % 6); j++) {
							writeBinary.append("0");
							leadingZeroes++;
						}
					}
					
					if(i == 0) {
						writeBinary.append(binaryLength.substring(i, i + (6 - leadingZeroes)));
					} else {
						writeBinary.append(binaryLength.substring((i * 6) - leadingZeroes, (i * 6) + (6 - leadingZeroes)));
					}
					
					dataStream.write(Integer.parseInt(writeBinary.toString(), 2));
				}
				
				// Add the source servent ID
				// Servant Identifier
				byte[] sourceGuidData =
					Utilities.getClientGUID().getData();
				for (int i = 0; i < sourceGuidData.length; i++) {
					dataStream.writeByte(sourceGuidData[i]);
				}
				
				// Add the actual URL prefix
				for(char c : urlPrefix.toCharArray()) {
					dataStream.write(c);
				}
			}
			
			addPayload(byteStream.toByteArray());
			dataStream.close();
		}
		catch (IOException io) {}
	}

}
