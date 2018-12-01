package application;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.concurrent.LinkedBlockingQueue;

import javax.print.DocFlavor.STRING;
import javax.swing.JOptionPane;

import org.omg.CosNaming.NamingContextExtPackage.AddressHelper;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class Controller {
	String serverHost = "192.168.43.136";
	int portNumber = 8312;
	String myUserName;

	int currDisplayRoomIndex = -1;			//index in joined room
	int roomIndex;				//index in room list
	boolean privateChat = false;
	boolean loginSuccess = false;
	
	public ArrayList<User> listUserData = new ArrayList<>();		//List of all users		
	public ArrayList<ChatRoom> listRoomData = new ArrayList<>();	//List chat room
	public ArrayList<ChatRoom> listJoinedRoom = new ArrayList<>();	//List joined room
	public ArrayList<String> listJoinedRoomNames = new ArrayList<>();
	public Button btnSignIn;	
	public Button btnNewRoom; 
	public Button btnSendChat;
	public Button btnLeaveRoom;
	
	public TextField txtUserName;	
	public TextField txtNewRoom;
	
	public TextArea txtChat;
	public TextArea txtChatWindow;
	
	public TabPane tabUserList;
	
	public ListView<String> listRooms;
	public ListView<String> listAllUser;
	public ListView<String> listRoomUser;
	
	public Tab tabRoom;
	public Tab tabAll;
		
	public Label lbChatWindow;
	 
	ObservableList<String> ObserAllUser = FXCollections.observableArrayList();
	ObservableList<String> ObserAllRoom = FXCollections.observableArrayList();
	ObservableList<String> ObserRoomUser = FXCollections.observableArrayList();
	
	Socket clientSocket;
	Controller myself = null;
	Stage currStage = null;
	Thread serverListener = new Thread(new Runnable() {

		
		@Override
		public void run() {

			
			try {
				if (clientSocket == null) {
					System.out.println("Client socket is null. Stop listening from server");
					return;
				}
				
				if (!clientSocket.isConnected()) {
					System.out.println("Client socket is not connected. Stop listening from server");
					return;
				}
				
				//set up input stream reader
				InputStream inputStream = clientSocket.getInputStream();
				byte[] buff = new byte[1024];
				
				//keep reading from client
				int readBytes;
				byte[] arrIncompleteMsg = new byte[0];
				ArrayList<WSMessage> listMsgs;
				ArrayList<Byte> listNewIncompleteMsg = new ArrayList<>();
				while (true) {
					ErrandBoy.println("Waiting for input from server ");
					readBytes = inputStream.read(buff);
					if (readBytes <= 0) {
						break;
					}
					
					//parse read bytes into messages
					byte[] actualReadBuff = ByteBuffer.allocate(readBytes).put(buff, 0, readBytes).array();
					listMsgs = WSMessage.parse2Msgs(actualReadBuff, arrIncompleteMsg, listNewIncompleteMsg);
					ErrandBoy.println("  Read " + listMsgs.size() + " complete msgs from server ");
					
					//update incomplete message
					arrIncompleteMsg = ErrandBoy.convertList2Array(listNewIncompleteMsg);
					if (arrIncompleteMsg.length > 0) {
						ErrandBoy.println("  Incomplete msg from server: " + new String(arrIncompleteMsg));
					}
					
					//enqueue messages
					for (WSMessage msg : listMsgs) {
						msg.clientHandler = null;
						receivingMsgQueue.put(msg);
					}
					
				}
				
				//reach here means the input stream has reach eof
				if (readBytes < 0) {
					ErrandBoy.println("Server input is EOF, reading is stopped");
				}
			} catch (Exception e) {
				ErrandBoy.printlnError(e, "Error while reading from client's input stream");
			} finally {
				// TODO finalize client socket after error occurs
			}
			
		}
	
	});
	InetSocketAddress hostAddress = new InetSocketAddress(serverHost, portNumber);
	Thread msgReceiver = new  Thread(new Runnable() {


		@Override
		public void run() {



			if (receivingMsgQueue == null) {
				ErrandBoy.println("Received-message queue is not init. Receiver quits.");
				return;
			}
			
			try {
				ErrandBoy.println("Waiting to process messages in the queue...");
				WSMessage msg;
				
				
				while ( !((msg = receivingMsgQueue.take()) instanceof WSMStopSerer)) {
					
					try {
						
						ErrandBoy.println("Server sent: " + msg.toString());
						
						//process messages
						
						if (msg.opcode == WSMCode.OPCODE_LOGIN_SUCCESS) {
							send2Server(new WSMListRooms());
							
							Platform.runLater(new Runnable() {
								
								@Override
								public void run() {
									
									switch2ChatRoom();
								}
							});
						} 
						else if (msg.opcode == WSMCode.OPCODE_LIST_ROOMS_RESP) {
							WSMListRoomsResp msgListRoomResp = (WSMListRoomsResp) msg;
							for (int i = 0; i < (msgListRoomResp.arrRoomNames.length); ++i) {
								if (!checkRoomExists(msgListRoomResp.arrRoomNames[i])) {
									listRoomData.add(new ChatRoom(msgListRoomResp.arrRoomNames[i]));
								}
											
							}
							Platform.runLater(new Runnable() {
								
								@Override
								public void run() {
									displayRoomNames();
									
								}
							});
																			
						} 
						
						else if (msg.opcode == WSMCode.OPCODE_LIST_USERS_RESP) {
							/*
							 * Cases to handle:
							 * - case 01_A: the room is NOT one of my joined room and I'm OUTSIDE
							 * 		+ local copy: remove all users
							 * 		+ server copy: duplicate list users into local copy
							 * 		
							 * - case 01_B: the room is NOT one of my joined room and I'm INSIDE
							 * 		+ local copy: remove all users
							 * 		+ server copy: duplicate list users into local copy
							 * 		+ add the room to list of JOINED ROOMS & update current-display-room-index
							 * 
							 * - case 02_A: the room is one of my JOINED ROOMS and I'm still INSIDE
							 * 		+ local copy: remove all users
							 * 		+ server copy: duplicate list users into local copy
							 * 
							 * - case 02_B: the room is one of my JOINED ROOMS and I'm OUTSIDE
							 * 		+ local copy: remove all users
							 * 		+ server copy: duplicate list users into local copy
							 * 		+ check if currDisplayRoomIndex is this room --> YES --> set it to -1
							 * 		+ remove the room out of listJoinedRooms
							 */
							
							WSMListUsersResp msgListUsersResp = (WSMListUsersResp) msg;
							int found;
							int serverRoomIndex = searchRoom(msgListUsersResp.roomName, listRoomData);
							
							found = searchRoom(msgListUsersResp.roomName, listJoinedRoom);
							if (found == -1) {
								if (serverRoomIndex >= 0) {
									ChatRoom theRoom = listRoomData.get(serverRoomIndex);
									theRoom.RoomUsers.clear();
									for (String userName : msgListUsersResp.arrUserNames) {
										theRoom.addNewUser(userName);
									}
									
									found = searchUser(myUserName, msgListUsersResp.arrUserNames);
									if (found == -1) {
										//case 01_A: NOT joined room & i am OUTSIDE
										
										//// do nothing
									} else {
										//case 01_B: NOT joined room & i am INSIDE
										
										////add the room to listJoinedRoom
										listJoinedRoom.add(theRoom);
										
										////we assume this is the case of creating new room, so set currDisplayRoomIndex to this room
										currDisplayRoomIndex = serverRoomIndex;
									}
									
								}
			
							} else {
								ChatRoom theJoinedRoom = listJoinedRoom.get(found);
								int theJoinedRoomIndexInListJoined = found;
								
								found = searchUser(myUserName, msgListUsersResp.arrUserNames);
								if (found >= 0) {
									//case 02_A: JOINED room & i am INSIDE
									theJoinedRoom.RoomUsers.clear();
									for (String userName : msgListUsersResp.arrUserNames) {
										theJoinedRoom.addNewUser(userName);
									}
								} else {
									//case 02_B: JOINED room & i am OUTSIDE
									theJoinedRoom.RoomUsers.clear();
									for (String userName : msgListUsersResp.arrUserNames) {
										theJoinedRoom.addNewUser(userName);
									}
									
									////check currDisplayRoomIndex
									if (currDisplayRoomIndex == serverRoomIndex) {
										currDisplayRoomIndex = -1;
									}
									
									////remove the room out of listJoinedRooms
									listJoinedRoom.remove(theJoinedRoomIndexInListJoined);
								}
								
								
							}
							
							//update UI
							displayUI();
							
							//old code
							
							/**
							
							WSMListUsersResp msgListUsersResp = (WSMListUsersResp) msg;
							int indexRoomFromServer = searchRoom(msgListUsersResp.roomName, listRoomData);
							if (indexRoomFromServer != -1) {
								String [] arrayUserName = msgListUsersResp.arrUserNames;
								
								//remove leaving-users from client list
								ChatRoom room = listRoomData.get(indexRoomFromServer);
								for (int i = room.RoomUsers.size()-1; i >= 0; i--) {
									User user = room.RoomUsers.get(i);
									int found = searchUser(user.UserName, room);
									if (found == -1) {
										room.RoomUsers.remove(i);
									}
								}
								
								//if code reaches this line, means there is no leaving-user in my list
								
								//update new user (if available) to the room
					
								boolean myselfBelongToTheRoom = false;
								for (int j = 0; j < arrayUserName.length; ++j) {
									if (myUserName.equals(arrayUserName[j])) {
										myselfBelongToTheRoom = true;
									}
									
									if (searchUser(arrayUserName[j], listRoomData.get(indexRoomFromServer)) >= 0) {
										continue;
									}
									
									listRoomData.get(indexRoomFromServer).addNewUser(arrayUserName[j]);
								}
								
								//check if MYSELF belongs to the room --> YES --> add to JOINED-ROOM list
								if (myselfBelongToTheRoom) {
									if (searchRoom(msgListUsersResp.roomName, listJoinedRoom) == -1) {
										listJoinedRoom.add(listRoomData.get(indexRoomFromServer));
									}
									
									
									
								}
								
								//double check: go through all JOINED ROOMS, if myself does not exist in some room, remove that room from this list
								for (int j2 = listJoinedRoom.size()-1; j2 >=0 ; j2--) {
									if (searchUser(myUserName, listJoinedRoom.get(j2)) == -1) {
										listJoinedRoom.remove(j2);
									}
								}
								
								if (indexRoomFromServer == currDisplayRoomIndex) {
									Platform.runLater(new Runnable() {
										
										@Override
										public void run() {
											displayChatHistory();
											displayRoomUsers(currDisplayRoomIndex);												
										}
									});
								}
								
							} else {
								//code should not reach here
							}
		
							Platform.runLater(new Runnable() {
								
								@Override
								public void run() {

									displayRoomNames();
									displayRoomUsers(currDisplayRoomIndex);
									
								}
							});
						
						
							 */													
						}
						
						else if (msg.opcode == WSMCode.OPCODE_LIST_USERS_ALL) {
							WSMListUsersAll msgListUserAll = (WSMListUsersAll) msg;
							for (int i = 0; i < (msgListUserAll.arrUserNames.length); ++i) {							
								if (!checkAllUserExist(msgListUserAll.arrUserNames[i])) {
									listUserData.add(new User(msgListUserAll.arrUserNames[i]));
									
														
								}{
									
								}
							}
							Platform.runLater(new Runnable() {
								
								@Override
								public void run() {
									displayAllUserList();
									
								}
							});						
						} 
						else if (msg.opcode == WSMCode.OPCODE_ERROR) {
							WSMError msgError = (WSMError) msg;
							if (msgError.errCode == WSMCode.ERR_NAME_EXISTS) {
								Platform.runLater(new Runnable() {

									
									@Override
									public void run() {

											
										displayErrorMsg("Username has already been taken");
								
									}
								});
							}
							else if (msgError.errCode == WSMCode.ERR_KICKED_OUT) {
								Platform.runLater(new Runnable() {									
									@Override
									public void run() {
											
										displayErrorMsg("You are have been kicked out");
								
									}
								});
							}
							else if (msgError.errCode == WSMCode.ERR_TOO_MANY_ROOMS) {
								Platform.runLater(new Runnable() {									
									@Override
									public void run() {
											
										displayErrorMsg("Max room limit reached");
								
									}
								});
							}
							else if (msgError.errCode == WSMCode.ERR_TOO_MANY_USERS) {
								Platform.runLater(new Runnable() {	
								
									@Override
									public void run() {

											
										displayErrorMsg("Max user limit reached");
								
									}
								});
							}
													
						} 
						else if (msg.opcode == WSMCode.OPCODE_TELL_ROOM_MSG) {
							WSMTellRoomMsg msgRoomMsg = (WSMTellRoomMsg) msg;
							int index = searchRoom(msgRoomMsg.roomName, listRoomData);
							if (index >= 0){
								listRoomData.get(index).addChat(msgRoomMsg.userName + " ::::: " + msgRoomMsg.chatContent);
								
							}
							Platform.runLater(new Runnable() {
								
								@Override
								public void run() {
									displayChatHistory();
									
								}
							});	
						}
						else if (false) {
						} else {
							/*
							 * ignore all other message types
							 */
							ErrandBoy.println("Server send un-expected message " + msg.opcode + ". Ignore the message.");
						
						}
						
						
					} catch (Exception e) {
						ErrandBoy.printlnError(e, "Error while processing message: " + msg.opcode + " from server");
					}
				}
				
				ErrandBoy.println("Receiver-thread has stopped");
			} catch (Exception e) {
				ErrandBoy.printlnError(e, "Error when processing received messages");
			}
		}
	
	});
	
	LinkedBlockingQueue<WSMessage> receivingMsgQueue = new LinkedBlockingQueue<>();	
	
	/**
	 * Update the whole UI according to the current state of data.<br>
	 * <strong>NOTE:</strong> this function will be called later by the UI-thread, using Platform.runLater(...)
	 */
	public void displayUI() {
		Platform.runLater(new Runnable() {
			
			@Override
			public void run() {
				//UI: list room names
				displayRoomNames();
				
				//UI: chat history
				displayChatHistory();
			}
		});
	}
	
	/**
	 * Add fake room and users for testing purposes
	 */
	public void genFakeData() {
		User user1 = new User("user1-1");
		User user2 = new User("user2-1");
		User user3 = new User("user2-2");
		User user4 = new User("user3-1");
		
		listUserData.add(new User("dr Thong"));
		listUserData.add(new User("thu ky An hay ngu"));
		listUserData.add(user1);
		listUserData.add(user2);
		listUserData.add(user3);
		listUserData.add(user4);
		
		ChatRoom room1 = new ChatRoom("room1");
		room1.RoomUsers.add(user1);
		
		ChatRoom room2 = new ChatRoom("room2");
		room2.RoomUsers.add(user2);
		room2.RoomUsers.add(user3);

		ChatRoom room3 = new ChatRoom("room3");
		room3.RoomUsers.add(user4);
		
		listRoomData.add(room1);
		listRoomData.add(room2);
		listRoomData.add(room3);
	}
	
	/**
	 * if the "Leave" button is clicked on, the user will be removed from the 
	 * Joined Room List 
	 * @param even
	 */
	public void onBtnLeaveRoom_Click(ActionEvent event) 
	{
		if (currDisplayRoomIndex == -1) {
			return;
		}
		
		String currentRoomName = listRoomData.get(currDisplayRoomIndex).NameRoom;
		WSMLeaveRoom leaveRoom = new WSMLeaveRoom(currentRoomName);
		send2Server(leaveRoom);
		
	}

	/**
	 * Display Chat Window when "Log In" is clicked
	 * @param event
	 */
	public void onBtnSignIn_Click(ActionEvent event) {
		myUserName = txtUserName.getText();
		
		
		listUserData.add(new User(myUserName));
		//genFakeData();

		try {
			//TODO make sure start threads once only!!!!
			
			if (clientSocket == null || !clientSocket.isConnected()) {
				// connect to server at "localhost"
				clientSocket = new Socket();

				// this functions will block until server is reached
				clientSocket.connect(hostAddress);
				
				// if reach here -> server has been connected
				System.out.println("server connected");
			}
	
			//start serverListener & msgReceiver
			if (!serverListener.isAlive()) {
				serverListener.start();	
			}
			
			if (!msgReceiver.isAlive()) {
				msgReceiver.start();	
			}
		
			//send LOG_IN msg
			WSMLogin logIn = new WSMLogin(myUserName);
			send2Server(logIn);
	
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	
	}

	/**
	 * Switch to chat room window
	 */
	public void switch2ChatRoom() {
		try {
			
			FXMLLoader chatRoomLoader = new FXMLLoader(getClass().getResource("ChatRoom.fxml"));
			chatRoomLoader.setController(this);
			Parent root = chatRoomLoader.load();
			Scene scene = new Scene(root);

			currStage.setScene(scene);
			currStage.setTitle(myUserName);
			displayAllUserList();
			displayRoomNames();
			
			btnLeaveRoom.setDisable(true);
			tabRoom.setDisable(true);
			tabUserList.getSelectionModel().select(tabAll);

			
		} catch (IOException e) {
			e.printStackTrace();
		
		}
	}

	/**
	 * Display ERROR message
	 */
	public void displayErrorMsg(String errorMsg) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(errorMsg);

		alert.showAndWait();
	}

	/**
	 * Send message to server
	 * @param msg
	 */
 	public void send2Server(WSMessage msg)  {
		try {
			if (clientSocket == null) {
				System.out.println("Client socket is null. Stop sending msg 2 server");
				return;
			}
			if (!clientSocket.isConnected()) {
				System.out.println("Not connected to server. Stop sending msg 2 server");
				return;
			}
			
			OutputStream output = clientSocket.getOutputStream();
			output.write(msg.msgBytes);
		} catch (IOException e) {
			System.out.println("Cannot send to server " + msg.opcode);
		}
	}

 	/**
	 * Double click on an username on the ALL USERs list to
	 * create a private chat
	 */
	public void onListUser_Click(MouseEvent event) {
		if (event.getClickCount() == 2) {
			txtChatWindow.clear();
			privateChat = true;
			int index = listAllUser.getSelectionModel().getSelectedIndex();
			// TODO ***critical: currDisplayRoomIndex = use name instead of index
			currDisplayRoomIndex= index;
			displayPrivateChat();
			
						
		}
		else
			return;
		
	}

	/**
	 * Function to create a new room when button "Create" is clicked
	 * @param event
	 */
	public void new_room(ActionEvent event)
	{
		String new_Room = txtNewRoom.getText();	
		WSMJoinRoom room = new WSMJoinRoom(new_Room);
		send2Server(room);
		txtNewRoom.clear();
		
	}

	/**
	 * Double click on an element in the listRoom of to join room
	 * Single click on an element in the JOINED listRoom to enable
	 * 		"Leave Room" button
	 * @param event
	 */
	public void onListRooms_Click(MouseEvent event) {
		int index = listRooms.getSelectionModel().getSelectedIndex();
		if (index == -1) {
			return;
		}
		
		
		if (event.getClickCount() == 1)
		{
			//identify the select room
			
			String selectedRoomName = listRoomData.get(index).NameRoom;
			roomIndex = index;
			tabRoom.setDisable(true);
			tabUserList.getSelectionModel().select(tabAll);
			
			//Search if the selected Room Name is in the JOINED list
			for (int i = 0; i < listJoinedRoom.size(); ++i)
			{
				
				if (selectedRoomName.compareTo(listJoinedRoom.get(i).NameRoom) == 0)
				{
					// TODO ***critical: currDisplayRoomIndex = use name instead of index
					currDisplayRoomIndex = i;
					displayRoomUsers(index);
					displayChatHistory();					
					btnLeaveRoom.setDisable(false);		
					tabRoom.setDisable(false);
					
					privateChat = false;
				}
				
				else
				{
					//btnLeaveRoom.setDisable(true);
					//tabRoom.setDisable(true);				
				}				
			}
		}
		else if (event.getClickCount() == 2)
		{
			String selectedRoomName = listRoomData.get(index).NameRoom;
			// TODO ***critical: currDisplayRoomIndex = use name instead of index
			currDisplayRoomIndex = index;
			
			//if the selected room is one of my JOINED rooms --> currDisplayRoomIndex = selcted
			int found = searchRoom(selectedRoomName, listJoinedRoom);
			if (found >= 0) {
				displayChatHistory();
				displayRoomUsers(currDisplayRoomIndex);
				return;
			}
			
			//otherwise, send JOIN_ROOM msg & currDisplayRoomIndex = selected
			WSMJoinRoom msg = new WSMJoinRoom(selectedRoomName);
			send2Server(msg);	

		}
	
					
	
	}

	/**
	 * Display chat history for private chat
	 */
	public void displayPrivateChat()
	{
		String [] chatHistory = listUserData.get(currDisplayRoomIndex).displayPrivateChat();
		//ArrayList<String> l = new ArrayList<>(Arrays.asList(chatHistory));
		for (int i = 0; i < chatHistory.length; ++i) {
			txtChatWindow.appendText(chatHistory[i]);
			
		}
	}

	/**
	 * Display Chat convos for each selected joined Room
	 * @param roomIndex
	 */
	public void displayChatHistory() {

		//user does not select any conversation
		if (currDisplayRoomIndex < 0) {
			txtChatWindow.clear();
			lbChatWindow.setText("<Select a ROOM or an USER to see chat>");
			return;
		}
		
		/********************************************************************
		 * ROOM CHAT
		 ********************************************************************/
		if (!privateChat) {
			//update chat content
			ChatRoom room = listJoinedRoom.get(currDisplayRoomIndex);
			String [] chatHistory = room.displayChat();
			txtChatWindow.clear();
			for (int i = 0; i < chatHistory.length; ++i) {
				txtChatWindow.appendText(chatHistory[i] + "\n");
			}
			lbChatWindow.setText(room.NameRoom);
			
			//update list of users in the room
			String [] arrayUserName = room.dislayAllUser();
			ArrayList<String> l = new ArrayList<>(Arrays.asList(arrayUserName));
			ObserRoomUser.clear();
			ObserRoomUser.addAll(l);
			listRoomUser.setItems(ObserRoomUser);
			
			//display tab: room users
			tabRoom.setText(room.NameRoom);
			tabUserList.getSelectionModel().select(tabRoom);
			return;	
		}
		
		
		/********************************************************************
		 * PRIVATE CHAT
		 ********************************************************************/
	}

	/**
	 * Display a list of all users in the current JOINED room
	 * @param roomIndex
	 */
	public void displayRoomUsers(int roomIndex) {
		if (roomIndex < 0 || roomIndex >= listRoomData.size()) {
			return;
		}
		
		String [] arrayUserName = listRoomData.get(roomIndex).dislayAllUser();
		ArrayList<String> l = new ArrayList<>(Arrays.asList(arrayUserName));
		ObserRoomUser.clear();
		ObserRoomUser.addAll(l);
		listRoomUser.setItems(ObserRoomUser);
	}

	/**
	 * Display the list of available ROOMs
	 */
	public void displayRoomNames()
	{
		ObserAllRoom.clear();
		for (ChatRoom room : listRoomData) {
			String finalName = room.NameRoom;
			
			//check if this is a JOINED room
			for (ChatRoom joinedRoom : listJoinedRoom) {
				if (joinedRoom.NameRoom.compareTo(room.NameRoom) == 0) {
					finalName = "@ " + room.NameRoom;
					break;
				}
			}			
			ObserAllRoom.add(finalName);
		}
		
		listRooms.setItems(ObserAllRoom);
	}

	/**
	 * When the button "All" is clicked on
	 * show the list of online users
	 */
	public void displayAllUserList() {
	
		try {
			ObserAllUser.clear();
			for (int i = 0; i < listUserData.size(); ++i)
			{
				String name = listUserData.get(i).UserName;
				ObserAllUser.add(name);
			
			}
			listAllUser.setItems(ObserAllUser);
		} catch (Exception e) {
			System.out.println("error: ");
			e.printStackTrace();
		}
	}

	/**
	 * This function will be called when user click "send" button
	 * @param event
	 */
	public void onBtnSendChat_Click(ActionEvent event) {
		if (currDisplayRoomIndex == -1) {
			return;
		}
		
		String chat = txtChat.getText();
		if (privateChat == false) {
			//PUBLIC
			
			//listJoinedRoom.get(currDisplayRoomIndex).addChat(chat);
			
			txtChat.clear();
		}
		else
		{
			//listUserData.get(currDisplayRoomIndex).add_msg(chat);
			txtChat.clear();
		}
		
		WSMSendRoomMsg msg = new WSMSendRoomMsg(listRoomData.get(currDisplayRoomIndex).NameRoom, chat);
		send2Server(msg);
		

	}

	/**
	 * Compare the room name from the user to the listUserData
	 * Return the index room in the listUserData
	 */
	public int searchRoom(String roomName, ArrayList<ChatRoom> listRooms) {
		for (int i = 0; i < listRooms.size(); ++i) {
			if (roomName.equals(listRooms.get(i).NameRoom)) {
				return i;
			}
				
		}
		return -1;
	}
	
	/**
	 * Check if the given user exists in the given room
	 */
	public int searchUser(String userName, ChatRoom room) {
		for (int i = 0; i < room.RoomUsers.size(); i++) {
			if (userName.equals(room.RoomUsers.get(i).UserName)) {
				return i;
			}
		}
		
		return -1;
	}

	/**
	 * Check if the given user exists in the given user list
	 */
	public int searchUser(String userName, String[] arrUserNames) {
		for (int i = 0; i < arrUserNames.length; i++) {
			if (userName.equals(arrUserNames[i])) {
				return i;
			}
		}
		
		return -1;
	}
		
	/**
	 * Check if the given room exists or not
	 */
	public boolean checkRoomExists(String roomName) {
		for (int j = 0; j < listRoomData.size(); ++j) {
			if (roomName.equals(listRoomData.get(j).NameRoom)) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Check if the users from server is already in the ListUserData
	 * 
	 */
	public boolean checkAllUserExist(String username) {
		for (int i = 0; i < listUserData.size(); ++i)
		{	
			//System.out.println(listUserData.get()));
			if (username.equals(listUserData.get(i).UserName)){
				return true;
			}
		}
		return false;
	}
}

	
