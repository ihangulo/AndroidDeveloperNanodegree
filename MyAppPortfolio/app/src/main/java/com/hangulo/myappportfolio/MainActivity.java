package com.hangulo.myappportfolio;

import android.content.res.Resources;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


/*
    Android Developer Nanodegree

    Kwanghyun JUNG
    ihangulo@gmail.com

    29 MAY 2015

    My App Portfolio - Instructions
    Project

 */

public class MainActivity extends ActionBarActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }


    /*
        Show Toast Message

        INPUT : myStr - App name
     */
    void showMyToast(String myStr)
    {

        Toast.makeText(this, "This button will launch "+myStr+"!",Toast.LENGTH_SHORT).show();

    }

    /*

       Click listener of Buttons

     */

    public void onClick (View v)
    {

        Resources res = getResources();
        String[] nameOfApp = res.getStringArray(R.array.names_of_myapps); // app name (lower case)

        switch (v.getId())
        {
            case R.id.button1: //if button1 clicked
            {
                showMyToast( nameOfApp[0]);
            }
            break;

            case R.id.button2: //if button2 clicked
            {
                showMyToast( nameOfApp[1]);
            }
            break;

            case R.id.button3: //if button3 clicked
            {
                showMyToast( nameOfApp[2]);
            }
            break;

            case R.id.button4: //if button4 clicked
            {
                showMyToast( nameOfApp[3]);
            }
            break;

            case R.id.button5: //if button5 clicked
            {
                showMyToast( nameOfApp[4]);
            }
            break;

            case R.id.button6: //if button6 clicked
            {
                showMyToast( nameOfApp[5]);
            }

            break;

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
