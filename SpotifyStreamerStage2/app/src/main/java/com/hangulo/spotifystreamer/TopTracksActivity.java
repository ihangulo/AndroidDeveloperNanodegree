package com.hangulo.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.widget.Toast;

/**
 *  SpotifyStreamer, Stage 2
 *  ------------------------
 *  Kwanghyun JUNG
 *  24th JUN 2015
 *
* Top 10 track main Activity -> TopTracksFragment
*
*/
public class TopTracksActivity extends AppCompatActivity {
    private static final String LOG_TAG = TopTracksActivity.class.getSimpleName();
    private static final String TOPTRACKS_FRAGMENT = "TOP_TRACKS_FRAGMENT";
    private static final String MEDIA_PLAYER_FRAGMENT ="MEDIA_PLAYER_FRAGMENT";

    String mArtistId;
    String mArtistName;
    // now playing track's info
    String mNowPlayingSpotifyUrl; // spotify url for sharing
    String mNowPlayingTrack;
    String mNowPlayingArtist;
    int mNowPlayingPosition;
    private boolean isShareInfo; // only use two pane mode
    private boolean nowPlaying;
    //String mPlayingUrl;
    //private boolean nowPlaying;
    public boolean mTwoPane;
    TopTracksFragment mTopTrackFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toptracks);

        mTwoPane = getResources().getBoolean(R.bool.two_pane); // two_pane mode check http://developer.android.com/guide/topics/ui/dialogs.html


        if(savedInstanceState == null) { // if new...
            Intent intent=getIntent();
            if (intent != null)
            {
                TopTracksFragment mTopTrackFragment = new TopTracksFragment();

                mArtistId = intent.getStringExtra("ARTIST_ID");
                mArtistName=intent.getStringExtra("ARTIST_NAME");


                mTopTrackFragment.setArguments(intent.getExtras()); // pass the all extra argument

                getSupportFragmentManager().beginTransaction()
                    .add(R.id.toptrack_container, mTopTrackFragment, TOPTRACKS_FRAGMENT) // add (new intent)
                        .commit();


                Log.v(LOG_TAG, "Intent=" + mArtistId + "/" + mArtistName);
            }
            else
                Toast.makeText(this, getResources().getText(R.string.msg_system_error) + ": 777", Toast.LENGTH_LONG).show();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(MyMusic.BROADCAST)); // broadcast receiver

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu); // same menu with main
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;


            case R.id.action_now_playing:  // only one pane
                if(getSupportFragmentManager().findFragmentByTag(MEDIA_PLAYER_FRAGMENT) == null) { // not now MediaPlayer is not showing

                    if (mTopTrackFragment != null)
                        mTopTrackFragment.showMediaPlayer(true);
                    else {
                        mTopTrackFragment = (TopTracksFragment) getSupportFragmentManager().findFragmentByTag(TOPTRACKS_FRAGMENT);
                        if (mTopTrackFragment != null) // 그리고 현재 상태가 이미 떠 있는 상태가 아니어야 한다.
                            mTopTrackFragment.showMediaPlayer(true);
                    }
                }

                return true;
            default : return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem itemNowPlaying = menu.findItem(R.id.action_now_playing);

        if(itemNowPlaying != null) {
            if(nowPlaying)
                itemNowPlaying.setVisible(true);

            else
                itemNowPlaying.setVisible(false);
        }

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();

    }



    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String message = intent.getStringExtra(MyMusic.BROADCAST_MSG_TAG);


            switch (message) {

                case MyMusic.NOTI_NOW_PLAYING:
                case MyMusic.NOTI_RESUME_PLAYING:
                    nowPlaying = true;
                    isShareInfo = true; // only two_pane mode
                    invalidateMyOptionsMenu(); // turn on  onAir icon (actionbar menu)
                    // Toast.makeText(getActivity(), "NOTI_NOW_PLAYING", Toast.LENGTH_SHORT).show();  // debug_hangulo
                    break;
                case MyMusic.NOTI_PAUSE_PLAYING:
                case MyMusic.NOTI_STOP_PLAYING:
                case MyMusic.NOTI_REWIND_PLAYING:
                    // if start playing
                    nowPlaying = false;
                    invalidateMyOptionsMenu(); // turn on  "now playing" icon (actionbar menu)
                    break;

                case MyMusic.NOTI_NOW_LOADING:
                    nowPlaying = false;
                    invalidateMyOptionsMenu();
                    break;

                case MyMusic.NOTI_MUSIC_INFO:

                    TopTracksItem topTracksItem = intent.getParcelableExtra("TRACK");
                    mNowPlayingPosition = intent.getIntExtra("POSITION", 0); // get current list position
                    mNowPlayingSpotifyUrl = topTracksItem.getSpotify_url(); // not preview URL!
                    mNowPlayingTrack = topTracksItem.getTrack_name();
                    mNowPlayingArtist = topTracksItem.getArtist_name();
                    isShareInfo=true; // Share button show on
                    invalidateMyOptionsMenu();

                    // listview selection
                    if (mArtistName==null)
                        break;
                    if (mArtistName.equals(mNowPlayingArtist)) {
                        if (mTopTrackFragment != null)
                            mTopTrackFragment.setTopTracksSelection(mNowPlayingPosition); // change list selection
                        else {
                            mTopTrackFragment = (TopTracksFragment) getSupportFragmentManager().findFragmentByTag(TOPTRACKS_FRAGMENT);
                            if (mTopTrackFragment != null)
                                mTopTrackFragment.setTopTracksSelection(mNowPlayingPosition); // change list selection
                        }
                    }
                    break;

            }

        }
    };

    // Avoid null pointer exception
    void invalidateMyOptionsMenu()
    {
            try {

                supportInvalidateOptionsMenu();
            } catch (NullPointerException e) {
                Log.e(LOG_TAG, "Error : NullPointerException / invalidateMyOptionsMenu()");
            }
    }

    /* ********************* NOT USED **************************
    // Playercallback
    @Override
    public void onMusicPlay() {

        nowPlaying=true;
        supportInvalidateOptionsMenu(); // turn on  onAir icon (actionbar menu)
    }


    @Override
    public void onMusicStop() {
        nowPlaying=false;
        supportInvalidateOptionsMenu();
    }


    @Override
    public void onMusicSelect(int selection) {
        return;
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra("RESULT_NOW_PLAYING",nowPlaying); // return nowPlaying flag
        setResult(RESULT_OK, intent);

        super.finish();
    }
*/


}
