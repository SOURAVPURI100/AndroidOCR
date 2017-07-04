package com.example.sourav.ocrconvert;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ConvertOCR extends AppCompatActivity
{
    private static String fragmentTag = "FragmentRunning";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity_convert_ocr);
        if(savedInstanceState == null){
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            ActivityFragment fragment = new ActivityFragment();
            fragmentTransaction.add(R.id.main_activity_convert_ocr, fragment, fragmentTag);
            fragmentTransaction.commit();
        }
        else{
            ActivityFragment fragment = (ActivityFragment) getSupportFragmentManager().findFragmentByTag(fragmentTag);
        }

    }

}