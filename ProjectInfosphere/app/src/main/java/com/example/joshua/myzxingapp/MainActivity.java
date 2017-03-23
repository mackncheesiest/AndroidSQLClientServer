package com.example.joshua.myzxingapp;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    //this int variable will determine what language to display the information in
    //English = 1
    //German = 2
    //Spanish = 3
    //English is set as default. Therefor language = 1
    int language = 1;

    //Huge thanks to Stack Overflow! (http://stackoverflow.com/questions/8009309/how-to-create-barcode-scanner-android)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Load the default view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Get the Button and add a listener to it
        Button myButton = (Button) findViewById(R.id.button);


        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*
                //User chooses English
                if(myButton.id == button){
                    language = 1;
                }

                //User chooses German
                else if(myButton.id == button2){
                    language = 2;
                }
                //user chooses Spanish
                else if(myButton.id == button3){
                    language = 3;
                }
                 */

                //When the Button is clicked, create an intent to scan a barcode and launch the intent
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                startActivityForResult(intent, 0);

                //Go straight to viewing the results (i.e. skip barcode reading)
                /*
                Intent showMachineIntent = new Intent(getApplicationContext(), ViewMachine.class);
                String machineLoc = "Line: \n1\n\nModule: \n2\n\nReel: \n3";
                showMachineIntent.putExtra("MachineLocation", machineLoc);
                startActivity(showMachineIntent);
                */
            }
        });

    }

    //Called when the "startActivityForResult(intent, 0)" finishes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            // Handle successful scan
            if (resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

                //BOOP-BEEP I'M QUERYING A DATABASE
                ArrayList<String> result = null;
                try {
                    result = new RetrieveQueryTask().execute(contents).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                //If we didn't get anything back from the database...
                if (result == null || result.size() == 0) {
                    //...Tell the user
                    Toast.makeText(getApplicationContext(), "Barcode Data Not Found :(", Toast.LENGTH_SHORT).show();
                }
                //Otherwise, say things were successful and change to the results screen
                else {
                    Toast.makeText(getApplicationContext(), "Barcode Data Found :)", Toast.LENGTH_LONG).show();

                    Intent showMachineIntent = new Intent(this, ViewMachine.class);
                    //Thanks http://stackoverflow.com/questions/2091465/how-do-i-pass-data-between-activities-on-android

                    //English selected
                    if(language == 1){
                        String machineLoc = "Line: \n" + result.get(0) + "\n\nModule: \n" + result.get(1) + "\n\nReel: \n" + result.get(2);
                        showMachineIntent.putExtra("MachineLocation", machineLoc);
                        startActivity(showMachineIntent);
                    }

                    //Spanish Selected
                    else if(language == 2){
                        String machineLoc = "Leitung: \n" + result.get(0) + "\n\nModul: \n" + result.get(1) + "\n\nHaspel: \n" + result.get(2);
                        showMachineIntent.putExtra("MachineLocation", machineLoc);
                        startActivity(showMachineIntent);
                    }

                    //German Selected
                    else if(language == 3){
                        String machineLoc = "Fila: \n" + result.get(0) + "\n\nModulo: \n" + result.get(1) + "\n\nCarrete: \n" + result.get(2);
                        showMachineIntent.putExtra("MachineLocation", machineLoc);
                        startActivity(showMachineIntent);
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                //The user cancelled without scanning a barcode
            }
        }
    }

    //If the back button is ever pressed while in this MainActivity (i.e. the start screen), then go back to whatever was before and kill this app
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }


}

//The Generics for AsyncTask are <Type you give this task, Type of objects used for Progress updates, Type of object returned>
//Thanks to stackoverflow http://stackoverflow.com/questions/6343166/how-to-fix-android-os-networkonmainthreadexception
class RetrieveQueryTask extends AsyncTask<String, Void, ArrayList<String>> {

    //doInBackground is normally where the action would go, but I already had queryDatabase written, so I just did it this way
    @Override
    protected ArrayList<String> doInBackground(String... params) {
        return queryDatabase(params[0]);
    }


    private ArrayList<String> queryDatabase(String contents) {
        //Begin by assuming you'll be let down
        ArrayList<String> result = null;

        //Initialize all of the network constants
        Socket sock;

        final String ADDRESS = "192.168.1.101";
        final int SERVER_PORT = 9001;
        final ObjectInputStream ois;
        final ObjectOutputStream oos;

        try {
            //Try to connect to the server
            sock = new Socket(ADDRESS, SERVER_PORT);
            Log.d("CONNECTION", "Successfully connected to socket!");
            oos = new ObjectOutputStream(sock.getOutputStream());
            Log.d("CONNECTION", "Got OOS for this socket!");
            ois = new ObjectInputStream(sock.getInputStream());
            Log.d("CONNECTION", "Got OIS for this socket!");

            //Write the barcode data to the server thing
            oos.writeObject(contents);
            Log.d("CONNECTION", "Wrote barcode contents to socket!");
            //And wait for the ArrayList<String> response
            result = (ArrayList<String>) ois.readObject();
            Log.d("CONNECTION", "Received response from server!");

            oos.close();
            ois.close();
            sock.close();

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        return result;

    }

}