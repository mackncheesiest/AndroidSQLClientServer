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

import static java.lang.System.exit;

public class JDBC_Server {

    public static final int SERVER_PORT = 9001;

    private static ServerSocket sock;
    private static List<ObjectOutputStream> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {

        //TODO: Allow the user to pass in these parameters through args
        //Initialize the database connection
        //The basic idea is along the lines of
        //                    use jdbc thin driver to connect to user hr @ localhost through port 1521, and it's an eXpress Edition of Oracle Database
        final String oracleURL = "jdbc:oracle:thin:hr/hr@localhost:1521/XE";
        //The user's name is josh
        final String username = "josh";
        //And the password is incredibly secure
        final String password = "OracleDB";

        Connection dbconn = null;

        //Try to load the driver to connect to the Oracle Database server
        //Basically, calling Class.forName("str") initializes/loads a given class in memory with the default ClassLoader
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            exit(-1);
        }

        //Try to actually connect to the Oracle Database server
        try {
            dbconn = DriverManager.getConnection(oracleURL, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
            exit(-1);
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
        while (true) {

            String sql = null;

            // Read a String from the client that will be used to construct our SQL command
            try {
                sql = (String) input.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                this.cleanup();
                return;
            }
            //If the user sent nothing or something didn't read properly (but wasn't caught above), just quit this thread
            if (sql == null) {
                this.cleanup();
                return;
            }

            //Now that we have a valid string, let's build an SQL statement out of it, execute it, and return the results to the client
            try {
                Statement stmt = dbconn.createStatement();

                System.out.println("Recieved string " + sql);
                //Execute the actual SQL query -- a select query that assumes a table BCTABLE exists and it has elements
                //With fields (id, name) each VARCHAR2(256) and id as the PRIMARY KEY
                //ResultSet rs = stmt.executeQuery("SELECT LINE, MODULE, SLOT FROM MOCKCONTINENTAL1 WHERE BARCODE = " + sql + "\n");
                System.out.println("SQL Statement: " + "SELECT ID, LENUM_IST FROM IMPORTED_CONT_SAP_RPT_REEL WHERE MATNR = " + sql + "\n");
                ResultSet rs = stmt.executeQuery("SELECT ID, LENUM_IST FROM IMPORTED_CONT_SAP_RPT_REEL WHERE MATNR = '" + sql + "'\n");

                int minOrderNumber = Integer.MAX_VALUE;
                String ReelID = "";
                int tempOrderNum;
                while (rs.next()) {
                    tempOrderNum = rs.getInt("ID");
                    if (tempOrderNum < minOrderNumber) {
                        ReelID = rs.getString("LENUM_IST");
                    }
                }

                //Now select the line, module, and slot pieces from the second database where the ReelID matches
                rs = stmt.executeQuery("SELECT Buffer, Slot from IMPORTED_CONTINENTAL_PICKS2 WHERE SUD = " + ReelID + "\n");

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

                /*
                //Note that a ResultSet does not appear to actually contain the data. Rather, it contains the information needed to find it in the database
                //Thus, we need to first extract the data from the database before sending it back to the client
                ArrayList<String> temp = new ArrayList<String>();

                //In our use case, I'm pretty sure there will basically only be one match for each SELECT query, but just in case, send them all back
                //Iterate through all the matching rows (i.e. only one)
                while (rs.next()) {
                    //Extract the pieces we need from it
                    temp.add(rs.getString("LINE"));
                    temp.add(rs.getString("MODULE"));
                    temp.add(rs.getString("SLOT"));
                }
                */

                //Physically send the results back to the client
                output.writeObject(headsetResults);
                System.out.println("Replied back with an ArrayList of " + headsetResults.size() + " elements");

            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }

    }

    // Closes all the resources of a ClientHandler and logs a message.
    // It is called from every place that a fatal error occurs in ClientHandler (the catch blocks that
    // you can't recover from).
    private void cleanup() {
        try {
            this.input.close();
            this.output.close();
            //DONT close dbconn as it is a shared connection among all the clients
            System.out.println("Closed connection!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
