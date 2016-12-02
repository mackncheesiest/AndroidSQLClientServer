package com.example.joshua.myzxingapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Joshua on 11/3/2016.
 */

public class ViewMachine extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_machine);
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
