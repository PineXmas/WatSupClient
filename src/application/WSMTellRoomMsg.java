package application;

import java.nio.ByteBuffer;

/**
 * Used as base class for 3 messages: SEND_PRIVATE_MSG, TELL_PRIVATE_MSG & SEND_ROOM_MSG, since they all require only 1 label for sender/receiver's name
 * @author pinex
 *
 */
public class WSMTellRoomMsg extends WSMessage {

	public String userName;
	public String roomName;
	public String chatContent;
	
	public WSMTellRoomMsg(int opcode, int dataLength, byte[] msgBytes, WSClientHandler sender) {
		super(opcode, dataLength, msgBytes, sender);
	}
	
	public WSMTellRoomMsg(String userName, String roomName, String chatContent) {
		
		//opcode
		this.opcode = WSMCode.OPCODE_TELL_ROOM_MSG;
		
		//data length
		dataLength = 2*WSSettings._LABEL_SIZE + chatContent.length();
		
		//attributes
		int userNameLength = Math.min(userName.length(), WSSettings._LABEL_SIZE);
		this.userName = userName.substring(0, userNameLength);
		int roomNameLength = Math.min(roomName.length(), WSSettings._LABEL_SIZE);
		this.roomName = roomName.substring(0, roomNameLength);
		this.chatContent = chatContent;
		
		//message-byte-array
		msgBytes = ByteBuffer.allocate(8 + dataLength)
				.putInt(this.opcode.rawCode)
				.putInt(dataLength)
				.put(ByteBuffer.allocate(WSSettings._LABEL_SIZE).put (userName.getBytes(), 0, userNameLength).array())
				.put(ByteBuffer.allocate(WSSettings._LABEL_SIZE).put (roomName.getBytes(), 0, roomNameLength).array())
				.put(chatContent.getBytes())
				.array()
				;
	}

	@Override
	public void parse2Attributes() {
		userName = parse2String(msgBytes, 8, WSSettings._LABEL_SIZE);
		roomName = parse2String(msgBytes, 8 + WSSettings._LABEL_SIZE, WSSettings._LABEL_SIZE);
		chatContent = parse2String(msgBytes, 8 + 2*WSSettings._LABEL_SIZE, dataLength - 2*WSSettings._LABEL_SIZE);
	}

	@Override
	public String toString() {
		
		return opcode + "|" +  dataLength + "|(user)" + userName + "|(room)" + roomName + "|(chat)" + chatContent;
	}

	@Override
	public String toStringOfBytes() {
		return 	super.toStringOfBytes() + "|" +
				displayBytes(msgBytes, 8, WSSettings._LABEL_SIZE) + "|" +
				displayBytes(msgBytes, 8 + WSSettings._LABEL_SIZE, WSSettings._LABEL_SIZE) + "|" +
				displayBytes(msgBytes, 8 + 2*WSSettings._LABEL_SIZE, dataLength - 2*WSSettings._LABEL_SIZE)
				;
	}

}
