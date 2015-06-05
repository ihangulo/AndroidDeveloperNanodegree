package com.hangulo.spotifystreamer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

/*

    ================================================
    Spotify Streamer, Stage 1: Implementation Guide
    ================================================

    from : 2nd JUN 2015
    to : 5th JUN 2015

    Kwanghyun JUNG
    ihangulo@gmail.com

    Android Devlelopment Nanodegree
    Udacity

    MainActivity --> ArtistFragment

*/

public class MainActivity extends AppCompatActivity
{

    // ActionBarActivity is deprecated -> change to AppCompatActivity
    // http://android-developers.blogspot.in/2015/04/android-support-library-221.html

    private final String LOG_TAG= MainActivity.class.getSimpleName(); // for error log
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       // http://developer.android.com/training/implementing-navigation/ancestral.html
       // https://speakerdeck.com/jgilfelt/this-way-up-implementing-effective-navigation-on-android
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
        return super.onOptionsItemSelected(item);
    }


}
