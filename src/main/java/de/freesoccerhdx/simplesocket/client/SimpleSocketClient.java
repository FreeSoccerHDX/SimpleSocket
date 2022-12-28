package de.freesoccerhdx.simplesocket.client;

import de.freesoccerhdx.simplesocket.Pair;
import de.freesoccerhdx.simplesocket.ResponseStatus;
import de.freesoccerhdx.simplesocket.SocketBase;
import de.freesoccerhdx.simplesocket.SocketMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.UUID;
import java.util.function.Consumer;


public class SimpleSocketClient extends SocketBase {

	public static boolean DEBUG = false;
	public static final String NAME = "SimpleSocketClient";
	private static Thread cmdthread = null;


	public static void main(String[] args) {
		String ip = "localhost";//"timcloud.ddns.net"; // localhost
		int port = 11111;
		try {
			String clientname = "TestClient";
			if(args.length > 0) {
				clientname = args[0];
			}
			SimpleSocketClient client = new SimpleSocketClient(clientname,ip,port);
			client.start();
			
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
											break;
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
						
						//while(true && client.running) {
							boolean erfolg = client.sendMessage("test response", "Tim", "einfach nur eine msg", new SocketResponse() {
								
								@Override
								public void response(SimpleSocketClient ssc, ResponseStatus status, String source_channel) {
									System.out.println(ssc.name+": " + status + " source_client: " + source_channel);							
								}
							});
							
							System.out.println(System.currentTimeMillis()+" >>>working="+erfolg);
							
						//}
							
							System.out.println("Test stopped!");
							
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

							StringBuilder nameBuilder = new StringBuilder();
							for(String s : client.getClientNames()) {
								nameBuilder.append(s);
								if(!client.getClientNames()[client.getClientNames().length-1].equals(s)) {
									nameBuilder.append(",");
								}
							}
							
							System.out.println("All connected Clients["+client.getClientNames().length+"]: \n >>> " + nameBuilder);
							
							
						}
						
					}
				}
				
			},"cmdthread");
			cmdthread.start();
			
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
 	}
	
	//private long ping = -1;
	
	private Socket client;
	private final String name;
	private boolean reconnect = true;
	private boolean running = true;
	private boolean login_succesfull = false;
	private final List<String> msgbuffer = new ArrayList<>();
	
	private final HashMap<String, ClientListener> socketlistener = new HashMap<>();
	private final HashMap<UUID,SocketResponse> responselistener = new HashMap<>();

	private PrintWriter printWriter = null;
	private BufferedReader bufferedReader = null;
	private Thread reciveMsg_thread;
	private Timer timer;
	private Thread reconnect_thread = null;

	private PingHandler pingHandler;
	
	private String[] clientlist = new String[0];
	
	private final String ip;
	private final int port;
	
	private boolean stopped = false;

	private Consumer<Boolean> onLogin = null;
	private Consumer<ClientStatusInfo> statusInfo = null;

	public SimpleSocketClient(String name, String ip, int port) {
		this(name,ip,port, null);
	}

	public SimpleSocketClient(String name, String ip, int port, Consumer<Boolean> onLogin) {
		this(name,ip,port, onLogin, null);
	}

	public SimpleSocketClient(String name, String ip, int port, Consumer<Boolean> onLogin, Consumer<ClientStatusInfo> statusInfo) {
		this.ip = ip;
		this.port = port;
		this.name = name;
		this.onLogin = onLogin;
		this.statusInfo = statusInfo;
		this.timer = new Timer("timed_clientsocket",true);
		this.pingHandler = new PingHandler(this);
	}

	public void setOnLogin(Consumer<Boolean> onLogin) {
		this.onLogin = onLogin;
	}

	public void setStatusInfo(Consumer<ClientStatusInfo> statusInfo) {
		this.statusInfo = statusInfo;
	}

	protected Timer getTimer() {
		return this.timer;
	}

	public boolean isLoginSuccesfull() {
		return login_succesfull;
	}

	public boolean isRunning() {
		return running;
	}

	@Override
	public void interrupt() {
		stopConnection();
		super.interrupt();
	}

	private void stopConnection() {
		if(stopped && !running) return;

		stopped = true;
		running = false;
		this.pingHandler.onStop();

		login_succesfull = false;
		
		if(client != null) {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(reconnect_thread != null) {
			reconnect_thread.interrupt();
		}
		
		if(reciveMsg_thread != null) {
			reciveMsg_thread.interrupt();
		}
	}

	@Override
	public void run() {
		try {
			connect();
		} catch (ConnectException e) {
			startReconnecting();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void connect() throws IOException {
		client = new Socket(ip,port);
		System.out.println("["+SimpleSocketClient.NAME+"] Client connected.");
		if(this.statusInfo != null){
			this.statusInfo.accept(ClientStatusInfo.CLIENT_CONNECTED);
		}
		printWriter = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
		bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
		running = true;
		startReciveMessages();

		//login_succesfull = true;
		//boolean b = sendMessage("login", new String[]{"Server"}, name);
		//login_succesfull = false;

		JSONObject json = new JSONObject();

		json.put("trace", Arrays.asList(getClientName()));
		json.put("channel", "login");
		json.put("targets", Arrays.asList("Server"));
		json.put("msg", this.name);

		String json_str = json.toString();
		String completemsg = SocketBase.createMessageLengthString(json_str);

		boolean b = super.sendMessage(this.printWriter, completemsg);

		if(b) {
			System.out.println("["+SimpleSocketClient.NAME+"] Login message was sended successful.");
			if(this.statusInfo != null){
				this.statusInfo.accept(ClientStatusInfo.LOGIN_MESSAGE_SENDED);
			}
		}else {
			if(this.statusInfo != null){
				this.statusInfo.accept(ClientStatusInfo.LOGIN_MESSAGE_SENDED_FAILED);
			}
			throw new ConnectException("["+SimpleSocketClient.NAME+"] Could not send login message to Server.");
		}
	}
	
	
	private void startReconnecting() {
		
		if(isReconnecting()) {
			//System.out.println("["+SimpleSocketClient.NAME+"] Client will try to reconnect...");
			if(this.statusInfo != null){
				this.statusInfo.accept(ClientStatusInfo.CLIENT_WILL_TRY_RECONNECT);
			}
			reconnect_thread = Thread.currentThread();

			
			while(!stopped) {
				try {
					Thread.sleep(1000*5);
					
					if(stopped) break;


					//System.out.println("["+SimpleSocketClient.NAME+"] Reconnecting...");
					if(this.statusInfo != null){
						this.statusInfo.accept(ClientStatusInfo.CLIENT_RECONNECTING);
					}

					connect();
					
					break;
					
				}catch(Exception ex) {
					if(stopped) break;
					if(ex instanceof ConnectException) {
						//System.out.println("["+SimpleSocketClient.NAME+"] Reconnect failed (" + ex.getMessage() + ")");
						if(this.statusInfo != null){
							this.statusInfo.accept(ClientStatusInfo.CLIENT_RECONNECTING_FAILED);
						}
					}else {
						ex.printStackTrace();
					}
				}
			}
			
		}else {
			//System.out.println("["+SimpleSocketClient.NAME+"] Client will not try to reconnect!");
			if(this.statusInfo != null){
				this.statusInfo.accept(ClientStatusInfo.CLIENT_WILL_NOT_RECONNECT_AND_STOP);
			}
			stop();
		}
	}
	
	
	private void startReciveMessages() {

		reciveMsg_thread = new Thread(new Runnable() {

			@Override
			public void run() {
				while(running) {
					try {
						Pair<SocketInfo, SocketMessage> pairInfo = readNextMessage();
						SocketInfo socketInfo = pairInfo.getFirst();
						SocketMessage sm = pairInfo.getSecond();

						if(socketInfo != SocketInfo.SUCCESS) {
							if(socketInfo != SocketInfo.COULD_NOT_PARSE_JSON) {
								System.err.println("Reading Messages will stop because of " + socketInfo);
								running = false;
								break;
							}else{
								System.err.println("Reading Messages had an error but will not stop: " + socketInfo);
							}
						}else {
							if (sm.channelid.equals("response")) {
								JSONObject json = (JSONObject) sm.json.get("msg");

								UUID uuid = UUID.fromString((String) json.get("id"));

								if (SimpleSocketClient.this.responselistener.containsKey(uuid)) {
									SocketResponse socketresponse = SimpleSocketClient.this.responselistener.get(uuid);
									JSONArray responseData = json.getJSONArray("reached");
									//List<String> responseData = (List<String>) json.get("reached");
									responseData.forEach(s -> {
										String[] data = ((String) s).split("#");
										String name = data[0];
										ResponseStatus status = ResponseStatus.values()[Integer.parseInt(data[1])];
										socketresponse.response(SimpleSocketClient.this, status, name);
									});
									SocketBase.createThread("removeResponseListener", true, () -> {
										try {
											Thread.sleep(1000 * 10);
											SimpleSocketClient.this.responselistener.remove(uuid);
										} catch (Exception ex) {
											ex.printStackTrace();
										}
									});
								} else {
									System.err.println(" > UUID for Response-Handling was not found.");
									System.err.println(" >>> Was it to slow ?");
								}

							} else if (sm.channelid.equals("clientlist")) {

								if (sm.message.split(",").length > 0) {
									clientlist = sm.message.split(",");
								} else {
									clientlist = new String[]{sm.message};
								}

							} else if (sm.channelid.equals("kick") || sm.channelid.equals("stop")) {
								String reasonMsg = sm.message;
								if (sm.channelid.equals("kick")) {
									System.out.println("[" + SimpleSocketClient.NAME + "] Client kicked.\n > " + reasonMsg);
								} else {
									System.out.println("[" + SimpleSocketClient.NAME + "] Server Stopped.\n > " + reasonMsg);
								}
								login_succesfull = false;
								running = false;
								clientlist = new String[0];

								// Needs to be in new thread since this one get interrupted after that.
								SocketBase.createThread("reconnect_handler", false, () -> {
										startReconnecting();
								});

								if(reciveMsg_thread != null) {
									reciveMsg_thread.interrupt();
								}
								break;
							} else if (sm.channelid.equals("login")) {
								if (sm.message.equals("true")) {
									login_succesfull = true;
									System.out.println("[" + SimpleSocketClient.NAME + "] Login was successful");
									if(SimpleSocketClient.this.onLogin != null){
										SocketBase.createThread("onLogin", true, () -> {
											SimpleSocketClient.this.onLogin.accept(true);
										});
									}
									SimpleSocketClient.this.pingHandler.sendPingChannelMessage();

									for (String s : msgbuffer) {

										sendMessage(printWriter, s);
									}
									msgbuffer.clear();
								} else {
									// TODO: Login was not successful
									System.out.println("[" + SimpleSocketClient.NAME + "] Login was not successful");
									if(SimpleSocketClient.this.onLogin != null){
										SocketBase.createThread("onLogin", true, () -> {
											SimpleSocketClient.this.onLogin.accept(false);
										});
									}
								}

							} else if (socketlistener.containsKey(sm.channelid)) {
								try {
									socketlistener.get(sm.channelid).recive(SimpleSocketClient.this, sm.channelid, sm.trace.get(0), sm.message);
								} catch (Exception exception) {
									System.err.println("Exception while SimpleSocketClient#recive. Not canceling reading new messages.");
									exception.printStackTrace();
								}
							}
							if (DEBUG) {
								System.out.println("ServerMessage: " + sm);
							}
						}

					} catch (Exception e) {
						SimpleSocketClient.this.stop();
						if(e instanceof SocketException){
							// TODO: Stopped by server/client ?
						}else{
							e.printStackTrace();
						}

						
						break;
					}
				}
			}
			
		}, "client_reciveMessages");
		reciveMsg_thread.setDaemon(true);
		reciveMsg_thread.start();
	}
	
	public long getPing() {
		return this.pingHandler.getPing();
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
	
	public boolean sendMessage(String channel, String target, String msg, SocketResponse response) {
		return sendMessage(channel, new String[] {target}, msg, response);
	}
	
	public boolean sendMessage(String channel, String target, String msg) {
		return sendMessage(channel, new String[] {target}, msg);
	}
	
	public boolean sendMessage(String channel, String[] targets, String msg) {
		return sendMessage(channel,targets,msg,null);
	}

	public boolean sendMessage(String channel, String[] targets, JSONObject jsonMsg) {
		return sendMessage0(channel,targets,jsonMsg,null);
	}
	public boolean sendMessage(String channel, String target, JSONObject jsonMsg) {
		return sendMessage0(channel,new String[]{target},jsonMsg,null);
	}
	public boolean sendMessage(String channel, String targets, JSONObject jsonMsg, SocketResponse response) {
		return sendMessage0(channel,new String[]{targets},jsonMsg,response);
	}
	public boolean sendMessage(String channel, String[] targets, JSONObject jsonMsg, SocketResponse response) {
		return sendMessage0(channel,targets,jsonMsg,response);
	}

	public boolean sendMessage(String channel, String[] targets, String msg, SocketResponse response) {
		return sendMessage0(channel,targets,msg,response);
	}

	// Object msg can be String or JSONObject
	private boolean sendMessage0(String channel, String[] targets, Object msg, SocketResponse response) {
		JSONObject json = new JSONObject();
		
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
		
		
		String json_str = json.toString();
		String completemsg = SocketBase.createMessageLengthString(json_str);
		
		
		if(!this.login_succesfull) {
			//System.err.println("[SimpleSocketClient] Login not successful until now. Adding message to buffer.");
			this.msgbuffer.add(completemsg);
			return false;
		}
		//System.out.println("sendmsg completemsg of '" + completemsg + "'");
		try{
			//System.out.println("abc start");
			return super.sendMessage(this.printWriter, completemsg);
		}catch (Exception exception){
			//System.out.println("abc error");
			exception.printStackTrace();;
		}
		//System.out.println("abc end");

		return false;
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
	
	private Pair<SocketInfo,SocketMessage> readNextMessage() throws Exception {
		//try{
		Pair<SocketInfo, JSONObject> s = super.readNextMessage(this.bufferedReader);
		SocketInfo info = s.getFirst();
		JSONObject json = s.getSecond();

		if(info != SocketInfo.SUCCESS){
			return Pair.of(info,null);
		}

		JSONArray trace = json.getJSONArray("trace");
		trace.put(getClientName());
		json.put("trace", trace);

		String channelid = (String) json.get("channel");
		JSONArray targets = json.getJSONArray("targets");
		//List<String> targets = (List<String>) json.get("targets");

		String msg = null;
		Object responseData = json.get("msg");
		if (responseData instanceof JSONObject) {
			msg = json.get("msg").toString();
		} else {
			msg = (String) json.get("msg");
		}

		//System.out.println("json: " + json_msg);

		if (json.has("response_id")) {
			UUID uuid = UUID.fromString((String) json.get("response_id"));

			String send_source = trace.getString(0);

			sendMessage("response", send_source, ResponseStatus.TARGET_REACHED.getID() + "/" + uuid.toString());
		}


		return Pair.of(SocketInfo.SUCCESS, new SocketMessage(json, trace, channelid, targets, msg));
	}
	
}
