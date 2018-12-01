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
import sun.util.xml.PlatformXmlPropertiesProvider;

public class Controller {
	String serverHost = "localhost";
	int portNumber = 8312;

	// TODO wrap currDisplayRoomIndex & privateChat to a function, let say
	// setChatHistoryTarget: Room, Private, None

	// DATA
	boolean privateChat = false;
	String myUserName;
	String currChatTargetName = null;
	public ArrayList<User> listUserData = new ArrayList<>(); // List of all users
	public ArrayList<ChatRoom> listRoomData = new ArrayList<>(); // List chat room
	public ArrayList<ChatRoom> listJoinedRoomNames = new ArrayList<>(); // List joined room
	public ArrayList<PrivateChatRecord> listPrivateChat = new ArrayList<>();

	//TODO myself should have a map <user-name, private-chat-content>. Also, every time I receive LIST_USERS_ALL, I go to my map to remove chat content accordingly
	
	public Button btnSignIn;
	public Button btnNewRoom;
	public Button btnSendChat;
	public Button btnLeaveRoom;
	public Button btnLogOut;

	public TextField txtUserName;
	public TextField txtNewRoom;

	public TextArea txtChat;
	public TextArea txtChatWindow;

	public ListView<String> listRooms;
	public ListView<String> listAllUser;
	public ListView<String> listRoomUser;

	public TabPane tabPaneNameList;
	public Tab tabRoom;
	public Tab tabAllUsers;

	public Label lbChatWindow;

	ObservableList<String> ObserAllUser = FXCollections.observableArrayList();
	ObservableList<String> ObserAllRoom = FXCollections.observableArrayList();
	ObservableList<String> ObserRoomUser = FXCollections.observableArrayList();

	Socket clientSocket;
	Controller myself = null;
	Stage currStage = null;
	InetSocketAddress hostAddress = new InetSocketAddress(serverHost, portNumber);
	LinkedBlockingQueue<WSMessage> receivingMsgQueue = new LinkedBlockingQueue<>();
	Thread msgReceiver;
	Thread serverListener;

	/**
	 * The job for serverListener-thread
	 */
	Runnable runnableServerListener = new Runnable() {

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

				// set up input stream reader
				InputStream inputStream = clientSocket.getInputStream();
				byte[] buff = new byte[1024];

				// keep reading from client
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

					// parse read bytes into messages
					byte[] actualReadBuff = ByteBuffer.allocate(readBytes).put(buff, 0, readBytes).array();
					listMsgs = WSMessage.parse2Msgs(actualReadBuff, arrIncompleteMsg, listNewIncompleteMsg);
					ErrandBoy.println("  Read " + listMsgs.size() + " complete msgs from server ");

					// update incomplete message
					arrIncompleteMsg = ErrandBoy.convertList2Array(listNewIncompleteMsg);
					if (arrIncompleteMsg.length > 0) {
						ErrandBoy.println("  Incomplete msg from server: " + new String(arrIncompleteMsg));
					}

					// enqueue messages
					for (WSMessage msg : listMsgs) {
						msg.clientHandler = null;
						receivingMsgQueue.put(msg);
					}

				}

				// reach here means the input stream has reach eof
				if (readBytes < 0) {
					ErrandBoy.print("Server input is EOF, ");
				}
				ErrandBoy.println("Server-listener-thread has stopped");
			} catch (Exception e) {
				ErrandBoy.printlnError(e, "Error while reading from client's input stream");
			} finally {
				// TODO finalize client socket after error occurs
			}

		}
	};

	public Controller() {

	}

	/**
	 * The job for msgReceiver-thread
	 */
	Runnable runnableMsgReceiver = new Runnable() {

		@Override
		public void run() {
			if (receivingMsgQueue == null) {
				ErrandBoy.println("Received-message queue is not init. Receiver quits.");
				return;
			}

			try {
				ErrandBoy.println("Waiting to process messages in the queue...");
				WSMessage msg;

				while (!((msg = receivingMsgQueue.take()) instanceof WSMStopSerer)) {

					try {

						ErrandBoy.println("Server sent: " + msg.toString());

						// process messages

						if (msg.opcode == WSMCode.OPCODE_LOGIN_SUCCESS) {
							send2Server(new WSMListRooms());

							Platform.runLater(new Runnable() {

								@Override
								public void run() {

									switch2ChatRoom();
								}
							});
						} else if (msg.opcode == WSMCode.OPCODE_LIST_ROOMS_RESP) {
							WSMListRoomsResp msgListRoomResp = (WSMListRoomsResp) msg;
							for (int i = 0; i < (msgListRoomResp.arrRoomNames.length); ++i) {

								if (searchRoom(msgListRoomResp.arrRoomNames[i], listRoomData) == -1) {
									listRoomData.add(new ChatRoom(msgListRoomResp.arrRoomNames[i]));
								}
							}

							displayUI();
						}

						else if (msg.opcode == WSMCode.OPCODE_LIST_USERS_RESP) {
							/*
							 * Cases to handle: - case 01_A: the room is NOT one of my joined room and I'm
							 * OUTSIDE + local copy: remove all users + server copy: duplicate list users
							 * into local copy
							 * 
							 * - case 01_B: the room is NOT one of my joined room and I'm INSIDE + local
							 * copy: remove all users + server copy: duplicate list users into local copy +
							 * add the room to list of JOINED ROOMS & update current-display-room-index
							 * 
							 * - case 02_A: the room is one of my JOINED ROOMS and I'm still INSIDE + local
							 * copy: remove all users + server copy: duplicate list users into local copy
							 * 
							 * - case 02_B: the room is one of my JOINED ROOMS and I'm OUTSIDE + local copy:
							 * remove all users + server copy: duplicate list users into local copy + check
							 * if currDisplayRoomIndex is this room --> YES --> set it to -1 + remove the
							 * room out of listJoinedRooms
							 */

							WSMListUsersResp msgListUsersResp = (WSMListUsersResp) msg;
							int found;

							// decide whether the room is JOINED or NOT
							found = searchRoom(msgListUsersResp.roomName, listJoinedRoomNames);
							if (found == -1) {
								found = searchRoom(msgListUsersResp.roomName, listRoomData);
								if (found >= 0) {
									ChatRoom theRoom = listRoomData.get(found);
									theRoom.RoomUsers.clear();
									for (String userName : msgListUsersResp.arrUserNames) {
										theRoom.addNewUser(userName);
									}

									// decide whether i am INSIDE or OUTSIDE
									found = searchUser(myUserName, msgListUsersResp.arrUserNames);
									if (found == -1) {
										/*
										 * case 01_A: NOT joined room & i am OUTSIDE
										 */

										//// do nothing
									} else {

										/*
										 * case 01_B: NOT joined room & i am INSIDE
										 */

										//// add the room to listJoinedRoom
										listJoinedRoomNames.add(theRoom);

										//// we assume this is the case of creating new room, so set
										//// currDisplayRoomIndex to this room
										currChatTargetName = msgListUsersResp.roomName;
										privateChat = false;
									}

								}

							} else {
								ChatRoom theJoinedRoom = listJoinedRoomNames.get(found);
								int theJoinedRoomIndexInListJoined = found;

								found = searchUser(myUserName, msgListUsersResp.arrUserNames);
								if (found >= 0) {
									/*
									 * case 02_A: JOINED room & i am INSIDE
									 */

									theJoinedRoom.RoomUsers.clear();
									for (String userName : msgListUsersResp.arrUserNames) {
										theJoinedRoom.addNewUser(userName);
									}
								} else {
									/*
									 * case 02_B: JOINED room & i am OUTSIDE
									 */

									theJoinedRoom.RoomUsers.clear();
									for (String userName : msgListUsersResp.arrUserNames) {
										theJoinedRoom.addNewUser(userName);
									}

									//// check currDisplayRoomIndex
									if (currChatTargetName.equals(msgListUsersResp.roomName)) {
										currChatTargetName = null;
									}

									//// remove the room out of listJoinedRooms
									listJoinedRoomNames.remove(theJoinedRoomIndexInListJoined);
								}

							}

							displayUI();

						}

						else if (msg.opcode == WSMCode.OPCODE_LIST_USERS_ALL) {
							WSMListUsersAll msgListUserAll = (WSMListUsersAll) msg;
							
							listUserData.clear();
							for (int i = 0; i < (msgListUserAll.arrUserNames.length); ++i) {
								listUserData.add(new User(msgListUserAll.arrUserNames[i]));
							}
							
							//check to remove private-chat-record from disconnected users
							for (int j = listPrivateChat.size()-1; j >= 0; j--) {
								if (searchUser(listPrivateChat.get(j).userName, listUserData) == -1) {
									listPrivateChat.remove(j);
								}
							}

							displayUI();
						} else if (msg.opcode == WSMCode.OPCODE_ERROR) {
							WSMError msgError = (WSMError) msg;
							if (msgError.errCode == WSMCode.ERR_NAME_EXISTS) {
								Platform.runLater(new Runnable() {

									@Override
									public void run() {

										displayErrorMsg("Username has already been taken");

									}
								});
							} else if (msgError.errCode == WSMCode.ERR_KICKED_OUT) {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {

										displayErrorMsg("You have been kicked out");
										reset();
										switch2LogIn();

									}
								});
							} else if (msgError.errCode == WSMCode.ERR_TOO_MANY_ROOMS) {
								Platform.runLater(new Runnable() {
									@Override
									public void run() {

										displayErrorMsg("Max room limit reached");

									}
								});
							} else if (msgError.errCode == WSMCode.ERR_TOO_MANY_USERS) {
								Platform.runLater(new Runnable() {

									@Override
									public void run() {

										displayErrorMsg("Max user limit reached");

									}
								});
							}

						} else if (msg.opcode == WSMCode.OPCODE_TELL_ROOM_MSG) {
							WSMTellRoomMsg msgRoomMsg = (WSMTellRoomMsg) msg;
							int index = searchRoom(msgRoomMsg.roomName, listRoomData);
							if (index >= 0) {
								listRoomData.get(index).addChat(
										msgRoomMsg.userName + " ::: " + msgRoomMsg.chatContent);

							}

							displayUI();
						} else if (msg.opcode == WSMCode.OPCODE_TELL_PRIVATE_MSG) {
							/**
							 * case 01: record of the sender is already created --> append chat content
							 * case 02: record is not created, since i never talk with the sender before --> create --> append chat content
							 */
							WSMTellPrivateMsg msgTellPrivateMsg = (WSMTellPrivateMsg) msg;
							int found = searchUser(msgTellPrivateMsg.name, listUserData);
							if (found == -1) {
								return;
							}
							
							found = searchPrivateChatRecord(msgTellPrivateMsg.name);
							PrivateChatRecord record = null;
							if (found == -1) {
								//case 02
								addPrivateChatRecord(msgTellPrivateMsg.name);
								found = searchPrivateChatRecord(msgTellPrivateMsg.name);	
							}
							
							record = listPrivateChat.get(found);
							record.addChat(msgTellPrivateMsg.chatContent, false);
							
							displayUI();
						}

						else {
							/*
							 * ignore all other message types
							 */

							ErrandBoy.println(
									"Server sends un-expected message " + msg.opcode + ". Ignore the message.");

						}

					} catch (Exception e) {
						ErrandBoy.printlnError(e, "Error while processing message: " + msg.opcode + " from server");
					}
				}

				// empty the queue before die
				receivingMsgQueue.clear();

				ErrandBoy.println("Message-receiver-thread has stopped");
			} catch (Exception e) {
				ErrandBoy.printlnError(e, "Error when processing received messages");
			}
		}
	};

	/**
	 * Set initial state for all UI controls
	 */
	public void initUI() {
		btnLeaveRoom.setDisable(true);
		tabPaneNameList.getSelectionModel().select(tabAllUsers);
	}

	/**
	 * Reset controller to ready for a fresh start
	 */
	public void reset() {
		listJoinedRoomNames.clear();
		listUserData.clear();
		listRoomData.clear();
		listPrivateChat.clear();
		currChatTargetName = null;

		// kill msg-receiver thread
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					receivingMsgQueue.put(new WSMStopSerer());
				} catch (Exception e) {
					System.out.println("Error while killing thread msg-receiver: " + e.getMessage());
				}
			}
		}).start();
		msgReceiver = null;

		// kill server-listener
		try {
			if (clientSocket != null) {
				clientSocket.close();
				clientSocket = null;
			}

		} catch (Exception e) {
			System.out.println("Error while killing thread server-listener: " + e.getMessage());
		}
		serverListener = null;
	}

	/**
	 * Update the whole UI according to the current state of data.<br>
	 * <strong>NOTE:</strong> this function will be called later by the UI-thread,
	 * using Platform.runLater(...)
	 */
	public void displayUI() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				// UI: list room names
				displayRoomNames();

				// UI: chat history, tab of user names
				displayChatHistory();

				// UI: list all users
				displayAllUserList();
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
	 * if the "Leave" button is clicked on, the user will be removed from the Joined
	 * Room List
	 * 
	 * @param even
	 */
	public void onBtnLeaveRoom_Click(ActionEvent event) {
		if (currChatTargetName == null) {
			return;
		}

		int found = searchRoom(currChatTargetName, listRoomData);
		String currentRoomName = listRoomData.get(found).NameRoom;
		WSMLeaveRoom leaveRoom = new WSMLeaveRoom(currentRoomName);
		send2Server(leaveRoom);

	}

	/**
	 * Display Chat Window when "Log In" is clicked
	 * 
	 * @param event
	 */
	public void onBtnSignIn_Click(ActionEvent event) {
		myUserName = txtUserName.getText();

		try {

			if (clientSocket == null || !clientSocket.isConnected()) {
				// connect to server
				clientSocket = new Socket();

				// this functions will block until server is reached
				clientSocket.connect(hostAddress);

				// start serverListener & msgReceiver
				if (serverListener == null) {
					serverListener = new Thread(runnableServerListener);
					serverListener.start();
				} else {
					if (!serverListener.isAlive()) {
						serverListener.start();
					}					
				}
				
				if (msgReceiver == null) {
					msgReceiver = new Thread(runnableMsgReceiver);
					msgReceiver.start();
				} else {
					if (!msgReceiver.isAlive()) {
						msgReceiver.start();
					}					
				}

				// if reach here -> server has been connected
				System.out.println("Server connected");
			}

			// send LOG_IN msg
			WSMLogin logIn = new WSMLogin(myUserName);
			send2Server(logIn);

		} catch (Exception ex) {
			System.out.println("Error while logging in: " + ex.getMessage());
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

			initUI();

			displayUI();

		} catch (IOException e) {
			e.printStackTrace();

		}
	}

	/**
	 * Display ERROR message
	 */
	public void displayErrorMsg(String errorMsg) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Error");
				alert.setHeaderText(null);
				alert.setContentText(errorMsg);
				alert.showAndWait();
			}
		});
	}

	/**
	 * Send message to server
	 * 
	 * @param msg
	 */
	public void send2Server(WSMessage msg) {
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
	 * Double click on an user name on the ALL USERs list to create a private chat
	 */
	public void onListUser_Click(MouseEvent event) {
		if (event.getClickCount() == 2) {
			int index = listAllUser.getSelectionModel().getSelectedIndex();

			currChatTargetName = listUserData.get(index).UserName;
			privateChat = true;
			
			addPrivateChatRecord(currChatTargetName);

			displayUI();
		}

	}

	/**
	 * Function to create a new room when button "Create" is clicked
	 * 
	 * @param event
	 */
	public void onBtnCreate_Click(ActionEvent event) {
		String roomName = txtNewRoom.getText();
		if (roomName.isEmpty()) {
			return;
		}
		
		WSMJoinRoom room = new WSMJoinRoom(roomName);
		send2Server(room);
		txtNewRoom.clear();

	}

	/**
	 * Double click on an element in the listRoom of to join room Single click on an
	 * element in the JOINED listRoom to enable "Leave Room" button
	 * 
	 * @param event
	 */
	public void onListRooms_Click(MouseEvent event) {
		int selectedItemIndex = listRooms.getSelectionModel().getSelectedIndex();
		if (selectedItemIndex == -1) {
			return;
		}

		String selectedRoomName = listRoomData.get(selectedItemIndex).NameRoom;

		/*
		 * 1-click: select room to leave, do nothing else
		 * 
		 * 2-click: double click to - if NOT joined --> send JOIN_ROOM - if JOINED -->
		 * display chat history
		 */

		if (event.getClickCount() == 1) {
			int found = searchRoom(selectedRoomName, listJoinedRoomNames);

			// case 01: the selected room is NOT joined room
			if (found == -1) {
				btnLeaveRoom.setDisable(true);
				return;
			}

			// case 02: the selected room is a JOINED room
			btnLeaveRoom.setDisable(false);
		} else if (event.getClickCount() == 2) {
			currChatTargetName = selectedRoomName;
			privateChat = false;

			// case 01: i already JOINED this room
			int found = searchRoom(selectedRoomName, listJoinedRoomNames);
			if (found >= 0) {
				displayUI();
				return;
			}

			// case 02: i want to CREATE a new room
			WSMJoinRoom msg = new WSMJoinRoom(selectedRoomName);
			send2Server(msg);
		}

	}

	/**
	 * Display Chat conversation for each selected joined Room
	 * 
	 * @param roomIndex
	 */
	public void displayChatHistory() {

		// common settings
		tabPaneNameList.setDisable(false);

		/********************************************************************
		 * NOT SELECT
		 ********************************************************************/
		if (currChatTargetName == null) {
			// chat section
			txtChatWindow.clear();
			txtChatWindow.setText("<Please select/join a ROOM or an USER to see chat>");
			lbChatWindow.setText("");

			// tab section
			tabRoom.setText("");
			ObserRoomUser.clear();
			listRoomUser.setItems(ObserRoomUser);

			return;
		}

		/********************************************************************
		 * ROOM CHAT
		 ********************************************************************/
		if (!privateChat) {
			int found = searchRoom(currChatTargetName, listJoinedRoomNames);
			if (found == -1) {
				return;
			}

			// chat section
			ChatRoom room = listJoinedRoomNames.get(found);
			String[] chatHistory = room.displayChat();
			txtChatWindow.clear();
			for (int i = 0; i < chatHistory.length; ++i) {
				txtChatWindow.appendText(chatHistory[i] + "\n");
			}
			lbChatWindow.setText(room.NameRoom);

			// tab section
			String[] arrayUserName = room.dislayAllUser();
			ArrayList<String> l = new ArrayList<>(Arrays.asList(arrayUserName));
			ObserRoomUser.clear();
			ObserRoomUser.addAll(l);
			listRoomUser.setItems(ObserRoomUser);
			tabRoom.setText(room.NameRoom);
			tabPaneNameList.getSelectionModel().select(tabRoom);

			return;
		}

		/********************************************************************
		 * PRIVATE CHAT
		 ********************************************************************/
		int found = searchUser(currChatTargetName, listUserData);
		if (found == -1) {
			return;
		}
		found = searchPrivateChatRecord(currChatTargetName);
		if (found == -1) {
			return;
		}

		// chat section
		PrivateChatRecord record = listPrivateChat.get(found);
		lbChatWindow.setText(currChatTargetName);
		txtChatWindow.clear();
		for (String line : record.listChatLines) {
			txtChatWindow.appendText(line + "\n");
		}

		// tab section
		ObserRoomUser.clear();
		listRoomUser.setItems(ObserRoomUser);
		tabRoom.setText("");
	}

	/**
	 * Display a list of all users in the current JOINED room
	 * 
	 * @param roomIndex
	 */
	public void displayRoomUsers(int roomIndex) {
		if (roomIndex < 0 || roomIndex >= listRoomData.size()) {
			return;
		}

		String[] arrayUserName = listRoomData.get(roomIndex).dislayAllUser();
		ArrayList<String> l = new ArrayList<>(Arrays.asList(arrayUserName));
		ObserRoomUser.clear();
		ObserRoomUser.addAll(l);
		listRoomUser.setItems(ObserRoomUser);
	}

	/**
	 * Display the list of available ROOMs
	 */
	public void displayRoomNames() {
		ObserAllRoom.clear();
		for (ChatRoom room : listRoomData) {
			String finalName = room.NameRoom;

			// check if this is a JOINED room
			for (ChatRoom joinedRoom : listJoinedRoomNames) {
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
	 * When the button "All" is clicked on show the list of online users
	 */
	public void displayAllUserList() {
		ObserAllUser.clear();
		for (int i = 0; i < listUserData.size(); ++i) {
			String name = listUserData.get(i).UserName;
			ObserAllUser.add(name);

		}
		listAllUser.setItems(ObserAllUser);
	}

	/**
	 * This function will be called when user click "send" button
	 * 
	 * @param event
	 */
	public void onBtnSendChat_Click(ActionEvent event) {
		
		if (currChatTargetName == null) {
			return;
		}

		String chat = txtChat.getText();
		if (chat.isEmpty()) {
			return;
		}

		if (privateChat == false) {
			// ROOM

			int found = searchRoom(currChatTargetName, listJoinedRoomNames);
			if (found == -1) {
				return;
			}

			WSMSendRoomMsg msg = new WSMSendRoomMsg(listJoinedRoomNames.get(found).NameRoom, chat);
			send2Server(msg);

		} else {
			// PRIVATE
			int found = searchPrivateChatRecord(currChatTargetName);
			if (found == -1) {
				return;
			}
			listPrivateChat.get(found).addChat(chat, true);
			
			WSMSendPrivateMsg msg = new WSMSendPrivateMsg(currChatTargetName, chat);
			send2Server(msg);
			
			//// show the message I send to the other user
			displayUI();
		}

		txtChat.clear();
	}

	/**
	 * Compare the room name from the user to the listUserData Return the index room
	 * in the listUserData
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
	 * Check if the given user exists in the given user name array
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
	 * Check if the given user exists in the given user list
	 */
	public int searchUser(String userName, ArrayList<User> listUsers) {
		for (int i = 0; i < listUsers.size(); i++) {
			if (userName.equals(listUsers.get(i).UserName)) {
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
	 * Log out, reset controller, switch back to log-in-window
	 */
	public void onBtnLogOut_Click(ActionEvent event) {
		send2Server(new WSMLogout());
		reset();
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				switch2LogIn();
			}
		});

	}

	/**
	 * Switch to log in window
	 */
	public void switch2LogIn() {
		try {

			FXMLLoader chatRoomLoader = new FXMLLoader(getClass().getResource("LogIn.fxml"));
			chatRoomLoader.setController(this);
			Parent root = chatRoomLoader.load();
			Scene scene = new Scene(root);

			currStage.setScene(scene);
			currStage.setTitle("Log In");

			displayUI();

		} catch (IOException e) {
			e.printStackTrace();

		}
	}

	/**
	 * Search for the given user name in the record-list
	 * @param userName
	 * @return
	 */
	public int searchPrivateChatRecord(String userName) {
		for (int i = 0; i < listPrivateChat.size(); i++) {
			if (listPrivateChat.get(i).userName.equals(userName)) {
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * Try to add new private chat record if not existed
	 */
	public boolean addPrivateChatRecord(String userName) {
		int found = searchUser(userName, listUserData);
		if (found == -1) {
			return false;
		}
		
		found = searchPrivateChatRecord(userName);
		if (found >= 0) {
			return false;
		}
		
		listPrivateChat.add(new PrivateChatRecord(userName));
		return true;
	}
	
	/**
	 * Remove the given user name's record out of the record-list
	 * @param userName
	 * @return
	 */
	public boolean removePrivateChatRecord(String userName) {
		for (int i = 0; i < listPrivateChat.size(); i++) {
			if (listPrivateChat.get(i).userName.equals(userName)) {
				listPrivateChat.remove(i);
				return true;
			}
		}
		
		return false;
	}
}
