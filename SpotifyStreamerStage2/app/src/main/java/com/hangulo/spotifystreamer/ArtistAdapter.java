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
 *  SpotifyStreamer, Stage 2
 *  ------------------------
 *  Kwanghyun JUNG
 *  24th JUN 2015
 *
 *
 * Array adapter for Aritist search
 *
 * from http://stackoverflow.com/questions/21833181/arrayadapter-text-and-image
  * http://theopentutorials.com/tutorials/android/listview/android-custom-listview-with-image-and-text-using-arrayadapter/
 */

public class ArtistAdapter extends ArrayAdapter<ArtistItem>
{

    Context context;
    final static String DEFAULT_IMG_URL = "noimage.jpg"; // i will improve

    public ArtistAdapter(Context context, int resourceId,
                         List<ArtistItem> items) {
        super(context, resourceId, items);
        this.context = context;
    }

   //private view holder class
    private class ViewHolder
    {
        TextView txtView;
        ImageView imageView;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        ViewHolder holder;
        ArtistItem artistItem = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_album, null);
            holder = new ViewHolder();

            holder.txtView = (TextView) convertView.findViewById(R.id.list_item_artist_name);
            holder.imageView = (ImageView) convertView.findViewById(R.id.list_item_album_image);
            convertView.setTag(holder);
        } else
            holder = (ViewHolder) convertView.getTag();

        holder.txtView.setText(artistItem.getArtist_name());

        if (artistItem.getThumb_small_url().equals(DEFAULT_IMG_URL)) // there is no image
        {
            Picasso.with(context)
                    .load(R.drawable.noimage)
                    .fit().centerCrop()
                    .into(holder.imageView);

        } else
        {

            Picasso.with(context)
                    .load(artistItem.getThumb_small_url())
                    .fit().centerCrop()
                    .error(R.drawable.noimage)
                    .into(holder.imageView);
        }

        return convertView;
    }
}