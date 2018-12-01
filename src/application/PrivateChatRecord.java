package application;

import java.util.ArrayList;

public class PrivateChatRecord {
	String userName;
	ArrayList<String> listChatLines = new ArrayList<>();
	
	public PrivateChatRecord(String userName) {
		this.userName = userName;
		listChatLines.add("[PRIVATE CHAT with " + userName + "]");
	}
	
	public void addChat(String chatContent, boolean isMeSend) {

		if (isMeSend) {
			listChatLines.add("me ::: " + chatContent);
		} else {
			listChatLines.add(userName + " ::: " + chatContent);
		}
	}
}
