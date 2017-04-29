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
import java.util.*;

import static java.lang.System.exit;

public class JDBC_Server {

    //Enables various debug info related print statements if true
    public static final boolean DebugMode = false;

    //Change this to the port you want the Android/Headset App to connect to
    //Note that it needs to be defined in the headset application project as well
    public static final int SERVER_PORT = 9001;
    //Change this to the necessary value for your given OracleDB setup
    private static final String oracleURL = "jdbc:oracle:thin:hr/hr@localhost:1521/XE";

    private static ServerSocket sock;
    private static List<ObjectOutputStream> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {

        //Initialize the database connection
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
            //TODO: If eventually using this as a true commandline application, migrate this to a System.console.readPassword() function call to hide user input
            //System.console.readPassword() doesn't work in most IDE command lines, so that's why I'm not using it now
            System.out.print("Password: ");
            password = myScanner.next();
            //TODO: If readPassword is infeasible on the destination system, at least put some kind of platform-dependent print command here that clears the screen.

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
            ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());

            // Save the output stream to our clients list so we can broadcast to this client later
            // Pretty sure this really isn't needed here (leftover from when this was a chat server), but I'll remove it later
            clients.add(os);
            if (DebugMode)
                System.out.println("Added it to list of clients!");

            // Start a new ClientHandler thread for this client.
            ClientHandler c = new ClientHandler(is, os, dbconn);
            c.start();
            if (DebugMode)
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

    private static final HashMap<String, Integer> machineNumbers;

    static {
        machineNumbers = new HashMap<>();
        machineNumbers.put("NG02NX2BT", 1);
        machineNumbers.put("NG02NX1BT", 2);
        machineNumbers.put("NG05NX1BT", 3);
        machineNumbers.put("NG06NX1BT", 4);
        machineNumbers.put("NG07NX1BT", 5);
        machineNumbers.put("NG08NX1BT", 6);
        machineNumbers.put("NG09NX2BT", 7);
        machineNumbers.put("NG10NX1BT", 8);
        machineNumbers.put("NG11NX1BT", 9);
        machineNumbers.put("NG15NX1BT", 10);
        machineNumbers.put("NG15NX2BT", 11);
        machineNumbers.put("NG16NX1BT", 12);
        machineNumbers.put("NG17NX1BT", 13);
        machineNumbers.put("NG18NX1BT", 14);
        machineNumbers.put("NG19NX1BT", 15);
        machineNumbers.put("NG20NX1BT", 16);
        machineNumbers.put("NG20NX2BT", 17);
        machineNumbers.put("NG21NX1BT", 18);
        machineNumbers.put("NG24NX1BT", 19);
        machineNumbers.put("NG24NX2BT", 20);
        machineNumbers.put("NG26NX1BT", 21);
        machineNumbers.put("NG25NX1BT", 22);
        machineNumbers.put("NG27NX1BT", 23);
        machineNumbers.put("NG28NX1BT", 24);
        machineNumbers.put("NG28NX2BT", 25);
        machineNumbers.put("NG29NX1BT", 26);
        machineNumbers.put("NG29NX2BT", 27);
        machineNumbers.put("NG29NX2BT", 28);
    }

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
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            this.cleanup();
            return;
        }

        //Now that we have a valid string, let's build an SQL statement out of it, execute it, and return the results to the client
        try {
            //Replace all non alphanumeric entries with empty strings in the barcode as a form of input sanitation. Should be good enough.
            if (JDBC_Server.DebugMode)
                System.out.println("Received barcode: " + barcode);
            barcode = barcode.replaceAll("[^A-Za-z0-9]", "");
            if (JDBC_Server.DebugMode)
                System.out.println("Sanitized barcode: " + barcode);
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

            if (JDBC_Server.DebugMode)
                System.out.println("I chose ID: " + minOrderNumber + ", REEL_ID: " + ReelID);

            //Repeat the process with the second database using what we learned from the first one
            //Now select the line, module, and slot pieces from the second database where the ReelID matches
            query = "SELECT Station, Buffer, Slot from IMPORTED_CONTINENTAL_PICKS2 WHERE SUD = ? ";
            pstmt = dbconn.prepareStatement(query);
            pstmt.setString(1, ReelID);
            rs = pstmt.executeQuery();

            //There should only be one match for this, but prepare a boilerplate response for if the result set is empty
            ArrayList<String> headsetResults = new ArrayList<String>();

            String Line = "No data found for this barcode in PICKS2 database";
            String Machine = "";
            String Slot = "";

            while (rs.next()) {
                String buffer = rs.getString("Buffer");
                //i.e. NG24NX1M09 has Line = 24, Module = 09
                Line = buffer.substring(2, 4);

                //Start building the first piece of M2, 09
                Slot = "M" + buffer.substring(buffer.length() - 2) + ",";

                //Add the slot piece to the tail end of it, forming the full "Slot: 09" becomes "M2,09"
                String moduleSlot = rs.getString("Slot");
                Slot = Slot + moduleSlot.substring(moduleSlot.length()-2);

                Machine = machineNumbers.get(rs.getString("Station")).toString();
            }

            headsetResults.add(Line);
            headsetResults.add(Machine);
            headsetResults.add(Slot);

            //Physically send the results back to the client
            output.writeObject(headsetResults);
            System.out.println("Replied back with an ArrayList of " + headsetResults.size() + " elements");
            //Tear down the connection
            //Even though, without this, it manages to tear itself down anyway by trying to read a second barcode and failing
            cleanup();

        } catch (SQLException | IOException e) {
            e.printStackTrace();
            this.cleanup();
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
            if (JDBC_Server.DebugMode)
                System.out.println("Closed connection!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


