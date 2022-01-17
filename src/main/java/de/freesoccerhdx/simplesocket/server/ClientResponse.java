package de.freesoccerhdx.simplesocket.server;

import de.freesoccerhdx.simplesocket.ResponseStatus;

public abstract class ClientResponse {

	public abstract void response(SimpleSocketServer sss, ResponseStatus status, String source_client);
	
}
