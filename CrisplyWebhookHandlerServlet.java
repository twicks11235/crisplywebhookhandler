/**
* CrisplyWebhookHandlerServlet.java
*
* @author   Tim Wicks
*
* This program is designed to handle incoming POST requests from the Wufoo.com
* webhook system, check their validity, and submit activity posts to the author's
* Crisply.com account using the Crisply API. It is written in accordance with the
* Crisply programming challenge document.
*
*/

package CrisplyWebhookHandler;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.net.*;
import java.util.Enumeration;
//I didn't especially want to write my own Base 64 encoder, so I'm using the Apache Commons Codec one
import org.apache.commons.codec.binary.Base64;

public class CrisplyWebhookHandlerServlet extends HttpServlet{

//This key was randomly generated and is included in all POSTS from Tim Wicks's Wufoo account
private static String handshakeKey = "casduogh2495n3rg803";
//API key provided by Crisply for basic HTTP authorization
private static String CrisplyAPIKey = "ViPjDUWe5GkIPtQ5uKTG";
//URL for activity items xml page for Tim Wicks's Crisply account 
private static String CrisplyURL = "http://twix.crisply.com/timesheet/api/activity_items.xml";

protected void doPost(HttpServletRequest req, HttpServletResponse resp){
try{

//Check incoming POST info to confirm sender

/**  Note: This is a handshake check to confirm that the post is coming from my account (or any account
*    to whom I've distributed the key), but we could easily generalize this to any incoming Wufoo POST
*    by replacing the handshake check with a sender check to just confirm the source.
*/

String postKey = req.getParameter("HandshakeKey");
String timestamp = req.getParameter("DateCreated");
if( !postKey.equals( handshakeKey ) ){
throw new ServletException( "HandshakeKey included in POST request (" + postKey + ") was blank or did not match known Handshake Key");
}
else{
//Open and set up connection
    URL url = new URL(CrisplyURL);
    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
	conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");
    conn.setDoOutput(true);

	String userpass = CrisplyAPIKey + ": ";
    String authorization = "Basic " + new String(new Base64().encode((CrisplyAPIKey + ": ").getBytes()));
    conn.setRequestProperty ("Authorization", authorization);
	
	// Generate the data to be sent to Crisply
	
	/**  Note: While this servlet is only processing data with a known structure from one web site,
	*    in general instead of hardcoding the HTML/XML data, we would want to use a more generalized
	*    approach, probably XSD docs and a Marshaller
	*/
	
	String data = "<activity-item xmlns=\"http://crisply.com/api/v1\">"
	+ "<guid>wufoo-activity-" + timestamp + "</guid>"
	+ "<text>Timestamp: " + timestamp;
	Enumeration< String > content = req.getParameterNames();
	while( content.hasMoreElements() ){
	String header = content.nextElement();
	data = data + "\n" + header + ": " + req.getParameter( header );
	}
	data = data + "</text>" + "</activity-item>";
	
	conn.setRequestProperty("Content-Length", "" + Integer.toString(data.getBytes().length));

    DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
    outputStream.writeBytes(data);
    outputStream.flush();
	outputStream.close();
	
	//Get response from Crisply and make sure everything worked
	
	int status = conn.getResponseCode();
	
	if( status < 200 || status >= 300 ){
	System.err.println( "Something went wrong: Error code " + status );
	}
	
	//For debugging
	/*
	BufferedReader info = new BufferedReader( new InputStreamReader(conn.getInputStream() ) );
	while( info.ready() ){
	System.err.println( info.readLine());
	}*/
	}
}
catch( Exception e ){
ServletContext sc = getServletContext();
sc.log( "Something went wrong: ", e );
}
}

/** Note: This method is just here so that visiting the URL where this program is hosted
 *  (http://crisplywebhookhandler.appspot.com/CrisplyWebhookHandler) doesn't display
 *  an 'HTML GET not supported' page
 */
 
protected void doGet(HttpServletRequest req, HttpServletResponse resp){
try{
resp.getWriter().println( "Welcome the the Crisply Webhook Handler! This program automatically handles incoming HTTP POST requests and doesn't do anything else, so this page isn't very interesting." );
}
catch( Exception e ){
ServletContext sc = getServletContext();
sc.log( "Something went wrong: ", e );
}
}
}