package application;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Observable;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class DemoCodes {
	public Button btnSendChat;
	public ListView<String> listUsers;
	public Button btnShowList;
	public TextArea txtChat;
	//public Button btnSendChat1;
	public TextArea txtChatWindow;
	ObservableList<String> list = FXCollections.observableArrayList();
	
	/**
	 * demo function
	 * @param event
	 */
	public void send_chat(ActionEvent event)
	{
		System.out.println("hello");
		
		try {
			//connect to server at "localhost" 
			Socket clientSocket = new Socket();
		    InetSocketAddress hostAddress = new InetSocketAddress("192.168.43.11", 8312);
			clientSocket.connect(hostAddress);
			
			//if reach here -> server has been connected
			System.out.println("server connected");
			
			//prepare message: opcode + data-length + data
			int opcode = 3;
			String s = "hello server";
			int dataLength = s.length();
			
			//construct message
			ByteBuffer msg = ByteBuffer.allocate(4 + 4 + dataLength);
			msg.putInt(opcode);
			msg.putInt(dataLength);
			msg.put(s.getBytes()); 
			
			//get output stream from socket
			OutputStream writer = clientSocket.getOutputStream();
			
			//write to output stream to send to server
			writer.write(msg.array());
			System.out.println("done sending");
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		//display message box
		/*
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Information Dialog");
		alert.setHeaderText(null);
		alert.setContentText("I have a great message for you!");

		alert.showAndWait();
		*/
	}
	
	public static void main() {
		//test whatever you want here
		ArrayList<String> array = new ArrayList<>();
		array.add("an");
		System.out.println(array.get(0));
		
		String name = "hello";
		int len = name.length();
		System.out.println(len);
	}
//	InvalidationListener listener_listRooms = new InvalidationListener() {
//		@Override
//		public void invalidated(Observable observable) {
//			
//			int index = listRooms.getSelectionModel().getSelectedIndex();
//			String currentRoom = listRoomData.get(index).NameRoom;
//			System.out.println(currentRoom);
//			
//			for (int i = 0; i < listJoinedRoom.size(); ++i)
//			{
//				if (currentRoom.compareTo(listJoinedRoom.get(i).NameRoom) == 0)
//					roomToRemove = currentRoom;
//			}
//			
//			
//			
//		}
//	};	
}





