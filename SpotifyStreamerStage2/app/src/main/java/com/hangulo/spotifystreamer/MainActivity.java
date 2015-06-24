package com.hangulo.spotifystreamer;




/*

    ================================================
    Spotify Streamer, Stage 2: Implementation Guide
    ================================================

    from : 6th JUN 2015
    to : 24th JUN 2015


    ================================================
    Spotify Streamer, Stage 1: Implementation Guide
    ================================================

    from : 2nd JUN 2015
    to : 5th JUN 2015 (stage 1)


    Kwanghyun JUNG
    ihangulo@gmail.com

    Android Devlelopment Nanodegree
    Udacity

    MainActivity --> ArtistFragment

*/

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;

import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity implements ArtistFragment.ArtistCallback {


    // ActionBarActivity is deprecated -> change to AppCompatActivity
    // http://android-developers.blogspot.in/2015/04/android-support-library-221.html
    private final String LOG_TAG= MainActivity.class.getSimpleName(); // for error log
    private static final String TOPTRACKS_FRAGMENT = "TOP_TRACKS_FRAGMENT"; // for tablet;
    private static final String MEDIA_PLAYER_DIALOG = "MEDIA_PLAYER_DIALOG";

    public Boolean mTwoPane;

    String mArtistId;
    String mArtistName;
    // now playing track's info
    String mNowPlayingSpotifyUrl; // spotify url for sharing
    String mNowPlayingTrack;
    String mNowPlayingArtist;
    int mNowPlayingPosition;
    private boolean isShareInfo; // only use two pane mode
    private boolean nowPlaying;

    TopTracksFragment mTopTrackFragment=null;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTwoPane = getResources().getBoolean(R.bool.two_pane); // two_pane mode check http://developer.android.com/guide/topics/ui/dialogs.html


    }

    @Override
    protected void onStart() {



            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                    new IntentFilter(MyMusic.BROADCAST));

            startMySerivce(MyMusic.ACTION_NEW); // start music service


        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_main, menu);
        if(mTwoPane) {
            MenuItem shareMenuItem = menu.findItem(R.id.action_share_menu);

            // Get the provider and hold onto it to set/change the share intent.
            ShareActionProvider mShareActionProvider =
                    (ShareActionProvider) MenuItemCompat.getActionProvider(shareMenuItem);

            // Attach an intent to this ShareActionProvider.  You can update this at any time,
            // like when the user selects a new piece of data they might like to share.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(getDefaultIntent());
            } else {
                Log.d(LOG_TAG, "Share Action Provider is null?");
            }

            shareMenuItem.setVisible(false); // until selection, it must be hidden
        }

        return true;
    }

    private Intent getDefaultIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Let's listen to music together!\n [" + mNowPlayingArtist + "]" + mNowPlayingTrack + ".  " + mNowPlayingSpotifyUrl);
        return shareIntent;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.action_now_playing: {

                if(!nowPlaying) return true;

                Bundle args = new Bundle();
                args.putBoolean("IS_RESUME", true);

                DialogFragment newFragment = PlayerFragment.newInstance();
                newFragment.setArguments(args);

                newFragment.show(getSupportFragmentManager(), MEDIA_PLAYER_DIALOG); // make now dialog


         /* not used this version

            else {   // add dialogfragment
                    Bundle args = new Bundle();
                    args.putBoolean("IS_RESUME", true);

                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

                    if (ft == null)
                        return true;

                    DialogFragment newFragment = PlayerFragment.newInstance();

                    newFragment.setArguments(args);
                    ft.add(R.id.fragment_artist, newFragment, MEDIA_PLAYER_FRAGMENT);
                    ft.addToBackStack(null);
                    ft.commit();
                }*/
            }

            default:   return super.onOptionsItemSelected(item);
        }
    }


    // Artist item callback (from fragment_artist)
    // according to mTwoPane(pan mode) update fragment(two-pane) or make new Intent(one pane)
    @Override
    public void onArtistItemSelected(ArtistItem item) {

        mArtistId = item.getArtist_id();
        mArtistName= item.getArtist_name();


        if(mTwoPane) {  // if two pane mode (tablet)

            Bundle args = new Bundle();
            args.putString("ARTIST_ID", mArtistId);       // set extra data - artist id
            args.putString("ARTIST_NAME", mArtistName );   // artist name

            TopTracksFragment mTopTracksFragment = new TopTracksFragment();
            mTopTracksFragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.toptrack_container, mTopTracksFragment, TOPTRACKS_FRAGMENT)
                    .commit();
        }
        else {// if not two pane mode
            Intent intent = new Intent(this, TopTracksActivity.class);
                intent.putExtra("ARTIST_ID",mArtistId);      // set extra data - artist id
                intent.putExtra("ARTIST_NAME", mArtistName);  // artist name

            startActivity(intent);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem itemNowPlaying = menu.findItem(R.id.action_now_playing);

        if(itemNowPlaying != null)
            if(nowPlaying)
                itemNowPlaying.setVisible(true);
            else
                itemNowPlaying.setVisible(false);


        MenuItem itemShare = menu.findItem(R.id.action_share_menu);

        if (itemShare != null)
            if (isShareInfo) // if there is information
                itemShare.setVisible(true);
            else
                itemShare.setVisible(false);

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    protected void onDestroy() {

        if(isFinishing()) { // is that not rotation, real destroy
            startMySerivce(MyMusic.ACTION_STOP); // stop music
            stopMySerivce(); // stop service
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        Log.v(LOG_TAG, "Main: onDestroy");
        super.onDestroy();

    }

    // start music service with ease
    void startMySerivce(String action) {
        Intent intent = new Intent(this, MyMusicService.class);
        if (intent != null) {
            intent.setAction(action);
            startService(intent);
        }
    }

    // stop service
    void stopMySerivce()  {
        Intent intent = new Intent(this, MyMusicService.class);
        if (intent!=null)
            stopService(intent);
    }


    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String message = intent.getStringExtra(MyMusic.BROADCAST_MSG_TAG);

            Log.d(LOG_TAG, "Got message[mMessageReceiver] " + message);

            switch (message) {

                case MyMusic.NOTI_NOW_PLAYING:
                case MyMusic.NOTI_RESUME_PLAYING:
                    nowPlaying = true;
                    isShareInfo = true;
                    invalidateOptionsMenu(); // turn on  onAir icon (actionbar menu)
                    break;

                case MyMusic.NOTI_PAUSE_PLAYING:
                case MyMusic.NOTI_STOP_PLAYING:
                case MyMusic.NOTI_REWIND_PLAYING:
                    nowPlaying = false;
                    if(mTwoPane)
                        isShareInfo = true;
                    else
                        isShareInfo = false; // if phone mode, 1)MainActivity 2) music is stopped then remove share button
                    invalidateOptionsMenu(); // turn on  "now playing" icon (actionbar menu)

                    break;

                case MyMusic.NOTI_MUSIC_INFO: // if music information is received -> turn on share button
                    TopTracksItem topTracksItem = intent.getParcelableExtra("TRACK");
                    mNowPlayingPosition = intent.getIntExtra("POSITION", 0); // get current list position
                    mNowPlayingSpotifyUrl = topTracksItem.getSpotify_url(); // not preview URL!
                    mNowPlayingTrack = topTracksItem.getTrack_name();
                    mNowPlayingArtist = topTracksItem.getArtist_name();

                    isShareInfo=true; // Share button show on
                    invalidateOptionsMenu();

                    if(mTwoPane) {
                        // listview selection
                        if(mArtistName == null) break;
                        if (mArtistName.equals(mNowPlayingArtist)) {
                            if (mTopTrackFragment != null)
                                mTopTrackFragment.setTopTracksSelection(mNowPlayingPosition); // change list selection
                            else {
                                mTopTrackFragment = (TopTracksFragment) getSupportFragmentManager().findFragmentByTag(TOPTRACKS_FRAGMENT);
                                if (mTopTrackFragment != null)
                                    mTopTrackFragment.setTopTracksSelection(mNowPlayingPosition); // change list selection
                            }
                        }

                    }
                    break;

                case MyMusic.NOTI_NOW_LOADING:

                    nowPlaying = false;
                    invalidateOptionsMenu();
                    break;

            }

        }
    };


    /**************** not used ***************/


/*    // get result and setting "now playing" mode icon
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {

            nowPlaying = data.getBooleanExtra("RESULT_NOW_PLAYING",false); // now playing?
            invalidateOptionsMenu(); // set menu icon
        }


        super.onActivityResult(requestCode, resultCode, data);
    }
*/

    // Playercallback // only two pane mode

    /*
    // set "now Playing" & play song
     @ Override
    public void onMusicPlay() {

  //      nowPlaying=true;
 //       supportInvalidateOptionsMenu(); // turn on  onAir icon (actionbar menu)


    }

    // set listview item selected
    @Override
    public void onMusicSelect(int position) {

        FragmentManager manager = getSupportFragmentManager();
        if (manager != null)
       {
            TopTracksFragment topTracksFragment = (TopTracksFragment) manager.findFragmentByTag(TOPTRACKS_FRAGMENT_TAG);
            if (topTracksFragment != null)
                topTracksFragment.setTopTracksSelection(position); // change list selection
        }

    }

    @Override
    public void onMusicStop() {
      //  nowPlaying=false;
  //      supportInvalidateOptionsMenu(); // just remove icon
    }

*/


}
