package csc660;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Simple Message class
 */
public class Message {
	/**
	 * How long to delay before sending
	 */
	public final int delay;

	/**
	 * Who sent the message
	 */
	public final int source;

	/**
	 * The target of the message
	 */
	public final int target;

	/**
	 * The message
	 */
	public final String message;

	/**
	 * The time the message was sent
	 */
	private double time;

	/**
	 * The done message
	 */
	public static String done = "done";

	/**
	 * The close message
	 */
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
	}

	/**
	 * @param t
	 *            The time to set
	 */
	public void setTime(double t) {
		this.time = t;
	}

	/**
	 * @return the time
	 */
	public double getTime() {
		return this.time;
	}

	@Override
	public String toString() {
		return "delay: " + delay + "\n" + "source: " + source + "\n" + "target: " + target + "\n" + "message: "
				+ message + "\n" + "time: " + time + "\n";
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
		out.writeDouble(m.time);
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
		Message m = new Message(in.readInt(), in.readInt(), in.readUTF());
		m.setTime(in.readDouble());
		return m;
	}
}