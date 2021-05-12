This little Project includes a SocketServer and SocketClient:

Testing if it works:
  To run the Server(local) use: <br>
  ```java -jar SimpleSocket -server```

  To run the Client(local) use: <br>
  ```java -jar SimpleSocket -client <name>```

The Client will automaticly connect to the Server and start an automated Ping-Handler.
You can use this to send messages between other Clients and handle the response automaticly.

For development you need to create an SimpleSocketServer-Object: <br>
```SimpleSocketServer server = new SimpleSocketServer(port);``` <br>
and to connect with a Client you need to create an SimpleSocketClient-Object: <br>
```SimpleSocketClient client = new SimpleSocketClient(clientname,ip,port);```


Now you can just use the client.sendMessage(Channel, Target, Message) - Method to send a Message <br>
Channel = Specific Channel for the reciver to listen <br>
Target  = The Name of a Client (client.getClientNames() or 'Server' for the Server itself) <br>
Message = The Message for the Client/Server <br>

To listen for a Channel use: <br>
```client.setSocketListener(Channel, ClientListener);```


The Server can listen for a Channel too: <br>
```server.setServerListener(Channel, ServerListener);```
