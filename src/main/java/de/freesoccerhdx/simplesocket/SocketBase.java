package de.freesoccerhdx.simplesocket;

import com.sun.source.tree.ReturnTree;
import de.freesoccerhdx.simplesocket.client.SimpleSocketClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public abstract class SocketBase extends Thread {

    public static Thread createThreadUnstarted(String name, boolean daemon, Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(daemon);
        thread.setName(name == null ? "" : name);
        return thread;
    }

    public static Thread createThread(String name, boolean daemon, Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(daemon);
        thread.setName(name == null ? "" : name);
        thread.start();
        return thread;
    }

    public static String createMessageLengthString(String msg) {
        String msg_lng = ""+msg.length();

        int l = msg_lng.length();
        while(l < 8) {
            msg_lng += " ";
            l++;
        }
        return msg_lng + msg;
    }


    private boolean isSending = false;

    public boolean sendMessage(PrintWriter printWriter, String msg) {
        if(isSending){
            try {
                Thread.sleep(0, 100);
            }catch (Exception e) {
                e.printStackTrace();
            }
            return sendMessage(printWriter, msg);
        }

        isSending = true;

        try {
            //OutputStreamWriter osw = new OutputStreamWriter(outputStream);
            //PrintWriter printWriter = new PrintWriter(osw);
            //System.out.println("###");
            //System.out.println("### Sending: " + msg);
            //System.out.println("###");
            printWriter.write(msg);
            printWriter.flush();

            isSending = false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        isSending = false;
        return false;
    }

    private String requiereMessage(BufferedReader reader, int length) throws Exception {
        char[] buffer = new char[length];
        int countChars = 0;
        //System.out.println("Start: " + countChars + " of " + length);
        countChars += reader.read(buffer, countChars, length); // blockiert bis Nachricht empfangen
        //System.out.println("After first: " + countChars + " of " + length);
        if(countChars == -1) {
            return null;
        }
        while(countChars < length){
            int dif = length-countChars;
            int newzeichen = reader.read(buffer, countChars, dif);
            if(newzeichen == -1) {
                return null;
            }
            countChars += newzeichen;
            //System.out.println("while try: " + countChars + " of " + length);
        }
        String msg = new String(buffer, 0, length);
        //System.out.println("End: " + countChars + " of " + length);

        return msg;
    }

    private Pair<SocketInfo,String> readNewMessage(BufferedReader bufferedReader) throws Exception {
        //BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String length_msg = requiereMessage(bufferedReader, 8);

        if (length_msg == null) {
            return Pair.of(SocketInfo.COULD_NOT_GETLENGTH_MSG, null);
        }
        int total_length = Integer.parseInt(length_msg.replaceAll(" ", ""));

        String json_msg = requiereMessage(bufferedReader, total_length);
        if(json_msg == null) {
            return Pair.of(SocketInfo.COULD_NOT_GETMESSAGE, null);
        }

        return Pair.of(SocketInfo.SUCCESS, json_msg);
    }

    protected Pair<SocketInfo,JSONObject> readNextMessage(BufferedReader bufferedReader) throws Exception {
        Pair<SocketInfo, String> s = readNewMessage(bufferedReader);
        SocketInfo info = s.getFirst();
        String jsonmsg = s.getSecond();

        if (info != SocketInfo.SUCCESS) {
            return Pair.of(info, null);
        }

        JSONObject json = null;
        try {
            json = new JSONObject(jsonmsg);
        } catch (Exception e) {
            e.printStackTrace();
            return Pair.of(SocketInfo.COULD_NOT_PARSE_JSON, null);
        }

    //    System.out.println("Message got= '" + json + "'");
        return Pair.of(SocketInfo.SUCCESS, json);
    }



    public enum SocketInfo{

        COULD_NOT_GETLENGTH_MSG,
        COULD_NOT_PARSE_JSON,
        COULD_NOT_GETMESSAGE,
        SERVER_DISCONNECTED,
        SUCCESS;

    }

}
