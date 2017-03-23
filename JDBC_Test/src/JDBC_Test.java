import java.sql.*;

import static java.lang.System.exit;

/**
 * Created by Joshua on 11/30/2016.
 */
public class JDBC_Test {

    public static void main(String args[]) {
        //Note: 10.0.2.2 comes from https://developer.android.com/studio/run/emulator-commandline.html#networkaddresses
        //TL;DR it allows for accessing the host machine's localhost from inside the Android Emulator
        final String oracleURL = "jdbc:oracle:thin:hr/hr@localhost:1521/XE";
        final String username = "josh";
        final String password = "OracleDB";

        Connection dbconn = null;

        //If this doesn't get changed, then the database junk didn't work
        String output = "this database junk didn't work";

        //Try to load the driver to connect to the Oracle Database server
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

        //Do some kind of query
        //Taken from https://www.tutorialspoint.com/jdbc/jdbc-create-database.htm
        try {
            Statement stmt = dbconn.createStatement();

            //Create a database called BARCODES

//            stmt.executeUpdate("DROP DATABASE BARCODES");

            //stmt.executeUpdate("CREATE DATABASE BARCODES");

            stmt.executeUpdate("DROP TABLE BCTABLE");

            //Create a table in this database that holds (int, string) pairs
            stmt.executeUpdate("CREATE TABLE BCTABLE (id VARCHAR2(255) not NULL, name VARCHAR2(255), PRIMARY KEY ( id ))");

            //Insert two new elements into this table
            stmt.executeUpdate("INSERT INTO BCTABLE VALUES (1001, '1.5k Resistor, 10W')");
            stmt.executeUpdate("INSERT INTO BCTABLE VALUES (1002, '3.0k Resistor, 10W')");

            //Access one of these entries from the table
            ResultSet rs = stmt.executeQuery("SELECT id, name FROM BCTABLE WHERE id = 1002");
            //This should end up storing the string "1.5k Resistor, 10W"
            rs.next();
            output = rs.getString("name");

            //And now we're done with this database junk
            dbconn.close();

        } catch (SQLException e) {
            e.printStackTrace();
            exit(-1);
        }

        System.out.println(output);
        exit(0);
    }

}
