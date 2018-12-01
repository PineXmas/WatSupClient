package application;

import java.util.ArrayList;

public class User {
	public String UserName;
	public ArrayList<String> UserMsg;
	
	public User(String user_name)
	{
		UserName = user_name;
		UserMsg = new ArrayList<>();	
	}
	public void add_msg (String msg_to_add)
	{
		UserMsg.add(msg_to_add);
	}
	public String [] displayPrivateChat() {
		String [] chat = new String[UserMsg.size()];
		for (int i = 0; i < UserMsg.size(); ++i) {
			chat[i] = UserMsg.get(i);
		}
		return chat;
	}
	public boolean compare(String user_name)
	{
		return (UserName.compareTo(user_name) == 0);
	}
	public String get_user() {
		return this.UserName;
	}
	public void removeUser(String user_name) {
		UserName = null;
	}
	
}
