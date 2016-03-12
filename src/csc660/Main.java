package csc660;

import java.util.ArrayList;
import java.util.Random;

public class Main {
	public static void main(String[] args) {

		int nodeCount = 10; // how many nodes to spawn
		int portOffset = 7777; // the port offset

		// The spawned nodes
		ArrayList<Node> nodes = new ArrayList<Node>(nodeCount);

		// The main server
		Server mainServer = new Server(-1, portOffset - 1);

		boolean debug = true;
		if (debug) {
			int messageCount = 10; // how many messages each node should send
			int maxDelay = 3000; // max possible delay

			// Spawn nodeCount random nodes with messageCount random messages
			// each
			// with a random delay up to maxDelay. Create nodes before starting
			// them so all required ports are open
			Random r = new Random();
			for (int i = 0; i < nodeCount; i++) {
				ArrayList<Message> messages = new ArrayList<Message>();
				for (int j = 0; j < messageCount; j++) {
					int d = r.nextInt(maxDelay);

					// Make sure no node sends message to self
					int t = i;
					while (t == i) {
						t = r.nextInt(nodeCount);
					}

					String msg = "Hello from " + i;
					messages.add(new Message(d, i, t, msg));
				}

				Node n = new Node(i, portOffset + i, messages, mainServer);
				mainServer.addConnection(n);
				nodes.add(n);
			}
		} else {
			// read from file
		}

		// Start the main server thread
		Thread serverThread = new Thread(mainServer);
		serverThread.start();

		// Start the nodes in separate threads
		for (Node n : nodes) {
			Thread t = new Thread(n);
			t.start();
		}
	}
}