package com.hangulo.spotifystreamer;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import kaaes.spotify.webapi.android.models.Artist;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;



/**
 *  SpotifyStreamer, Stage 2
 *  ------------------------
 *  Kwanghyun JUNG
 *  24th JUN 2015
 *
 * ArtistFragment <- for MainActivity
 *
 * from now on, I change the {} style. :) 8th JUN 2015
 */
public class ArtistFragment extends Fragment {
    final String LOG_TAG= ArtistFragment.class.getSimpleName(); // error processing
    private static final String DETAILFRAGMENT_TAG = "DFTAG"; // for tablet;
    private static final String SELECTED_KEY = "selected_position"; // for saving selected position
    private int mPosition = ListView.INVALID_POSITION; // for saving position

    private static final String SAVE_STATE_STR = new String("save_artist");

    private ArtistAdapter mArtistAdapter;
    private ArrayList<ArtistItem> mArtistItems;

    private EditText mEditTextSearch;
    private ListView mListViewArtist;
    private View rootView;
    private ProgressBar mProgressLoadingCircle;


    final static String DEFAULT_IMG_URL = "noimage.jpg";

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface ArtistCallback {
        /**
         * FragmentCallback for when an item has been selected.
         */
        public void onArtistItemSelected(ArtistItem item);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_artist, container, false);

        if (rootView == null) // system error
            return null;

        // Search Textfield (EditText) set
        mListViewArtist = (ListView) rootView.findViewById(R.id.listview_artist);
        mEditTextSearch = (EditText) rootView.findViewById(R.id.search_artist_name); // search keyword

        // set empty view (2015.6.8)
        if(mListViewArtist == null || mEditTextSearch == null) // system error
            return rootView;

        View emptyView = rootView.findViewById(R.id.list_artist_emtpy); // set empty view
        mListViewArtist.setEmptyView(emptyView); // set empty list view

        mProgressLoadingCircle = (ProgressBar) rootView.findViewById(R.id.artist_progressBar); // progress bar (circle)
        if(mProgressLoadingCircle != null)
            mProgressLoadingCircle.setVisibility(View.INVISIBLE); // set progress bar invisible


        mEditTextSearch.setImeActionLabel("Search", KeyEvent.KEYCODE_ENTER);

        // after reviewing of stage1, I change to this routine 08 JUNE 2015
        mEditTextSearch.setOnEditorActionListener( new TextView.OnEditorActionListener () {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                // http://stackoverflow.com/questions/5932982/reliable-way-to-know-when-android-soft-keyboard-resigns-callback-that-works-on
                if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE ||
                        (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    String myArtist = mEditTextSearch.getText().toString().trim(); // get artist name (remove space)

                    if (myArtist.length() > 0)
                        searchAndUpdateList(myArtist); // Search it!
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    return true;

                }
                return false;
            }

        });

        // Restore state members from saved instance
        if(savedInstanceState != null) {

            mArtistItems =  savedInstanceState.getParcelableArrayList(SAVE_STATE_STR);
        }

        if(mArtistItems == null)
            mArtistItems = new ArrayList<>(0); // prevent null exception error

        mArtistAdapter = new ArtistAdapter(getActivity(), R.layout.list_item_album, mArtistItems);

        mListViewArtist.setAdapter(mArtistAdapter);
        mListViewArtist.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArtistItem item = mArtistAdapter.getItem(position); // get item
                mPosition = position; // for saving postion
                // Notify the  parent activity --> call TopTracks
                ((ArtistCallback) getActivity()).onArtistItemSelected(item);
            }
        });



            // restore position
            if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
                            // The listview probably hasn't even been populated yet.  Actually perform the
                            // swapout in onLoadFinished.
                        mPosition = savedInstanceState.getInt(SELECTED_KEY);
            }

        return rootView;
    }


    private void searchAndUpdateList(String artistName) {


        SpotifyService spotifyService;
        SpotifyApi api;

        if (artistName == null) { // prevent null exception
            Toast.makeText(getActivity(), getResources().getText(R.string.msg_system_error)+ ": 100", Toast.LENGTH_LONG).show();
            return;
        }

        if (artistName.length()<1) {
            Toast.makeText(getActivity(), getResources().getText(R.string.msg_system_error)+ ": 200", Toast.LENGTH_LONG).show();
            return;
        }

        api = new SpotifyApi();

        spotifyService = api.getService();

        if (spotifyService == null ) return; // error


        if(mProgressLoadingCircle != null)
          mProgressLoadingCircle.setVisibility(View.VISIBLE); // set progress bar visible

        spotifyService.searchArtists(artistName, new Callback<ArtistsPager>()
        {
            // 제대로 가져왔을때
            @Override
            public void success(final ArtistsPager artistsPager, Response response)
            {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                                            // run
                        if (artistsPager == null) {  // system error... but try again
                           Toast.makeText(getActivity(), getResources().getText(R.string.msg_no_data_null), Toast.LENGTH_SHORT).show();
                            mArtistAdapter.clear();
                            if(mProgressLoadingCircle != null)
                                mProgressLoadingCircle.setVisibility(View.INVISIBLE); // set progress bar invisible

                            mEditTextSearch.requestFocus();
                            return;
                        }
                        if (artistsPager.artists.total == 0) { // there is no data
                            Toast.makeText(getActivity(), getResources().getText(R.string.msg_there_is_no_artist), Toast.LENGTH_SHORT).show();
                            mArtistAdapter.clear();
                            if(mProgressLoadingCircle != null)
                                mProgressLoadingCircle.setVisibility(View.INVISIBLE); // set progress bar invisible
                            mEditTextSearch.requestFocus();
                            return;
                        }

                        ArrayList<ArtistItem> retList = new ArrayList<>(artistsPager.artists.total); // make return items

                        for (Artist item: artistsPager.artists.items) {
                            ArtistItem myItem;
                            switch (item.images.size()) {

                                case 0:
                                    myItem = new ArtistItem(item.id, item.name, DEFAULT_IMG_URL); // has no thumbnail
                                    break;
                                case 1:
                                    myItem = new ArtistItem(item.id, item.name, item.images.get(0).url); // only 1
                                    break;
                                case 2:
                                    myItem = new ArtistItem(item.id, item.name, item.images.get(0).url, item.images.get(1).url); // 2 thumbnail
                                    break;
                                default :
                                    myItem = new ArtistItem(item.id, item.name, item.images.get(0).url,  item.images.get(2).url); // 3 thumbnail large & small thumbnaul
                                    break;
                            }
                            Log.v(LOG_TAG, myItem.toString()) ;
                            retList.add(myItem);
                        }
                        if (mArtistAdapter != null) {
                            mArtistAdapter.clear();
                            mArtistAdapter.addAll(retList); // set returned data
                        //  mArtistAdapter.notifyDataSetChanged(); // update view
                        }

                        if(mProgressLoadingCircle != null)
                            mProgressLoadingCircle.setVisibility(View.INVISIBLE); // set progress bar invisible
                    }

                });
            }

            // error
            @Override
            public void failure(final RetrofitError error) {


                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(mProgressLoadingCircle != null)
                            mProgressLoadingCircle.setVisibility(View.INVISIBLE); // set progress bar invisible

                        SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                        // https://github.com/kaaes/spotify-web-api-android/blob/master/src/main/java/kaaes/spotify/webapi/android/SpotifyError.java

                            Log.e(LOG_TAG, spotifyError.getErrorDetails().toString());
                            Toast.makeText(getActivity(), spotifyError.getErrorDetails().toString(), Toast.LENGTH_SHORT).show();  // show error message


                        if(mProgressLoadingCircle != null)
                            mProgressLoadingCircle.setVisibility(View.INVISIBLE); // set progress bar invisible


                    }
                });

            }
        });
    }

    // http://developer.android.com/training/basics/activity-lifecycle/recreating.html#SaveState
    //http://stackoverflow.com/questions/12503836/how-to-save-custom-arraylist-on-android-screen-rotate
    @Override
    public void  onSaveInstanceState(Bundle savedInstanceState) {

            // Save list data
        if (mArtistItems != null) // save list data
                if (mArtistItems.size()>0)
                    savedInstanceState.putParcelableArrayList(SAVE_STATE_STR, mArtistItems ); // Parcelable save

        if (mPosition != ListView.INVALID_POSITION) {
                savedInstanceState.putInt(SELECTED_KEY, mPosition); // save this position
        }


        super.onSaveInstanceState(savedInstanceState); // always put end of method (for savedInstanceState = true)
    }
}


/**************************** NOT USED ***************************************/

        /* change to Spotify Callback by reviewing comment 8th Jun 2015
        FetchArtistTask artistTask = new FetchArtistTask();
        artistTask.execute(artistName);*/

        /* comment

        It’s not wrong at all use AsyncTask, but SpotifyWrapper API gives a more handy way to
        handle requests, a Callback<T>. You can add a callback at the end of every method call
        from this API to handle with incoming remote data asynchronously.

        spotifyService.searchArtists(search, new Callback<ArtistsPager>() {
            @Override
            public void success(ArtistsPager artistsPager, Response response) {
            }
            @Override
            public void failure(RetrofitError error) {

            }
         });


         */
