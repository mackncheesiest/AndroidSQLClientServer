package com.example.joshua.myzxingapp;

import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Joshua on 11/3/2016.
 */

public class ViewMachine extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Change the user's app view
        setContentView(R.layout.show_machine);

        //Get handles for the GUI's text and image view objects
        TextView tv = (TextView) findViewById(R.id.LineNoTextView);
        ImageView imView = (ImageView) findViewById(R.id.imageView);

        //Thanks http://stackoverflow.com/questions/39938391/how-to-change-the-imageview-source-dynamically-from-a-string-xamarin-android
        //ImageView.setImageResource requires an integer "id" parameter, so first, we need to get the id for the corresponding image
        int id = getResources().getIdentifier("a" + getIntent().getStringExtra("LineNo"), "drawable", getPackageName());
        //Then, set that id to change the image that is displayed
        imView.setImageResource(id);

        //Set the text of the "Line, Module, Slot" information
        tv.setText(getIntent().getStringExtra("MachineLocation"));

        //And do some scaling on the chosen image to fit the image view better
        Matrix myMat = new Matrix();
        myMat.postScale(0.95f, 0.70f);
        //myMat.preTranslate(800.00f, -000.00f);
        imView.setImageMatrix(myMat);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onPause() {
        super.onPause();
        finish();
    }

}
