package application;

import java.nio.ByteBuffer;

public class WSMListUsersResp extends WSMessage {
	
	public String roomName;
	public String[] arrUserNames;
	
	public WSMListUsersResp(int opcode, int dataLength, byte[] msgBytes, WSClientHandler sender) {
		super(opcode, dataLength, msgBytes, sender);
	}
	
	public WSMListUsersResp(String roomName, String[] arrUserNames) {
		//opcode
		opcode = WSMCode.OPCODE_LIST_USERS_RESP;
		
		//attributes
		this.roomName = verifyLabelLength(roomName);
		this.arrUserNames = new String[arrUserNames.length];
		
		//data length
		dataLength = (arrUserNames.length +1)* WSSettings._LABEL_SIZE;
		
		//message-byte-array
		ByteBuffer buff = ByteBuffer.allocate(8 + dataLength).putInt(opcode.rawCode).putInt(dataLength);
		ByteBuffer nameBuff = ByteBuffer.allocate(WSSettings._LABEL_SIZE);
		buff.put(nameBuff.put(roomName.getBytes()).array());
		for (int i = 0; i < arrUserNames.length; i++) {
			//update name array
			String name = verifyLabelLength(arrUserNames[i]);
			this.arrUserNames[i] = name;
			
			// prepare name buffer
			nameBuff = ByteBuffer.allocate(WSSettings._LABEL_SIZE);
			nameBuff.put(name.getBytes());
			
			//copy onto message-byte-array
			buff.put(nameBuff.array());
		}
		msgBytes = buff.array();
	}
	
	@Override
	public void parse2Attributes() {
		//room name
		roomName = parse2String(msgBytes, 8, WSSettings._LABEL_SIZE);
		
		//users' names
		int nUsers = dataLength / WSSettings._LABEL_SIZE - 1;
		arrUserNames = new String[nUsers];
		for (int i = 0; i < nUsers; i++) {
			arrUserNames[i] = parse2String(msgBytes, 8 + (i+1)*WSSettings._LABEL_SIZE, WSSettings._LABEL_SIZE);
		}
	}

	@Override
	public String toString() {
		String s = opcode + "|" + dataLength;
		
		//room
		s += "|(room)" + roomName;
		
		//users
		for (int i = 0; i < arrUserNames.length; i++) {
			s += "|" + arrUserNames[i];
		}
		
		return s;
	}

	@Override
	public String toStringOfBytes() {
		String s = super.toStringOfBytes();
		
		//room
		s += "|" + displayBytes(msgBytes, 8, WSSettings._LABEL_SIZE);
		
		//users
		for (int i = 0; i < arrUserNames.length; i++) {
			s += "|" + displayBytes(msgBytes, 8 + (i+1)*WSSettings._LABEL_SIZE, WSSettings._LABEL_SIZE);
		}
		
		return s;
	}
}
