package de.freesoccerhdx.simplesocketserver.haupt;

import java.io.BufferedReader;


import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor.Requires;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;


import de.freesoccerhdx.simplesocket.haupt.JSON;
import de.freesoccerhdx.simplesocketclient.haupt.ResponseStatus;


public class SimpleSocketServer {
	
	protected static final boolean DEBUG = false;
	public static final String NAME = "SimpleSocketServer";	
	private static final int port = 11111;
	
	
	private boolean running = true;
	private ServerSocket serverSocket;
	
	private HashMap<String, ServerListener> serverlisteners = new HashMap<>();
	
	
	private Thread client_thread = null;
	private Thread timer_thread = null;
	private Thread cmd_thread = null;
	
	private HashMap<String,ClientSocket> clients = new HashMap<>();
	
	public static void main(String[] args) {
		try {
			SimpleSocketServer server = new SimpleSocketServer(port);
			
			server.handleConsoleInput();
			
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		/*
		for(Thread thread : Thread.getAllStackTraces().keySet()) {
			System.out.println("Thread running: " + thread.getName());
		}*/
	}
	
	
	
	public SimpleSocketServer(int port) throws IOException{
		serverSocket = new ServerSocket(port);
		

		// start Client Registration
		handleClientRegistration();
		
		// start Client Timer Messages
		startTimerMessages();

	
		Runtime.getRuntime().addShutdownHook(new Thread() {
	        public void run() {
	            try {
	                Thread.sleep(200);
	          //      System.out.println("["+NAME+"] Shutting down ...");
	                try {
	                	if(running) {
	                		SimpleSocketServer.this.stop("The Server is shutting down.");
	                	}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

	            } catch (InterruptedException e) {
	                Thread.currentThread().interrupt();
	                e.printStackTrace();
	            }
	        }
	    });
	}
	
	
	private void startTimerMessages() {
		
		
		timer_thread = new Thread(new Runnable() {

			@Override
			public void run() {
				
				int count = 0;
				
				while(running) {
					try {
						Thread.sleep(1000);
						if(!running) break;
						count ++;
						
						if(count % 60 == 0) {
							updateClientList();
						}
						
						if(count % 100 == 0) {
							count = 0;
						}
						
						
					}catch(Exception ex) {
						if(ex instanceof InterruptedException) {
							break;
						}else {
							ex.printStackTrace();
						}
					}
				}
				
			}
		}, "timer_thread");
		
		timer_thread.start();
	}

	public Set<String> getClientsNametList() {
		return clients.keySet();
	}
	
	private void handleConsoleInput() throws IOException {
		
		cmd_thread = new Thread(new Runnable() {

			@Override
			public void run() {
				System.out.println("["+NAME+"] Console-Handle started!\n");
				System.out.println("Type Message here... \nUse 'help' or '?' to get some help!");
				while(running){
					String name = System.console().readLine();
				//	System.out.println("Input: "+name);
					
					if(name == null) name = "";
					
					if(name.equalsIgnoreCase("help") || name.equalsIgnoreCase("?")) {
						System.out.println("\nCommands");
						System.out.println("- help/?			- Shows this List");
						System.out.println("- stop				- Stops the Server");
						System.out.println("- list				- Lists all Clientnames");
						System.out.println("- ping <name/all>		- Pings a Client with specific name or all");
						System.out.println("\n");
					}else if(name.equalsIgnoreCase("stop")) {
						System.out.println("["+NAME+"] Stopping the Server...");
						stop("The Server was stopped.");
						break;
						
					}else if(name.toLowerCase().startsWith("test response")) {	
						
						System.out.println("test response!!!!");
						
						broadcastMessage("testchannel", "mycustommsg", new ClientResponse() {
							
							@Override
							public void response(SimpleSocketServer sss, ResponseStatus status, String source_client) {
								System.out.println("ClientResponse > " + status + " " + source_client);
								
							}
						});
						
					}else if(name.toLowerCase().startsWith("ping")) {	
						
						
						if(name.replace("ping ", "").length() > 0) {
							String argu = name.replace("ping ", "");
							
							if(argu.equalsIgnoreCase("all")) {
								broadcastMessage("ping", ""+System.currentTimeMillis());
								System.out.println("["+NAME+"] Ping was sended to " + clients.size() + " Clients!");
							}else {
								
								boolean b = sendNewMessage(argu, "ping", ""+System.currentTimeMillis(), null);
								if(b) {
									System.out.println("["+NAME+"] Ping was sended to '"+argu+"'");
								}else {
									System.out.println("["+NAME+"] The Client '"+argu+"' was not found!");
								}
							}
						}else {
							System.out.println("- ping <name/all>	- Pings a Client with specific name or all");
						}
						
						
					}else if(name.equalsIgnoreCase("list")) {
						
						String clnames = "";
						for(String clname : clients.keySet()) {
							clnames += clname+",";
						}
						if(clnames.endsWith(",")) {
							clnames = clnames.substring(0, clnames.length()-1);
						}
						
						System.out.println("All connected Clients["+clients.size()+"]:" + (clients.size() > 0 ?  ("\n >>> "+clnames) : ""));
						
					}else {
						System.out.println("Unknown Command. Type 'help' or '?' to get some help!");
					}
				}
			}
			
		},"cmd_thread");
		cmd_thread.start();
		
		
	}
	
	
	protected void addClient(String clientname, ClientSocket client) {
		if(clients.containsKey(clientname) || clientname.equals("Server")) {
			System.out.println("["+NAME+"] Client with duplicated Name tried to join. ("+clientname+")");
			client.sendNewMessage("login","false",null);
			try {
				client.getSocket().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else {
			System.out.println("["+NAME+"] New Connection: \n	Name: "+clientname+"\n	Ip: "+client.getSocket().getInetAddress().getCanonicalHostName());
			clients.put(clientname, client);
			client.sendNewMessage("login", "true",null);
			
			
			updateClientList();
			
			
			client.startReadingMessages();
			
		}
	}
	
	// Sends a List with all Connected clients
	public void updateClientList() {
		String clientnames = "";
		for(String s : clients.keySet()) {
			clientnames += s + ",";
		}
		if(clientnames.endsWith(",")) {
			clientnames = clientnames.substring(0, clientnames.length()-1);
		}
		
		broadcastMessage("clientlist", clientnames);
	}
	
	public void stop(String stopmsg) {
		
		running = false;
		
		// broadcast Server-Close
		broadcastMessage("stop", stopmsg);
		
		
		
		// stopping Server socket
		try {
			serverSocket.close();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		
		/*
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(1000);
			//		System.exit(0);
					for(Thread thread : Thread.getAllStackTraces().keySet()) {
						System.out.println("Thread running: " + thread.getName());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
			
		},"Stop Thread").start();
		*/
		
		
		// stopping Client input-message-reader etc.
		for(ClientSocket cs : clients.values()) {
			cs.stop();
		}
		
		if(this.client_thread != null) {
			this.client_thread.interrupt();
			
			
		}
		if(this.timer_thread != null) {
			this.timer_thread.interrupt();
			
		}
		
		if(this.cmd_thread != null) {
			this.cmd_thread.interrupt();
			
		}
		
		
		
		
		
		
		
		
		
		
	}
	
	
	
	private void handleClientRegistration(){
		
		
		this.client_thread = new Thread(new Runnable() {
			
			
			@Override
			public void run() {
				System.out.println("["+NAME+"] Client-Registration started!\n");
				try {
					while(running) {
						try {
							Socket client = serverSocket.accept();
							if(SimpleSocketServer.this.running) {
								handleNameLogin(SimpleSocketServer.this,client);
							}
							
						}catch(SocketException se) {
							System.out.println("["+NAME+"] Client-Registration stopped!");
							break;
						}
						
					}

				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			private void handleNameLogin(SimpleSocketServer server, Socket client) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						ClientSocket.handleConnection(SimpleSocketServer.this,client);
					}
					
				},"handleConnection-"+client.getInetAddress().toString()).start();
			}
			
		}, "client_thread");
		this.client_thread.start();

	}
	
	
	public void broadcastMessage(String channel, String msg){
		broadcastMessage(channel, msg, null);
	}
	public void broadcastMessage(String channel, String msg, ClientResponse response){
		if(DEBUG) {
			System.out.println("Broadcast: " + channel + " -> " + msg);
		}
		for(String clientname : clients.keySet()) {
			clients.get(clientname).sendNewMessage(channel, msg, response);
		}
	}
	
	public boolean sendNewMessage(String targetclientname, String channel, String msg) {
		return sendNewMessage(targetclientname, channel, msg, null);
	}
	
	public boolean sendNewMessage(String targetclientname, String channel, String msg, ClientResponse response) {
		for(String clientname : clients.keySet()) {
			if(clientname.equals(targetclientname)) {
				return clients.get(clientname).sendNewMessage(channel, msg, response);
			}
		}
		return false;
	}
	
	protected void removeClient(String clientName) {
		if(clients.containsKey(clientName)) {
			clients.remove(clientName);
			updateClientList();
		}else {
			// TODO: Handle Client removed without beeing there
		}
	}
	
	
	public void setServerListener(String channel, ServerListener sl) {
		if(sl != null) {
			serverlisteners.put(channel, sl);
		}
	}
	
	protected boolean handleCustom(ClientSocket cs, String channelid, List<String> targets, String message) {
		
		if(serverlisteners.containsKey(channelid)) {
			
			serverlisteners.get(channelid).recive(this, cs, channelid, message);
			
			return true;
		}
		
		return false;
	}
	
	
	protected String createMessageLengthString(String msg) {
		String msg_lng = ""+msg.length();
		
		int l = msg_lng.length();
		while(l < 8) {
			msg_lng += " ";
			l++;
		}
		
		
		return msg_lng;
	}


	protected boolean transferMessage(String target, JSON json) {
		
		if(clients.containsKey(target)) {
			String json_msg = json.toJSON();
			clients.get(target).sendMessage(createMessageLengthString(json_msg)+json_msg);
			return true;
		}
		return false;
	}

}
