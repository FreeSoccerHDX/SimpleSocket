package de.freesoccerhdx.simplesocket.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.freesoccerhdx.simplesocket.Pair;
import de.freesoccerhdx.simplesocket.ResponseStatus;
import de.freesoccerhdx.simplesocket.SocketBase;
import de.freesoccerhdx.simplesocket.SocketMessage;
import org.json.JSONArray;
import org.json.JSONObject;

public class ClientSocket extends SocketBase {

	protected static void handleConnection(SimpleSocketServer sss, Socket client) {
		// make sure the Client registers with a Name
		
		// if success: send back success msg
		
		
		try {
		//	System.out.println("a creating obj");
			ClientSocket clsock = new ClientSocket(sss, client, "<unset>");
		//	System.out.println("b now reading next...");
			Pair<SocketInfo, SocketMessage> pairInfo = clsock.readNextMessage(sss);
		//	System.out.println("c finished reading");
			SocketInfo info = pairInfo.getFirst();
			SocketMessage socketMessage = pairInfo.getSecond();
		//	System.out.println("d= " + info + " " + socketMessage);
			//System.out.println("loginmsg:  " + nachricht);

			if(info != SocketInfo.SUCCESS){
		//		System.out.println(" not ");
				System.err.println("Client-Login was not successful!");
				try{
					client.close();
				}catch (Exception exception){
					exception.printStackTrace();
				}
				return;
			}

		 	if(socketMessage.channelid.equals("login")) {
		 		if(socketMessage.message != null) {
		 			String clientname = socketMessage.message;
		 			
		 			clsock.updateName(clientname);
		 			
		 			sss.addClient(clientname, clsock);
		 			
		 		}
		 	}
		 	
		 	
		} catch (Exception e) {
			e.printStackTrace();
			if(!(e instanceof IllegalStateException) || !e.getMessage().equals("Client recieved null.")) {
				e.printStackTrace();
			}
		}
	 	
	}

	private Socket client = null;
	private SimpleSocketServer sss;
	private String clientname;
	private boolean connected = true;
	
	protected HashMap<UUID, ClientResponse> socketrespons = new HashMap<>();
	
	
	
	protected ClientSocket(SimpleSocketServer sss, Socket client, String clientname) {
		this.client = client;
		this.sss = sss;
		this.clientname = clientname;
	}
	
	private void updateName(String s) {
		clientname = s;
	}
	
	protected void startReadingMessages() {
		try {
			while (connected) {
				Pair<SocketInfo, SocketMessage> pairInfo = this.readNextMessage(sss);
				SocketInfo socketInfo = pairInfo.getFirst();
				SocketMessage cm = pairInfo.getSecond();

				if(socketInfo != SocketInfo.SUCCESS) {
					if(socketInfo != SocketInfo.COULD_NOT_PARSE_JSON) {
						connected = false;
						if(socketInfo != SocketInfo.COULD_NOT_GETLENGTH_MSG && socketInfo != SocketInfo.COULD_NOT_GETMESSAGE) {
							throw new IllegalStateException("Reading Messages did not work! Cause=" + socketInfo);
						}else{
							throw new SocketException("Reading Messages did not work! Cause=" + socketInfo);
						}
					}else{
						System.err.println("Reading Messages had an error but will not stop: " + socketInfo);
					}
				}else {

					if (cm.channelid.equals("ping")) {
						for (String target : cm.targets) {
							if (target.equals("Server")) {
								JSONObject jsonObject = new JSONObject(cm.message);
								jsonObject.put("targettime",System.currentTimeMillis());
								sendNewMessage("pong", jsonObject, null);
							} else {
								sss.transferMessage(target, cm.json);
							}
						}

					} else if (cm.channelid.equals("response") && cm.targets.size() > 0 && cm.targets.get(0).equals("Server")) {

						String data = cm.message;

						int rs_id = Integer.parseInt(data.split("/", 2)[0]);

						ResponseStatus rs = ResponseStatus.values()[rs_id];
						UUID uuid = UUID.fromString(data.split("/", 2)[1]);
						if (socketrespons.containsKey(uuid)) {

							socketrespons.get(uuid).response(sss, rs, cm.trace.get(0));

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {
										Thread.sleep(1000 * 10);

										socketrespons.remove(uuid);
									} catch (Exception ex) {
										ex.printStackTrace();
									}
								}

							}).start();

						} else {
							System.err.println(" - ");
							System.err.println(" UUID not found for ResponseListener!");
							System.err.println(" - ");
							// TODO: UUID not in list ?
						}

					} else if (cm.channelid.equals("clientlist")) {
						String clientnames = "";
						for (String s : sss.getClientsNametList()) {
							clientnames += s + ",";
						}
						if (clientnames.endsWith(",")) {
							clientnames = clientnames.substring(0, clientnames.length() - 1);
						}
						sendNewMessage("clientlist", clientnames, null);

					} else if (cm.channelid.equals("pong")) {
						if (cm.targets.size() == 1) {
							String target = cm.targets.get(0);
							if (target.equals("Server")) {
								String[] times = cm.message.split("#");

								long started = Long.parseLong(times[0]);
								long sendback = Long.parseLong(times[1]);
								long timenow = System.currentTimeMillis();


								System.out.println("Ping to " + getClientName() + ":"
										+ "\n	Time to Client: " + (sendback - started) + "ms"
										+ "\n	Ping: " + (timenow - started) + "ms");


							} else {


								// Pong weiterleiten an target Client
								sss.transferMessage(target, cm.json);
								// TODO: Send back to target client
							}
						}
					} else {
						// move this
						if (cm.targets.size() > 0) {
							for (String target : cm.targets) {
								if (!target.equals("Server")) {
									if (sss.getClientsNametList().contains(target)) {
										sss.transferMessage(target, cm.json);
									}
								} else {
									if (!sss.handleCustom(ClientSocket.this, cm.channelid, cm.targets, cm.message)) {
										System.out.println("---------------");
										System.out.println("---------------");
										System.out.println("Retrieving Msg for no Handling to Server: " + cm);
										System.out.println("---------------");
										System.out.println("---------------");
									}
								}
							}
						}
					}
				}

				
				if(SimpleSocketServer.DEBUG){
					System.out.println("readmsg: " + cm);
				}
				
			}
			
		} catch (Exception e) {
			this.connected = false;
			this.sendNewMessage("disconnect", "", null);
			System.out.println("[" + SimpleSocketServer.NAME + "] Client disconnected. (" + this.getClientName() + ")");
			this.sss.removeClient(this.getClientName());
			this.stop();

			if(!(e instanceof SocketException)){
				e.printStackTrace();
			}
			//if(!(e instanceof IllegalStateException)){

			//}

		}
	}
	
	public String getClientName() {
		return clientname;
	}
	
	protected Socket getSocket() {
		return client;
	}

	
	public boolean sendNewMessage(String channel, JSONObject jsonmsg, ClientResponse response) {
		if(!connected) return false;
		try {

			JSONObject json = new JSONObject();
			
			ArrayList<String> trace = new ArrayList<>();
			trace.add("Server");
			
			json.put("trace", trace);
			json.put("channel", channel);
			json.put("targets", Arrays.asList(new String[] {getClientName()}));
			json.put("msg", jsonmsg);
			
			if(response != null) {
				UUID uuid = UUID.randomUUID();
				socketrespons.put(uuid, response);
				
				json.put("response_id", uuid.toString());
			}
			
			String json_str = json.toString();
			String json_lng = ""+json_str.length();
			
			int l = json_lng.length();
			while(l < 8) {
				json_lng += " ";
				l++;
			}
			
			
			String completemsg = json_lng + json_str;
			
			return sendMessage(this.getSocket().getOutputStream(),completemsg);
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		 	
		return false;
	}

	public boolean sendNewMessage(String channel, String msg, ClientResponse response) {
		if(!connected) return false;
		try {

			JSONObject json = new JSONObject();
			
			ArrayList<String> trace = new ArrayList<>();
			trace.add("Server");
			
			json.put("trace", trace);
			json.put("channel", channel);
			json.put("targets", Arrays.asList(new String[] {getClientName()}));
			json.put("msg", msg);
			
			if(response != null) {
				UUID uuid = UUID.randomUUID();
				socketrespons.put(uuid, response);
				
				json.put("response_id", uuid.toString());
			}
			
			String json_str = json.toString();
			String json_lng = ""+json_str.length();
			
			int l = json_lng.length();
			while(l < 8) {
				json_lng += " ";
				l++;
			}
			
			
			String completemsg = json_lng + json_str;
			
			sendMessage(this.getSocket().getOutputStream(), completemsg);
		 	
		 	return true;
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		 	
		return false;
	}

	private synchronized Pair<SocketInfo,SocketMessage> readNextMessage(SimpleSocketServer sss) throws Exception {
		//try {
	//	System.out.println("read next...");
		Pair<SocketBase.SocketInfo, JSONObject> s = super.readNextMessage(this.getSocket().getInputStream());
	//	System.out.println("read finish");
		SocketBase.SocketInfo info = s.getFirst();
		JSONObject json = s.getSecond();

		if(info != SocketBase.SocketInfo.SUCCESS){
			return Pair.of(info,null);
		}

		JSONArray trace = json.getJSONArray("trace");
		trace.put("Server");
		json.put("trace", trace);

		String channel = (String) json.get("channel");
		JSONArray targets = json.getJSONArray("targets");
		Object msg = json.has("msg") ? json.get("msg") : ""; // can be JSONObject or String

		if (json.has("response_id")) {
			UUID uuid = UUID.fromString((String) json.get("response_id"));
			JSONObject responsejson = new JSONObject();
			responsejson.put("id", uuid.toString());

			List<String> reachedlist = new ArrayList<>();
			reachedlist.add("Server#" + ResponseStatus.SERVER_REACHED.getID());


			Set<String> clientnames = sss.getClientsNametList();
			Iterator<Object> targetIterator = targets.iterator();
			while (targetIterator.hasNext()) {
				String target = (String) targetIterator.next();
				if (!target.equals("Server") && !clientnames.contains(target)) {
					reachedlist.add(target + "#" + ResponseStatus.TARGET_NOT_FOUND.getID());
				}
			}

			responsejson.put("reached", reachedlist);
			this.sendNewMessage("response", responsejson, null);
		}

		if (msg instanceof JSONObject) {
			return Pair.of(SocketInfo.SUCCESS, new SocketMessage(json, trace, channel, targets, ((JSONObject) msg).toString()));
		} else {
			return Pair.of(SocketInfo.SUCCESS, new SocketMessage(json, trace, channel, targets, (String) msg));
		}

	}


	public void stopClient() {
		connected = false;

		try {
			this.client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		interrupt();
	}

}
