package com.matthew.fittracker.fit_tracker.view;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import com.matthew.fittracker.fit_tracker.R;

import java.io.File;
import java.io.IOException;

/**
 * Created by dalton on 22/08/2015.
 */
public class Exercise_Results extends Activity{

    private ImageView iv, iv2;
    private Bitmap photo, snapShot;
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        refXML();
        retrieveValues();
    }
    protected void onPause(){ super.onPause(); }
    protected void onResume(){
        super.onResume();
    }
    protected void onStop(){
        super.onStop();
    }
    protected void onDestroy(){ super.onDestroy(); }

    private void refXML(){
        iv = (ImageView) findViewById(R.id.yourPhoto);
        iv2 = (ImageView) findViewById(R.id.showRoute);
    }
    private void retrieveValues(){
        // Retrieve the camera snapshot that was saved and carried by intent from the previous activity.
        photo = getIntent().getExtras().getParcelable("photo");
        iv.setImageBitmap(photo);
        // Retrieve the Google map snapshot saved in the
        File imgFile = new File("mnt/sdcard/Pictures/MyMapScreen.png");
        if(imgFile.exists()) {
            snapShot = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            iv2.setImageBitmap(snapShot);
        }
    }
}
