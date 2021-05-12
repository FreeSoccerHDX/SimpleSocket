This little Project includes a SocketServer and SocketClient:

Testing if it works:
  To run the Server(local) use:
  ```java -jar SimpleSocket -server```

  To run the Client(local) use:
  ```java -jar SimpleSocket -client <name>```

The Client will automaticly connect to the Server and start an automated Ping-Handler.
You can use this to send messages between other Clients and handle the response automaticly.

For development you need to create an SimpleSocketServer-Object:
```SimpleSocketServer server = new SimpleSocketServer(port);```
and to connect with a Client you need to create an SimpleSocketClient-Object:
```SimpleSocketClient client = new SimpleSocketClient(clientname,ip,port);```


Now you can just use the client.sendMessage(Channel, Target, Message) - Method to send a Message
Channel = Specific Channel for the reciver to listen
Target  = The Name of a Client (client.getClientNames() or 'Server' for the Server itself)
Message = The Message for the Client/Server

To listen for a Channel use: 
```client.setSocketListener(Channel, ClientListener);```


The Server can listen for a Channel too:
```server.setServerListener(Channel, ServerListener);```
