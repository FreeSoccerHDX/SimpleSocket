package de.freesoccerhdx.simplesocket.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.freesoccerhdx.simplesocket.ResponseStatus;
import de.freesoccerhdx.simplesocket.SocketMessage;
import org.json.JSONArray;
import org.json.JSONObject;

public class ClientSocket {
	
	protected static void handleConnection(SimpleSocketServer sss, Socket client) {
		// make sure the Client registers with a Name
		
		// if success: send back success msg
		
		
		try {
			ClientSocket clsock = new ClientSocket(sss, client, "<unset>");
			SocketMessage nachricht = readNextMessage(sss, clsock);
			
			//System.out.println("loginmsg:  " + nachricht);
			
		 	if(nachricht.channelid.equals("login")) {
		 		if(nachricht.message != null) {
		 			String clientname = nachricht.message;
		 			
		 			clsock.updateName(clientname);
		 			
		 			sss.addClient(clientname, clsock);
		 			
		 		}
		 	}
		 	
		 	
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
	 	
	}

	private Socket client = null;
	private SimpleSocketServer sss;
	private String clientname;
	private boolean connected = true;
	
	protected HashMap<UUID, ClientResponse> socketrespons = new HashMap<>();
	
	private Thread mainthread = null;
	
	
	
	protected ClientSocket(SimpleSocketServer sss, Socket client, String clientname) {
		this.client = client;
		this.sss = sss;
		this.clientname = clientname;
		this.mainthread = Thread.currentThread();
	}
	
	private void updateName(String s) {
		clientname = s;
	}
	
	protected void startReadingMessages() {
		try {
			while (connected) {
				SocketMessage cm = readNextMessage(sss,this);
				
				
				if(cm == null) {
					connected = false; 
					break;
				}
				
				if(cm.channelid.equals("ping")) {
					
					for(String target : cm.targets) {
						if(target.equals("Server")) {
							sendNewMessage("pong", cm.message+"#"+System.currentTimeMillis(), null);
						}else {
							sss.transferMessage(target,cm.json);
						}
					}
					
				}else if(cm.channelid.equals("response") && cm.targets.size() > 0 && cm.targets.get(0).equals("Server")) {
					
					String data = cm.message;
					
					int rs_id = Integer.parseInt(data.split("/", 2)[0]);
					
					ResponseStatus rs = ResponseStatus.values()[rs_id];
					UUID uuid = UUID.fromString(data.split("/", 2)[1]);
					if(socketrespons.containsKey(uuid)) {
						
						socketrespons.get(uuid).response(sss, rs, cm.trace.get(0));
						
						new Thread(new Runnable() {

							@Override
							public void run() {
								try {
									Thread.sleep(1000*10);
									
									socketrespons.remove(uuid);
								}catch(Exception ex) {
									ex.printStackTrace();
								}
							}
							
						}).start();
						
					}else {
						System.err.println(" - ");
						System.err.println(" UUID not found for ResponseListener!");
						System.err.println(" - ");
						// TODO: UUID not in list ?
					}
					
				}else if(cm.channelid.equals("clientlist")) {
					String clientnames = "";
					for(String s : sss.getClientsNametList()) {
						clientnames += s + ",";
					}
					if(clientnames.endsWith(",")) {
						clientnames = clientnames.substring(0, clientnames.length()-1);
					}
					sendNewMessage("clientlist", clientnames, null);
					
				}else if(cm.channelid.equals("pong")) {
					if(cm.targets.size() == 1) {
						String target = cm.targets.get(0);
						if(target.equals("Server")) {
							String[] times = cm.message.split("#");
							
							long started = Long.parseLong(times[0]);
							long sendback = Long.parseLong(times[1]);
							long timenow = System.currentTimeMillis();
							
							
							System.out.println("Ping to " + getClientName()+":"
							+"\n	Time to Client: " + (sendback-started) +"ms"
							+"\n	Ping: " + (timenow-started)+"ms");
							
							
						}else {
							
							
							
							// Pong weiterleiten an target Client
							sss.transferMessage(target, cm.json);
							// TODO: Send back to target client
						}
					}
				}else {
					// move this
					if(cm.targets.size() > 0) {
						for(String target : cm.targets) {
							if(!target.equals("Server")) {
								if(sss.getClientsNametList().contains(target)) {
									sss.transferMessage(target, cm.json);
								}
							}else {
								if(!sss.handleCustom(ClientSocket.this, cm.channelid,cm.targets,cm.message)) {
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
				
				if(SimpleSocketServer.DEBUG){
					System.out.println("readmsg: " + cm);
				}
				
			}
			
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getClientName() {
		return clientname;
	}
	
	protected Socket getSocket() {
		return client;
	}
	
	protected boolean sendMessage(String msg) {
		if(!connected) return false;
		
		try {
			OutputStreamWriter osw = new OutputStreamWriter(client.getOutputStream());
			PrintWriter printWriter = new PrintWriter(osw);
			
			printWriter.print(msg);
		 	printWriter.flush();
		 	
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return false;
	}
	
	protected boolean sendNewMessage(String channel, JSONObject jsonmsg, ClientResponse response) {
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
			
			sendMessage(completemsg);
		 	
		 	return true;
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
			
			sendMessage(completemsg);
		 	
		 	return true;
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		 	
		return false;
	}
	
	private static String requiereMessage(BufferedReader reader, int length) throws IOException {
		char[] buffer = new char[length];
	 	int anzahlZeichen = reader.read(buffer, 0, length); // blockiert bis Nachricht empfangen
	 	String msg = new String(buffer, 0, anzahlZeichen);
	 	
	 	return msg;
	}
	
	private static SocketMessage readNextMessage(SimpleSocketServer sss, ClientSocket cs) throws IOException,NumberFormatException {
		BufferedReader bufferedReader =  new BufferedReader(
											new InputStreamReader(
													cs.getSocket().getInputStream()));
		try {
		
			String length_msg = requiereMessage(bufferedReader, 8);
		 	
		 	int total_length = Integer.parseInt(length_msg.replaceAll(" ", ""));
		 	
		 	String json_msg = requiereMessage(bufferedReader, total_length);
		 	
		 
		 	
		 	JSONObject json = null;
			try {
				json = new JSONObject(json_msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			if(json == null) {
				return null;
			}
			
			JSONArray trace = json.getJSONArray("trace");
			trace.put("Server");
			json.put("trace", trace);
			
			String channel = (String) json.get("channel");
			JSONArray targets = json.getJSONArray("targets");
			String msg = (String) json.get("msg");
			
			if(json.has("response_id")) {
		 		UUID uuid = UUID.fromString((String) json.get("response_id"));
				JSONObject responsejson = new JSONObject();
		 		responsejson.put("id", uuid.toString());
		 		
		 		List<String> reachedlist = new ArrayList<>();
		 		reachedlist.add("Server#"+ResponseStatus.SERVER_REACHED.getID());
		 		
		 		
		 		
		 		Set<String> clientnames = sss.getClientsNametList();
				Iterator<Object> targetIterator = targets.iterator();
		 		while(targetIterator.hasNext()) {
					String target = (String) targetIterator.next();
		 			if(!target.equals("Server") && !clientnames.contains(target)) {
		 				reachedlist.add(target+"#"+ResponseStatus.TARGET_NOT_FOUND.getID());
		 			}
		 		}
		 		
		 		responsejson.put("reached", reachedlist);
		 		cs.sendNewMessage("response", responsejson, null);
		 		
		 		//cs.sendNewMessage("response", ResponseStatus.SERVER_REACHED.getID()+"/"+uuid.toString(), null);
		 	}

			//System.out.println("json: " + json_msg);

			return new SocketMessage(json, trace, channel, targets, msg);
		 	
		}catch(Exception ex) {
			if(ex instanceof NumberFormatException) {
				System.out.println("["+SimpleSocketServer.NAME+"] Client("+cs.getClientName()+") got messages to fast!");
			}else if(ex instanceof StringIndexOutOfBoundsException) { // StringIndexOutOfBoundsException => client.close was called
				System.out.println("["+SimpleSocketServer.NAME+"] Client("+cs.getClientName()+") closed Connection intentionally!");
			}else{ 
				ex.printStackTrace();
			}
			cs.connected = false;
			cs.sendNewMessage("disconnect", "", null);
			System.out.println("["+SimpleSocketServer.NAME+"] Client disconnected. ("+cs.getClientName()+")");
			cs.sss.removeClient(cs.getClientName());
			
		}
		
	 	return null;
	
	}

	public void stop() {
		connected = false;
		
	//	System.out.println("Stopping mainthread: " + mainthread.getName());
		
		try {
			this.client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mainthread.interrupt();
	}

}
