package com.hangulo.spotifystreamer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;

import kaaes.spotify.webapi.android.models.Tracks;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 *  SpotifyStreamer, Stage 2
 *  ------------------------
 *  Kwanghyun JUNG
 *  24th JUN 2015
 *
 *  Show top 10 tracks Fragment
 */
public class TopTracksFragment extends Fragment
{
    private static final String LOG_TAG = TopTracksFragment.class.getSimpleName();
    private static final String SAVE_STATE_STR = "save_track";
    private static final String MEDIA_PLAYER_DIALOG = "MEDIA_PLAYER_DIALOG";
    private static final String MEDIA_PLAYER_FRAGMENT ="MEDIA_PLAYER_FRAGMENT";


    //private ShareActionProvider mShareActionProvider;
    final static String DEFAULT_IMG_URL = "noimage.jpg"; // will improve
    private TopTracksAdapter mTopTracksAdapter;
    private String mArtistId; // artist id
    private String mArtistName; // artist name
    private String mCountryCode;
    private String mCountryName;

    private ArrayList<TopTracksItem> mTopTrackItems;
    private boolean mTwoPane; //is it tablet?
    private ProgressBar mProgressBar;
    private ListView mListView;
    private int mPosition = -1;

    //LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
    public TopTracksFragment()
    {
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_toptracks, container, false);

        // Search Textfield (EditText) set
        mListView = (ListView) rootView.findViewById(R.id.listview_spotify_toptracks);
        if (mListView == null) return rootView; // system error

        /********************************************************************************/
        if (savedInstanceState != null) { // if tehre is saved data


            mTopTrackItems =   savedInstanceState.getParcelableArrayList(SAVE_STATE_STR);
            mArtistId =  savedInstanceState.getString("ARTIST_ID");
            mArtistName = savedInstanceState.getString("ARTIST_NAME");


            if (mTopTrackItems == null) // prevent error
                mTopTrackItems = new ArrayList<>(); // TopTracksItem type

            if(mTopTracksAdapter == null)
                mTopTracksAdapter = new TopTracksAdapter(getActivity(), R.layout.list_item_track, mTopTrackItems);

            else
                if(mTopTracksAdapter.getCount()>0)
                    mTopTracksAdapter.clear();

            mListView.setAdapter(mTopTracksAdapter);
            mTopTracksAdapter.addAll(mTopTrackItems);
        }
        else {

            mTopTrackItems = new ArrayList<>(); // TopTracksItem type
            mTopTracksAdapter = new TopTracksAdapter(getActivity(), R.layout.list_item_track, mTopTrackItems);
            if (mTopTracksAdapter == null)  //system error
                return rootView;

            Bundle arguments = getArguments();
            if(arguments == null ) return rootView;
            mArtistId = arguments.getString("ARTIST_ID");
            mArtistName = arguments.getString("ARTIST_NAME");

            mListView.setAdapter(mTopTracksAdapter);

        }
        /****************************/
        if (mArtistName==null || mArtistName.isEmpty()) {
            mArtistName = getResources().getText(R.string.unknown_artist).toString();
            return rootView; // for tablet
        }

        mTwoPane = getResources().getBoolean(R.bool.two_pane); // two_pane mode check http://developer.android.com/guide/topics/ui/dialogs.html

        mCountryCode = getCountryCode(getActivity()); // get country code
        mCountryName = getCountryNameFromCode(mCountryCode); // get country name

        // set subtitle
        ActionBar myActionBar =  ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (myActionBar != null) {
            myActionBar.setSubtitle(mArtistName); // set subtitle (Aritst's name)
                if(!mTwoPane) // if not tablet
                    myActionBar.setDisplayHomeAsUpEnabled(true); // set 'up icon'
        }


        mProgressBar = (ProgressBar) rootView.findViewById(R.id.toptracks_progressBar); // progress bar (circle)
        if(mProgressBar != null)
            mProgressBar.setVisibility(View.INVISIBLE); // set progress bar invisible

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TopTracksItem item = mTopTracksAdapter.getItem(position); // get item

                mPosition = position;
                //--------------------------------
                //여기서는 서비스로 "틀어라"를 보낸다

                Intent intent = new Intent(getActivity(), MyMusicService.class);
                intent.setAction(MyMusic.ACTION_LOADLIST);
                intent.putParcelableArrayListExtra("TOP_TRACKS", mTopTrackItems);

                //intent.putExtra("ARTIST_NAME", myArtistName);
                intent.putExtra("POSITION", mPosition);
                getActivity().startService(intent);


                showMediaPlayer(false); //  if click list item, then new play
            }
        });



        if (savedInstanceState == null)
            searchAndUpdateList(mArtistId);  // now Search!


        Log.v(LOG_TAG, "TopTracksFrag: onCreate()");
        return rootView;
    }

    // get country from settings
    public static String getCountryCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_country_code_key),
                context.getString(R.string.pref_country_code_default));

    }

    Map<String, String> mCountryMap=null;

    public  String getCountryNameFromCode(String countryCode) {

        if (mCountryMap == null)
            makeCountryTable();

        return mCountryMap.get(countryCode);
    }



    // make map...
    void makeCountryTable(){
        String countryName[] = getResources().getStringArray(R.array.spotify_country_code_labels);
        String countryCode[] = getResources().getStringArray(R.array.spotify_country_code_values);

        mCountryMap = new HashMap<> (countryName.length); // <String, String>

        for (int i=0; i<countryName.length; i++) {

           mCountryMap.put(countryCode[i],countryName[i]);
        }

    }


    @Override
    public void onStart() {
        super.onStart();
        startMySerivce(MyMusic.ACTION_ISPLAYING); //sync status
    }

    // start music service with ease
    void startMySerivce(String action)
    {
        Intent intent = new Intent(getActivity(), MyMusicService.class);
        intent.setAction(action);
        getActivity().startService(intent);
    }


    // play music (using dialogfragment)
    public void showMediaPlayer(boolean isResume) {

        // set transfer data
        Bundle args = new Bundle();

        if(isResume) { // is it touch menu "Now Playing"
            args.putBoolean("IS_RESUME", true);
        }

       // http://developer.android.com/guide/topics/ui/dialogs.html#FullscreenDialog
        if (mTwoPane) {
            DialogFragment newFragment = PlayerFragment.newInstance();
            newFragment.setArguments(args);

            // The device is using a large layout, so show the fragment as a dialog
            newFragment.show(getFragmentManager(), MEDIA_PLAYER_DIALOG); // make now dialog

        }  else {

            FragmentTransaction ft = getFragmentManager().beginTransaction();

            if (ft == null)
                return;

            DialogFragment newFragment = PlayerFragment.newInstance();

            newFragment.setArguments(args);
            ft.add(R.id.toptrack_container, newFragment, MEDIA_PLAYER_FRAGMENT);
            ft.addToBackStack(null);
            ft.commit();
        }



    }

    private void searchAndUpdateList(String artistId)
    {
        //FetchToptracksTask trackTask = new FetchToptracksTask();
        //trackTask.execute(artistId, mArtistName); // search top album of artistId

        SpotifyService spotifyService;
        SpotifyApi api;

        if (artistId == null) { // prevent null exception
            // errmsg (no artist name)
            Toast.makeText(getActivity(), getResources().getText(R.string.msg_system_error)+ ": 600", Toast.LENGTH_LONG).show();
            return;
        }

        if (artistId.length()<1) {
            Toast.makeText(getActivity(), getResources().getText(R.string.msg_system_error) +": 700", Toast.LENGTH_LONG).show();
            return;
        }


        Map<String,Object> hashMapUS = new HashMap<>(); // <String, Object>
        hashMapUS.put("country", mCountryCode);

        api = new SpotifyApi();
        spotifyService = api.getService();

        if(mProgressBar != null)
          mProgressBar.setVisibility(View.VISIBLE); // set progress bar visible


        spotifyService.getArtistTopTrack(artistId, hashMapUS, new Callback<Tracks>() {
            @Override
            public void success(final Tracks tracks, Response response) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (tracks == null) {// system error
                            if (!mTwoPane) {
                                //Toast.makeText(getActivity(), getResources().getText(R.string.msg_no_data_null), Toast.LENGTH_SHORT).show();
                                showMyErrorMessageDialog(getResources().getText(R.string.msg_no_data_null).toString());
                                getActivity().finish(); // close this activity. so tough :( I will improve.
                            }
                        }
                        if(tracks.tracks == null) return ; // error

                        if (tracks.tracks.size() == 0) { // there is no track

                            if (mArtistName != null) {
                                if (mTwoPane) {

                                    Toast.makeText(getActivity(), getResources().getText(R.string.msg_there_is_no_track_of) + " " + mArtistName + ". " +
                                            getResources().getText(R.string.msg_pls_search_other_artist)
                                            , Toast.LENGTH_SHORT).show();
                                } else { // if phone, then show alert dialog

                                    showMyErrorMessageDialog(getResources().getText(R.string.msg_there_is_no_track_of) + " " + mArtistName + ". " +
                                            getResources().getText(R.string.msg_pls_search_other_artist));

                                }

                            }
                        }


                        ArrayList<TopTracksItem> retList = new ArrayList<>(tracks.tracks.size()); // make return items

                        for (Track item : tracks.tracks) {
                            TopTracksItem myItem;

                            String SpotifyUrl=item.external_urls.get("spotify"); // get Spotify URL for sharing link
                            if (SpotifyUrl == null) SpotifyUrl = "http://open.spotify.com/"; // default

                            switch (item.album.images.size()) { // number of images (sorted from big size) --> I will improve ( the method select big biggest size & small size)
                                case 0:
                                    myItem = new TopTracksItem(mArtistName, item.name, item.album.name, DEFAULT_IMG_URL, item.preview_url, SpotifyUrl); // has no thumbnail
                                    break;
                                case 1:
                                    myItem = new TopTracksItem(mArtistName,item.name, item.album.name, item.album.images.get(0).url, item.preview_url,SpotifyUrl);
                                    break;
                                case 2:
                                    myItem = new TopTracksItem(mArtistName, item.name, item.album.name, item.album.images.get(0).url, item.album.images.get(1).url, item.preview_url, SpotifyUrl);
                                    break;
                                default:
                                    myItem = new TopTracksItem(mArtistName, item.name, item.album.name, item.album.images.get(0).url, item.album.images.get(2).url, item.preview_url, SpotifyUrl); // large & small thumbnaul
                                    break;
                            }
                            Log.v(LOG_TAG, myItem.toString());
                            retList.add(myItem);
                        }
                        if (mTopTracksAdapter != null) {
                            mTopTracksAdapter.clear();
                            mTopTracksAdapter.addAll(retList);
                            mTopTracksAdapter.notifyDataSetChanged(); // update view
                        }
                        if (mProgressBar != null)
                            mProgressBar.setVisibility(View.INVISIBLE); // set progress bar invisible
                    }


                });
            }

            void showMyErrorMessageDialog(String msg)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(msg);


                builder.setCancelable(true);
                builder.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                getActivity().finish();
                            }
                        });

                AlertDialog alert = builder.create();
                alert.show();
            }

            @Override
            public void failure(final RetrofitError error) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                        // https://github.com/kaaes/spotify-web-api-android/blob/master/src/main/java/kaaes/spotify/webapi/android/SpotifyError.java
                        Log.e(LOG_TAG, spotifyError.getErrorDetails().toString());

                        int code=spotifyError.getErrorDetails().status;
                        String msg=spotifyError.getErrorDetails().message; // show error message

                        if ( code==400) // if error is about country...
                            msg = "<"+ mCountryName+">\n "+ msg +". \n "+ getActivity().getResources().getString(R.string.msg_country_error);

                        showMyErrorMessageDialog(msg);


                        if (mProgressBar != null)
                            mProgressBar.setVisibility(View.INVISIBLE); // set progress bar invisible

                    }
                });
            }
        });

    }



    // http://developer.android.com/training/basics/activity-lifecycle/recreating.html#SaveState
    //http://stackoverflow.com/questions/12503836/how-to-save-custom-arraylist-on-android-screen-rotate

    @Override
    public void  onSaveInstanceState(Bundle savedInstanceState)
    {

        if (mTopTrackItems != null)
            if (mTopTrackItems.size()>0)
                savedInstanceState.putParcelableArrayList(SAVE_STATE_STR, mTopTrackItems); // save current list

        savedInstanceState.putString("ARTIST_ID", mArtistId);
        savedInstanceState.putString("ARTIST_NAME", mArtistName);

        super.onSaveInstanceState(savedInstanceState);
    }


    // call from parent activity --> when playing music & pressed next or prev button
    public void setTopTracksSelection(int position)
    {
        final int pos = position;

        if (mListView != null && mPosition != pos) { // if position is changed

            mListView.post(new Runnable() {
                @Override
                public void run() {
                    mListView.clearFocus();
                    mListView.setItemChecked(pos, true);
                    mListView.smoothScrollToPosition(pos);
                    mPosition = pos;
                }
            });

        }
    }

}

/********************** NOT USED *************************************************/
/* from 8th JUN 2015... not used (API wrapper callback)
    class FetchToptracksTask extends AsyncTask<String, Void, Tracks>
    {

        private final String LOG_TAG= TopTracksActivity.class.getSimpleName(); // for log msg
        private String mArtist;

        @Override
        protected Tracks doInBackground(String... params)
        {

            // Step2 : top 10 tracks whose selected artist
            Tracks results = null;
            try
            {
                SpotifyApi api = new SpotifyApi();
                SpotifyService spotify = api.getService();
                results = spotify.getArtistTopTrack(params[0],
                        new HashMap<String, Object>() {
                            {
                               put ("country", "US"); // hard coding
                            }}

                ); // https://developer.spotify.com/web-api/search-item/
            }
            catch (RetrofitError error) {
                  SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                  // https://github.com/kaaes/spotify-web-api-android/blob/master/src/main/java/kaaes/spotify/webapi/android/SpotifyError.java
                  Log.e(LOG_TAG, spotifyError.getErrorDetails().toString());
            }
            mArtist = params[1]; // artist name

            return results;
        }

        @Override
        protected void onPostExecute(Tracks tracks)
        {


            if (tracks == null) // system error
            {
                Toast.makeText(getActivity(), getResources().getText(R.string.msg_no_data_null), Toast.LENGTH_LONG).show();
                getActivity().finish(); // close this activity. so tough :( I will improve.
                return;
            }
            if (tracks.tracks.size() == 0) // there is no track
            {

                Toast.makeText(getActivity(), getResources().getText(R.string.msg_there_is_no_track_of) + " "+ mArtist+". "+
                        getResources().getText(R.string.msg_pls_search_other_track)
                        ,  Toast.LENGTH_LONG).show();
                        getActivity().finish(); // close this activity. so tough :( I will improve.

                return;
            }

            ArrayList<TopTracksItem> retList = new ArrayList<TopTracksItem>(tracks.tracks.size()); // make return items



            for (Track item: tracks.tracks)
            {

                TopTracksItem myItem;

                switch (item.album.images.size()) // number of images (sorted from big size) --> I will improve ( the method select big biggest size & small size)
                {
                    case 0:
                        myItem = new TopTracksItem(item.name , item.album.name, DEFAULT_IMG_URL, item.preview_url); // has no thumbnail
                        break;
                    case 1:
                        myItem = new TopTracksItem(item.name , item.album.name, item.album.images.get(0).url, item.preview_url);
                        break;
                    case 2:
                        myItem = new TopTracksItem(item.name , item.album.name, item.album.images.get(0).url, item.album.images.get(1).url, item.preview_url);
                        break;
                    default :
                        myItem = new TopTracksItem(item.name , item.album.name, item.album.images.get(0).url,  item.album.images.get(2).url, item.preview_url); // large & small thumbnaul
                        break;

                }

                Log.v(LOG_TAG, myItem.toString()) ;
                retList.add(myItem);
            }
            if (mTopTracksAdapter != null)
            {
                mTopTracksAdapter.clear();
                mTopTracksAdapter.addAll(retList);
                mTopTracksAdapter.notifyDataSetChanged(); // update view
            }

        }
    }

    */

 /*

    public class Tracks {
        public List<Track> tracks;
    }



      <a href="https://developer.spotify.com/web-api/object-model/#track-object-full">Track object model</a>

                public class Track extends TrackSimple {
                    public AlbumSimple album;
                    public Map<String, String> external_ids;
                    public Integer popularity;
                }


    public class TrackSimple {
        public List<ArtistSimple> artists;
        public List<String> available_markets;
        public Boolean is_playable;
        public LinkedTrack linked_from;
        public int disc_number;
        public long duration_ms;
        public boolean explicit;
        public Map<String, String> external_urls;
        public String href;
        public String id;
        public String name;
        public String preview_url;
        public int track_number;
        public String type;
        public String uri;
    }
     */

    /*
    *** found http://discussions.udacity.com/t/asynctask-vs-callbacks/21223/2
    *
    *
	public void getArtistTopTrack(@Path("id") String artistId, @QueryMap Map<String, Object> options, Callback<Tracks> callback);

    /**
     * Get Spotify catalog information about an artist’s top tracks by country.
     *
     * @param artistId The Spotify ID for the artist.
     * @param options  Optional parameters. For list of supported parameters see
     *                 <a href="https://developer.spotify.com/web-api/get-artists-top-tracks/">endpoint documentation</a>
     * @return An object whose key is "tracks" and whose value is an array of track objects.
     * @see <a href="https://developer.spotify.com/web-api/get-artists-top-tracks/">Get an Artist’s Top Tracks</a>
     */
    /*
    // Call to update the share intent
    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }*/




/*

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_toptrack, menu);
    }

    // Processing option menu actions
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items

        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onPrepareOptionsMenu(Menu menu) {


        super.onPrepareOptionsMenu(menu);
    }

 */