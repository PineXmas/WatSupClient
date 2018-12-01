package application;

import java.nio.ByteBuffer;

public class WSMListRoomsResp extends WSMessage {
	
	public String[] arrRoomNames;
	
	public WSMListRoomsResp(int opcode, int dataLength, byte[] msgBytes, WSClientHandler sender) {
		super(opcode, dataLength, msgBytes, sender);
	}
	
	public WSMListRoomsResp(String[] arrRoomNames) {
		//opcode
		opcode = WSMCode.OPCODE_LIST_ROOMS_RESP;
		
		//attributes
		this.arrRoomNames = new String[arrRoomNames.length];
		
		//data length
		dataLength = arrRoomNames.length * WSSettings._LABEL_SIZE;
		
		//message-byte-array
		ByteBuffer buff = ByteBuffer.allocate(8 + dataLength).putInt(opcode.rawCode).putInt(dataLength);
		for (int i = 0; i < arrRoomNames.length; i++) {
			//update name array
			String name = verifyLabelLength(arrRoomNames[i]);
			this.arrRoomNames[i] = name;
			
			// prepare name buffer
			ByteBuffer nameBuff = ByteBuffer.allocate(WSSettings._LABEL_SIZE);
			nameBuff.put(name.getBytes());
			
			//copy onto message-byte-array
			buff.put(nameBuff.array());
		}
		msgBytes = buff.array();
	}
	
	@Override
	public void parse2Attributes() {
		int nRooms = dataLength / WSSettings._LABEL_SIZE;
		arrRoomNames = new String[nRooms];
		
		for (int i = 0; i < nRooms; i++) {
			arrRoomNames[i] = parse2String(msgBytes, 8 + i*WSSettings._LABEL_SIZE, WSSettings._LABEL_SIZE);
		}
	}

	@Override
	public String toString() {
		String s = opcode + "|" + dataLength;
		
		for (int i = 0; i < arrRoomNames.length; i++) {
			s += "|" + arrRoomNames[i];
		}
		
		return s;
	}

	@Override
	public String toStringOfBytes() {
		String s = super.toStringOfBytes();
		
		for (int i = 0; i < arrRoomNames.length; i++) {
			s += "|" + displayBytes(msgBytes, 8 + i*WSSettings._LABEL_SIZE, WSSettings._LABEL_SIZE);
		}
		
		return s;
	}
}
