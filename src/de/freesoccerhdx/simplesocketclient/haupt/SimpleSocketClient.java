package de.freesoccerhdx.simplesocketclient.haupt;

import java.io.BufferedReader;



import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;


import de.freesoccerhdx.simplesocket.haupt.JSON;
import de.freesoccerhdx.simplesocket.haupt.SocketMessage;


public class SimpleSocketClient {
	
	public static boolean DEBUG = false;
	public static final String NAME = "SimpleSocketClient";
	private static Thread cmdthread = null;

	public static void main(String[] args) {
		String ip = "127.0.0.1"; // localhost
		int port = 11111;
		try {
			String clientname = "TestClient";
			if(args.length > 0) {
				clientname = args[0];
			}
			SimpleSocketClient client = new SimpleSocketClient(clientname,ip,port);
			//client.sendm
			
			cmdthread = new Thread(new Runnable() {

				@Override
				public void run() {
					while (true) {
						String name = System.console().readLine();
						
						
						//	System.out.println("Input: "+name);	
						if(name == null) name = "";
						
						if(name.toLowerCase().startsWith("ping")) {
							
							String target = "Server";
							
							if(name.length() > 5) {
								if(name.toLowerCase().replace("ping ", "").length() > 0) {
									target = name.split(" ",2)[1];
									boolean ok = false;
									for(String s : client.getClientNames()) {
										if(s.equals(target)) {
											ok = true;
										}
									}
									if(!ok) {
										target = "";
									}
								}
							}
							
							if(!target.equals("")) {
								System.out.println("Sending Ping to " + target +".");
								
								String preusers = client.getClientName()+":"+target+",";
								
								client.sendMessage("ping", new String[] {target}, (!target.equals("Server") ? preusers : "") + ""+System.currentTimeMillis());
							}else {
								System.out.println("The Client does not exist!");
							}
						}else if(name.toLowerCase().startsWith("test response")) {
							client.sendMessage("test response", "Tim", "einfach nur eine msg", new SocketResponse() {
								
								@Override
								public void response(SimpleSocketClient ssc, ResponseStatus status, String source_channel) {
									System.out.println(ssc.name+": " + status + " source_client: " + source_channel);
									
								}
							});
							
							System.out.println("Test started!");
							
						}else if(name.toLowerCase().startsWith("send")) { //send <chanel> <target,...> <msg>
							String[] args = name.split(" ",4);
							String chanel = args[1];
							String target = args[2];
							String msg = args[3];
							
							String[] targets;
							if(target.split(",").length > 1) {
								targets = target.split(",");
							}else {
								targets = new String[] {target};
							}
							client.sendMessage(chanel, targets, msg);
							
							
						}else if(name.equals("stop")) {
							System.out.println("You stopped the Client-Connection.");
							client.stop();
							break;
						}else if(name.equals("getping")) {
							System.out.println("Ping: " + client.getPing());
						}else if(name.toLowerCase().startsWith("list")) {
							
							String names = "";
							for(String s : client.getClientNames()) {
								if(client.getClientNames()[client.getClientNames().length-1].equals(s)) {
									names += s;
								}else {
									names += s + ",";
								}
							}
							
							System.out.println("All connected Clients["+client.getClientNames().length+"]: \n >>> " + names);
							
							
						}
						
					}
				}
				
			},"cmdthread");
			cmdthread.start();
			
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
 	}
	
	
	
	private long ping = -1;
	
	private Socket client;
	private String name;
	private boolean reconnect = true;
	private boolean running = true;
	private boolean login_succesfull = false;
	private List<String> msgbuffer = new ArrayList<>();
	
	private HashMap<String,ClientListener> socketlistener = new HashMap<>();
	private HashMap<UUID,SocketResponse> responselistener = new HashMap<>();
	
	
	private Thread reciveMsg_thread;
	private Thread timer_thread;
	private Thread reconnect_thread = null;
	
	private String[] clientlist = new String[] {};
	
	private String ip;
	private int port;
	
	private boolean stopped = false;
	
	public SimpleSocketClient(String name,String ip, int port) {
		this.ip = ip;
		this.port = port;
		this.name = name;
		
		try {
			connect();
		}catch(Exception ex) {
			if(ex instanceof ConnectException) {
				System.out.println("["+SimpleSocketClient.NAME+"] Login failed.");	
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						startReconnecting();
						
					}
				},"reconnect_handler").start();
			}else {
				ex.printStackTrace();
			}
		}
		
		
	}
	
	public void stop() {
		if(stopped) {
			return;
		}
		stopped = true;
		
		if(client != null) {
			try {
				client.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(reconnect_thread != null) {
			reconnect_thread.interrupt();
			if(reconnect_thread.isAlive()) {
				reconnect_thread.stop();
			}
		}
		
		if(reciveMsg_thread != null) {
			reciveMsg_thread.interrupt();
			if(reciveMsg_thread.isAlive()) {
				reciveMsg_thread.stop();
			}
		}
		
		if(timer_thread != null) {
			timer_thread.interrupt();
			if(timer_thread.isAlive()) {
				timer_thread.stop();
			}
		}
		
		//for(Thread thread : Thread.getAllStackTraces().keySet()) {
		//	System.out.println("Thread running: " + thread.getName());
		//}
	}
	
	private void connect() throws Exception {
		client = new Socket(ip,port);
		System.out.println("["+SimpleSocketClient.NAME+"] Client connected.");
		running = true;
		startReciveMessages();
		startTimerThread();
		
		//send Login msg
		login_succesfull = true;
		boolean b = sendMessage("login", new String[0], name);
		login_succesfull = false;
		if(b) {
			System.out.println("["+SimpleSocketClient.NAME+"] Login message was sended successful.");
		}else {
			throw new ConnectException("["+SimpleSocketClient.NAME+"] Could not send login message to Server.");
		}
	}
	
	
	private void startReconnecting() {
		
		if(isReconnecting()) {
			reconnect_thread = Thread.currentThread();
			System.out.println("["+SimpleSocketClient.NAME+"] Client will try to reconnect...");
			
			while(!stopped) {
				try {
					Thread.sleep(1000*5);
					
					if(stopped) break;
					
					System.out.println("["+SimpleSocketClient.NAME+"] Reconnecting...");
					
					connect();
					
					break;
					
				}catch(Exception ex) {
					if(stopped) break;
					if(ex instanceof ConnectException) {
						System.out.println("["+SimpleSocketClient.NAME+"] Reconnect failed");
					}else {
						ex.printStackTrace();
					}
				}
			}
			
		}else {
			System.out.println("["+SimpleSocketClient.NAME+"] Client will not try to reconnect!");
			if(cmdthread != null) {
				cmdthread.interrupt();
			}
		}
	}
	
	
	private void startReciveMessages() {
		reciveMsg_thread = new Thread(new Runnable() {

			@Override
			public void run() {
				while(running) {
					try {
						SocketMessage sm = readNextMessage(SimpleSocketClient.this);
						
						if(sm == null) {
							// TODO: Handle Server stopped ?
							running = false;
							break;
						}

						if(sm.channelid.equals("ping")) {
							if(DEBUG) {
								System.out.println("["+SimpleSocketClient.NAME+"] Ping recived");
							}
							String target = "Server";
							if(sm.message.split(",",2).length == 2) {
								target = sm.message.split(",",2)[0].split(":",2)[0];
							}
							
							SimpleSocketClient.this.sendMessage("pong", new String[] {target}, sm.message+"#"+System.currentTimeMillis());
						
						}else if(sm.channelid.equals("pong")) {
							
							String targeted = "Server";
							String timemsg = sm.message;
							
							if(sm.message.split(",",2).length == 2) {
								targeted = sm.message.split(",",2)[0].split(":",2)[1];
								timemsg = sm.message.split(",",2)[1];
							}
							
							String[] times = timemsg.split("#");
							
							if(times.length == 2) {
								
								long started = Long.parseLong(times[0]);
								long sendback = Long.parseLong(times[1]);
								long timenow = System.currentTimeMillis();
								
								if(DEBUG) {
									System.out.println("Ping to " + targeted+":"
									+"\n	Time to Client: " + (sendback-started) +"ms"
									+"\n	Ping: " + (timenow-started)+"ms");
								}
								
								if(ping == -1) {
									ping = (timenow-started);
								}else {
									ping = (ping * 3 + (timenow-started)) / 4;
								}
								
							
							}else if(times.length == 4) {
								long started = Long.parseLong(times[0]);
								long servertime = Long.parseLong(times[1]);
								long otherclienttime = Long.parseLong(times[2]);
								long serverbacktime = Long.parseLong(times[3]);
								long timenow = System.currentTimeMillis();
								
								System.out.println("Ping to " + targeted+":"
								+"\n	Time to Server: " + (servertime-started) +"ms"
								+"\n	Time to Client: " + (otherclienttime-started) +"ms"
								+"\n	Time to Client to Server: " + (serverbacktime-started) +"ms"
								+"\n	Ping: " + (timenow-started) +"ms");
							}
						
						}else if(sm.channelid.equals("response")) {
							String data = sm.message;
							
							int rs_id = Integer.parseInt(data.split("/", 2)[0]);
							
							ResponseStatus rs = ResponseStatus.values()[rs_id];
							UUID uuid = UUID.fromString(data.split("/", 2)[1]);
							if(SimpleSocketClient.this.responselistener.containsKey(uuid)) {
								
								SimpleSocketClient.this.responselistener.get(uuid).response(SimpleSocketClient.this, rs, sm.trace.get(0));
								
								new Thread(new Runnable() {

									@Override
									public void run() {
										try {
											Thread.sleep(1000*10);
											
											SimpleSocketClient.this.responselistener.remove(uuid);
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
							
						}else if(sm.channelid.equals("clientlist")) {
							
							if(sm.message.split(",").length > 0) {
								clientlist = sm.message.split(",");
							}else {
								clientlist = new String[] {sm.message};
							}
							
						}else if(sm.channelid.equals("stop")) {	
							String stopmsg = sm.message;
							
							System.out.println("["+SimpleSocketClient.NAME+"] Server Stopped.\n > "+stopmsg);
							
							login_succesfull = false;
							running = false;
							clientlist = new String[] {};
						
							
							new Thread(new Runnable() {
								
								@Override
								public void run() {
									startReconnecting();
									
								}
							},"reconnect_handler").start();
							
							reciveMsg_thread.interrupt();
							timer_thread.interrupt();						
						
							
							break;
							
						}else if(sm.channelid.equals("login")) {	
							if(sm.message.equals("true")) {
								login_succesfull = true;
								for(String s : msgbuffer) {
									sendMessage(s);
								}
								msgbuffer.clear();
								System.out.println("["+SimpleSocketClient.NAME+"] Login was successful");
							}else {
								// TODO: Login was not successful
								System.out.println("["+SimpleSocketClient.NAME+"] Login was not successful");
							}
							
						}else if(socketlistener.containsKey(sm.channelid)) {
							socketlistener.get(sm.channelid).recive(SimpleSocketClient.this, sm.channelid, sm.trace.get(0) ,sm.message);
						}
						if(DEBUG) {
							System.out.println("ServerMessage: " + sm);
						}
						
						
						
					} catch (NumberFormatException | IOException e) {
						login_succesfull = false;
						running = false;
						clientlist = new String[] {};
					
						
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								startReconnecting();
								
							}
						},"reconnect_handler").start();
						
						reciveMsg_thread.interrupt();
						timer_thread.interrupt();			
						e.printStackTrace();
						
						break;
					}
				}
			}
			
		}, "client_reciveMessages");
		reciveMsg_thread.start();
	}
	
	private void startTimerThread() {
		timer_thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				int count = 0;
				while(running) {

					try {
						Thread.sleep(1000*5);
						count += 5;
						if(count % 5 == 0) {
							sendMessage("ping", new String[] {"Server"}, ""+System.currentTimeMillis());
						}
						
						if(count >= 100) {
							count -= 100;
						}
						
						
					} catch (InterruptedException e) {
						break;
					}
				}
				
				
			}
		},"timer_thread");
		timer_thread.start();
		
	}
	
	public long getPing() {
		return ping;
	}
	
	public String[] getClientNames() {
		return clientlist.clone();
	}
	
	public void setSocketListener(String channel, ClientListener sl) {
		socketlistener.put(channel, sl);
	}
	
	public boolean isReconnecting() {
		return reconnect;
	}
	public void setReconnecting(boolean b) {
		reconnect = b;
	}
	public String getClientName() {
		return name;
	}
	private Socket getClientSocket() {
		return client;
	}
	private boolean sendMessage(String msg) {
		
		try {
			PrintWriter printWriter = new PrintWriter(
					new OutputStreamWriter(
							client.getOutputStream()));

				printWriter.print(msg);
		 		printWriter.flush();
		 		
		 	return true;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean sendMessage(String channel, String target, String msg, SocketResponse response) {
		return sendMessage(channel, new String[] {target}, msg, response);
	}
	
	public boolean sendMessage(String channel, String target, String msg) {
		return sendMessage(channel, new String[] {target}, msg);
	}
	
	public boolean sendMessage(String channel, String[] targets, String msg) {
		return sendMessage(channel,targets,msg,null);
	}

	
	@SuppressWarnings("unchecked")
	public boolean sendMessage(String channel, String[] targets, String msg, SocketResponse response) {

		JSON json = new JSON();
		
		ArrayList<String> trace = new ArrayList<>();
		trace.add(getClientName());
		
		json.put("trace", trace);
		json.put("channel", channel);
		json.put("targets", Arrays.asList(targets));
		json.put("msg", msg);
		
		if(response != null) {
			UUID uuid = UUID.randomUUID();
			responselistener.put(uuid, response);
			
			json.put("response_id", uuid.toString());
		}
		
		
		String json_str = json.toJSON();
		String json_lng = ""+json_str.length();
		
		int l = json_lng.length();
		while(l < 8) {
			json_lng += " ";
			l++;
		}
		
		
		String completemsg = json_lng + json_str;
		
		
		if(!login_succesfull) {
			msgbuffer.add(completemsg);
			return false;
		}
		
		return sendMessage(completemsg);
	}
	
	public void broadcastMessage(String channel, String msg) {
		
		for(String clname : this.clientlist) {
			sendMessage(channel, new String[] {clname}, msg);
		}
	}
	
	public void broadcastMessage(String channel, String msg, SocketResponse response) {
		
		for(String clname : this.clientlist) {
			sendMessage(channel, new String[] {clname}, msg, response);
		}
	}
	
	private static String requiereMessage(BufferedReader reader, int length) throws IOException {
		char[] buffer = new char[length];
	 	int anzahlZeichen = reader.read(buffer, 0, length); // blockiert bis Nachricht empfangen
	 	String msg = new String(buffer, 0, anzahlZeichen);
	 	
	 	return msg;
	}
	
	private static SocketMessage readNextMessage(SimpleSocketClient cs) throws IOException,NumberFormatException {
		BufferedReader bufferedReader =  new BufferedReader(
											new InputStreamReader(
													cs.getClientSocket().getInputStream()));
		try {
			
		 	String length_msg = requiereMessage(bufferedReader, 8);
		 	int total_length = Integer.parseInt(length_msg.replaceAll(" ", ""));
		 	
		 	String json_msg = requiereMessage(bufferedReader, total_length);
		 	
		 	JSON json = null;
		 	
			try {
				json = JSON.parseJSON(json_msg);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			if(json == null) {
				return null;
			}
			
			
			ArrayList<String> trace = (ArrayList<String>) json.get("trace");
			trace.add(cs.getClientName());
			json.put("trace", trace);
			
		 	String channelid = (String) json.get("channel");
		 	List<String> targets = (List<String>) json.get("targets");
		 	String msg = (String) json.get("msg");
		 	
		 	//System.out.println("json: " + json_msg);
		 	
		 	if(json.containsKey("response_id")) {
		 		UUID uuid = UUID.fromString((String) json.get("response_id"));
		 		
		 		String send_source = trace.get(0);
		 		
		 		cs.sendMessage("response", send_source, ResponseStatus.TARGET_REACHED.getID()+"/"+uuid.toString());
		 	}
		 	
		 	
		 	return new SocketMessage(json,trace, channelid, targets, msg);
		}catch(SocketException ex) {
			System.out.println("["+SimpleSocketClient.NAME+"] Server disconnected.");
			cs.login_succesfull = false;
			cs.running = false;
			cs.clientlist = new String[] {};
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					cs.startReconnecting();
					
				}
			}).start();
			
			cs.reciveMsg_thread.stop();
			cs.timer_thread.stop();
			
		}
	 	
	 	return null;
	
	}
	
}
