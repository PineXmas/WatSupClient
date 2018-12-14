package application;

import java.util.ArrayList;

public class ChatRoom {
	public ArrayList<User> RoomUsers;
	public String NameRoom;
	public ArrayList<String> RoomMsg;
	/**
	 * add name to the room
	 * @param room_name
	 */
	public ChatRoom(String room_name)
	{	
		NameRoom = room_name;
		RoomUsers = new ArrayList<>();
		RoomMsg = new ArrayList<>();
	}
	/**
	 * REmove a user from this room
	 * @param userName
	 */
	public void remove (String userName) {
		for (int i = 0; i < RoomUsers.size(); ++i) {
			if (RoomUsers.get(i).UserName == userName)	{
				RoomUsers.remove(i);
				break;
			}				
		}

	}
	/**
	 * Compare if the room name is duplicated
	 * @param room_name
	 * @return
	 */
	public boolean compare(String room_name) {
		return (NameRoom.compareTo(room_name) == 0);
	}
	/**
	 * Add new user to the room
	 * @param new_user
	 */
	public void addNewUser(String new_user) {
		RoomUsers.add(new User(new_user));		
	}
	/**
	 * Add chat contents to the room (public chat)
	 * @param chat
	 */
	public void addChat(String chat) {
		RoomMsg.add(chat);
	}
	/**
	 * Display all chat contents in the room
	 * @return
	 */
	public String [] displayChat() {
		String [] chat = new String[RoomMsg.size()];
		for (int i = 0; i < RoomMsg.size(); ++i) {
			chat[i] = RoomMsg.get(i);
		}
		return chat;
	}
	/**
	 * Check if the user is already in the room
	 * @param username
	 * @return
	 */
	public User check_existUser(String username) {
		for (User user: RoomUsers) {
			if (user.UserName.compareTo(username) == 0) {
				return user;
			}
		}		
		return null;
	}
	/**
	 * Display all users are in the room
	 * @return
	 */
	public String[] dislayAllUser() {
		String[] arrUserName = new String[RoomUsers.size()];
		for (int i = 0; i < RoomUsers.size(); ++i) {
			arrUserName[i] = RoomUsers.get(i).get_user();			
		}
		return arrUserName;
		
	}
	

	
}
