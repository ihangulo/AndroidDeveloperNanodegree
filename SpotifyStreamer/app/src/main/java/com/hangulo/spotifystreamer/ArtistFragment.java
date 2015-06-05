package com.hangulo.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Artist;


/**
 * ArtistFragment <- for MainActivity
 */
public class ArtistFragment extends Fragment
{
    final String LOG_TAG= ArtistFragment.class.getSimpleName(); // error processing
    private static final String SAVE_STATE_STR = new String("save_artist");

    private ArtistAdapter mArtistAdapter;
    private ArrayList<ArtistItem> mArtistItems;

    private EditText mEditTextSearch;
    private ListView mListViewArtist;
    private View rootView;

    final static String DEFAULT_IMG_URL = "noimage.jpg";


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
       // setHasOptionsMenu(true);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {

        rootView = inflater.inflate(R.layout.fragment_artist, container, false);

        // Search Textfield (EditText) set
        mListViewArtist = (ListView) rootView.findViewById(R.id.listview_artist);
        mEditTextSearch = (EditText) rootView.findViewById(R.id.search_artist_name); // search keyword

        mEditTextSearch.setOnKeyListener(new View.OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER))
                {
                    // Perform action on key press
                    // Toast.makeText(getActivity(), mEditTextSearch.getText(), Toast.LENGTH_SHORT).show();
                    searchAndUpdateList(mEditTextSearch.getText().toString()); // Search it!
                    mEditTextSearch.clearFocus(); // remove focus
                    InputMethodManager imm =
                            (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE); // remove keyboard
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    return true;
                }
                return false;
            }

        });

        //mArtistItems = new ArrayList<ArtistItem>();
        // Restore state members from saved instance
        if(savedInstanceState != null)
        {
            mArtistItems =  savedInstanceState.getParcelableArrayList(SAVE_STATE_STR);
        }

        if(mArtistItems == null)
            mArtistItems = new ArrayList<ArtistItem>(0); // prevent null exception error

        mArtistAdapter = new ArtistAdapter(getActivity(), R.layout.list_item_album, mArtistItems);



        if (mArtistAdapter!=null)
        {
            mListViewArtist.setAdapter(mArtistAdapter);
            mListViewArtist.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    ArtistItem item = mArtistAdapter.getItem(position); // get item

                    Intent intent = new Intent(getActivity(), TopTracksActivity.class);

                    intent.putExtra("ARTIST_ID", item.getArtist_id()); // set extra data - artist id
                    intent.putExtra("ARTIST_NAME", item.getArtist_name()); // artist name
                    startActivity(intent); // start!
                }
            });
        }


        return rootView;
    }


    private void searchAndUpdateList(String artistName)
    {
        FetchArtistTask artistTask = new FetchArtistTask();
        artistTask.execute(artistName);
    }

    // http://developer.android.com/training/basics/activity-lifecycle/recreating.html#SaveState
    //http://stackoverflow.com/questions/12503836/how-to-save-custom-arraylist-on-android-screen-rotate

    @Override
    public void  onSaveInstanceState(Bundle savedInstanceState)
    {

            // Save list data
            if (mArtistItems != null)
                if (mArtistItems.size()>0)
                    savedInstanceState.putParcelableArrayList(SAVE_STATE_STR, mArtistItems ); // Parcelable save

        super.onSaveInstanceState(savedInstanceState); // always put end of method (for savedInstanceState = true)
    }


    /*
    public class ArtistsPager
    {
        public Pager<Artist> artists;
    }

    public class Pager<T>
    {
        public String href;
        public List<T> items;
        public int limit;
        public String next;
        public int offset;
        public String previous;
        public int total;
    }

    public class Artists
    {
        public List<Artist> artists;
    }
    public class Artist extends ArtistSimple
    {
        public Followers followers;
        public List<String> genres;
        public List<Image> images;
        public Integer popularity;
    }

    public class ArtistSimple {
    public Map<String, String> external_urls;
    public String href;
    public String id;
    public String name;
    public String type;
    public String uri;
    }

    public class Image {
    public Integer width;
    public Integer height;
    public String url;
}

 */

    class FetchArtistTask extends AsyncTask<String, Void, ArtistsPager>
    {

        private final String LOG_TAG= FetchArtistTask.class.getSimpleName(); // 에러 처리 위함


        @Override
        protected ArtistsPager doInBackground(String... params)
        {

            // Spotify web API structure
            // https://github.com/kaaes/spotify-web-api-android/blob/master/src/main/java/kaaes/spotify/webapi/android/SpotifyService.java

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();
            ArtistsPager results = spotify.searchArtists(params[0]); // https://developer.spotify.com/web-api/search-item/

            return results;
        }


        @Override
        protected void onPostExecute(ArtistsPager artistPager)
        {
            if (artistPager == null) // system error... but try again
            {
                Toast.makeText(getActivity(), getResources().getText(R.string.msg_no_data_null), Toast.LENGTH_SHORT).show();
                mArtistAdapter.clear();
                mEditTextSearch.requestFocus();
                return;
            }
            if (artistPager.artists.total == 0) // there is no data
            {

                Toast.makeText(getActivity(), getResources().getText(R.string.msg_there_is_no_artist), Toast.LENGTH_SHORT).show();
                mArtistAdapter.clear();
                mEditTextSearch.requestFocus();

                /* not used (for  showing keyboard)
                mEditTextSearch.selectAll();
                InputMethodManager imm = (InputMethodManager)
                        mEditTextSearch.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mEditTextSearch, InputMethodManager.SHOW_FORCED);
                imm.showSoftInputFromInputMethod(mEditTextSearch.getApplicationWindowToken(), InputMethodManager.SHOW_FORCED);
                */


                return;
            }

            ArrayList<ArtistItem> retList = new ArrayList<ArtistItem>(artistPager.artists.total); // make return items

            for (Artist item: artistPager.artists.items)
            {
                ArtistItem myItem;

                switch (item.images.size())
                {
                    case 0:
                            myItem = new ArtistItem(item.id, item.name, DEFAULT_IMG_URL); // has no thumbnail
                            break;
                    case 1:
                            myItem = new ArtistItem(item.id, item.name, item.images.get(0).url); // only 1
                            break;
                    case 2:
                            myItem = new ArtistItem(item.id, item.name, item.images.get(0).url); // 2 thumbnail
                            break;
                    default :
                            myItem = new ArtistItem(item.id, item.name, item.images.get(0).url,  item.images.get(2).url); // 3 thumbnail large & small thumbnaul
                            break;
                }
                Log.v(LOG_TAG, myItem.toString()) ;
                retList.add(myItem);
            }

            if (mArtistAdapter != null)
            {
                mArtistAdapter.clear();
                mArtistAdapter.addAll(retList); // set returned data
                mArtistAdapter.notifyDataSetChanged(); // update view
            }

        }
    }
}

