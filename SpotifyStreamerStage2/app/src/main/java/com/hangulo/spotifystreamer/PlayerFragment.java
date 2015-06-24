package com.hangulo.spotifystreamer;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import android.widget.Toast;

import 	android.content.Context;

import com.hangulo.spotifystreamer.MyMusic.MusicStatus;

import butterknife.ButterKnife;
import butterknife.InjectView;


/**
 * Spotify Streamer PlayerFragment
 * Created by khjung on 2015-06-11.
 */
public class PlayerFragment extends DialogFragment implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private static final String LOG_TAG = PlayerFragment.class.getSimpleName();

    private View rootView;

    // Inject views using Butter knife
    @InjectView(R.id.textview_player_artist_name)
    TextView txtViewArtistName;
    @InjectView(R.id.textview_player_album_name)
    TextView txtViewAlbumName;
    @InjectView(R.id.textview_player_track_name)
    TextView txtViewTrackName;

    @InjectView(R.id.textview_player_current)
    TextView txtViewCurrent;
    @InjectView(R.id.textview_player_duration)
    TextView txtViewDuration;
    @InjectView(R.id.imageview_player_album_artwork)
    ImageView imgViewAlbumArtwork;
    @InjectView(R.id.imagebutton_player_share)
    ImageButton imgBtnShare;
    @InjectView(R.id.button_player_play)
    ImageButton imgBtnPlay;
    @InjectView(R.id.button_player_prev)
    ImageButton imgBtnPrev;
    @InjectView(R.id.button_player_next)
    ImageButton imgBtnNext;
    @InjectView(R.id.progress_player)
    ProgressBar loadingCircle;
    //@InjectView(R.id.progress_player_album_artwork) ProgressBar mLoadingAlbum;
    @InjectView(R.id.seekBar_player)
    SeekBar mSeekBar;


    //private Handler myHandler = new Handler();
    private final static boolean TO_PLAY_BUTTON = false;
    private final static boolean TO_PAUSE_BUTTON = true;
    private Drawable drawblePlayImg;
    private Drawable drawblePauseImg;

    //for loading flag
    //boolean isOneLoadingDone; // if one of two (image or music) loading done

    ArrayList<TopTracksItem> mTopTracks;
    TopTracksItem mTopTrackItem;

    int mPosition = 0;
    boolean mTwoPane; // two pane mode

    int mSeekBarMax; // max item
    int mSeekBarProgress;  // long --> int (32bit int is enough. if we process long music, then more processing need)
    int mTrackMax; // number of tracks

    boolean isResume;
    boolean mBroad;
    MusicStatus mStatus = MusicStatus.STOP;

    LocalBroadcastManager mLocalBroadcastManager;

    // play button status
    enum PlayButtonStatus {
        BTN_PLAY,
        BTN_PAUSE
    }

    PlayButtonStatus playButtonStatus = PlayButtonStatus.BTN_PLAY;

    static PlayerFragment newInstance() {
        return new PlayerFragment();
    }

    // my Broadcast Recevicer
    BroadcastReceiver mMessageReceiver = new PlayerBroadcastReceiver();

    /**
     * The system calls this to get the DialogFragment's layout, regardless
     * of whether it's being displayed as a dialog or an embedded fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout to use as dialog or embedded fragment
        rootView = inflater.inflate(R.layout.fragment_player, container, false);

        Bundle arguments;

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                new IntentFilter(MyMusic.BROADCAST));
        mBroad=true;

        if (rootView == null) // system error
            return null;

        mTwoPane = getResources().getBoolean(R.bool.two_pane); // two_pane mode check http://developer.android.com/guide/topics/ui/dialogs.html

        ButterKnife.inject(this, rootView); // inject (fragment)


        if (mTwoPane)
            setCancelable(true);// can use prev key to cancel dialog
        else  // if one pane
        {
            FrameLayout fl = (FrameLayout) rootView.findViewById(R.id.fragment_player_framelayout);

            // if this fragment is runned by phone, then touch event have problem
            // (it works like tranparent view (it touch background & forground views. it is very difficult to slove)
            // so I intercept touch event only for playerfragemnt.
            // help of http://stackoverflow.com/questions/11460789/how-to-process-all-touchevents-at-the-viewgroup-level-ie-bubble-all-events-up-t
            fl.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    int id = v.getId();

                    switch (id) {
                        // thease are TopTracksFragment items.
                        case R.id.toptracks_progressBar:
                        case R.id.listview_spotify_toptracks:
                        case R.id.toptracks_frame:
                        case R.id.list_item_track_image:
                        case R.id.list_item_track_name:
                        case R.id.list_item_track_album_name:
                            return false; // i will ignore

                        default:
                            return true; // ok, pass to child view
                    }
                }
            });
        }
        imgBtnPlay.setOnClickListener(this);
        imgBtnNext.setOnClickListener(this);
        imgBtnPrev.setOnClickListener(this);
        imgBtnShare.setOnClickListener(this);

        drawblePlayImg = getResources().getDrawable(android.R.drawable.ic_media_play);
        drawblePauseImg = getResources().getDrawable(android.R.drawable.ic_media_pause);

        mSeekBar.setOnSeekBarChangeListener(this); // set seekbar listener


        if (savedInstanceState != null) { // if saved instance
            isResume = savedInstanceState.getBoolean("IS_RESUME");
            mTopTrackItem = savedInstanceState.getParcelable("TOPTRACK_ITEM");
            mSeekBarProgress= savedInstanceState.getInt("SEEKBAR_PROGRESS",0);
            mSeekBarMax= savedInstanceState.getInt("SEEKBAR_MAX",0);

//            setNewMusicInfoScreen();

//            startMySerivce(MyMusic.ACTION_ISPLAYING); // get current music info
            Log.v(LOG_TAG, "SavedInstance....recover : SeekBar Max, SeekBarProgress" + String.valueOf(mSeekBarMax) + "," + String.valueOf((mSeekBarProgress)));
            //imgBtnShare.setClickable(true);



        //    changePlayButtonImg(TO_PAUSE_BUTTON);
          imgBtnShare.setClickable(true);

        } else if ((arguments = getArguments()) != null) {

           isResume = arguments.getBoolean("IS_RESUME", false);

            if (!isResume) { // new loading mode

                setMusicStatus(MusicStatus.LOADING_LIST);
                getMusicinfo(); // start new music automatically


            } else { // if resume mode
                  imgBtnShare.setClickable(true);
            }
        }
        // http://stackoverflow.com/questions/8802157/how-to-use-localbroadcastmanager
        // Register to receive messages.
        return rootView;
    }



    @Override
    public void onStart() {

        if(isResume) {
            startMySerivce(MyMusic.ACTION_ISPLAYING); // get current music info
            setNewMusicInfoScreen(); // anyway it is playing...
        }
        if (!mBroad) {
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,
                    new IntentFilter(MyMusic.BROADCAST));
            mBroad=true;
        }

        isResume=false;

        ; // anyway it is playing...
        Log.v(LOG_TAG, "onStart()");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.v(LOG_TAG, "onStop()");
        super.onStop();
        // Unregister since the activity is about to be closed.

    }




    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

                //*** 여기서 뭘 저장해야 하는지? --> 그냥 저장할 것은 없을 것 같은데? 지금 것만 저장하면 될 듯 ***/

        savedInstanceState.putBoolean("IS_RESUME", true);
        savedInstanceState.putParcelable("TOPTRACK_ITEM", mTopTrackItem);
        savedInstanceState.putInt("SEEKBAR_PROGRESS", mSeekBarProgress);
        savedInstanceState.putInt("SEEKBAR_MAX", mSeekBarMax);

        mBroad=false;

        Log.v(LOG_TAG, "onSaveInstanceState() : mSeekBarMax, mSeekBarProgress" + String.valueOf(mSeekBarMax) +","+ String.valueOf((mSeekBarProgress)) );
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * The system calls this only when creating the layout in a dialog.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
        mBroad=false;
        super.onDestroy();
    }

    String getFormattedTimeFromMillis(int finalTime) {
        String sDuration;

        if (finalTime < 0) finalTime = 0;

        int second = (finalTime / 1000) % 60;
        int minute = (finalTime / (1000 * 60)) % 60;
        int hour = (finalTime / (1000 * 60 * 60)) % 60;

        if (hour > 1)
            sDuration = String.format("%d:%02d:%02d", hour, minute, second);
        else
            sDuration = String.format("%d:%02d", minute, second);
        return sDuration;
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.button_player_play:
                if (getMusicStatus() == MusicStatus.LOADING) // if LOADING status, then cannot use play button
                {
                    setButtonClickableAlpha(imgBtnPlay,false);
                    return;
                }
                if (isPlaying())
                    pauseMusicService();
                else
                    resumeMusicService();
                break;

            case R.id.button_player_next: {
                getNextMusicinfo();
                break;
            }

            case R.id.button_player_prev:
                getPrevMusicinfo();
                break;


            case R.id.imagebutton_player_share: // share
                shareNowPlaying(mTopTrackItem); // share it!
                break;

        }
    }

    private void shareNowPlaying(TopTracksItem item) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Let's listen to music together!\n [" + item.getArtist_name() + "] " + item.getTrack_name() + ".  " + item.getSpotify_url() + " #SpotifyStreamer");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_send_to)));
    }


    //-----------------Seek bar Change listner --------------
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }


    // seekbar changed listener
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if (!fromUser) return;

        seekToMusicService(progress);
        txtViewCurrent.setText(getFormattedTimeFromMillis(progress)); // set textView (current time)

            // seekBar.setProgress(mSeekBarProgress); // back to current

    }

    // https://www.youtube.com/watch?v=tC3FUF47tlo&list=PL14AA2548E3C96B50&index=8
    // update seek bar UI
    void updateSeekBar() {

        if (mSeekBar == null) {
            return; // system error
        }
        mSeekBar.setMax(mSeekBarMax);
        mSeekBar.setProgress(mSeekBarProgress);
        txtViewCurrent.setText(getFormattedTimeFromMillis(mSeekBarProgress)); // set textView (current time)
        txtViewDuration.setText(getFormattedTimeFromMillis(mSeekBarMax));
    }


    //------------- / Seek bar

    // start music service with ease
    void startMySerivce(String action) {
        Intent intent = new Intent(getActivity(), MyMusicService.class);
        intent.setAction(action);
        getActivity().startService(intent);

        Log.d(LOG_TAG, "Send Service: " + action);
    }

    void setButtonClickableAlpha(ImageButton imgbtn, boolean ok) {

        if (ok) {
            imgbtn.setClickable(true);
            imgbtn.setAlpha(1.0f);
            //    imgbtn.setEnabled(true);
        } else {
            imgbtn.setClickable(false);
            imgbtn.setAlpha(0.4f);
            //   imgbtn.setEnabled(false);
        }

    }

    // get current music info only (not playing)
    // response will be back by Broadcast
    public void getMusicinfo() {
        Intent intent = new Intent(getActivity(), MyMusicService.class);
        intent.setAction(MyMusic.ACTION_GET_MUSIC_NOW);
        //intent.putExtra("POSITION", myPosition);
        getActivity().startService(intent);

        imgBtnShare.setClickable(false); // loading을 시작하면 일단 share button off
        setButtonClickableAlpha(imgBtnPlay,false);
        mSeekBar.setEnabled(false);

        Log.d(LOG_TAG, "Send Service: getMusicinfo() ");
    }

    // get next music
    public void getNextMusicinfo() {
        startMySerivce(MyMusic.ACTION_GET_MUSIC_NEXT);
        imgBtnShare.setClickable(false);



    }

    // get prev music
    public void getPrevMusicinfo() {
        startMySerivce(MyMusic.ACTION_GET_MUSIC_PREV);
        imgBtnShare.setClickable(false);
    }

    // item version 2015.6.17
    public void setNewMusicInfoScreen() {

        if (mTopTrackItem == null) return;

        // check music position
        if (mPosition == 0)
            setButtonClickableAlpha(imgBtnPrev, false);
        else
            setButtonClickableAlpha(imgBtnPrev, true);

        if (mPosition == mTrackMax)
            setButtonClickableAlpha(imgBtnNext, false);
        else
            setButtonClickableAlpha(imgBtnNext, true);

        if(mSeekBarMax > 0) {
            mSeekBar.setProgress(mSeekBarProgress);
            mSeekBar.setMax(mSeekBarMax);
        }

        txtViewCurrent.setText(getFormattedTimeFromMillis(mSeekBarProgress));

        switch(mStatus)
        {
            case PLAYING:
                changePlayButtonImg(TO_PAUSE_BUTTON);
                setButtonClickableAlpha(imgBtnPlay, true);
                loadingCircle.setVisibility(View.INVISIBLE);

                break;
            case LOADING:
                changePlayButtonImg(TO_PAUSE_BUTTON);
                setButtonClickableAlpha(imgBtnPlay, false);
                loadingCircle.setVisibility(View.VISIBLE);
            default:
                changePlayButtonImg(TO_PLAY_BUTTON);
                setButtonClickableAlpha(imgBtnPlay,true);

        }

        //txtViewArtistName.setText(mArtistName);
        txtViewArtistName.setText(mTopTrackItem.getArtist_name());
        txtViewAlbumName.setText(mTopTrackItem.getAlbum_name());
        txtViewTrackName.setText(mTopTrackItem.getTrack_name());





        //mLoadingAlbum.setVisibility(View.VISIBLE);
        Picasso.with(getActivity())
                .load(mTopTrackItem.getThumb_large_url())
                .fit().centerCrop()
                .error(R.drawable.noimage) // error (not found)
                .placeholder(R.drawable.nowloading) // loading
                .into(imgViewAlbumArtwork, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError() {

                            }
                        }
                );
        Log.v(LOG_TAG, "setNewMusicInfoScreen() end ");
    }

    void changePlayButtonImg(boolean isplay) {
        if (isplay) {// now playing => pause button
            imgBtnPlay.setImageDrawable(drawblePauseImg);
            playButtonStatus = PlayButtonStatus.BTN_PAUSE;
        } else {// else play button
            imgBtnPlay.setImageDrawable(drawblePlayImg);
            playButtonStatus = PlayButtonStatus.BTN_PLAY;
        }
    }


    // resum music
    public void resumeMusicService() {

        if (isPlaying()) return;
        startMySerivce(MyMusic.ACTION_RESUME);

    }

    // pause music
    public void pauseMusicService() {

        if(!isPlaying()) return;
        startMySerivce(MyMusic.ACTION_PAUSE);
    }

    // change the timeline of music (seek)
    public void seekToMusicService(int value) {

        Intent intent = new Intent(getActivity(), MyMusicService.class);
        intent.setAction(MyMusic.ACTION_SEEKTO);
        intent.putExtra("SEEK_TO", value);
        getActivity().startService(intent);

    }

    // get current status (from outside of this service)
    public MusicStatus getMusicStatus() {
        return mStatus;
    }

    void setMusicStatus(MusicStatus status) {
        mStatus = status;
    }

    public boolean isPlaying() {
        return (mStatus == MusicStatus.PLAYING);
    }

    // my Broadcast Recevicer
    // http://stackoverflow.com/questions/8802157/how-to-use-localbroadcastmanager

    class PlayerBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(MyMusic.BROADCAST_MSG_TAG);
            Log.d(LOG_TAG, "Got message[PlayerFragment] " + message);

            switch (message) {

                case MyMusic.NOTI_NOW_PLAYING: { // if start playing

                    mPosition = intent.getIntExtra("POSITION", 0);

                    // if image & music all  done, then remove loading progress
                    if (loadingCircle != null)
                        loadingCircle.setVisibility(View.INVISIBLE);

                    setMusicStatus(MusicStatus.PLAYING); // nowPlaying

                    // now playing
                    loadingCircle.setVisibility(View.INVISIBLE);
                    changePlayButtonImg(TO_PAUSE_BUTTON);
                    setButtonClickableAlpha(imgBtnPlay, true);
                    mSeekBar.setEnabled(true);


                    break;
                }


                case MyMusic.NOTI_NOW_LOADING: {// if loading is started
                    loadingCircle.setVisibility(View.VISIBLE);
                    loadingCircle.bringToFront();
                    // reset seek bar & current time
                    mSeekBar.setProgress(0);
                    txtViewCurrent.setText(getFormattedTimeFromMillis(0));
                    mSeekBar.setEnabled(false);
                    setMusicStatus(MusicStatus.LOADING);    // now start loading
                    break;

                }

                case MyMusic.NOTI_MUSIC_INFO: { // New music info arrived, play!

                    // get information from INTENT
                    mTopTrackItem = intent.getParcelableExtra("TRACK");
                    mPosition = intent.getIntExtra("POSITION", -1);
                    mTrackMax = intent.getIntExtra("TRACK_MAX", 0);
                    String status = intent.getStringExtra("MUSIC_STATUS");
                    setMusicStatus(MusicStatus.valueOf(status));

                    if (mTopTrackItem != null) // if not error
                    {
//                        setMusicStatus(MusicStatus.READY);
                        // now play
                        setNewMusicInfoScreen();

                    } else
                        setMusicStatus(MusicStatus.ERROR_NO_DATA);

                    // just asking?? what??? --> 더해야 한다
                    //imgBtnShare.setClickable(true);

                    break;
                }

                case MyMusic.NOTI_MUSIC_NODATA: {
                    // invalid music data get....
                    Toast.makeText(getActivity(), getResources().getText(R.string.msg_system_error), Toast.LENGTH_SHORT).show();
                    break;
                }

                case MyMusic.NOTI_MUSIC_NEXT_NODATA: { // this is last music~
                    Toast.makeText(getActivity(), getResources().getText(R.string.msg_last_track), Toast.LENGTH_SHORT).show();
                    imgBtnShare.setClickable(true);
                    // Toast.makeText(getActivity(), "NOTI_PAUSE_PLAYING", Toast.LENGTH_SHORT).show();  // debug_hangulo
                    break;
                }


                case MyMusic.NOTI_MUSIC_PREV_NODATA: { // this is first music
                    //setMusicStatus(MyMusic.MusicStatus.PAUSE);
                    //changePlayButtonImg(TO_PLAY_BUTTON);
                    //Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT).show();  // debug_hangulo
                    imgBtnShare.setClickable(true);
                    Toast.makeText(getActivity(), getResources().getText(R.string.msg_first_track), Toast.LENGTH_SHORT).show();
                    break;
                }

                case MyMusic.NOTI_PAUSE_PLAYING: { // if start playing
                    setMusicStatus(MyMusic.MusicStatus.PAUSE);
                    changePlayButtonImg(TO_PLAY_BUTTON);
                    // Toast.makeText(getActivity(), "NOTI_PAUSE_PLAYING", Toast.LENGTH_SHORT).show();  // debug_hangulo

                    break;
                }

                case MyMusic.NOTI_RESUME_PLAYING: { // if start playing
                    setMusicStatus(MusicStatus.PLAYING);
                    changePlayButtonImg(TO_PAUSE_BUTTON);
                    break;
                    //Toast.makeText(getActivity(), "NOTI_RESUME_PLAYING", Toast.LENGTH_SHORT).show();  // debug_hangulo
                }
                case MyMusic.NOTI_MUSIC_STATUS: { // if start playing
                    String tStatus = intent.getStringExtra("MUSIC_STATUS");
                    if (tStatus != null) {
                        setMusicStatus(MusicStatus.valueOf(tStatus));
                    }

                    // now set sometings
                    if(mStatus == MusicStatus.PLAYING) {
                        // now playing
                        loadingCircle.setVisibility(View.INVISIBLE);
                        changePlayButtonImg(TO_PAUSE_BUTTON);
                        setButtonClickableAlpha(imgBtnPlay, true);
                        mSeekBar.setEnabled(true);
                    }

                    break;

                }
                case MyMusic.NOTI_SEEKBAR_INFO: {
                    setMusicStatus(MyMusic.MusicStatus.PLAYING);
                    mSeekBarProgress = intent.getIntExtra("SEEK_PROGRESS", 0);
                    mSeekBarMax = intent.getIntExtra("SEEK_MAX", 0);
                    updateSeekBar(); // update seekbar UI
                    break;
                }

                case MyMusic.NOTI_REWIND_PLAYING:
                case MyMusic.NOTI_STOP_PLAYING: {// if quit playing

                    //runCallBackMusicStop(); // turn off "Now Playing" icon

                    setMusicStatus(MusicStatus.STOP);
                    changePlayButtonImg(TO_PLAY_BUTTON);

                    setMusicStatus(MusicStatus.REWIND);
                    changePlayButtonImg(TO_PLAY_BUTTON);

                    mSeekBar.setProgress(0); // reset
                    txtViewCurrent.setText(getFormattedTimeFromMillis(0));// reset

                    // Toast.makeText(getActivity(), "NOTI_END_PLAYING", Toast.LENGTH_SHORT).show();  // debug_hangulo

                    break;
                }

                case MyMusic.NOTI_SEEKTO_OK: { // if start playing
                    //Toast.makeText(getActivity(), "SEEKTO_OK", Toast.LENGTH_SHORT).show(); // debug_hangulo

                    break;
                }

            }
        }
    }
}

/**End of Program*********************************************************************************************/

/*

    // remove "now playing" icon on actionbar
    void runCallBackMusicStop() {

        PlayerCallback activity = (PlayerCallback)getActivity();
        if (activity != null)
            activity.onMusicStop();
    }

 // set "now playing" icon on actionbar
    void runCallBackMusicResume() {

        PlayerCallback activity = (PlayerCallback)getActivity();
        if (activity != null)
            activity.onMusicPlay();
    }


    void runCallBackMusicPlay() {

        PlayerCallback activity = (PlayerCallback)getActivity();
        if (activity != null)
            activity.onMusicPlay();
    }

    void runCallBackMusicSelect(int position) {

        PlayerCallback activity = (PlayerCallback)getActivity();
        if (activity != null)
            activity.onMusicSelect(position);
    }

    */





/* NOT USED THINGS --------------------------------------------------------------------------------


    private Runnable UpdateSongTime = new Runnable() {
        public void run() {

            if (isPlaying()) {
                mSeekProgress = 0; //getCurrentPosition();  hangulo
                mSeekBar.setProgress((int) mSeekProgress); // only update
                txtViewCurrent.setText(getFormattedTimeFromMillis(mSeekProgress));
            }
                //myHandler.postDelayed(this, 500);
        }


    };

*/

// for Music service
// for binding
//MyMusicService mService;
//boolean mBound = false;
//MyMusicService.MyMusicServiceBinder mBinder;

/** Defines callbacks for service binding, passed to bindService() */

    /* not used
    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Because we have bound to an explicit
            // service that is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mBinder = (MyMusicService.MyMusicServiceBinder) service;
            mService = mBinder.getService();
            mBound = true;
        }

        // Called when the connection with the service disconnects unexpectedly
        public void onServiceDisconnected(ComponentName className) {
            Log.e(LOG_TAG, "onServiceDisconnected");
            mBound = false;
        }
    };

*/    // call from parent (or outside fragment)
/* not used
    public void startNewMusic(ArrayList<TopTracksItem> topTracks, String artistName, int position)
    {
        mTopTracks = topTracks;
        mArtistName = artistName;
        mPosition = position;


        startNewMusic();
    }
*/

        /* (on Start)
        // Bind to Music service
        Intent intent = new Intent(getActivity(), MyMusicService.class);

        intent.putParcelableArrayListExtra("TOP_TRACKS", mTopTracks);
        intent.putExtra("POSITION", mPosition);
        intent.putExtra("ARTIST_NAME", mArtistName);

        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        */

/**
 * FragmentCallback for when an item has been selected. (NOW, use broadcast)
 */

    /*
    public interface PlayerCallback {
        public void onMusicPlay();

        public void onMusicStop();

        public void onMusicSelect(int position);
        // on pause
    }
*/
/*
    // stop music & call back
    void stopMusicService()
    {
        if(!isPlaying()) return;
        startMySerivce(MyMusic.ACTION_STOP);
       // runCallBackMusicStop(); // remove "now playing" icon on actionbar
    }


    public void playNextMusicService() {
        getNextMusicinfo();
        return;
    }

    public void playPrevMusicService() {
        getPrevMusicinfo();
        return;
    }
*/
