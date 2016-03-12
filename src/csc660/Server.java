package csc660;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * The server class. A node that will just forward messages. Does not keep track
 * of time, just redirects messages
 */
public class Server extends Node {

	/**
	 * The connected nodes
	 */
	HashSet<Node> connections = new HashSet<Node>();

	/**
	 * How many nodes have sent a done message
	 */
	private int doneCounter = 0;

	/**
	 * Start a new server listening on the given port
	 * 
	 * @param id
	 *            The server id
	 * @param port
	 *            The server port
	 */
	public Server(int id, int port) {
		super(id, port);
	}

	@Override
	public void run() {
		forwardMessages();
	}

	/**
	 * Increment done by one. Synchronized since multiple threads may try to
	 * increment at once.
	 */
	private synchronized void incrementDone() {
		doneCounter++;
//		System.out.println(id + ": " + doneCounter + " done, " + (connections.size() - doneCounter) + " remaining");
	}

	/**
	 * Add a new connected Node
	 * 
	 * @param n
	 *            New node to add
	 */
	public void addConnection(Node n) {
		connections.add(n);
	}

	/**
	 * Determine the port that the node with the given is is listening on.
	 * 
	 * @param id
	 *            The id of the node
	 * @return The node port or -1 if it's not found
	 */
	private int getPort(int id) {
		for (Node n : connections) {
			if (n.id == id) {
				return n.port;
			}
		}
//		System.out.println(this.id + ": Missing connection to " + id);
//		for (Node n : connections) {
//			System.out.println(n.id + ":" + n.port);
//		}
		return -1;
	}

	/**
	 * Send close instruction to all of the connected nodes
	 */
	private void sendCloseNotifications() {
		for (final Node n : connections) {
			threadPool.submit(new Callable<Message>() {
				@Override
				public Message call() throws Exception {
					try {
						// Connect to the parent server and get the in and out
						// streams
						Socket connection = new Socket("localhost", getPort(n.id));
//						DataInputStream in = new DataInputStream(connection.getInputStream());
						DataOutputStream out = new DataOutputStream(connection.getOutputStream());

						// Send the message
//						System.out.println(id + ": Sending close message to " + n.id);
						Message.writeMessage(new Message(id, n.id, Message.close), out);

						// Wait for a response
//						Message response = Message.readMessage(in);
//						System.out.println(id + ": Received close response from " + response.source);

						// And close the connection
						connection.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return null;
				}
			});
		}
	}

	/**
	 * Forward a message and return the Future response
	 * 
	 * @param m
	 *            Message to forward
	 * @return Future containing the response message
	 */
	private Future<Message> forwardMessage(final Message m) {
		return threadPool.submit(new Callable<Message>() {
			@Override
			public Message call() throws Exception {
				try {
					// Connect to the parent server and get the in and out
					// streams
					Socket connection = new Socket("localhost", getPort(m.target));
//					DataInputStream in = new DataInputStream(connection.getInputStream());
					DataOutputStream out = new DataOutputStream(connection.getOutputStream());

					// Send the message
//					System.out.println(id + ": Forwarding message to " + m.target + ": " + m.message);
					Message.writeMessage(m, out);

					// (don't) Wait for a response
//					Message response = Message.readMessage(in);
//					System.out.println(
//							id + ": Received forward response from " + response.source + " for " + response.target);

					// And close the connection
					connection.close();

					// Return the Future response
//					return response;
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}

	/**
	 * Listen for any messages and forward them to the correct location. Forward
	 * and responses to the correct location too.
	 */
	private void forwardMessages() {
		threadPool.submit(new Runnable() {
			@Override
			public void run() {
				while (connections.size() > doneCounter) {
					try {
//						System.out.println(id + ": Waiting for message");

						// Listen for any connections
						Socket socket = messageListener.accept();
						DataInputStream in = new DataInputStream(socket.getInputStream());
//						DataOutputStream out = new DataOutputStream(socket.getOutputStream());

						// Receive the message
						Message received = Message.readMessage(in);

						// Handle the message
						if (received.target == id) {
							if (received.message.equals(Message.done)) {
								incrementDone();
//								Message.writeMessage(new Message(id, received.source, "Done message received " + id),
//										out);
							} else {
//								Message.writeMessage(new Message(id, received.source, "Direct message from " + id),
//										out);
							}
						} else {
//							System.out.println(
//									id + ": Forwarding message from " + received.source + " to " + received.target);
							Future<Message> response = forwardMessage(received);
//							Message.writeMessage(response.get(), out);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

//				System.out.println(id + ": All nodes done, shutting down");
				sendCloseNotifications();
				close();
			}

		});
	}
}