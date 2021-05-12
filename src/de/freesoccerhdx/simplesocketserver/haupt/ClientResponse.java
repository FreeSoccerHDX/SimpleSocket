package de.freesoccerhdx.simplesocketserver.haupt;

import de.freesoccerhdx.simplesocketclient.haupt.ResponseStatus;

public abstract class ClientResponse {

	public abstract void response(SimpleSocketServer sss, ResponseStatus status, String source_client);
	
}
