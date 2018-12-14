package application;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Used as a base class for all of our message types. This class also provide many utility functions useful for message processing.
 * @author pinex
 *
 */
public abstract class WSMessage {
	
	/***********************************************
	 * [MEMBERS]
	 ***********************************************/
	
	WSMCode opcode = WSMCode.OPCODE_UNKNOWN;
	int dataLength = 0;
	byte[] msgBytes = {};
	WSClientHandler clientHandler;
	
	/***********************************************
	 * [STATIC METHODS]
	 ***********************************************/
	
	/** 
	 * Determine opcode from the given byte-array
	 * @param arrBytes the bytes read so far from the input stream, could be empty
	 * @return the opcode or -1 if not enough byte to determine
	 */
	static public int getOpcode(byte[] arrBytes) {
		if (arrBytes.length < 4) {
			return -1;
		}
		
		return ByteBuffer.allocate(4).put(arrBytes, 0, 4).getInt(0);
	}
	
	/** 
	 * Determine data-length from the given byte-array
	 * @param arrBytes the bytes read so far from the input stream, could be empty
	 * @return the data-length or -1 if not enough byte to determine
	 */
	static public int getDataLength(byte[] arrBytes) {
		if (arrBytes.length < 8) {
			return -1;
		}
		
		return ByteBuffer.allocate(4).put(arrBytes, 4, 4).getInt(0);
	}
	
	/** 
	 * Determine remaining-byes-to-read from the given byte-array, return -1 if not enough byte to determine
	 * @param arrBytes the bytes read so far from the input stream, could be empty
	 * @return the remaining-byes-to-read or -1 if not enough byte to determine
	 */
	static public int getRemainingBytes2Read(byte[] arrBytes) {
		if (arrBytes.length < 8) {
			return -1;
		}
		
		int need2Read = getDataLength(arrBytes);
		int readSoFar = arrBytes.length - 8;
		
		return need2Read - readSoFar;
	}	
	
	/**
	 * Parse the given byte-array into messages (and return leftover bytes if available)
	 * @param arrReadBytes array of bytes read from the input stream
	 * @param arrIncompleteMsg array of bytes from an incomplete message
	 * @param listNewIncompleteMsg list of bytes left after this operation (an incomplete message)
	 * @return list of 0 or more complete messages
	 */
	static public ArrayList<WSMessage> parse2Msgs(byte[] arrReadBytes, byte[] arrIncompleteMsg, ArrayList<Byte> listNewIncompleteMsg){
		return parse2Msgs(arrReadBytes, arrIncompleteMsg, listNewIncompleteMsg, null);
	}
	
	/**
	 * Create the specific message according to the given opcode. The created message is always a subclass of the class WSMessage.
	 * @param opcode
	 * @param dataLength
	 * @param msgBytes the message in forms of array of bytes
	 * @return
	 */
	static public WSMessage createSpecificMsg(int opcode, int dataLength, byte[] msgBytes) {
		WSMCode code = WSMCode.GetCode(opcode);
		
		switch (code) {
		
		case OPCODE_LOGIN:
			return new WSMLogin(opcode, dataLength, msgBytes, null);
		case OPCODE_LOGIN_SUCCESS:
			return new WSMLoginSuccess(opcode, dataLength, msgBytes, null);
		case OPCODE_LOGOUT:
			return new WSMLogout(opcode, dataLength, msgBytes, null);
		case OPCODE_LIST_ROOMS:
			return new WSMListRooms(opcode, dataLength, msgBytes, null);
		case OPCODE_LIST_ROOMS_RESP:
			return new WSMListRoomsResp(opcode, dataLength, msgBytes, null);
		case OPCODE_LIST_USERS_RESP:
			return new WSMListUsersResp(opcode, dataLength, msgBytes, null);
		case OPCODE_LIST_USERS_ALL:
			return new WSMListUsersAll(opcode, dataLength, msgBytes, null);
		case OPCODE_JOIN_ROOM:
			return new WSMJoinRoom(opcode, dataLength, msgBytes, null);
		case OPCODE_LEAVE_ROOM:
			return new WSMLeaveRoom(opcode, dataLength, msgBytes, null);
		case OPCODE_SEND_ROOM_MSG:
			return new WSMSendRoomMsg(opcode, dataLength, msgBytes, null);
		case OPCODE_TELL_ROOM_MSG:
			return new WSMTellRoomMsg(opcode, dataLength, msgBytes, null);
		case OPCODE_SEND_PRIVATE_MSG:
			return new WSMSendPrivateMsg(opcode, dataLength, msgBytes, null);
		case OPCODE_TELL_PRIVATE_MSG:
			return new WSMTellPrivateMsg(opcode, dataLength, msgBytes, null);
		case OPCODE_ERROR:
			return new WSMError(opcode, dataLength, msgBytes, null);
		default:
			break;
			
		}
		
		return new WSMUnknown(WSMCode.OPCODE_UNKNOWN.rawCode, dataLength, msgBytes, null);
	}
	
	/**
	 * Parse the given byte-array into messages (and return leftover bytes if available)
	 * @param arrReadBytes array of bytes read from the input stream
	 * @param arrIncompleteMsg array of bytes from an incomplete message
	 * @param listNewIncompleteMsg list of bytes left after this operation (an incomplete message)
	 * @param sender the socket where the message is received (optional, could be NULL)
	 * @return list of 0 or more complete messages
	 */
	static public ArrayList<WSMessage> parse2Msgs(byte[] arrReadBytes, byte[] arrIncompleteMsg, ArrayList<Byte> listNewIncompleteMsg, Socket sender){
		ArrayList<WSMessage> listComplete = new ArrayList<>();
		int currPos = 0;
		int currOpcode = -1;
		int currDataLength = -1;
		int remainingBytes = -1;
		
		//init the new-incomplete to be the curr-incomplete
		for (int i = 0; i < arrIncompleteMsg.length; i++) {
			listNewIncompleteMsg.add(arrIncompleteMsg[i]);
		}
		
		//determine opcode, data-length, remaining-bytes-to-read
		currOpcode = getOpcode(arrIncompleteMsg);
		currDataLength = getDataLength(arrIncompleteMsg);
		remainingBytes = getRemainingBytes2Read(arrIncompleteMsg);
		
		
		//read bytes & construct messages
		while (currPos < arrReadBytes.length) {
			
			//check for complete message: add to complete-list & reset the incomplete message
			if (currOpcode != -1 && currDataLength != -1 && remainingBytes == 0) {
				WSMessage msg = createSpecificMsg(currOpcode, currDataLength, ErrandBoy.convertList2Array(listNewIncompleteMsg));
				listComplete.add(msg);
				
				currOpcode = -1;
				currDataLength = -1;
				remainingBytes = -1;
				listNewIncompleteMsg.clear();
			}
			
			//read next byte
			Byte currByte = arrReadBytes[currPos];
			listNewIncompleteMsg.add(currByte);
			currPos++;
			
			//read opcode if not yet
			if (currOpcode == -1) {
				currOpcode = getOpcode(ErrandBoy.convertList2Array(listNewIncompleteMsg));
				continue;
			}
			
			//read data-length if not yet
			if (currDataLength == -1) {
				currDataLength = getDataLength(ErrandBoy.convertList2Array(listNewIncompleteMsg));
				
				//compute remaining-bytes-to-read
				remainingBytes = getRemainingBytes2Read(ErrandBoy.convertList2Array(listNewIncompleteMsg));
				
				continue;
			}
			
			//decrease remaining-bytes-to-read
			--remainingBytes;
		}
		
		//try to finalize the incomplete message before exit
		if (currOpcode != -1 && currDataLength != -1 && remainingBytes == 0) {
			
			WSMessage msg = createSpecificMsg(currOpcode, currDataLength, ErrandBoy.convertList2Array(listNewIncompleteMsg));
			listComplete.add(msg);
			listNewIncompleteMsg.clear();
		}
		
		return listComplete;
	}
	
	/**
	 * Parse the given bytes into a label, which could be used for user name or room name.
	 * @param arrBytes the byte-array containing the label
	 * @param offset beginning index of the label-segment in the array
	 * @param length length of the label-segment in the array, mostly equal LABEL_SIZE
	 * @return
	 */
	static public String parse2String(byte[] arrBytes, int offset, int length) {
		int labelLength = length;
		for (int i = offset; i < offset + length; i++) {
			if (arrBytes[i] == 0) {
				labelLength = i-offset;
				break;
			}
		}
		
		return new String(arrBytes, offset, labelLength);
	}
	
	/**
	 * Present the given byte segment as a string of hex values 
	 * @param data a byte array
	 * @param offset starting index
	 * @param length length of bytes to process
	 * @return
	 */
	public static String displayBytes(byte[] data, int offset, int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = offset; i < offset + length; i++) {
			sb.append(String.format("%02x ", data[i]));
		}
		
		return sb.toString();
	}
	
    /**
     * Generate an array of 8 bytes storing opcode & data-length (0) only. Useful for message without additional data.
     * @param opcode
     * @param dataLength
     * @return
     */
	public static byte[] genBasicMsgBytes(int opcode, int dataLength) {
		return ByteBuffer.allocate(8).putInt(opcode).putInt(dataLength).array();
	}
	
	/**
	 * Shorten the given label if it is longer than LABEL_SIZE
	 * @param label
	 * @return the shortened label
	 */
	public static String verifyLabelLength(String label) {
		if (label.length() > WSSettings._LABEL_SIZE) {
			return label.substring(0, WSSettings._LABEL_SIZE);
		}
		
		return label;
	}
	
	/***********************************************
	 * [METHODS]
	 ***********************************************/
	
	public WSMessage() {
	}
	
	public WSMessage(int opcode, int dataLength, byte[] msgBytes) {
		this(opcode, dataLength, msgBytes, null);
	}
	
	/**
	 * This constructor mostly is used in case the message is received from a socket-input-stream
	 * @param opcode first 4 byte of the message
	 * @param dataLength next 4 byte of the message
	 * @param msgBytes remaining bytes of the message
	 * @param sender the socket where the message is received (optional, could be NULL)
	 */
	public WSMessage(int opcode, int dataLength, byte[] msgBytes, WSClientHandler sender) {
		this.opcode = WSMCode.GetCode(opcode);
		this.dataLength = dataLength;
		this.msgBytes = msgBytes;
		this.clientHandler = sender;
		
		parse2Attributes();
	}
	
	/**
	 * Present the message-bytes in forms of decimal values. </br>
	 * *** <strong> DO NOT</strong> use inherited class's attributes to display
	 */
	public String toStringOfBytes() {
		return 
				displayBytes(msgBytes, 0, 4) + "|" +
				displayBytes(msgBytes, 4, 4)
				;
	}
	
	/***********************************************
	 * [ABSTRACT METHODS]
	 ***********************************************/
	
	/**
	 * Parse the data-bytes into different attributes, depending on particular message types.
	 */
	public abstract void parse2Attributes(); 
	
	/**
	 * Present the message in human-readable text
	 */
	public abstract String toString();
	
	/***********************************************
	 * [TESTING]
	 ***********************************************/
	
	public static void main() {
		testMessages();
	}
	
	public static void testMessages() {
//		//test message: LOGIN
//		WSMLogin msgLogin = new WSMLogin("abcd");
//		ErrandBoy.println(msgLogin.toString());
//		ErrandBoy.println(msgLogin.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMLogin msgLogin02 = new WSMLogin(msgLogin.opcode.rawCode, msgLogin.dataLength, msgLogin.msgBytes, null);
//		ErrandBoy.println(msgLogin02.toString());
//		ErrandBoy.println(msgLogin02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		
//		//test message: LOGOUT
//		WSMLogout msgLogout = new WSMLogout();
//		ErrandBoy.println(msgLogout.toString());
//		ErrandBoy.println(msgLogout.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMLogout msgLogout02 = new WSMLogout(msgLogout.opcode.rawCode, msgLogout.dataLength, msgLogout.msgBytes, null);
//		ErrandBoy.println(msgLogout02.toString());
//		ErrandBoy.println(msgLogout02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		
//		//test message: LOGIN_SUCCESS
//		WSMLoginSuccess msgLoginSuccess = new WSMLoginSuccess();
//		ErrandBoy.println(msgLoginSuccess.toString());
//		ErrandBoy.println(msgLoginSuccess.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMLoginSuccess msgLoginSuccess02 = new WSMLoginSuccess(msgLoginSuccess.opcode.rawCode, msgLoginSuccess.dataLength, msgLoginSuccess.msgBytes, null);
//		ErrandBoy.println(msgLoginSuccess02.toString());
//		ErrandBoy.println(msgLoginSuccess02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		
//		//test message: KEEPALIVE
//		WSMKeepAlive msgKeepAlive = new WSMKeepAlive();
//		ErrandBoy.println(msgKeepAlive.toString());
//		ErrandBoy.println(msgKeepAlive.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMKeepAlive msgKeepAlive02 = new WSMKeepAlive(msgKeepAlive.opcode.rawCode, msgKeepAlive.dataLength, msgKeepAlive.msgBytes, null);
//		ErrandBoy.println(msgKeepAlive02.toString());
//		ErrandBoy.println(msgKeepAlive02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		
//		//test message: LIST_ROOMS
//		WSMListRooms msgListRooms = new WSMListRooms();
//		ErrandBoy.println(msgListRooms.toString());
//		ErrandBoy.println(msgListRooms.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMListRooms msgListRooms02 = new WSMListRooms(msgListRooms.opcode.rawCode, msgListRooms.dataLength, msgListRooms.msgBytes, null);
//		ErrandBoy.println(msgListRooms02.toString());
//		ErrandBoy.println(msgListRooms02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		
//		//test message: ERROR
//		WSMCode[] arrErrs = {WSMCode.ERR_ILLEGAL_OPCODE, WSMCode.ERR_KICKED_OUT, WSMCode.ERR_NAME_EXISTS, WSMCode.ERR_TOO_MANY_ROOMS, WSMCode.ERR_TOO_MANY_USERS, WSMCode.ERR_UNKNOWN};
//		for (WSMCode errCode : arrErrs) {
//			WSMError msgError = new WSMError(errCode);
//			ErrandBoy.println(msgError.toString());
//			ErrandBoy.println(msgError.toStringOfBytes());
//			ErrandBoy.println("-----------------------------------------------");
//			WSMError msgError02 = new WSMError(msgError.opcode.rawCode, msgError.dataLength, msgError.msgBytes, null);
//			ErrandBoy.println(msgError02.toString());
//			ErrandBoy.println(msgError02.toStringOfBytes());
//			ErrandBoy.println("");
//			ErrandBoy.println("");
//		} 
//		
//		//test message: LIST_ROOMS_RESP
//		String[] arrRoomNames = {"PSU", "TheLongestRoomNameEverExistingInTheServerOfWatSup", "Hello Kitty", "An The Bad Girl", "Thong The Good Boy"};
//		WSMListRoomsResp msgListRoomsResp = new WSMListRoomsResp(arrRoomNames);
//		ErrandBoy.println(msgListRoomsResp.toString());
//		ErrandBoy.println(msgListRoomsResp.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMListRoomsResp msgListRoomsResp02 = new WSMListRoomsResp(msgListRoomsResp.opcode.rawCode, msgListRoomsResp.dataLength, msgListRoomsResp.msgBytes, null);
//		ErrandBoy.println(msgListRoomsResp02.toString());
//		ErrandBoy.println(msgListRoomsResp02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");		
//
//		//test message: LIST_USERS_RESP
//		String[] arrUserNames = {"thong", "an", "dai", "thao"};
//		WSMListUsersResp msgListUsersResp = new WSMListUsersResp("PSU", arrUserNames);
//		ErrandBoy.println(msgListUsersResp.toString());
//		ErrandBoy.println(msgListUsersResp.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMListUsersResp msgListUsersResp02 = new WSMListUsersResp(msgListUsersResp.opcode.rawCode, msgListUsersResp.dataLength, msgListUsersResp.msgBytes, null);
//		ErrandBoy.println(msgListUsersResp02.toString());
//		ErrandBoy.println(msgListUsersResp02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//
//		//test message: JOIN ROOM
//		WSMJoinRoom msgJoinRoom = new WSMJoinRoom("PSU");
//		ErrandBoy.println(msgJoinRoom.toString());
//		ErrandBoy.println(msgJoinRoom.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMJoinRoom msgJoinRoom02 = new WSMJoinRoom(msgJoinRoom.opcode.rawCode, msgJoinRoom.dataLength, msgJoinRoom.msgBytes, null);
//		ErrandBoy.println(msgJoinRoom02.toString());
//		ErrandBoy.println(msgJoinRoom02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//
//		//test message: LEAVE ROOM
//		WSMLeaveRoom msgLeaveRoom = new WSMLeaveRoom("PSU PhD degree");
//		ErrandBoy.println(msgLeaveRoom.toString());
//		ErrandBoy.println(msgLeaveRoom.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMLeaveRoom msgLeaveRoom02 = new WSMLeaveRoom(msgLeaveRoom.opcode.rawCode, msgLeaveRoom.dataLength, msgLeaveRoom.msgBytes, null);
//		ErrandBoy.println(msgLeaveRoom02.toString());
//		ErrandBoy.println(msgLeaveRoom02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//
//		//test message: SEND ROOM MSG
//		WSMSendRoomMsg msgSendRoomMsg = new WSMSendRoomMsg("thong", "there's always a reason to meet someone in our life");
//		ErrandBoy.println(msgSendRoomMsg.toString());
//		ErrandBoy.println(msgSendRoomMsg.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMSendRoomMsg msgSendRoomMsg02 = new WSMSendRoomMsg(msgSendRoomMsg.opcode.rawCode, msgSendRoomMsg.dataLength, msgSendRoomMsg.msgBytes, null);
//		ErrandBoy.println(msgSendRoomMsg02.toString());
//		ErrandBoy.println(msgSendRoomMsg02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//
//		//test message: SEND PRIVATE MSG
//		WSMSendPrivateMsg msgSendPrivateMsg = new WSMSendPrivateMsg("my partner", "you are the best thing ever happened to my life");
//		ErrandBoy.println(msgSendPrivateMsg.toString());
//		ErrandBoy.println(msgSendPrivateMsg.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMSendPrivateMsg msgSendPrivateMsg02 = new WSMSendPrivateMsg(msgSendPrivateMsg.opcode.rawCode, msgSendPrivateMsg.dataLength, msgSendPrivateMsg.msgBytes, null);
//		ErrandBoy.println(msgSendPrivateMsg02.toString());
//		ErrandBoy.println(msgSendPrivateMsg02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//
//		//test message: TELL PRIVATE MSG
//		WSMTellPrivateMsg msgTellPrivateMsg = new WSMTellPrivateMsg("thong", "is this trial-and-error period a requirement for meeting you, babe?");
//		ErrandBoy.println(msgTellPrivateMsg.toString());
//		ErrandBoy.println(msgTellPrivateMsg.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMTellPrivateMsg msgTellPrivateMsg02 = new WSMTellPrivateMsg(msgTellPrivateMsg.opcode.rawCode, msgTellPrivateMsg.dataLength, msgTellPrivateMsg.msgBytes, null);
//		ErrandBoy.println(msgTellPrivateMsg02.toString());
//		ErrandBoy.println(msgTellPrivateMsg02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		
//		//test message: TELL ROOM MSG
//		WSMTellRoomMsg msgTellRoomMsg = new WSMTellRoomMsg("thong", "strangers", "does any body see my one? Tell her that I'm standing next to the Pine tree!");
//		ErrandBoy.println(msgTellRoomMsg.toString());
//		ErrandBoy.println(msgTellRoomMsg.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//		WSMTellRoomMsg msgTellRoomMsg02 = new WSMTellRoomMsg(msgTellRoomMsg.opcode.rawCode, msgTellRoomMsg.dataLength, msgTellRoomMsg.msgBytes, null);
//		ErrandBoy.println(msgTellRoomMsg02.toString());
//		ErrandBoy.println(msgTellRoomMsg02.toStringOfBytes());
//		ErrandBoy.println("-----------------------------------------------");
//
		//test message: LIST_USERS_ALL
		String[] arrUserNames = {"thong", "an", "dai", "thao"};
		WSMListUsersAll msgListUsersAll = new WSMListUsersAll(arrUserNames);
		ErrandBoy.println(msgListUsersAll.toString());
		ErrandBoy.println(msgListUsersAll.toStringOfBytes());
		ErrandBoy.println("-----------------------------------------------");
		WSMListUsersAll msgListUsersAll02 = new WSMListUsersAll(msgListUsersAll.opcode.rawCode, msgListUsersAll.dataLength, msgListUsersAll.msgBytes, null);
		ErrandBoy.println(msgListUsersAll02.toString());
		ErrandBoy.println(msgListUsersAll02.toStringOfBytes());
		ErrandBoy.println("-----------------------------------------------");
	}
}
