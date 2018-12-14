package application;

public enum WSMCode {
	
	//opcodes
	OPCODE_UNKNOWN (-1),
	OPCODE_ERROR (1001),
	OPCODE_KEEPALIVE (1002),
	OPCODE_LOGIN (1003),
	OPCODE_LOGOUT (1004),
	OPCODE_LOGIN_SUCCESS (1005),
	OPCODE_LIST_ROOMS (1006),
	OPCODE_LIST_ROOMS_RESP (1007),
	OPCODE_LIST_USERS_RESP (1008),
	OPCODE_JOIN_ROOM (1009),
	OPCODE_LEAVE_ROOM (1010),
	OPCODE_SEND_ROOM_MSG (1011),
	OPCODE_TELL_ROOM_MSG (1012),
	OPCODE_SEND_PRIVATE_MSG (1013),
	OPCODE_TELL_PRIVATE_MSG (1014),
	OPCODE_LIST_USERS_ALL (1015),
	
	//error codes
	ERR_UNKNOWN (2001),
	ERR_ILLEGAL_OPCODE (2002),
	ERR_NAME_EXISTS (2003),
	ERR_TOO_MANY_USERS (2004),
	ERR_TOO_MANY_ROOMS (2005),
	ERR_KICKED_OUT (2006),
	
	;
	
	public int rawCode;
	
	WSMCode(int rawCode) {
		this.rawCode = rawCode;
	}
	
	/**
	 * Return the enum corresponding to the given opcode
	 * @param rawCode the code specified in the RFC document
	 * @return
	 */
	public static WSMCode GetCode(int rawCode) {
		for (WSMCode code : WSMCode.values()) {
			if (code.rawCode == rawCode) {
				return code;
			}
		}
		
		return WSMCode.OPCODE_UNKNOWN;
	}
}
