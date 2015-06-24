package com.hangulo.spotifystreamer;

/**
 *  SpotifyStreamer, Stage 2
 *  ------------------------
 *  Kwanghyun JUNG
 *  24th JUN 2015
 *
  *
 * Constants Broadcast & music service
 */
public class MyMusic {
    public static final String NOTI_NOW_PLAYING = "NOW_PLAYING"; // braodcast message
    public static final String NOTI_NOW_LOADING = "NOW_LOADING"; // braodcast message
    public static final String NOTI_RESUME_PLAYING = "RESUME_PAYING";
    public static final String NOTI_STOP_PLAYING = "STOP_PLAYING";
    public static final String NOTI_REWIND_PLAYING = "REWIND_PLAYING";
    public static final String NOTI_PAUSE_PLAYING = "PAUSE_PAYING";
    public static final String NOTI_SEEKTO_OK = "SEEKTO_OK";
    public static final String NOTI_SEEKBAR_INFO = "SEEKBAR_INFO";
    public static final String NOTI_LOADLIST_ERROR = "LOADLIST_ERROR"; // list data loading error
    public static final String NOTI_LOADLIST_OK = "LOADLIST_OK";
    public static final String NOTI_MUSIC_INFO = "MUSIC_INFO_NOW";
    public static final String NOTI_MUSIC_NODATA = "MUSIC_INFO_NODATA";
    public static final String NOTI_MUSIC_STATUS = "MUSIC_STATUS";

    public static final String NOTI_MUSIC_NEXT_NODATA = "MUSIC_INFO_NEXT_NODATA";
    public static final String NOTI_MUSIC_PREV_NODATA = "MUSIC_INFO_PREV_NODATA";

    public static final String BROADCAST = "com.hangulo.spotifystreamer.BROADCAST";
    public static final String BROADCAST_MSG_TAG = "MYMUSIC_MSG";

    // actions
    public static final String ACTION_PLAY = "com.hangulo.spotifystreamer.action.PLAY";
    public static final String ACTION_LOADLIST = "com.hangulo.spotifystreamer.action.LOADLIST"; // load new tracks lists
    public static final String ACTION_NEW = "com.hangulo.spotifystreame.action.NEW";
    public static final String ACTION_PAUSE ="com.hangulo.spotifystreamer.action.PAUSE";
    public static final String ACTION_RESUME = "com.hangulo.spotifystreamer.action.RESUME";
    public static final String ACTION_STOP = "com.hangulo.spotifystreamer.action.STOP";
    public static final String ACTION_SEEKTO = "com.hangulo.spotifystreamer.action.SEEKTO";
    public static final String ACTION_ISPLAYING = "com.hangulo.spotifystreamer.action.ISPLAYING";
    public static final String ACTION_SYNC_STATUS= "com.hangulo.spotifystreamer.action.SYNCSTATUS";


    public static final String ACTION_GET_MUSIC_NOW = "com.hangulo.spotifystreamer.action.ACTION_GET_MUSIC_NOW ";
    public static final String ACTION_GET_MUSIC_NEXT = "com.hangulo.spotifystreamer.action.ACTION_GET_MUSIC_NEXT";
    public static final String ACTION_GET_MUSIC_PREV = "com.hangulo.spotifystreamer.action.ACTION_GET_MUSIC_PREV";
    public static final String ACTION_REMOVE_NOTIFICATION = "com.hangulo.spotifystreamer.action.ACTION_REMOVE_NOTIFICATION";


    // music status
    public enum MusicStatus {
        PLAYING, // now playing
        PAUSE,
        STOP,
        REWIND,
        END,
        LOADING, // now loading start
        LOADING_LIST, // data is loaded but song streaming is not ready
        READY, // data & song streaming is ready
        ERROR, // get some error
        ERROR_NO_DATA // there is no fetched data
    }

}

