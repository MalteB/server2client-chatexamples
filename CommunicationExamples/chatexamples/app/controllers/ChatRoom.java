package controllers;

import java.util.ArrayList;
import java.util.List;

import models.ChatMessage;

public class ChatRoom {

	private List<ChatMessage> messages = new ArrayList<ChatMessage>();
	
	public void postMessage(ChatMessage message) {
		synchronized (messages) {
			messages.add(message);
		}
		synchronized (this) {
			this.notifyAll();
		}
	}
	
	public List<ChatMessage> getMessages(int fromIndex) {
		synchronized (messages) {
			return messages.subList(fromIndex, messages.size());
		}
	}
}
