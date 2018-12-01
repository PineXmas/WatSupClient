package application;

import java.nio.ByteBuffer;

public class WSMLogin extends WSMessage {

	public String userName;
	
	public WSMLogin(int opcode, int dataLength, byte[] msgBytes, WSClientHandler sender) {
		super(opcode, dataLength, msgBytes, sender);
	}
	
	public WSMLogin(String userName) {
		
		
		//opcode
		opcode = WSMCode.OPCODE_LOGIN;
		
		//data length
		dataLength = WSSettings._LABEL_SIZE;
		
		//user name
		int nameLength = Math.min(userName.length(), WSSettings._LABEL_SIZE);
		this.userName = userName.substring(0, nameLength);
		
		//message-byte-array
		msgBytes = ByteBuffer.allocate(8 + WSSettings._LABEL_SIZE)
				.putInt(opcode.rawCode)
				.putInt(dataLength)
				.put(userName.getBytes(), 0, nameLength).array();
	}

	@Override
	public void parse2Attributes() {
		userName = parse2String(msgBytes, 8, msgBytes.length);
	}

	@Override
	public String toString() {
		
		return opcode + "|" +  dataLength + "|" + userName;
	}

	@Override
	public String toStringOfBytes() {
		return 	super.toStringOfBytes() + "|" +
				displayBytes(msgBytes, 8, msgBytes.length-8)
				;
	}

}
