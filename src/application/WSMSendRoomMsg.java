package application;

public class WSMSendRoomMsg extends WSMOneLabelChatMsg {
	public WSMSendRoomMsg(int opcode, int dataLength, byte[] msgBytes, WSClientHandler sender) {
		super(opcode, dataLength, msgBytes, sender);
	}
	
	public WSMSendRoomMsg(String name, String chatContent) {
		super(WSMCode.OPCODE_SEND_ROOM_MSG, name, chatContent);
	}
}
