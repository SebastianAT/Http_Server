import java.net.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.io.*;

public class ServerThread implements Runnable {

	public static final String headerServer = "Server: ";
	public static final String headerContentLength = "Content-Length: ";
	public static final String headerContentLang = "Content-Language: de-DE";
	public static final String headerConnection = "Connection: ";
	public static final String headerContentType = "Content-Type: ";
	public static final String DEFAULT_FILE = "index.html";
	public static final String FILE_NOT_FOUND = "404.html";
	public static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	public static final File WEB_ROOT = new File("documentRoot/wwwroot/");

	Socket ClientSocket;
	BufferedReader is;
	BufferedWriter out;
	PrintWriter outPrint;
	OutputStream os;
	ServerFiles serverFiles;
	DataOutputStream binaryOut;
	String reqFile;
	
	Date d = new Date();
	SimpleDateFormat df = new SimpleDateFormat("MM/dd/YYYY HH:mm:ss");
	String dateFormat = df.format(d);
	
	public ServerThread(Socket ConnectionSocket)
	{
		ClientSocket = ConnectionSocket;
		
		try {
			is = new BufferedReader(new InputStreamReader(ConnectionSocket.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(ConnectionSocket.getOutputStream()));
			binaryOut = new DataOutputStream(ClientSocket.getOutputStream());
			outPrint = new PrintWriter(ClientSocket.getOutputStream());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run() {
		try {
			boolean alive = true;
			while (alive) {
				alive = false;
				String input = is.readLine();
				StringTokenizer token = new StringTokenizer(input);
				String method = token.nextToken().toUpperCase(); // we get the HTTP method of the client
				reqFile = token.nextToken().toLowerCase(); // we get the file which requested
				
				System.out.println(dateFormat + " " + method + " " + reqFile + " " + ClientSocket.getInetAddress() + " " + ClientSocket.getPort());
				if(!method.equals("GET") && !method.equals("POST")) {
					System.out.println("501 Not Implemented : " + method + " method.");
					
					// return not supported file
					File fileNew = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
					int fileNewLength = (int) fileNew.length();
					byte[] fileData = getFileData(fileNew, fileNewLength);
					
					//sent HTTP headers
					sendFailResponse("HTTP/1.1 501 Not Implemented", fileData, fileNewLength, "text/html", "Close");		
				} else {
					// GET or HEAD method
					if (reqFile.endsWith("/")) {
						System.out.println(reqFile);
						reqFile += DEFAULT_FILE;
					}
					
					File fileNew = new File(WEB_ROOT, reqFile);
					int fileNewLength = (int) fileNew.length();
					String content = getContentType(reqFile);
					if (method.equals("GET")) { // GET method so we return content
						byte[] fileData = getFileData(fileNew, fileNewLength);
						
						//sent HTTP headers
						sendOKResponse("HTTP/1.1 200 OK", fileData, fileNewLength, content, "Close");
						
					} else if (method.equals("POST")) { // POST method so we return content
						
						String headerLines = null;
							while((headerLines = is.readLine()).length() != 0) {
								//System.out.println(headerLines);
							}
						
						StringBuilder sb = new StringBuilder();
							while(is.ready()){
								sb.append((char) is.read());
							}
						
						String postContent = sb.toString();
						
						//System.out.println("Data is: " + postContent);
						
						String outputresponse = getPostResponse(postContent);
						
						sendOKResponse("HTTP/1.1 200 OK", (outputresponse).getBytes(), outputresponse.length(), content, "Keep-Alive");
	
						System.out.println("File " + reqFile + " of type " + content + " returned");
						alive = true;
					}
				}
				
				if(alive == false) {
			        out.close(); 
			        is.close();	
			        outPrint.close();
					ClientSocket.close();
					System.out.println("Connection closed.\r\n");
				} else {
					System.out.println("Connection keep-alive.\r\n");
				}
		}

		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound("HTTP/1.1 404 Not Found", outPrint, binaryOut, reqFile, "Close");
			} catch (IOException e) {
				System.err.println("Error file not found exception : " + e.getMessage());
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	private void sendOKResponse(String httpMessage, byte[] fileData, int fileNewLength, String content, String connectionType ) throws IOException {
		getHeaderInfos(httpMessage, fileNewLength, fileData, content, connectionType);
	}

	private void sendFailResponse(String httpMessage, byte[] fileData, int fileLength, String contentMimeType, String connectionType) throws IOException {
		getHeaderInfos(httpMessage, fileLength, fileData, contentMimeType, connectionType);
	}
	
	private void getHeaderInfos(String httpMessage, int fileLength, byte[] fileData, String content, String connectionType) throws IOException {
		// send HTTP Headers        You can use println instead of write so you dont have to write CRLF
		outPrint.println(httpMessage);
		outPrint.println(headerServer + "Java HTTP Server: 1.0");
		outPrint.println("Date: " + new Date());
		outPrint.println(headerContentType + content);
		outPrint.println(headerContentLength + fileLength);
		outPrint.println(headerConnection + connectionType);
		outPrint.println(headerContentLang);
		outPrint.println(); // blank line between headers and content, very important !
		outPrint.flush(); // flush character output stream buffer
		
		binaryOut.write(fileData, 0, fileLength);
		binaryOut.flush();
	}
	
	private void fileNotFound(String httpMessage, PrintWriter out, OutputStream dataOut, String reqFile, String connectionType) throws IOException {
		File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = getFileData(file, fileLength);

		getHeaderInfos(httpMessage, fileLength, fileData,  content, connectionType);
		
		System.out.println("File " + reqFile + " not found");
	}
	
	// return supported MIME Types
	private String getContentType(String reqFile) {
		if (reqFile.endsWith(".htm")  ||  reqFile.endsWith(".html"))
			return "text/html";
		else
			return "image/png";
	}
	
	private byte[] getFileData(File file, int fileLength) throws IOException {
		FileInputStream fileInput = null;
		byte[] fileData = new byte[fileLength];

		try {
			fileInput = new FileInputStream(file);
			fileInput.read(fileData);
		} finally {
			if (fileInput != null) 
				fileInput.close();
		}
		return fileData;
	}	
	
	private String getPostResponse(String postContent) {
		int counter = 0;
		
		//transform to char array to get the number of "=", thats the number of sended fields 
		char[] tempArray = postContent.toCharArray();
		char c = '=';
		for(int i = 0; i < postContent.length()-1; i++) {
			if(tempArray[i] == c) {
				counter++;
			}
		}
		
		//System.out.println("anzahl Felder = " + counter);
		
		// split array content with "&" and then split second array with "=" to get individual values
		//(firstname -> value, lastname -> value ...etc)
		List<String> tempAr = new ArrayList<String>();
		
		if(counter == 1) {
			String[] test = postContent.split("&", counter);
			for(int i = 0; i < test.length; i++) {
				String[] temp = test[i].split("=");
				for(int j = 0; j < temp.length; j++) {
					//System.out.println(temp[j]);
					
					tempAr.add(temp[j]);
				}
			}
			
			String sub = tempAr.get(1);
			String subNew = sub.substring(0, sub.indexOf("&"));
			tempAr.remove(1);
			tempAr.add(subNew);
			
		} else {

			String[] test = postContent.split("&", counter);
			for(int i = 0; i < test.length; i++) {
				String[] temp = test[i].split("=");
				for(int j = 0; j < temp.length; j++) {
					//System.out.println(temp[j]);
					
					tempAr.add(temp[j]);
				}
			}
			}
			//print individual values from arrayList
			for(int j = 0; j < tempAr.size(); j++) {
				//System.out.println(tempAr.get(j));
			}
			
			//specify counter and loop through Nr. of Fields and build dynamically the response String 
			int counterFields = 0;
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("<html><body>");
			
			try {
				
				for(int i = 0; i < counter; i++) {
					stringBuilder.append("<p> Received form variable with name [" + tempAr.get(i+counterFields) + "] and value [" + tempAr.get(i+counterFields+1) + "]</p>");
					counterFields++;
				}
				
			} catch (IndexOutOfBoundsException e) {
				System.err.println("Error field empty : " + e.getMessage());
			}
			
			stringBuilder.append("</body></html>");
			
			//final String response
			String outputresponse = stringBuilder.toString();
			//System.out.println(outputresponse);
			
			return outputresponse;
	}
}
	 

