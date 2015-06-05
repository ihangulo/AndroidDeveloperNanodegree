package com.hangulo.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;

import kaaes.spotify.webapi.android.models.Tracks;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.RetrofitError;


/**
 *  Show top 10 tracks
 */
public class TopTracksActivityFragment extends Fragment
{
    private static final String LOG_TAG = TopTracksActivityFragment.class.getSimpleName();
    private static final String SAVE_STATE_STR = new String("save_track");


    final static String DEFAULT_IMG_URL = "noimage.jpg"; // will improve
    private TopTracksAdapter mTopTracksAdapter;
    private String mArtistId; // artist id
    private String mArtistName; // artist name

    private ArrayList<TopTracksItem> mTopTrackItems;


    public TopTracksActivityFragment()
    {
        //setHasOptionsMenu(true); // no menu at this time
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        Intent intent=getActivity().getIntent();

        View rootView = inflater.inflate(R.layout.fragment_toptracks, container, false);

        if (intent != null && intent.hasExtra("ARTIST_ID"))
        {
            mArtistId = intent.getStringExtra("ARTIST_ID");
            mArtistName = intent.getStringExtra("ARTIST_NAME");
            Log.v(LOG_TAG, "Intent=" + mArtistId + "/" + mArtistName);
        }

        if (mArtistName.isEmpty())
            mArtistName = getResources().getText(R.string.unknown_artist).toString();

        // set subtitle
        ActionBar myActionBar =  ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (myActionBar != null)
            {
                myActionBar.setSubtitle(mArtistName); // set subtitle (Aritst's name)
                myActionBar.setDisplayHomeAsUpEnabled(true); // set 'up icon'
            }

        // Search Textfield (EditText) set
        ListView listView = (ListView) rootView.findViewById(R.id.listview_spotify_toptracks);



        mTopTrackItems = new ArrayList<TopTracksItem>();

        mTopTracksAdapter = new TopTracksAdapter(getActivity(), R.layout.list_item_track, mTopTrackItems);
        listView.setAdapter(mTopTracksAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                TopTracksItem item = mTopTracksAdapter.getItem(position); // get item
                //Toast.makeText(getActivity(), item.toString(), Toast.LENGTH_SHORT).show(); // for debugging

                Toast.makeText(getActivity(), getResources().getText(R.string.msg_not_yet_play_soon) +item.getTrack_name(), Toast.LENGTH_SHORT).show(); // for debugging


            }
        });

        if (savedInstanceState != null) // if tehre is saved data
        {
            mTopTrackItems =   savedInstanceState.getParcelableArrayList(SAVE_STATE_STR);

            if (mTopTrackItems == null) // prevent error
                mTopTrackItems = new ArrayList<TopTracksItem>();

            if(mTopTracksAdapter.getCount()>0)
                mTopTracksAdapter.clear();

            mTopTracksAdapter.addAll(mTopTrackItems);

        }
        else
        {
            searchAndUpdateList(mArtistId);  // now Search!
        }

        return rootView;
    }


    private void searchAndUpdateList(String artistId)
    {
        FetchToptracksTask trackTask = new FetchToptracksTask();
        trackTask.execute(artistId, mArtistName); // search top album of artistId
    }


    // http://developer.android.com/training/basics/activity-lifecycle/recreating.html#SaveState
    //http://stackoverflow.com/questions/12503836/how-to-save-custom-arraylist-on-android-screen-rotate

    @Override
    public void  onSaveInstanceState(Bundle savedInstanceState)
    {

        if (mTopTrackItems != null)
            if (mTopTrackItems.size()>0)
                savedInstanceState.putParcelableArrayList(SAVE_STATE_STR, mTopTrackItems); // save current list

        super.onSaveInstanceState(savedInstanceState);
    }


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
}
