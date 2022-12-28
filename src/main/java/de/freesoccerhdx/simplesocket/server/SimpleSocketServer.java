package de.freesoccerhdx.simplesocket.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import de.freesoccerhdx.simplesocket.ResponseStatus;
import de.freesoccerhdx.simplesocket.SocketBase;
import org.json.JSONObject;


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
	
	private HashMap<String, ServerClientSocket> clients = new HashMap<>();
	
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
	
	
	
	public SimpleSocketServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		// start Client Registration
		handleClientRegistration();
		
		// start Client Timer Messages
		startTimerMessages();

		Runtime.getRuntime().addShutdownHook(SocketBase.createThreadUnstarted("ShutdownHook", false, () -> {
			try {
				Thread.sleep(200);
				if(running) {
					SimpleSocketServer.this.stop("The Server is shutting down.");
				}
			} catch (Exception e) {
				Thread.currentThread().interrupt();
				e.printStackTrace();
			}
		}));
	}

	public boolean isRunning(){
		return this.running;
	}
	
	private void startTimerMessages() {
		timer_thread = SocketBase.createThread("timer_thread", true, () -> {
			while(running) {
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
		});
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
	
	
	protected void addClient(String clientname, ServerClientSocket client) {
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
		String clientnames = String.join(",", clients.keySet());
		/*
		for(String s : clients.keySet()) {
			clientnames += s + ",";
		}
		if(clientnames.endsWith(",")) {
			clientnames = clientnames.substring(0, clientnames.length()-1);
		}
		*/
		broadcastMessage("clientlist", clientnames);
	}
	
	public void stop(String stopmsg) {
		
		running = false;
		
		// broadcast Server-Close
		broadcastMessage("stop", stopmsg);

		// stopping Client input-message-reader etc.
		for(ServerClientSocket cs : clients.values()) {
			cs.stop();
		}

		// stopping Server socket
		try {
			serverSocket.close();
		}catch(Exception ex) {
			ex.printStackTrace();
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

		/*
		Set<Thread> threads = Thread.getAllStackTraces().keySet();
		for(Thread t : threads) {
			if (t.isAlive()) {
				System.out.println("[info] Thread still running: " + t.getName());
			}
		}*/

		System.exit(0);
		
	}
	
	
	
	private void handleClientRegistration() {
		this.client_thread = SocketBase.createThread("ClientThread", true, new Runnable() {
			@Override
			public void run() {
				System.out.println("["+NAME+"] Client-Registration started!\n");
				try {
					while(running) {
						try {
							Socket client = serverSocket.accept();
							if(SimpleSocketServer.this.running) {
								handleNameLogin(SimpleSocketServer.this, client);
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
						ServerClientSocket.handleConnection(SimpleSocketServer.this,client);
					}

				},"handleConnection-"+client.getInetAddress().toString()).start();
			}
		});
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
		}
	}

	public void setServerListener(String channel, ServerListener sl) {
		if(sl != null) {
			serverlisteners.put(channel, sl);
		}
	}
	
	protected boolean handleCustom(ServerClientSocket cs, String channelid, List<String> targets, String message) {
		
		if(serverlisteners.containsKey(channelid)) {
			try{
				serverlisteners.get(channelid).recive(this, cs, channelid, message);
			}catch (Exception exception){
				System.err.println("Got Error while handling Client-Message:");
				exception.printStackTrace();
			}

			
			return true;
		}
		
		return false;
	}



	protected boolean transferMessage(String target, JSONObject json) {
		
		if(clients.containsKey(target)) {
			String json_msg = json.toString();
			ServerClientSocket clientSocket = clients.get(target);

			try {
				clientSocket.sendMessage(clientSocket.getPrintWriter(), SocketBase.createMessageLengthString(json_msg) + json_msg);
				return true;
			}catch (Exception exception){
				exception.printStackTrace();
			}

		}
		return false;
	}

}
