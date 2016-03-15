package csc660;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Convert a file into a list of messages
 */
public class ProcessFileMessages {

	/**
	 * The path of the messages (%d is replaced)
	 */
	static String basePath = "src/data/%dinput.txt";

	/**
	 * @param source
	 *            The number of the source file
	 * @return list of messages
	 * @throws IOException
	 */
	public static ArrayList<Message> readfile(int source) throws IOException {
		String fileName = String.format(basePath, source);
		ArrayList<Message> messages = new ArrayList<Message>();
		String line;
		BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
		int delay = 0;
		while ((line = bufferedReader.readLine()) != null) {
			// System.out.println(line);
			String[] tab = line.split(" ");
			// If the line contains just the delay, add the delay before the next message
			if (tab.length == 1) {
				delay += Integer.parseInt(tab[0]);
			}
			// Create the new message
			else {
				int target = Integer.parseInt(tab[0]);
				String msg = tab[1];
				for (int j = 2; j < tab.length; j++) {
					msg = msg + " " + tab[j];
				}
				msg = msg.replaceAll("\"", "");
				messages.add(new Message(delay, source, target, msg));
				delay = 0;
			}
		}
		bufferedReader.close();
		return messages;
	}
}
