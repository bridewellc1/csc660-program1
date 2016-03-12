package csc660;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class ProcessFileMessages {

    public static ArrayList<Message> readfile(String fileName) throws IOException {
        int delay = 0;
        int target;
        String line = null;
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<Message> messages = new ArrayList<Message>();
		//Assume the file is located at the root directory. we can change the code
        //File name should start with 0, 1,...., or 9
        int source = Character.getNumericValue(fileName.charAt(0));

        FileReader fileReader = new FileReader(fileName);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
            lines.add(line);
        }
        bufferedReader.close();
        for (int i = 0; i < lines.size(); i++) {
            String[] tab = lines.get(i).split(" ");
            //If the line contains just the delay, then: target=source, message is empty.
            if (tab.length == 1) {
                delay = Integer.parseInt(tab[0]);
                messages.add(new Message(delay, source, source, ""));
            } //if not we create a message without delay
            else {
                target = Integer.parseInt(tab[0]);
                String msg = "";
                for (int j = 1; j < tab.length; j++) {
                    msg = msg + " " + tab[j];
                }
                messages.add(new Message(source, target, msg));
            }

        }

        return messages;
    }

 
}
