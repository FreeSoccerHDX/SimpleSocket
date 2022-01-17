package de.freesoccerhdx.simplesocket.server;

public abstract class ServerListener {

	public abstract void recive(SimpleSocketServer sss, ClientSocket cs, String channel, String message);
	
}
