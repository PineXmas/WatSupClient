package application;

import java.nio.ByteBuffer;

public class WSMLeaveRoom extends WSMessage {

	public String roomName;
	
	public WSMLeaveRoom(int opcode, int dataLength, byte[] msgBytes, WSClientHandler sender) {
		super(opcode, dataLength, msgBytes, sender);
	}
	
	public WSMLeaveRoom(String roomName) {
		
		//opcode
		opcode = WSMCode.OPCODE_LEAVE_ROOM;
		
		//data length
		dataLength = WSSettings._LABEL_SIZE;
		
		//user name
		int nameLength = Math.min(roomName.length(), WSSettings._LABEL_SIZE);
		this.roomName = roomName.substring(0, nameLength);
		
		//message-byte-array
		msgBytes = ByteBuffer.allocate(8 + WSSettings._LABEL_SIZE)
				.putInt(opcode.rawCode)
				.putInt(dataLength)
				.put(roomName.getBytes(), 0, nameLength).array();
	}

	@Override
	public void parse2Attributes() {
		roomName = parse2String(msgBytes, 8, msgBytes.length);
	}

	@Override
	public String toString() {
		
		return opcode + "|" +  dataLength + "|" + roomName;
	}

	@Override
	public String toStringOfBytes() {
		return 	super.toStringOfBytes() + "|" +
				displayBytes(msgBytes, 8, msgBytes.length-8)
				;
	}

}
