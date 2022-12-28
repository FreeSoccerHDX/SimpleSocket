package de.freesoccerhdx.simplesocket.server;

public abstract class ServerListener {

	public abstract void recive(SimpleSocketServer sss, ServerClientSocket cs, String channel, String message);
	
}
