package com.hangulo.spotifystreamer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import android.util.Log;
import android.os.Handler;

import com.hangulo.spotifystreamer.MyMusic.MusicStatus;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;


/**
 *  SpotifyStreamer, Stage 2
 *  ------------------------
 *  Kwanghyun JUNG
 *  24th JUN 2015
 *
 *  Music playing service
 *
 */

public class MyMusicService extends Service implements MediaPlayer.OnPreparedListener,MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnErrorListener {

    // Variables for seekbar processing
    int mSeekBarProgress; // currnet

    int mSeekBarMax; // the length of music
    private final Handler mHandler = new Handler();
    Intent mBroadIntent; // Broadcast intent
    Intent mBroadSeekBarIntent; // Broadcast intent
    Intent mBroadMusicIntent;

    static final int NOTIFICATION_ID=10;
    MediaPlayer mMediaPlayer = null;

    TopTracksItem mTrackItem; // temp
    ArrayList<TopTracksItem> mTopTracks;
    int mPosition;
    int mTrackMax;

    String mArtistName;
    MusicStatus mStatus= MusicStatus.STOP; // current status

    private static final String LOG_TAG = MyMusicService.class.getSimpleName();

    MediaSessionCompat mMediaSessionCompat;
    MediaControllerCompat mMediaControllerCompat;
    NotificationManagerCompat mNotificationManagerCompat;

   // LocalBroadcastManager mlocalBroadCastManager = LocalBroadcastManager.getInstance(this);
    @Override
    public void onCreate() {
        super.onCreate();

        mMediaPlayer = new MediaPlayer();

        mBroadIntent = new Intent(MyMusic.BROADCAST);
        mBroadSeekBarIntent = new Intent(MyMusic.BROADCAST);
        mBroadMusicIntent = new Intent(MyMusic.BROADCAST);

        setListeners();
        initMediaSessionsCompat(); // for notification & lock screen service

        Log.v(LOG_TAG, "Serivce: onCreate()");
    }

    // set MediaPlayer Listeners
    private void setListeners()
    {
        if (mMediaPlayer == null) return;

        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnSeekCompleteListener(this); // Seekbar
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent==null) return START_STICKY;

        String action = intent.getAction();

        if(action == null) return START_STICKY;

        switch(action) {

            case MyMusic.ACTION_NEW :  // make new service
                // DO NOTHING
                break;

            case MyMusic.ACTION_LOADLIST :  // Load new list (after select new artist)

                mTopTracks = intent.getParcelableArrayListExtra("TOP_TRACKS");
                mArtistName = intent.getStringExtra("ARTIST_NAME");
                mPosition = intent.getIntExtra("POSITION", 0);

                if (mTopTracks == null) // system error
                {
                    setMusicStatus(MusicStatus.ERROR_NO_DATA); // error
                    sendBroadcastMessage(MyMusic.NOTI_LOADLIST_ERROR); // error send
                } else {
                    setMusicStatus(MusicStatus.READY);
                    sendBroadcastMessage(MyMusic.NOTI_LOADLIST_OK); // okay
                }
                mTrackMax = mTopTracks.size() - 1; // last track no (start from 0)

                break;

            case MyMusic.ACTION_GET_MUSIC_NOW :
                mTrackItem = mTopTracks.get(mPosition); // get item

                if (mTrackItem == null)
                    sendBroadcastMessage(MyMusic.NOTI_MUSIC_NODATA); // invalid

                sendBroadcastMusicInfo(mBroadMusicIntent, MyMusic.NOTI_MUSIC_INFO, mTrackItem, mArtistName, mPosition, mTrackMax);
                startNewMusic(mTrackItem, mPosition);  // now play

                break;


            case MyMusic.ACTION_GET_MUSIC_NEXT :  // next play

                if (mPosition < mTrackMax) {
                    mPosition++;

                    mTrackItem = mTopTracks.get(mPosition); // get item

                    if (mTrackItem == null)
                        sendBroadcastMessage(MyMusic.NOTI_MUSIC_NODATA); // invalid

                    sendBroadcastMusicInfo(mBroadMusicIntent, MyMusic.NOTI_MUSIC_INFO, mTrackItem, mArtistName, mPosition, mTrackMax);
                    mMediaControllerCompat.getTransportControls().skipToNext();// callback

                    startNewMusic(mTrackItem, mPosition);

                } else
                    sendBroadcastMessage(MyMusic.NOTI_MUSIC_NEXT_NODATA);

                break;

            case MyMusic.ACTION_GET_MUSIC_PREV : { // prev play
                if (mPosition > 0) {
                    mPosition--;

                    mTrackItem = mTopTracks.get(mPosition); // get item

                    if (mTrackItem == null)
                        sendBroadcastMessage(MyMusic.NOTI_MUSIC_NODATA); // invalid

                    sendBroadcastMusicInfo(mBroadMusicIntent, MyMusic.NOTI_MUSIC_INFO, mTrackItem, mArtistName, mPosition, mTrackMax);
                    mMediaControllerCompat.getTransportControls().skipToPrevious();// callback

                    startNewMusic(mTrackItem, mPosition);

                } else
                    sendBroadcastMessage(MyMusic.NOTI_MUSIC_PREV_NODATA);

                break;
            }

            case MyMusic.ACTION_PLAY :
                resumeMusic();
                break;


            case MyMusic.ACTION_PAUSE :

                if (mStatus.equals(MusicStatus.PLAYING)) { // if now playing
                    pauseMusic();
                    mMediaControllerCompat.getTransportControls().pause();
                }
                break;


            case MyMusic.ACTION_RESUME :
                resumeMusic();
                mMediaControllerCompat.getTransportControls().play();// callback

                break;


            case MyMusic.ACTION_STOP :  // only for destroy notification

                stopMusic();
                mMediaControllerCompat.getTransportControls().stop();// callback
                Log.v(LOG_TAG, "Serivce: ACTION_STOP");
                break;


            case MyMusic.ACTION_SEEKTO :  // seek mode
                int i = intent.getIntExtra("SEEK_TO", 0);
                seekToMusic(i);

                break;


            case  MyMusic.ACTION_ISPLAYING :  // check status & information
                if (isPlayingMusic()) {

                    if (mTrackItem == null) break;

                    sendBroadcastMessageMusicStatus(getMusicStatus());

                    sendBroadcastMusicInfo(mBroadMusicIntent, MyMusic.NOTI_MUSIC_INFO, mTrackItem, mArtistName, mPosition, mTrackMax);
                    sendBroadcastMessageNowPlaying(mPosition); // send broadcast message to Player activity
                }
                else if (mStatus != MusicStatus.LOADING)
                    sendBroadcastMessageMusicStatus(getMusicStatus());
                if(mTrackItem != null)
                    sendBroadcastMusicInfo(mBroadMusicIntent, MyMusic.NOTI_MUSIC_INFO, mTrackItem, mArtistName, mPosition, mTrackMax);


                break;



            case MyMusic.ACTION_SYNC_STATUS :
                sendBroadcastMessageMusicStatus(getMusicStatus()); // send broadcast message to Player activity
                break;

            case MyMusic.ACTION_REMOVE_NOTIFICATION :
                if (mNotificationManagerCompat == null)
                    mNotificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
                //mNotificationManagerCompat.cancel(NOTIFICATION_ID);
                stopForeground(true);
                break;

        }

        return  START_STICKY ;
    }

    public IBinder onBind(Intent intent) {

        throw new UnsupportedOperationException("Not yet implemented");
    }

    private android.support.v7.app.NotificationCompat.Action generateActionCompat( int icon, String title, String intentAction ) {
        Intent intent = new Intent( getApplicationContext(), MyMusicService.class );
        intent.setAction( intentAction );
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder( icon, title, pendingIntent ).build();
    }



    /* ---------------------------------------------------------------------

        for lower version compatible

        https://developer.android.com/reference/android/support/v7/app/NotificationCompat.MediaStyle.html
        * Ian Lake's Google plus
          https://plus.google.com/+IanLake/posts/AS2HUUY8mKN

        Below Lollipop : only notification supported. (not available lockscreen mode)
        From Lollipop : notification & lockscreen notification is supported


       //help of :  https://github.com/karanbalkar/AndroidMediaStyleNotifications

      ---------------------------------------------------------------------*/


    private void initMediaSessionsCompat() {

        ComponentName myEventReceiver = new ComponentName(getPackageName(), MediaButtonEventReceiver.class.getName());

        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);

        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), "MyMusicService",myEventReceiver, pendingIntent);

        try {
            mMediaControllerCompat = new MediaControllerCompat(getApplicationContext(), mMediaSessionCompat.getSessionToken());
        } catch (RemoteException ex) {
            Log.e(LOG_TAG, "Error occurred :RemoteException / initMediaSessionsCompat() ");
        }


        mMediaSessionCompat.setActive(true);
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                Log.e(LOG_TAG, "onPlay");
                //showNotificationL(generateAction(android.R.drawable.ic_media_pause, "Pause", MyMusic.ACTION_PAUSE));
                showNotificationCompat(true);
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.e(LOG_TAG, "onPause");
                //showNotificationL(generateAction(android.R.drawable.ic_media_play, "Play", MyMusic.ACTION_RESUME));
                showNotificationCompat(false);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                Log.e(LOG_TAG, "onSkipToNext");
                //showNotificationL(generateAction(android.R.drawable.ic_media_pause, "Pause", MyMusic.ACTION_PAUSE));
                showNotificationCompat(true);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                Log.e(LOG_TAG, "onSkipToPrevious");
                //showNotificationL(generateAction(android.R.drawable.ic_media_pause, "Pause", MyMusic.ACTION_PAUSE));
                showNotificationCompat(true);
            }

            @Override
            public void onFastForward() {
                //   super.onFastForward();
                //    Log.e(LOG_TAG, "onFastForward");
            }

            @Override
            public void onRewind() {
                super.onRewind();
                showNotificationCompat(false);
                //  super.onRewind();
                //  Log.e(LOG_TAG, "onRewind");
            }

            @Override
            public void onStop() {
                super.onStop();
                Log.e(LOG_TAG, "onStop");
                //NotificationManagerCompat notificationManager = (NotificationManagerCompat) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManagerCompat =
                        NotificationManagerCompat.from(getApplicationContext());
                mNotificationManagerCompat.cancel(NOTIFICATION_ID);
                Intent intent = new Intent(getApplicationContext(), MyMusicService.class);
                stopService(intent);
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }


        });

    }

    /*
        http://stackoverflow.com/questions/26932457/lollipop-notification-setvisibility-does-not-work

    ** lockscreen options only for below circumstance

    1) above Lollipop version

    2) Hide sensitive notification content respects the new visibility types.

     */

    //lock screen option
    public  int getNotificationOption(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean  enable=prefs.getBoolean(context.getString(R.string.pref_enable_notifications_key), false);

        if (enable) // if enable
            return NotificationCompat.VISIBILITY_PUBLIC;

        else
            return NotificationCompat.VISIBILITY_SECRET;
    }


    void showNotificationCompat(final boolean play_pause) {

        if (mTrackItem == null) return;
        String url = mTrackItem.getThumb_small_url();

        Picasso.with(this)
                .load(url)
                .resizeDimen(android.R.dimen.notification_large_icon_width, android.R.dimen.notification_large_icon_width)
                .centerCrop()
                .into(new Target() {


                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) { // ok, bitmap is loaded

                        Context ctx = getApplicationContext();
                        NotificationCompat.Action action;
                        int visibility;

                        if (ctx == null) return;

                        if (play_pause)
                            action = generateActionCompat(android.R.drawable.ic_media_pause, "Pause", MyMusic.ACTION_PAUSE);
                        else
                            action = generateActionCompat(android.R.drawable.ic_media_play, "Play", MyMusic.ACTION_RESUME);

                        visibility = getNotificationOption(ctx); // get visibility //  NotificationCompat.VISIBILITY_PUBLIC or NotificationCompat.VISIBILITY_SECRET
                        presentNotification(ctx, bitmap, action, visibility);
                        //startForeground(NOTIFICATION_ID,  builder.build());
                    }

                    @Override
                    public void onBitmapFailed(Drawable drawable) {
                    }

                    @Override
                    public void onPrepareLoad(Drawable drawable) {
                    }
                });

    }


    private void presentNotification(Context ctx, Bitmap bitmap, NotificationCompat.Action action, int visibility) {

        Intent notificationIntent = new Intent(ctx, PlayerFragment.class); // 여긴 좀 고쳐야겠다.... 뭔지도 모르고...
        PendingIntent contentIntent = PendingIntent.getActivity(ctx,
                1
                , notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intent = new Intent(getApplicationContext(), MyMusicService.class);
            intent.setAction(MyMusic.ACTION_REMOVE_NOTIFICATION); // remove notification
            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0); // for lower version


        android.support.v7.app.NotificationCompat.Builder builder = new android.support.v7.app.NotificationCompat.Builder(ctx);

        android.support.v7.app.NotificationCompat.MediaStyle style = new android.support.v7.app.NotificationCompat.MediaStyle();
        style.setShowActionsInCompactView(0, 1, 2); // PREV / PLAY / NEXT
        style.setShowCancelButton(true);
        style.setCancelButtonIntent(pendingIntent); // remove notification only


        builder.setVisibility(visibility) // set visibility (from shared preference)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setShowWhen(false)
                        // Add media control buttons that invoke intents in your media service
                .addAction(generateActionCompat(android.R.drawable.ic_media_previous, "Previous", MyMusic.ACTION_GET_MUSIC_PREV))
                .addAction(action)
                .addAction(generateActionCompat(android.R.drawable.ic_media_next, "Next", MyMusic.ACTION_GET_MUSIC_NEXT))
                .setStyle(style)
                .setContentTitle(mTrackItem.getTrack_name())
                .setContentText(mTrackItem.getArtist_name())
                .setLargeIcon(bitmap)
                .setContentIntent(contentIntent);

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getApplicationContext());

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
              sendBroadcastMessage(MyMusic.NOTI_STOP_PLAYING); // send broadcast message to Player activity
              setMusicStatus(MyMusic.MusicStatus.END);
              mMediaPlayer.release();
              mMediaPlayer = null;

            Log.d(LOG_TAG, "Service : onDestroy / stop playing");
        }

        // notification

         stopForeground(true);
         if(mMediaSessionCompat != null)
                mMediaSessionCompat.release();

        // cancel the notification
        // Stop the seekbar handler (updates to UI)
        mHandler.removeCallbacks(sendUpdatesToUI);

    }

    public MediaPlayer getNewMediaPlayer()
    {

        mMediaPlayer = new MediaPlayer();

        setListeners(); // setting mMediaplayers listeners
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        return mMediaPlayer;
    }


    public void startNewMusic(TopTracksItem myItem, final int pos) // start new position
    {

        String myURL="";
        // set up screen
        if(isPlayingMusic())
            stopMusic();

        // start music
        setMusicStatus(MyMusic.MusicStatus.LOADING); // now loading

        // noti to loading start --> share button off, show loading prgress bar
        sendBroadcastMessage(MyMusic.NOTI_NOW_LOADING); // send broadcast message to Player activity

        if(myItem != null)
            myURL=myItem.getPreview_url();
        if (myURL == null) return;

        Uri myUri = Uri.parse(myURL); // initialize Uri here

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        mMediaPlayer=getNewMediaPlayer();
        try {
                mMediaPlayer.setDataSource(this, myUri);//not!!! (getApplicationContext(), myUri);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error occurred while I/O exception.");
            return;
        }
        mMediaPlayer.prepareAsync();

    }


    // MediaPlayer.OnPreparedListener
    @Override
    public void onPrepared(MediaPlayer player) {


        if (mMediaPlayer == player) {

            sendBroadcastMessageNowPlaying(mPosition); // send broadcast message to Player activity
            setMusicStatus(MusicStatus.READY);
            startMusic();
            showNotificationCompat(true); // call compat
        }

    }


    // MediaPlayer.OnCompletionListener
    @Override
    public void onCompletion(MediaPlayer player) {
        if (mMediaPlayer == player) {
            rewindMusic();
            mMediaControllerCompat.getTransportControls().rewind();// callback
        }
    }


    // MediaPlayer.OnErrorListener
    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {
        Log.e(LOG_TAG, "Error occurred while playing audio.");

        if(player.isPlaying())
            player.stop();
        player.release();
        setMusicStatus(MusicStatus.ERROR);

        mMediaPlayer = null;
        return false;
    }



    void stopMusic()
    {
        if(mMediaPlayer == null) return;
        if  (mStatus==MyMusic.MusicStatus.STOP) return;

        try {
            if (!mMediaPlayer.isPlaying()) return;
        } catch  (IllegalStateException e) // exception
        {
            Log.e(LOG_TAG, "Error occurred :IllegalStateException / stopMusic() ");
            return;
        }

        mMediaPlayer.stop();
        mMediaPlayer.release();
        mHandler.removeCallbacks(sendUpdatesToUI);
        sendBroadcastMessage(MyMusic.NOTI_STOP_PLAYING); // send broadcast message to Player activity
        setMusicStatus(MyMusic.MusicStatus.STOP);
        Log.v(LOG_TAG, "Sevice. stopMusic() is called ");
    }


    // when music play to end (30s)
    public void rewindMusic() {

        if (isPlayingMusic())
            mMediaPlayer.pause();

        seekToMusic(0);
        setMusicStatus(MyMusic.MusicStatus.REWIND);
        mHandler.removeCallbacks(sendUpdatesToUI);
        sendBroadcastMessage(MyMusic.NOTI_REWIND_PLAYING);
    }


    public void resumeMusic(){

        if(mMediaPlayer == null) return;

        mMediaPlayer.start();

        setMusicStatus(MyMusic.MusicStatus.PLAYING);
        setupHandler();

        sendBroadcastMessage(MyMusic.NOTI_RESUME_PLAYING);
    }

    public void startMusic() {
        if(mMediaPlayer == null) return;


        if(mStatus.equals(MusicStatus.ERROR_NO_DATA) || mStatus.equals(MusicStatus.LOADING))
            return;

        setupHandler();
        mMediaPlayer.start();
        setMusicStatus(MyMusic.MusicStatus.PLAYING);

    }

    public void pauseMusic() {
        if(mMediaPlayer == null) return;

        if(!mStatus.equals(MusicStatus.PLAYING)) { // if not now playing
            return;
        }

        if (isPlayingMusic()) {
            mMediaPlayer.pause();


        setMusicStatus(MyMusic.MusicStatus.PAUSE);
        mHandler.removeCallbacks(sendUpdatesToUI);
        sendBroadcastMessage(MyMusic.NOTI_PAUSE_PLAYING);
        }
    }

    // get current status (from outside of this service)
    public MusicStatus getMusicStatus() {

        return mStatus;
    }

    void setMusicStatus(MyMusic.MusicStatus status) {

        mStatus = status;

    }


    public void seekToMusic(int i) {

        if (mMediaPlayer != null)
            mMediaPlayer.seekTo(i);

    }

    public boolean isPlayingMusic() {
        boolean ret;
        if (mMediaPlayer == null) return false;
        try {
            ret = mMediaPlayer.isPlaying();
            return ret;

        } catch  (IllegalStateException e) // exception
        {
            Log.e(LOG_TAG, "Error occurred :IllegalStateException / isPlayingMusic() ");

            return false;
        }
    }

    public int getDuration() {
        if (mMediaPlayer == null) return -1;
        return mMediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        if (mMediaPlayer == null) return 0;
        if (isPlayingMusic())
            return mMediaPlayer.getCurrentPosition();
        else
            return 0;
    }


    // --------------- seek bar ------------------
    private void setupHandler() {
        mHandler.removeCallbacks(sendUpdatesToUI);
        mHandler.postDelayed(sendUpdatesToUI, 1000) ; // every 1 second
    }


    private Runnable sendUpdatesToUI = new Runnable () {

        public void run () {
            LogMediaPosition();
            mHandler.postDelayed(this,1000); // every 1 second
        }

    };


    // seekbar :Help of this.  https://www.youtube.com/watch?v=tC3FUF47tlo&list=PL14AA2548E3C96B50&index=8
    private void LogMediaPosition() {

        if (isPlayingMusic()) // if now playing
        {
            mSeekBarProgress = getCurrentPosition(); // get current position
            mSeekBarMax =getDuration(); // get max length of music
            sendBroadcastSeekbarMessage(mBroadSeekBarIntent, mSeekBarProgress, mSeekBarMax);
        }
    }

    // MediaPlayer.OnSeekCompleteListener
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        sendBroadcastMessage(MyMusic.NOTI_SEEKTO_OK); // send broadcast message to Player activity
    }

    // http://stackoverflow.com/questions/8802157/how-to-use-localbroadcastmanager
    // Send an Intent with an action named "custom-event-name". The Intent sent should
    // be received by the ReceiverActivity.
    private void sendBroadcastMessage(String message) {
        sendBroadcastMessage(mBroadIntent, message);
    }

    private void sendBroadcastMessage(Intent intent, String message) {

        // You can also include some extra data.
        intent.putExtra(MyMusic.BROADCAST_MSG_TAG, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(LOG_TAG, "Broadcast:" + message);
    }

    private void sendBroadcastSeekbarMessage(Intent seekIntent, int seekProgress, int seekMax) {

        // You can also include some extra data.
        seekIntent.putExtra(MyMusic.BROADCAST_MSG_TAG, MyMusic.NOTI_SEEKBAR_INFO);
        seekIntent.putExtra("SEEK_PROGRESS", seekProgress);
        seekIntent.putExtra("SEEK_MAX", seekMax);
        LocalBroadcastManager.getInstance(this).sendBroadcast(seekIntent);
        //Log.d(LOG_TAG, "Broadcast: Seekbar" + message);
    }


    private void sendBroadcastMusicInfo(Intent intent, String Message, TopTracksItem item,String artist, int newPos, int max) {

        intent.putExtra(MyMusic.BROADCAST_MSG_TAG, Message);
        intent.putExtra("TRACK",item);
        intent.putExtra("POSITION", newPos);
        intent.putExtra("TRACK_MAX",max);
        intent.putExtra("MUSIC_STATUS", mStatus.toString()); // enum to strig

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(LOG_TAG, "Broadcast: sendBroadcastMusicInfo");
    }

    private void sendBroadcastMessageNowPlaying(int newPos) {// send broadcast message to Player activity

        Intent intent = new Intent(MyMusic.BROADCAST);
        intent.putExtra(MyMusic.BROADCAST_MSG_TAG, MyMusic.NOTI_NOW_PLAYING);
        intent.putExtra("POSITION", newPos);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Log.d(LOG_TAG, "Broadcast: sendBroadcastMessageNowPlaying()");
        //Log.d(LOG_TAG, "Broadcast: Seekbar" + message);
    }

    private void sendBroadcastMessageMusicStatus(MusicStatus myStatus) {// send broadcast message to Player activity

        Intent intent = new Intent(MyMusic.BROADCAST);
        intent.putExtra(MyMusic.BROADCAST_MSG_TAG, MyMusic.NOTI_MUSIC_STATUS);
        intent.putExtra("MUSIC_STATUS", myStatus.toString()); // enum to strig
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(LOG_TAG, "Broadcast: sendBroadcastMessageMusicStatus()");
    }


//    private RemoteControlClient mRemoteControlClient;
    private void RegisterRemoteClient() {
    }
        class MediaButtonEventReceiver extends BroadcastReceiver
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                // nothing
            }
        }
}
/* ------------------------- NOT USED -------------------------------------
        Context mContext = getApplicationContext();
        ComponentName myEventReceiver = new ComponentName(getApplicationContext(), MyMusicService.class.getName());
        AudioManager mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.registerMediaButtonEventReceiver(myEventReceiver);


        int result = mAudioManager.requestAudioFocus( this   , AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);


        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { // http://202psj.tistory.com/505
            // could not get audio focus.
            return;
        }

        // build the PendingIntent for the remote control client
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(myEventReceiver);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, mediaButtonIntent, 0);

        // create and register the remote control client
        mRemoteControlClient = new RemoteControlClient(mediaPendingIntent);

        mAudioManager.registerRemoteControlClient(mRemoteControlClient);
        mRemoteControlClient.setTransportControlFlags(
                RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                        RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                        RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                        RemoteControlClient.FLAG_KEY_MEDIA_STOP |
                        RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                        RemoteControlClient.FLAG_KEY_MEDIA_NEXT);


        RemoteControlClient.MetadataEditor mEditor = mRemoteControlClient.editMetadata(true);
        mEditor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, "RCC Artist");
        mEditor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, "RCC Title");
        mEditor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, "RCC Album");
        mEditor.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, 6039000);
        mEditor.apply();
*/



/** ===============NOT USED ====================================================================
 // Binder given to clients http://developer.android.com/guide/components/bound-services.html
 //private final IBinder mBinder = new MyMusicServiceBinder();



 * Class used for the client Binder.  Because we know this service always
 * runs in the same process as its clients, we don't need to deal with IPC.
 */

/*
    public class MyMusicServiceBinder extends Binder {
        MyMusicService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MyMusicService.this;
        }
    }
*/

/** Called when MediaPlayer is ready */
    /*public void onPrepared(MediaPlayer player) {
            player.start();
    }*/

/* bind mode --> cancel
    @Override
    public IBinder onBind(Intent intent) {

        if (intent != null) {
            mTopTracks = intent.getParcelableArrayListExtra("TOP_TRACKS");
            mArtistName = intent.getStringExtra("ARTIST_NAME");
            mPosition = intent.getIntExtra("POSITION", 0);
            if(mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        }

        return mBinder;
    }

*/

/*** Method for clients */

/*

    private Notification.Action createAction( int icon, String title, String intentAction ) {
        Intent intent = new Intent( getApplicationContext(), MyMusicService.class);
        intent.setAction( intentAction );
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new Notification.Action.Builder( icon, title, pendingIntent ).build();

    }

    private void buildNotification( Notification.Action action ) {
        Notification.MediaStyle style = new Notification.MediaStyle();
       // style.setMediaSession( m_objMediaSession.getSessionToken() );

        Intent intent = new Intent( getApplicationContext(), MyMusicService.class );
        intent.setAction( SyncStateContract.Constants.ACTION_STOP );
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle( "Sample Title" )
                .setContentText( "Sample Artist" )
                .setDeleteIntent( pendingIntent )
                .setStyle(style);


        builder.addAction( createAction( android.R.drawable.ic_media_previous, "Previous", SyncStateContract.Constants.ACTION_PREVIOUS ) );
        builder.addAction( createAction( android.R.drawable.ic_media_rew, "Rewind", SyncStateContract.Constants.ACTION_REWIND ) );
        builder.addAction( action );
        builder.addAction( createAction( android.R.drawable.ic_media_ff, "Fast Foward", SyncStateContract.Constants.ACTION_FAST_FORWARD ) );
        builder.addAction( createAction( android.R.drawable.ic_media_next, "Next", SyncStateContract.Constants.ACTION_NEXT ) );

        NotificationManager notificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        notificationManager.notify( 1, builder.build() );
    }



*/
/*
// for Android L only --> use Compat
    private void initMediaSessions() {


        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            initMediaSessionsCompat();
            return;
        }

        //mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mMediaSession = new MediaSession(getApplicationContext(), "sample session");
        mMediaController = new MediaController(getApplicationContext(), mMediaSession.getSessionToken());
        mMediaSession.setActive(true);
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(new Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                Log.e(LOG_TAG, "onPlay");
                //showNotificationL(generateAction(android.R.drawable.ic_media_pause, "Pause", MyMusic.ACTION_PAUSE));
                showNotificationL(true);
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.e(LOG_TAG, "onPause");
                //showNotificationL(generateAction(android.R.drawable.ic_media_play, "Play", MyMusic.ACTION_RESUME));

                showNotificationL(false);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                Log.e(LOG_TAG, "onSkipToNext");
                //showNotificationL(generateAction(android.R.drawable.ic_media_pause, "Pause", MyMusic.ACTION_PAUSE));
                showNotificationL(true);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                Log.e(LOG_TAG, "onSkipToPrevious");
                //showNotificationL(generateAction(android.R.drawable.ic_media_pause, "Pause", MyMusic.ACTION_PAUSE));
                showNotificationL(true);
            }

            @Override
            public void onFastForward() {
             //   super.onFastForward();
            //    Log.e(LOG_TAG, "onFastForward");
            }

            @Override
            public void onRewind() {
                super.onRewind();
                showNotificationL(false);
              //  super.onRewind();
              //  Log.e(LOG_TAG, "onRewind");
            }

            @Override
            public void onStop() {
                super.onStop();
                Log.e(LOG_TAG, "onStop");
                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel( 1 );
                Intent intent = new Intent( getApplicationContext(), MyMusicService.class );
                stopService( intent );
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
            }

            @Override
            public void onSetRating(Rating rating) {
               // super.onSetRating(rating);
            }
        });

    }

    */

// https://plus.google.com/+AndroidDevelopers/posts/81YUVaLAnd2

// https://github.com/karanbalkar/AndroidMediaStyleNotifications  <-- now

    /*

    for only

    void showNotificationL(final boolean play_pause)
    {


        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            showNotificationCompat(true);
            return;
        }

        String url = mTrackItem.getThumb_small_url();

        Picasso.with(this)
                .load(url)
                .error(R.drawable.noimage)
                .placeholder(R.drawable.nowloading)
                .resizeDimen(android.R.dimen.notification_large_icon_width,
                        android.R.dimen.notification_large_icon_width)
                .centerCrop()
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                        Notification.MediaStyle style = new Notification.MediaStyle();
                        //style.setMediaSession( mMediaSession.getSessionToken() );
                        style.setShowActionsInCompactView(0, 1, 2);

                        Notification.Action action;

                        if(play_pause)
                            action=generateAction(android.R.drawable.ic_media_pause, "Pause", MyMusic.ACTION_PAUSE);
                        else
                            action = generateAction(android.R.drawable.ic_media_play, "Play", MyMusic.ACTION_RESUME);


                        Intent intent = new Intent( getApplicationContext(), MyMusicService.class);
                        intent.setAction( MyMusic.ACTION_STOP );
                        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);


                        Notification.Builder builder = new Notification.Builder( getApplicationContext() )
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(mTrackItem.getTrack_name())
                                .setContentText(mTrackItem.getArtist_name())
                                .setLargeIcon(bitmap)
                                .setShowWhen(false)
                                .setDeleteIntent(pendingIntent)
                                .setStyle(style);

                        builder.addAction( generateAction( android.R.drawable.ic_media_previous, "Previous", MyMusic.ACTION_GET_MUSIC_PREV ) );
                        builder.addAction( action );
                        builder.addAction( generateAction( android.R.drawable.ic_media_next, "Next", MyMusic.ACTION_GET_MUSIC_NEXT ) );

                        //NotificationManager notificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
                        //notificationManager.notify( 1, builder.build() );
                        startForeground(NOTIFICATION_ID, builder.build());
                        // main routine


                    }
                    @Override
                    public void onBitmapFailed(Drawable drawable) {
                    }

                    @Override
                    public void onPrepareLoad(Drawable drawable) {
                    }
                });
    }

*/

     /*
                        Context ctx = getApplicationContext();
                        if (ctx == null) return;
                        Intent notificationIntent = new Intent(ctx, PlayerFragment.class);
                        intent.setAction( Constants.ACTION_STOP );

                        PendingIntent contentIntent = PendingIntent.getActivity(ctx,
                                10// play music
                                , notificationIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT);

                        NotificationManager nm = (NotificationManager) ctx
                                .getSystemService(Context.NOTIFICATION_SERVICE);

                        Notification.Action action;
                        if(play_pause)
                            action=generateAction(android.R.drawable.ic_media_pause, "Pause", MyMusic.ACTION_PAUSE);
                        else
                            action = generateAction(android.R.drawable.ic_media_play, "Play", MyMusic.ACTION_RESUME);



                        // from LOLLIPOP
                        Notification notification = new Notification.Builder(ctx)
                                // show controls on lockscreen even when user hides sensitive content.
                                .setVisibility(Notification.VISIBILITY_PUBLIC)

                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setShowWhen(false)
                                        // Add media control buttons that invoke intents in your media service
                                .addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", MyMusic.ACTION_GET_MUSIC_PREV))
                                .addAction(action)
                                .addAction(generateAction(android.R.drawable.ic_media_next, "Next", MyMusic.ACTION_GET_MUSIC_NEXT))

                        // Apply the media style template
                                .setStyle(new Notification.MediaStyle()
                                        .setShowActionsInCompactView(0, 1, 2 /* #1: pause button )

                                        .setMediaSession(
                                                mMediaSession.getSessionToken()))
                                .setContentTitle(mTrackItem.getTrack_name())
                                .setContentText(mTrackItem.getArtist_name())
                                .setLargeIcon(bitmap)
                                .build();
                        // === test

                        startForeground(NOTIFICATION_ID, notification);
                        */
// https://github.com/karanbalkar/AndroidMediaStyleNotifications/blob/master/AndroidLollipopNewExamples/src/com/app/lollipop/test/MediaPlayerService.java

                    /*LOLLI
    private Notification.Action generateAction( int icon, String title, String intentAction ) {
        Intent intent = new Intent( getApplicationContext(), MyMusicService.class );
        intent.setAction( intentAction );
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new Notification.Action.Builder( icon, title, pendingIntent ).build();
    }
    */


    /* AudioManager.OnAudioFocusChangeListener
    // AudioManager.OnAudioFocusChangeListener
    // focus process
    // http://developer.android.com/guide/topics/media/mediaplayer.html#audiofocus
    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mMediaPlayer != null){
                    //initMediaPlayer();
                    if (!mMediaPlayer.isPlaying()) mMediaPlayer.start();
                    mMediaPlayer.setVolume(1.0f, 1.0f);

                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mMediaPlayer.isPlaying()) mMediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }
*/



        /* saving for next project
    // https://github.com/IntertechInc/notifications-demo/blob/master/app/src/main/java/com/intertech/notificationdemo/MainActivity.java

                      //presentNotification(Notification.VISIBILITY_PUBLIC, R.mipmap.ic_launcher, "test "test");
                        presentNotification(Notification.VISIBILITY_SECRET, R.mipmap.ic_launcher, "test", "test");
                        presentHeadsUpNotification(Notification.VISIBILITY_PUBLIC, R.mipmap.ic_launcher, "test","test");

    private void presentNotification(int visibility, int icon, String title, String text) {
        Notification notification = new NotificationCompat.Builder(this)
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(icon)
                .setAutoCancel(true)
                .setVisibility(visibility).build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(32, notification);
    }


    private void presentHeadsUpNotification(int visibility, int icon, String title, String text) {
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
        notificationIntent.setData(Uri.parse("http://www.wgn.com"));
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setCategory(Notification.CATEGORY_PROMO)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(icon)
                .setAutoCancel(true)
                .setVisibility(visibility)
                .addAction(android.R.drawable.ic_menu_view, "스트링", contentIntent)
                .setContentIntent(contentIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000}).build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(32, notification);
    }


*/


/*


    // Processing Bitmaps Off the UI Thread
    //http://developer.android.com/training/displaying-bitmaps/process-bitmap.html
    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }


* 1) http://stackoverflow.com/questions/12526228/how-to-put-media-controller-button-on-notification-bar
 * 2) http://tutorialsface.blogspot.kr/2014/07/music-player-with-notification-and-lock.html
 *
 *
 * ** lockscreen
 * 1) http://developer.android.com/reference/android/media/RemoteControlClient.html
 * 2) http://drcarter.tistory.com/m/post/150
 *
*/