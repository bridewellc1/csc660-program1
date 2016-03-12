package csc660;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The main Node class. Requires a port number that it will listen on and an id
 * number. If given a list of Messages it will send them to the parent server.
 *
 */
public class Node implements Runnable {

	/**
	 * The main thread pool for spawning threads.
	 */
	protected ExecutorService threadPool = Executors.newCachedThreadPool();

	/**
	 * Id of this node
	 */
	public final int id;

	/**
	 * The port this node uses
	 */
	public final int port;

	/**
	 * The central server that all messages are sent to
	 */
	private Node parent;

	/**
	 * The message sender. Sends the given messages in order.
	 */
	SerialExecutor messageSender = new SerialExecutor(threadPool);

	/**
	 * The messages to send (in order)
	 */
	ArrayList<Message> messages;

	/**
	 * The message listener socket
	 */
	ServerSocket messageListener;

	/**
	 * Whether this Node is listening for messages
	 */
	private boolean listening = true;

	/**
	 * The time that this node thinks it is. Incremented by one with each
	 * logical step and by the delay in seconds with each delay
	 */
	private double time = 0;

	/**
	 * @return current time
	 */
	public synchronized double getTime() {
		return this.time;
	}

	/**
	 * @param t
	 *            time to set
	 */
	public synchronized void setTime(double t) {
		this.time = t;
	}

	/**
	 * New node will just listen for messages - no messages to send and no
	 * parent
	 * 
	 * @param id
	 *            Id of the new node
	 * @param port
	 *            Port number of the new node
	 */
	public Node(int id, int port) {
		this.id = id;
		this.port = port;
		try {
			this.messageListener = new ServerSocket(this.port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Node will listen on the given port and send the messages to the parent
	 * 
	 * @param id
	 *            Id of the new node
	 * @param port
	 *            Port number of the new node
	 * @param messages
	 *            List of messages to send
	 * @param parent
	 *            The parent node
	 */
	public Node(int id, int port, ArrayList<Message> messages, Node parent) {
		this(id, port);
		this.messages = messages;
		this.parent = parent;
	}

	@Override
	public void run() {
		sendMessages();
		receiveMessages();
	}

	/**
	 * Shutdown this node - stop listening and shutdown once all active threads
	 * are finished
	 */
	public void close() {
//		System.out.println(id + ": Stopping...");
		listening = false;

		// This will wait until the threadPool is completely shutdown before
		// running
		Thread stopListener = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!threadPool.isTerminated()) {
					//
				}
//				System.out.println(id + ": has stopped, final time is " + getTime());
			}
		});
		stopListener.start();

		threadPool.shutdown();
	}

	/**
	 * Send all of the required messages and then send a done message to the
	 * parent. Messages are sent in order.
	 */
	private void sendMessages() {
		for (Message m : messages) {
			sendMessage(m);
		}

		sendMessage(new Message(id, parent.id, Message.done));
	}

	/**
	 * @param m
	 *            The message to send
	 */
	private void sendMessage(final Message m) {
		messageSender.execute(new Runnable() {
			@Override
			public void run() {
				try {
					// Delay the required time and update clock
//					System.out.println(id + ": Sleeping for " + m.delay / 1000.0 + " seconds, local time is " + getTime());
					System.out.println("PROCESS #" + id + " Sleeping for " + m.delay / 1000.0 + " seconds Local time = " + getTime());
					Thread.sleep(m.delay);
					setTime(getTime() + m.delay / 1000);
//					System.out.println(id + ": Done sleeping, local time is " + getTime());

					// Connect to the parent server and get the in and out
					// streams
					Socket connection = new Socket("localhost", parent.port);
					DataOutputStream out = new DataOutputStream(connection.getOutputStream());

					// Send the message
//					System.out.println(id + ": Sending message \"" + m.message + "\" to " + m.target + ", local time is " + getTime());
					System.out.println("PROCESS #" + id + " Sending message \"" + m.message + "\" to PROCESS #" + m.target + " Local time = " + getTime());
					Message.writeMessage(m, out);

//					// (don't) Wait for a response
//					DataInputStream in = new DataInputStream(connection.getInputStream());
//					Message response = Message.readMessage(in);
//					if (response.getTime() > time) {
////						System.out.println(id + ": Response time from " + response.source
////								+ " is higher, updating time to " + response.getTime());
//						setTime(response.getTime() + 1);
//					} else {
//						setTime(getTime() + 1);
//					}
//					System.out.println(id + ": Received response \"" + response.message + "\" from " + response.source
//							+ ", local time is " + getTime());

					// And close the connection
					connection.close();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Listen for any messages and handle them
	 */
	private void receiveMessages() {
		threadPool.submit(new Runnable() {
			@Override
			public void run() {
				while (listening) {
					try {
//						System.out.println(id + ": listening for messages");

						// Listen for any connections
						Socket socket = messageListener.accept();
						DataInputStream in = new DataInputStream(socket.getInputStream());

						// Receive the message
						Message received = Message.readMessage(in);

//						System.out.println(id + ": Received message \"" + received.message + "\" from "
//								+ received.source + ", local time is " + getTime());
						if (received.getTime() > time) {
//							System.out.println(id + ": Received time from " + received.source
//									+ " is higher, updating time to " + received.getTime());
							setTime(received.getTime() + 1);
						} else {
							setTime(getTime() + 1);
						}
						System.out.println("PROCESS #" + id + " receiving message \"" + received.message + "\" Local time = " + getTime());


//						DataOutputStream out = new DataOutputStream(socket.getOutputStream());
						// Handle the message
						Message m;
						if (received.target == id) {
							if (received.message.equals(Message.close)) {
								m = new Message(id, received.source, id + " will close");
								close();
							} else {
								m = new Message(id, received.source, "Hello from " + id);
							}
						} else {
							// This shouldn't happen, just handling possibility
							System.out.println(id + ": Received message for a different target");
							m = new Message(id, received.source,
									id + " wrong target, message should go to " + received.target);
						}
						m.setTime(getTime());
//						Message.writeMessage(m, out); //don't response to message

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		});
	}
}