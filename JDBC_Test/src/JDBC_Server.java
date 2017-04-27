/**
 * Created by Joshua on 12/1/2016.
 * And also taken by some code written by the TAs (i.e. Jeremy) in CSC 335
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.exit;

public class JDBC_Server {

    public static final int SERVER_PORT = 9001;

    private static ServerSocket sock;
    private static List<ObjectOutputStream> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {

        //Initialize the database connection
        //The basic idea is along the lines of
        //                    use jdbc thin driver to connect to user hr @ localhost through port 1521, and it's an eXpress Edition of Oracle Database
        final String oracleURL = "jdbc:oracle:thin:hr/hr@localhost:1521/XE";
        //The account used for development is "josh";"OracleDB"
        //Although, of course, it just depends on your database setup
        String username;
        String password;

        Connection dbconn = null;

        //Try to load the driver to connect to the Oracle Database server
        //Basically, calling Class.forName("str") initializes/loads a given class in memory with the default ClassLoader
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            exit(-1);
        }

        while (dbconn == null) {
            Scanner myScanner = new Scanner(System.in);

            System.out.print("Username: ");
            username = myScanner.next();
            System.out.print("Password: ");
            password = myScanner.next();

            //Try to actually connect to the Oracle Database server
            try {
                dbconn = DriverManager.getConnection(oracleURL, username, password);
            } catch (SQLException e) {
                System.out.println("Login to database failed...\n");
                System.out.flush();
            }
        }

        //So if we've made it this far, we've connected to the database. Now, just startup the remaining server pieces
        //Open the ServerSocket
        sock = new ServerSocket(SERVER_PORT);

        System.out.println("Server started on port " + SERVER_PORT);

        while (true) {
            // Accept incoming connections from the ServerSocket.
            Socket s = sock.accept();

            //Print out the IP of our new client
            System.out.println("Accepted a new connection from " + s.getInetAddress());
            //Note: Make sure that if the order is "InputStream OutputStream" on the server, they're initialized as "OutputStream InputStream" on the client
            //These are blocking operations that require the other side to be essentially requesting the opposite pair for each one to proceed
            ObjectInputStream is = new ObjectInputStream(s.getInputStream());
            System.out.println("Got an ObjectInputStream for it!");
            ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
            System.out.println("Got an ObjectOutputStream for it!");

            // Save the output stream to our clients list so we can broadcast to this client later
            // Pretty sure this really isn't needed here (leftover from when this was a chat server), but I'll remove it later
            clients.add(os);
            System.out.println("Added it to list of clients!");

            // Start a new ClientHandler thread for this client.
            ClientHandler c = new ClientHandler(is, os, dbconn);
            c.start();
            System.out.println("Started ClientHandler!");
        }
    }

}

/**
 * Handles all of the things associated with serving a client while allowing the server to continue accepting new ones
 */
class ClientHandler extends Thread {

    private ObjectInputStream input;
    private ObjectOutputStream output;
    private Connection dbconn;

    public ClientHandler(ObjectInputStream input, ObjectOutputStream output, Connection dbconn) {
        this.input = input;
        this.output = output;
        this.dbconn = dbconn;
    }

    @Override
    public void run() {
        String barcode = null;
        // Read a String from the client that will be used to construct our SQL command
        try {
            barcode = (String) input.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            this.cleanup();
            return;
        }
        //If the user sent nothing or something didn't read properly (but wasn't caught above), just quit this thread
        if (barcode == null) {
            this.cleanup();
            return;
        }

        //Now that we have a valid string, let's build an SQL statement out of it, execute it, and return the results to the client
        try {
            //TODO: We use PreparedStatements, but OWASP also recommends potentially sanitizing the input still
            //Use PreparedStatements to help prevent SQL Injection
            //https://www.owasp.org/index.php/SQL_Injection_Prevention_Cheat_Sheet
            //This is the query we want to execute
            String query = "SELECT ID, LENUM_IST FROM IMPORTED_CONTINENTAL_SAP WHERE MATNR = ? ";
            //So prepare a statement for it so that we know anything else we add is to be interpreted as data, not SQL commands
            PreparedStatement pstmt = dbconn.prepareStatement(query);
            //Set the prepared statement parameter
            pstmt.setString(1, barcode);
            //And execute the SQL query
            ResultSet rs = pstmt.executeQuery();

            //Find the entry in the SAP database that matches our barcode and has the smallest ID
            int minOrderNumber = Integer.MAX_VALUE;
            String ReelID = "";
            int tempOrderNum;
            while (rs.next()) {
                //System.out.println("There was a match in the SAP database");
                //System.out.println("ID: " + rs.getString("ID") + ", REEL_ID: " + rs.getString("LENUM_IST"));
                tempOrderNum = rs.getInt("ID");
                if (tempOrderNum < minOrderNumber) {
                    minOrderNumber = tempOrderNum;
                    ReelID = rs.getString("LENUM_IST");
                }
            }

            System.out.println("I chose ID: " + minOrderNumber + ", REEL_ID: " + ReelID);

            //Repeat the process with the second database using what we learned from the first one
            //Now select the line, module, and slot pieces from the second database where the ReelID matches
            query = "SELECT Buffer, Slot from IMPORTED_CONTINENTAL_PICKS2 WHERE SUD = ? ";
            pstmt = dbconn.prepareStatement(query);
            pstmt.setString(1, ReelID);
            rs = pstmt.executeQuery();

            //There should only be one match for this, but prepare a boilerplate response for if the result set is empty
            ArrayList<String> headsetResults = new ArrayList<String>();

            String Line = "No data found for this barcode in PICKS2 database";
            String Module = "";
            String Slot = "";

            while (rs.next()) {
                String buffer = rs.getString("Buffer");
                //i.e. NG24NX1M09 has Line = 24, Module = 09
                Line = buffer.substring(2, 4);
                Module = buffer.substring(buffer.length() - 2);

                String moduleSlot = rs.getString("Slot");
                Slot = moduleSlot.substring(moduleSlot.length()-2);
            }

            headsetResults.add(Line);
            headsetResults.add(Module);
            headsetResults.add(Slot);

            //Physically send the results back to the client
            output.writeObject(headsetResults);
            System.out.println("Replied back with an ArrayList of " + headsetResults.size() + " elements");
            //Tear down the connection
            //Even though, without this, it manages to tear itself down anyway by trying to read a second barcode and failing
            cleanup();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    // Closes all the resources of a ClientHandler and logs a message.
    // It is called from every place that a fatal error occurs in ClientHandler (the catch blocks that
    // you can't recover from).
    private void cleanup() {
        try {
            this.input.close();
            this.output.close();
            //DON'T close dbconn as it is the server's connection to the database itself.
            System.out.println("Closed connection!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
