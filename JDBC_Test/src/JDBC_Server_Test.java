import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by Joshua on 2/14/2017.
 */
public class JDBC_Server_Test {
    public static void main(String args[]) {
        try {
            Socket mySock = new Socket(InetAddress.getLocalHost(), 9001);

            ObjectOutputStream oos = new ObjectOutputStream(mySock.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(mySock.getInputStream());

            //String myStr = "A2C4000391600";
            String myStr = "A2C4000667400";
            oos.writeObject(myStr);

            try {
                ArrayList<String> results = (ArrayList<String>) ois.readObject();

                System.out.println("Line: " + results.get(0));
                System.out.println("Machine: " + results.get(1));
                System.out.println("Slot: " + results.get(2));

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
