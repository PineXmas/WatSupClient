package application;

public class WSMKeepAlive extends WSMNoData {

	public WSMKeepAlive(int opcode, int dataLength, byte[] data, WSClientHandler sender) {
		super(opcode, dataLength, data, sender);
	}
	
	public WSMKeepAlive() {
		super(WSMCode.OPCODE_KEEPALIVE);
	}

}
