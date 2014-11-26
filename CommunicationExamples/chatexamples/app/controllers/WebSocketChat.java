package controllers;

import java.util.List;

import models.ChatMessage;

import com.fasterxml.jackson.databind.JsonNode;

import play.Logger;
import play.libs.Json;
import play.libs.F.*;
import play.mvc.*;

public class WebSocketChat {

	private boolean running = false;
	private int lastIndex = 0;
	
	public WebSocketChat(final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out, final ChatRoom chatRoom) {

		running = true;
		
		in.onMessage(new Callback<JsonNode>() {
			public void invoke(JsonNode messageNode) {
				ChatMessage chatMessage = Json.fromJson(messageNode, ChatMessage.class);
				synchronized (chatRoom) {
					chatRoom.postMessage(chatMessage);
				}
			}
		});
		
		in.onClose(new Callback0() {
			public void invoke() {
				running = false;
				Logger.debug("WebSocketChat connection closed");
			}
		});
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (running) {
					synchronized (chatRoom) {
						List<ChatMessage> newMessages = chatRoom.getMessages(lastIndex);
						if (newMessages.size() > 0) {
							lastIndex += newMessages.size();
							JsonNode jsonMessages = Json.toJson(newMessages);
							out.write(jsonMessages);
						}
						else {
							out.write(Json.toJson(newMessages));	// send ping to keep connection alive
						}
						try {
							if (running) {
								chatRoom.wait(30000);
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
	}
}
