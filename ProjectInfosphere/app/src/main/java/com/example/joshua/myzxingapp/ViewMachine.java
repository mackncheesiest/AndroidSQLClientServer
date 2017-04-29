package com.example.joshua.myzxingapp;

import android.content.Intent;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.joshua.myzxingapp.language.english;
import static com.example.joshua.myzxingapp.language.german;

/**
 * Created by Joshua on 11/3/2016.
 */

public class ViewMachine extends AppCompatActivity {

    language chosenLanguage;
    boolean scanningNewBarcode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Change the user's app view
        setContentView(R.layout.show_machine);

        //Get a handle for the image view object
        ImageView imView = (ImageView) findViewById(R.id.imageView);

        //And do some scaling on the chosen image to fit the image view better
        Matrix myMat = new Matrix();
        myMat.postScale(0.95f, 0.70f);
        //myMat.preTranslate(800.00f, -000.00f);
        imView.setImageMatrix(myMat);

        //Extract the machine location and line number and pass them to the updateScreenContent method
        String machineLoc = getIntent().getStringExtra("MachineLocation");
        String lineNo = getIntent().getStringExtra("LineNo");
        chosenLanguage = (language) getIntent().getExtras().get("Language");

        //Change the text and such that are displayed on the screen
        updateScreenContent(machineLoc, lineNo);

        //If the user presses the "OK"/"Enter" button, go back to the barcode scanner to repeat the process
        Button nextBarcodeButton = (Button) findViewById(R.id.button5);
        nextBarcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                scanningNewBarcode = true;
                startActivityForResult(intent, 0);
            }
        });
    }

    //Called when the "startActivityForResult(intent, 0)" finishes
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            scanningNewBarcode = false;
            // Handle successful scan
            if (resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");

                //BOOP-BEEP I'M QUERYING A DATABASE
                ArrayList<String> result = null;
                try {
                    result = new RetrieveQueryTask().execute(contents).get(10000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | ExecutionException e) {
                    result = null;
                    e.printStackTrace();
                }
                catch (TimeoutException t) {
                    result = null;
                    Toast.makeText(getApplicationContext(), "Network timeout", Toast.LENGTH_SHORT).show();
                    return;
                }

                //If we didn't get anything back from the database...
                if (result == null || result.size() == 0) {
                    //...Tell the user
                    Toast.makeText(getApplicationContext(), "Barcode Data Not Found", Toast.LENGTH_SHORT).show();
                }
                //Otherwise, say things were successful and change to the results screen
                else {
                    String machineLoc;
                    //English selected
                    if(chosenLanguage == english){
                        machineLoc = "Line: \n" + result.get(0) + "\n\nMachine: \n" + result.get(1) + "\n\nSlot: \n" + result.get(2);
                    }
                    //German Selected
                    else if(chosenLanguage == german){
                        machineLoc = "Linie: \n" + result.get(0) + "\n\nMaschine: " + result.get(1) + "\n\nSlot: \n" + result.get(2);
                    }
                    //Spanish Selected
                    else {
                        machineLoc = "Línea: \n" + result.get(0) + "\n\nMáquina: \n" + result.get(1) + "\n\nEspacio: \n" + result.get(2);
                    }
                    String lineNo = result.get(0);
                    updateScreenContent(machineLoc, lineNo);
                }
            } else if (resultCode == RESULT_CANCELED) {
                //The user cancelled without scanning a barcode
            }
        }
    }

    private void updateScreenContent(String machineLoc, String lineNo) {
        //Get handles for the GUI's text and image view objects
        TextView tv = (TextView) findViewById(R.id.LineNoTextView);
        ImageView imView = (ImageView) findViewById(R.id.imageView);

        //Thanks http://stackoverflow.com/questions/39938391/how-to-change-the-imageview-source-dynamically-from-a-string-xamarin-android
        //ImageView.setImageResource requires an integer "id" parameter, so first, we need to get the id for the corresponding image
        int id = getResources().getIdentifier("a" + lineNo, "drawable", getPackageName());
        //Then, set that id to change the image that is displayed
        imView.setImageResource(id);

        //Set the text of the "Line, Module, Slot" information
        tv.setText(machineLoc);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onPause() {
        super.onPause();
        //If we are scanning a new barcode, then, don't close the activity.
        //If we aren't, though, do close it.
        if (!scanningNewBarcode) {
            finish();
        }
    }

}
