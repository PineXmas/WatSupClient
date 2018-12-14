package application;

import java.nio.ByteBuffer;

/**
 * Used as base class for 3 messages: SEND_PRIVATE_MSG, TELL_PRIVATE_MSG & SEND_ROOM_MSG, since they all require only 1 label for sender/receiver's name
 * @author pinex
 *
 */
public abstract class WSMOneLabelChatMsg extends WSMessage {

	public String name;
	public String chatContent;
	
	public WSMOneLabelChatMsg(int opcode, int dataLength, byte[] msgBytes, WSClientHandler sender) {
		super(opcode, dataLength, msgBytes, sender);
	}
	
	public WSMOneLabelChatMsg(WSMCode opcode, String name, String chatContent) {
		
		//opcode
		this.opcode = opcode;
		
		//data length
		dataLength = WSSettings._LABEL_SIZE + chatContent.length();
		
		//attributes
		int nameLength = Math.min(name.length(), WSSettings._LABEL_SIZE);
		this.name = name.substring(0, nameLength);
		this.chatContent = chatContent;
		
		//message-byte-array
		msgBytes = ByteBuffer.allocate(8 + dataLength)
				.putInt(this.opcode.rawCode)
				.putInt(dataLength)
				.put(ByteBuffer.allocate(WSSettings._LABEL_SIZE).put (name.getBytes(), 0, nameLength).array())
				.put(chatContent.getBytes())
				.array()
				;
	}

	@Override
	public void parse2Attributes() {
		name = parse2String(msgBytes, 8, WSSettings._LABEL_SIZE);
		chatContent = parse2String(msgBytes, 8 + WSSettings._LABEL_SIZE, dataLength - WSSettings._LABEL_SIZE);
	}

	@Override
	public String toString() {
		
		return opcode + "|" +  dataLength + "|(name)" + name + "|(chat)" + chatContent;
	}

	@Override
	public String toStringOfBytes() {
		return 	super.toStringOfBytes() + "|" +
				displayBytes(msgBytes, 8, WSSettings._LABEL_SIZE) + "|" +
				displayBytes(msgBytes, 8 + WSSettings._LABEL_SIZE, dataLength - WSSettings._LABEL_SIZE)
				;
	}

}
