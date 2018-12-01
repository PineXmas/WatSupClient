package application;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class WSMError extends WSMessage {

	public WSMCode errCode;
	
	public WSMError(int opcode, int dataLength, byte[] msgBytes, WSClientHandler sender) {
		super(opcode, dataLength, msgBytes, sender);
	}
	
	public WSMError(WSMCode errCode) {
		opcode = WSMCode.OPCODE_ERROR;
		dataLength = 4;
		this.errCode = errCode;
		
		//parse to bytes: opcode, data-length, data
		msgBytes = ByteBuffer.allocate(12).putInt(opcode.rawCode).putInt(dataLength).putInt(errCode.rawCode).array();
	}
	
	@Override
	public void parse2Attributes() {
		errCode = WSMCode.GetCode(ByteBuffer.wrap(Arrays.copyOfRange(msgBytes, 8, 12)).getInt() );

	}

	@Override
	public String toString() {
		return opcode + "|" + dataLength + "|" + errCode;
	}

	@Override
	public String toStringOfBytes() {
		return super.toStringOfBytes() + "|" + displayBytes(msgBytes, 8, 4);
	}
}
