package controllers;

import java.util.Arrays;
import java.util.List;

import models.ChatMessage;
import play.Logger;
import play.libs.F.Callback0;
import play.libs.Json;
import play.mvc.Results.Chunks;

import com.fasterxml.jackson.databind.JsonNode;

public class ForeverFrameChat {

	private boolean running = true;
	private int lastIndex = 0;

	public ForeverFrameChat(final Chunks.Out<String> out, final ChatRoom chatRoom) {

		Logger.debug("iframe connection started");
		// fix for firefox (1024 byte minimum chunk size) and IE (256 byte
		// minimum chunk size)
		char[] prefix = new char[1024];
		Arrays.fill(prefix, ' ');
		out.write(new String(prefix));

		out.onDisconnected(new Callback0() {

			@Override
			public void invoke() throws Throwable {
				running = false;
				Logger.debug("iframe connection closed");
				out.close();
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (running) {
					synchronized (chatRoom) {
						List<ChatMessage> newMessages = chatRoom
								.getMessages(lastIndex);
						if (newMessages.size() > 0) {
							lastIndex += newMessages.size();
							JsonNode jsonMessages = Json.toJson(newMessages);
							String message = "<script>parent.cometMessage("
									+ jsonMessages.toString() + ")</script>";
							out.write(message);
							Logger.debug("Chunked: sending: " + message);
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
