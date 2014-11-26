package controllers;

import java.util.Arrays;
import java.util.List;

import models.*;
import play.Logger;
import play.data.Form;
import play.libs.F.Callback0;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.index;
import views.html.chatiframe;
import views.html.chatlongpolling;
import views.html.chatwebsocket;

import com.fasterxml.jackson.databind.JsonNode;

public class Application extends Controller {

	private static ChatRoom chatRoom = new ChatRoom();
	private static int userCounter = 0;

	public static Result index() {
		return ok(index.render());
	}

	public static Result chatIFrame() {
		return ok(chatiframe.render("User " + ++userCounter));
	}
	
	public static Result chatLongPolling() {
		return ok(chatlongpolling.render("User " + ++userCounter));
	}
	
	public static Result chatWebSocket() {
		return ok(chatwebsocket.render("User " + ++userCounter));
	}

	
	/**
	 * REST method to post a chat message. Message must be contained in request body and have the same attributes as {@link ChatMessage}.
	 * @return HTTP 200
	 * @see ChatMessage
	 */
	public static Result postMessage() {
		// extract message from body
		Form<ChatMessage> form = Form.form(ChatMessage.class);
		ChatMessage message = form.bindFromRequest().get();

		// publish message in chatroom
		synchronized (chatRoom) {
			chatRoom.postMessage(message);
		}

		Logger.debug("message received: " + message.getUser() + ": "
				+ message.getMessage());

		return ok();
	}

	/**
	 * Method for XHR long polling chat. Returns immediately if there are messages newer or equal to {@code lastIndex}. Otherwise
	 * waits for new messages and holds the connection open. If there are no new messages within 60 seconds, returns no content.
	 * @param lastIndex - the index of the last received message
	 * @return HTTP 200 and all new messages as {@link ChatMessage} in a list from {@code lastIndex} to the latest; or HTTP 204 if there are no new messages
	 * @see ChatMessage
	 */
	public static Promise<Result> getMessagesLongPolling(final Integer lastIndex) {
		Promise<List<ChatMessage>> promiseOfMessages = Promise.promise(new Function0<List<ChatMessage>>() {
					public List<ChatMessage> apply() {
						synchronized (chatRoom) {
							List<ChatMessage> newMessages = chatRoom.getMessages(lastIndex);
							
							if (newMessages.size() > 0) {
								return newMessages;
							} else {
								try {
									Logger.debug("LongPolling: No new messages. Going to sleep for max. 60 sec.");
									// if we don't have newer messages, wait
									chatRoom.wait(60000);

									newMessages = chatRoom.getMessages(lastIndex);
									
									if (newMessages.size() > 0) {
										return newMessages;
									}
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							return null;
						}
					}
				});

		return promiseOfMessages.map(new Function<List<ChatMessage>, Result>() {
			public Result apply(List<ChatMessage> result) {
				// if no new messages available return 204
				if (result == null) {
					return noContent();
				}
				return ok(Json.toJson(result));
			}
		});
	}

	/**
	 * Method for comet streaming using the forever iframe technique. It returns new messages in
	 * chunks using transfer encoding "chunked". All messages are wrapped in a JavaScript {@code script}
	 * tag calling {@code parent.cometMessage()} with a list of the message objects {@link ChatMessage} as a parameter.
	 * @return the messages as JSONArray in chunked responses
	 * @see ChatMessage
	 */
	public static Result getMessagesChunked() {
		// Prepare a chunked text stream
		Chunks<String> chunks = new StringChunks() {

			private boolean running = true;

			// Called when the stream is ready
			public void onReady(final Chunks.Out<String> out) {
				new ForeverFrameChat(out, chatRoom);
			}
		};

		response().setContentType("text/html");
		return ok(chunks);
	}
	
	/**
	 * Returns a new WebSocket and starts a WebSocketChat when connection to client is established, which then
	 * sends and receives new messages to/from client as JsonNodes. It retrieves single ChatMessage objects as JSON
	 * and sends a JSONArray of ChatMessage objects to the client.
	 * @return the new WebSocket
	 */
	public static WebSocket<JsonNode> getMessagesWebSocket() {
		Logger.debug("Open WebSocket");
		return new WebSocket<JsonNode>(){
			
			// called when websocket handshake is done
			public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
				new WebSocketChat(in, out, chatRoom);
			}
		};
	}
}
