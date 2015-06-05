package com.hangulo.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *
 *  Artist item class for Artist search
 *
 * Created by khjung on 2015-06-04.
 */
public class ArtistItem implements Parcelable
{
    private String artist_id;
    private String aritist_name;
    private String thumb_large_url; // 640x640
    private String thumb_small_url; // 300x300 --> must change 200x200

    // Pacelabel
    private ArtistItem(Parcel in)
    {
        artist_id = in.readString();
        aritist_name = in.readString();
        thumb_large_url = in.readString();
        thumb_small_url =in.readString();
    }

    public int describeContents()
    {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags)
    {
        out.writeString(artist_id);
        out.writeString(aritist_name);
        out.writeString(thumb_large_url);
        out.writeString(thumb_small_url);
    }


    // for receiver
    public static final Parcelable.Creator<ArtistItem> CREATOR =
            new Parcelable.Creator<ArtistItem>()
            {

                @Override
                public ArtistItem createFromParcel(Parcel in) {
                    return new ArtistItem(in);
                }

                @Override
                public ArtistItem[] newArray(int size) {
                    return new ArtistItem[size];
                }
            };


    // if there exits only large thumbnaile or not
    public ArtistItem(String id, String name, String thumb_large_url)
    {
        this.artist_id = id;
        this.aritist_name = name;
        this.thumb_large_url = thumb_large_url;
        this.thumb_small_url = thumb_large_url;
    }

    // if there exits large & small thumbnail
    public ArtistItem(String id, String name, String thumb_large_url, String thumb_small_url)
    {
        this.artist_id = id;
        this.aritist_name = name;
        this.thumb_large_url = thumb_large_url;
        this.thumb_small_url = thumb_small_url;
    }

    // getter
    public String getArtist_id()
    {
        return artist_id;
    }

    public String getThumb_large_url()
    {
        return thumb_large_url;
    }

    public String getThumb_small_url()
    {
        return thumb_small_url;
    }

    public String getArtist_name()
    {
        return aritist_name;
    }


    // setter
    public void setArtist_Id(String id)
    {
        this.artist_id = id;
    }

    public void setArtist_name(String name)
    {
        this.aritist_name = name;
    }

    public void setThumb_large_url(String thumb_large_url) { this.thumb_large_url = thumb_large_url; }

    public void setThumb_small_url(String thumb_small_url) { this.thumb_small_url = thumb_small_url; }

    @Override
    public String toString()
    {
        return "Spotify{" +
                "id='" + artist_id + '\'' +
                ", artist_name='" + aritist_name + '\'' +
                ", thumb_small_url='" + thumb_small_url + '\'' +
                ", thumb_large_url='" + thumb_large_url + '\'' +
                '}';
    }
}

