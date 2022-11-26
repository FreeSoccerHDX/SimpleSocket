package de.freesoccerhdx.simplesocket.client;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class PingHandler {

    private SimpleSocketClient simpleSocketClient;

    private long ping = -1;

    public PingHandler(SimpleSocketClient simpleSocketClient){
        this.simpleSocketClient = simpleSocketClient;

        this.simpleSocketClient.setSocketListener("ping", new ClientListener() {
            @Override
            public void recive(SimpleSocketClient ssc, String channel, String source_name, String message) {
                JSONObject jsonObject = new JSONObject(message);
                jsonObject.put("clienttime", System.currentTimeMillis());
                ssc.sendMessage("pong",source_name,jsonObject.toString());

            }
        });

        this.simpleSocketClient.setSocketListener("pong", new ClientListener() {
            @Override
            public void recive(SimpleSocketClient ssc, String channel, String source_name, String message) {

                JSONObject jsonObject = new JSONObject(message);
                Long started = jsonObject.getLong("start");
                Long timenow = System.currentTimeMillis();

              //  System.out.println("PingDebug: " + started + " to " + timenow);

                if (ping == -1) {
                    ping = (timenow - started);
                } else {
                    ping = (ping * 3 + (timenow - started)) / 4;
                }

              //  System.out.println("New Ping: " + ping);

            }
        });

        this.simpleSocketClient.getTimer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(simpleSocketClient.isRunning()) {
                    sendPingChannelMessage();
                }
            }
        }, 1000*5, 1000*5);
    }

    public void sendPingChannelMessage(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("start", System.currentTimeMillis());
        PingHandler.this.simpleSocketClient.sendMessage("ping", new String[] {"Server"}, jsonObject);
    }

    public long getPing(){
        return this.ping;
    }

    public void onStop() {
        this.ping = -1;
    }
}
