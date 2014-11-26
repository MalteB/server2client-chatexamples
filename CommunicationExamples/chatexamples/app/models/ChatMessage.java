package models;

public class ChatMessage {

	private String user;
	
	private String message;
	
	public ChatMessage() {
		// default constructor
	}
	
	public ChatMessage(String user, String message) {
		this.user = user;
		this.message = message;
	}
	
	public String getUser() {
		return user;
	}
	
	public String getMessage() {
		return message;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
