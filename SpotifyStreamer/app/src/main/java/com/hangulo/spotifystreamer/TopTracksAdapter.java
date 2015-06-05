package com.hangulo.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by khjung on 2015-06-04.
 *
 * Array adapter for Top 10 tracks
 *
 * from http://stackoverflow.com/questions/21833181/arrayadapter-text-and-image
  * http://theopentutorials.com/tutorials/android/listview/android-custom-listview-with-image-and-text-using-arrayadapter/
 */

public class TopTracksAdapter extends ArrayAdapter<TopTracksItem>
{

    Context context;
    final static String DEFAULT_IMG_URL = "noimage.jpg";

    public TopTracksAdapter(Context context, int resourceId,
                            List<TopTracksItem> items) {
        super(context, resourceId, items);
        this.context = context;
    }

   //private view holder class
    private class ViewHolder
   {
       ImageView imageView;
       TextView txtViewTrack;
       TextView txtViewAlbum;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder = null;
        TopTracksItem topTracksItem = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if (convertView == null)
        {
            convertView = mInflater.inflate(R.layout.list_item_track, null);
            holder = new ViewHolder();
            holder.txtViewTrack = (TextView) convertView.findViewById(R.id.list_item_track_name);
            holder.txtViewAlbum = (TextView) convertView.findViewById(R.id.list_item_track_album_name);
            holder.imageView = (ImageView) convertView.findViewById(R.id.list_item_track_image);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.txtViewTrack.setText(topTracksItem.getTrack_name());
        holder.txtViewAlbum.setText(topTracksItem.getAlbum_name());


        if (topTracksItem.getThumb_small_url().equals(DEFAULT_IMG_URL)) // there is no image
        {
            Picasso.with(context)
                    .load(R.drawable.noimage) // load from drawble
                    .fit().centerCrop()
                    .into(holder.imageView);

        } else
        {

            Picasso.with(context)
                    .load(topTracksItem.getThumb_small_url())
                    .fit().centerCrop()
                    .error(R.drawable.noimage)
                    .into(holder.imageView);
        }



        return convertView;
    }
}