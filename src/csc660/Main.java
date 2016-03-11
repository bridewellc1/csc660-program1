package csc660;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
	public static void main(String[] args) {

		int nodeCount = 10; // how many nodes to spawn
		int messageCount = 10; // how many messages each node should send
		int maxDelay = 3000; // max possible delay
		int portOffset = 7777; // the port offset

		// The spawned nodes
		ArrayList<Node> nodes = new ArrayList<Node>(nodeCount);

		Server mainServer = new Server(0, portOffset);

		// Spawn nodeCount random nodes with messageCount random messages each
		// with a random delay up to maxDelay. Create nodes before starting
		// them so all required ports are open
		Random r = new Random();
		for (int i = 1; i < nodeCount+1; i++) {
			ArrayList<Message> instructions = new ArrayList<Message>();
			for (int j = 0; j < messageCount; j++) {
				int d = r.nextInt(maxDelay);

				// Make sure no node sends message to self
				int t = i;
				while (t == i) {
					t = r.nextInt(nodeCount);
				}

				String msg = "Hello from " + i;
				instructions.add(new Message(d, i, t, msg));
			}

			Node n = new Node(i, portOffset + i, instructions, mainServer);
			mainServer.addConnection(n);
			nodes.add(n);
		}

		Thread serverThread = new Thread(mainServer);
		serverThread.run();

		// Start the nodes in separate threads
		for (Node n : nodes) {
			Thread t = new Thread(n);
			t.run();
		}
	}
}

class Node implements Runnable {

	/**
	 * The message listener threads.
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
	
	private boolean listening = true;

	/**
	 * @param id
	 *            Id of the new node
	 * @param port
	 *            Port number of the new node
	 */
	public Node(int id, int port) {
		this(id, port, null, null);
	}

	/**
	 * @param id
	 *            Id of the new node
	 * @param port
	 *            Port number of the new node
	 * @param messages
	 *            List of messages to send
	 */
	public Node(int id, int port, ArrayList<Message> messages) {
		this(id, port, messages, null);
	}

	/**
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
		this.id = id;
		this.port = port;
		this.messages = messages;
		this.parent = parent;
		try {
			this.messageListener = new ServerSocket(this.port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param n
	 *            The new parent node
	 */
	public void setParent(Node n) {
		this.parent = n;
	}

	public Node getParent() {
		return this.parent;
	}

	@Override
	public void run() {
		sendMessages();
		receiveMessages();
	}

	public void close() {
		System.out.println(id + ": Stopping...");
		listening = false;
		threadPool.shutdown();
	}

	/**
	 * Send all of the required messages and then send a done message to the
	 * parent
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
					// Delay the required time
					System.out.println(id + ": Waiting " + m.delay + " ms");
					Thread.sleep(m.delay);

					// Connect to the parent server and get the in and out
					// streams
					Socket connection = new Socket("localhost", parent.port);
					DataInputStream in = new DataInputStream(connection.getInputStream());
					DataOutputStream out = new DataOutputStream(connection.getOutputStream());

					// Send the message
					System.out.println(id + ": Sending message to " + m.target + ": " + m.message);
					writeMessage(m, out);

					// Wait for a response
					Message response = readMessage(in);
					System.out.println(id + ": Received response from " + response.source + ": " + response.message);

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
	 * Write a message to the output stream
	 * 
	 * @param m
	 *            Message
	 * @param out
	 *            Output stream
	 * @throws IOException
	 */
	public static void writeMessage(Message m, DataOutputStream out) throws IOException {
		out.writeInt(m.source);
		out.writeInt(m.target);
		out.writeUTF(m.message);
	}

	/**
	 * Read a message from the input stream
	 * 
	 * @param in
	 *            The input stream
	 * @return The message
	 * @throws IOException
	 */
	public static Message readMessage(DataInputStream in) throws IOException {
		return new Message(in.readInt(), in.readInt(), in.readUTF());
	}

	private void receiveMessages() {
		threadPool.submit(new Runnable() {
			@Override
			public void run() {
				while (listening) {
					try {
						System.out.println(id + ": Waiting for message");
						
						// Listen for any connections
						Socket socket = messageListener.accept();
						DataInputStream in = new DataInputStream(socket.getInputStream());
						DataOutputStream out = new DataOutputStream(socket.getOutputStream());

						// Receive the message
						Message received = readMessage(in);

						// Handle the message
						if (received.target == id) {
							if (received.message.equals(Message.close)) {
								writeMessage(new Message(id, received.source, "Closing " + id), out);
								close();
							} else {
								writeMessage(new Message(id, received.source, "Hello from " + id), out);
							}
						} else {
							writeMessage(new Message(id, received.source,
									id + " received message meant for " + received.target), out);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		});
	}
}

class Server extends Node {

	HashSet<Node> connections = new HashSet<Node>();

	private int doneCounter = 0;

	public Server(int id, int port) {
		super(id, port);
	}

	@Override
	public void run() {
		forwardMessages();
	}

	private synchronized void incrementDone() {
		doneCounter++;
		System.out.println(id + ": " + doneCounter + " done, " + (connections.size() - doneCounter) + " remaining");
	}

	public void addConnection(Node n) {
		connections.add(n);
	}

	private int getPort(int id) {
		for (Node n : connections) {
			if (n.id == id) {
				return n.port;
			}
		}
		System.out.println(this.id + ": Missing connection to " + id);
		for (Node n : connections) {
			System.out.println(n.id + ":" + n.port);
		}
		return -1;
	}

	private void sendCloseNotifications() {
		for (final Node n : connections) {
			threadPool.submit(new Callable<Message>() {
				@Override
				public Message call() throws Exception {
					try {
						// Connect to the parent server and get the in and out
						// streams
						Socket connection = new Socket("localhost", getPort(n.id));
						DataInputStream in = new DataInputStream(connection.getInputStream());
						DataOutputStream out = new DataOutputStream(connection.getOutputStream());

						// Send the message
						System.out.println(id + ": Sending close message to " + n.id);
						writeMessage(new Message(id, n.id, Message.close), out);
						
						// Wait for a response
						Message response = readMessage(in);
						System.out.println(id + ": Received response from " + response.source + ": " + response.message);

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

	private Future<Message> forwardMessage(final Message m) {
		return threadPool.submit(new Callable<Message>() {
			@Override
			public Message call() throws Exception {
				try {
					// Connect to the parent server and get the in and out
					// streams
					Socket connection = new Socket("localhost", getPort(m.target));
					DataInputStream in = new DataInputStream(connection.getInputStream());
					DataOutputStream out = new DataOutputStream(connection.getOutputStream());

					// Send the message
					System.out.println(id + ": Forwarding message to " + m.target + ": " + m.message);
					writeMessage(m, out);

					// Wait for a response
					Message response = readMessage(in);
					System.out.println(id + ": Received response from " + response.source + ": " + response.message);

					// And close the connection
					connection.close();

					// Return the Future response
					return response;
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
		});
	}

	private void forwardMessages() {
		threadPool.submit(new Runnable() {
			@Override
			public void run() {
				while (connections.size() > doneCounter) {
					try {
						System.out.println(id + ": Waiting for message");

						// Listen for any connections
						Socket socket = messageListener.accept();
						DataInputStream in = new DataInputStream(socket.getInputStream());
						DataOutputStream out = new DataOutputStream(socket.getOutputStream());

						// Receive the message
						Message received = readMessage(in);

						// Handle the message
						if (received.target == id) {
							if (received.message.equals(Message.done)) {
								incrementDone();
								writeMessage(new Message(id, received.source, "Done message received " + id), out);
							} else {
								writeMessage(new Message(id, received.source, "Direct message from " + id), out);
							}
						} else {
							System.out.println(
									id + ": Forwarding message from " + received.source + " to " + received.target);
							Future<Message> response = forwardMessage(received);
							writeMessage(response.get(), out);
						}
					} catch (IOException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				}

				System.out.println(id + ": All nodes done, shutting down");
				sendCloseNotifications();
				close();
			}

		});
	}
}

/**
 * Simple Message class
 */
class Message {
	public final int delay;
	public final int source;
	public final int target;
	public final String message;
	private ArrayList<Integer> path = new ArrayList<Integer>();

	public static String done = "done";
	public static String close = "close";

	/**
	 * @param source
	 *            Who sent the message
	 * @param target
	 *            Where to send the message
	 * @param message
	 *            What to send
	 */
	public Message(int source, int target, String message) {
		this(0, source, target, message);
	}

	/**
	 * @param delay
	 *            How long to wait before sending
	 * @param source
	 *            Who sent the message
	 * @param target
	 *            Where to send the message
	 * @param message
	 *            What to send
	 */
	public Message(int delay, int source, int target, String message) {
		this.source = source;
		this.delay = delay;
		this.target = target;
		this.message = message;
		this.path.add(this.source);
	}

	/**
	 * @param source
	 *            Add a node to the path
	 */
	public void addToPath(int id) {
		path.add(new Integer(id));
	}

	/**
	 * @return path
	 */
	public ArrayList<Integer> getPath() {
		return path;
	}

	@Override
	public String toString() {
		return "delay: " + delay + "\n" + "source: " + source + "\n" + "target: " + target + "\n" + "message: "
				+ message + "\n";
	}
}

/**
 * Useful serial executor class. Executes the given runnables in order without
 * blocking the main thread.
 * 
 * Found at
 * https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executor.html
 */
class SerialExecutor implements Executor {
	final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
	final Executor executor;
	Runnable active;

	SerialExecutor(Executor executor) {
		this.executor = executor;
	}

	public synchronized void execute(final Runnable r) {
		tasks.offer(new Runnable() {
			public void run() {
				try {
					r.run();
				} finally {
					scheduleNext();
				}
			}
		});
		if (active == null) {
			scheduleNext();
		}
	}

	private synchronized void scheduleNext() {
		if ((active = tasks.poll()) != null) {
			executor.execute(active);
		}
	}
}