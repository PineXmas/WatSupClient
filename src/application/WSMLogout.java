package application;

public class WSMLogout extends WSMNoData {

	public WSMLogout(int opcode, int dataLength, byte[] data, WSClientHandler sender) {
		super(opcode, dataLength, data, sender);
	}
	
	public WSMLogout() {
		super(WSMCode.OPCODE_LOGOUT);
	}

}
