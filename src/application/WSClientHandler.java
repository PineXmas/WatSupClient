package application;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WSClientHandler {
	Socket clientSocket;
	Thread listener;
	final BlockingQueue<WSMessage> receivingMsgQueue;
	
	/**
	 * Store the user name bound to this client-socket
	 */
	String userName = null;
	
	/**
	 * Store messages waiting to be sent to the client
	 */
	LinkedBlockingQueue<WSMessage> sendingMsgQueue = new LinkedBlockingQueue<>();
	
	/**
	 * Keep waiting on the sending-message-queue & send message to client
	 */
	Thread sender;
	
	public WSClientHandler(Socket clientSocket, BlockingQueue<WSMessage> receivingMsgQueue) {
		this.clientSocket = clientSocket;
		this.receivingMsgQueue = receivingMsgQueue;
		WSClientHandler myself = this;
		
		//set up listener thread
		listener = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					//set up input stream reader
					InputStream inputStream = clientSocket.getInputStream();
					byte[] buff = new byte[1024];
					
					//keep reading from client
					int readBytes;
					byte[] arrIncompleteMsg = new byte[0];
					ArrayList<WSMessage> listMsgs;
					ArrayList<Byte> listNewIncompleteMsg = new ArrayList<>();
					while (true) {
						ErrandBoy.println("Waiting for input from client " + getName());
						readBytes = inputStream.read(buff);
						if (readBytes <= 0) {
							break;
						}
						
						//parse read bytes into messages
						byte[] actualReadBuff = ByteBuffer.allocate(readBytes).put(buff, 0, readBytes).array();
						listMsgs = WSMessage.parse2Msgs(actualReadBuff, arrIncompleteMsg, listNewIncompleteMsg);
						ErrandBoy.println("  Read " + listMsgs.size() + " complete msgs from client " + getName());
						
						//update incomplete message
						arrIncompleteMsg = ErrandBoy.convertList2Array(listNewIncompleteMsg);
						if (arrIncompleteMsg.length > 0) {
							ErrandBoy.println("  Incomplete msg from client " + getName() + ": " + new String(arrIncompleteMsg));
						}
						
						//enqueue messages
						for (WSMessage msg : listMsgs) {
							msg.clientHandler = myself;
							receivingMsgQueue.put(msg);
						}
						
					}
					
					//reach here means the input stream has reach eof
					if (readBytes < 0) {
						ErrandBoy.println("Client " + getName() + " input is EOF, reading is stopped");
					}
				} catch (Exception e) {
					ErrandBoy.printlnError(e, "Error while reading from client's input stream");
				} finally {
					// TODO finalize client socket after error occurs
				}
				
			}
		});
	
		//set up sender thread
		sender = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				try {
					//get client's output stream
					OutputStream outputStream = clientSocket.getOutputStream();
					
					ErrandBoy.println("Waiting to send messages to client " + getName() + " ...");
					WSMessage msg;
					while ( !((msg = sendingMsgQueue.take()) instanceof WSMStopSerer)) {
						ErrandBoy.println("--> Server sends to client " + getName() + ":");
						ErrandBoy.println("    " + msg.toString());
						
						//send the message
						outputStream.write(msg.msgBytes);
					}
					
					ErrandBoy.println("Sender-thread of client " + getName() + " has stopped");
				} catch (Exception e) {
					ErrandBoy.printlnError(e, "Error while sending message to client " + getName());
				}
				
			}
		});
	}
	
	/**
	 * Start 2 threads: reading from client's input stream & writing to client's output stream 
	 */
	public void start() {
		if (clientSocket == null) {
			ErrandBoy.println("Client socket is null, cannot listen from");
			return;
		}
		
		listener.start();
		sender.start();
	}
	
	/**
	 * Try to stop the 2 threads by closing the socket
	 */
	public void stop() {
		
		try {
			//enqueue the stop-server message to signal the sender-thread to stop sending messages
			enqueueMessage(new WSMStopSerer());
			
			clientSocket.close();
		} catch (Exception e) {
			ErrandBoy.printlnError(e, "Error when closing client socket " + getName());
		}
	}
	
	/**
	 * Enqueue the given message. The message will be sent later by the sender. <br>
	 * <strong>NOTE</strong>: calling this function will start a new thread to enqueue. This is to make sure the caller is not blocked by the SYNCHRONIZED queue.  
	 */
	public void enqueueMessage(WSMessage msg) {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					sendingMsgQueue.put(msg);
				} catch (InterruptedException e) {
					ErrandBoy.printlnError(e, "Error while enqueueing to sending-queue of client " + getName());
				}
				
			}
		}).start();
	}
	
	/**
	 * Get the name of this client: user-name@socket-address
	 */
	public String getName() {
		String prefix = "<null-user>";
		if (userName != null) {
			prefix = userName;
		}
		
		return "[" + prefix + "@"+ getClientName(clientSocket) + "]";
	}
	
	/**
	 * Return client's name in form of: address & port
	 * @param clientSocket
	 * @return
	 */
	public static String getClientName(Socket clientSocket) {
		if (clientSocket == null) {
			return "<null-socket>";
		}
		return clientSocket.getInetAddress().getHostName() + ":" + clientSocket.getPort();
	}
}
