import java.io.IOException;
import java.net.*;

public class Server {

	public static final int SERVER_PORT		= 8071
			;
	public static final String SERVER_STRING = "My Little Pony HTTP Server";
	public static final String SERVER_HTTP_VERSION = "0.9";
	public static final String CRLF = "\r\n";

	public static void main(String [] args)
	{
		try {
		    			
			ServerSocket srvSocket = new ServerSocket(Server.SERVER_PORT);
			System.out.println("HTTP server running at port: " + Server.SERVER_PORT);
			while(true){
				Socket ConnectionSocket = srvSocket.accept();
				System.out.println("Server got new request from client with IP: " + ConnectionSocket.getInetAddress()+ " and port: " + ConnectionSocket.getPort());
				if(ConnectionSocket != null) new Thread(new ServerThread(ConnectionSocket)).start();   // new ServerThread(ConnectionSocket);
				
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
