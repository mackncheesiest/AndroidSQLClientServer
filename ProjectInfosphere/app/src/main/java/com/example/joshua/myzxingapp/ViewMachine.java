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

        setContentView(R.layout.show_machine);

        TextView tv = (TextView) findViewById(R.id.LineNoTextView);
        ImageView imView = (ImageView) findViewById(R.id.imageView);
        imView.setImageResource("R.drawable.floorPlanImages." + getIntent().getStringExtra("LineNo"));

        tv.setText(getIntent().getStringExtra("MachineLocation"));

        Matrix myMat = new Matrix();
        myMat.preScale(0.5f, 0.5f);
        myMat.preTranslate(800.00f, -000.00f);
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
