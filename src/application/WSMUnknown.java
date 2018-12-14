package application;

public class WSMUnknown extends WSMessage {
	public String dataAsText;
	
	/**
	 * This constructor mostly is used in case the message is received from a socket-input-stream
	 * @param opcode first 4 byte of the message
	 * @param dataLength next 4 byte of the message
	 * @param data remaining bytes of the message
	 * @param sender the socket where the message is received (optional, could be NULL)
	 */
	public WSMUnknown(int opcode, int dataLength, byte[] data, WSClientHandler sender) {
		super(opcode, dataLength, data, sender);
	}

	@Override
	public void parse2Attributes() {
		dataAsText = new String(msgBytes, 8, dataLength);
	}

	@Override
	public String toString() {
		return opcode + "|" + dataLength + "|" + dataAsText;
	}

}
