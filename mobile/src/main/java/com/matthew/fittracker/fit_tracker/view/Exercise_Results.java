package com.matthew.fittracker.fit_tracker.view;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import com.matthew.fittracker.fit_tracker.R;

/**
 * Created by dalton on 22/08/2015.
 */
public class Exercise_Results extends Activity{

    private ImageView iv;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        refXML();
        init();
    }
    protected void onPause(){ super.onPause(); }
    protected void onResume(){
        super.onResume();
    }
    protected void onStop(){
        super.onStop();
    }
    protected void onDestroy(){ super.onDestroy(); }
    /*
        The Android Camera application encodes the photo in the return Intent delivered to onActivityResult() as a small Bitmap in the extras, under the key "data".
     */
    protected void onActivityReult(int requestCode, int resultCode, Intent data){
        if (resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            iv.setImageBitmap(imageBitmap);
        }
    }
    private void refXML(){
        iv = (ImageView) findViewById(R.id.yourPhoto);
    }
    private void init(){
    }
}
