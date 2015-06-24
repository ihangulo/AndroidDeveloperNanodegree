package com.hangulo.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *  SpotifyStreamer, Stage 2
 *  ------------------------
 *  Kwanghyun JUNG
 *  24th JUN 2015
 *
 *  Top 10 Tracks Item class
 *
 */
public class TopTracksItem implements Parcelable
{
    private String artist_name;
    private String track_name;
    private String album_name;
    private String thumb_large_url; // 640x640
    private String thumb_small_url; // 300x300 --> must change 200x200
    private String preview_url;  // 30s preview file URL
    private String spotify_url; // external Spotify URL (for share link)

    // Pacelabel
    private TopTracksItem(Parcel in) {

        artist_name = in.readString();
        track_name = in.readString();
        album_name = in.readString();
        thumb_large_url = in.readString();
        thumb_small_url =in.readString();
        preview_url = in.readString();
        spotify_url = in.readString();
    }

    public int describeContents() {
        return 0;
    }
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(artist_name);
        out.writeString(track_name);
        out.writeString(album_name);
        out.writeString(thumb_large_url);
        out.writeString(thumb_small_url);
        out.writeString(preview_url);
        out.writeString(spotify_url);
    }
    // for receiver
    public static final Parcelable.Creator<TopTracksItem> CREATOR =
            new Parcelable.Creator<TopTracksItem>()
            {

                 @Override
                 public TopTracksItem createFromParcel(Parcel in) {
                return new TopTracksItem(in);
                }

                @Override
                public TopTracksItem[] newArray(int size) {
                return new TopTracksItem[size];
                }
    };

    public TopTracksItem(String artist_name, String track_name, String album_name, String thumb_url, String preview_url, String spotify_url) {
        this.artist_name = artist_name;
        this.track_name = track_name;
        this.album_name = album_name;
        this.thumb_large_url = thumb_url;
        this.thumb_small_url = thumb_url;
        this.preview_url = preview_url;
        this.spotify_url = spotify_url;
    }


    public TopTracksItem(String artist_name,String track_name, String album_name, String thumb_large_url, String thumb_small_url, String preview_url, String spotify_url) {
        this.artist_name = artist_name;
        this.track_name = track_name;
        this.album_name = album_name;
        this.thumb_large_url = thumb_large_url;
        this.thumb_small_url = thumb_small_url;
        this.preview_url = preview_url;
        this.spotify_url = spotify_url;
    }

    // getter
    public String getArtist_name() { return artist_name;}

    public String getTrack_name()
    {
        return track_name;
    }

    public String getAlbum_name()
    {
        return album_name;
    }

    public String getThumb_large_url()
    {
        return thumb_large_url;
    }

    public String getThumb_small_url()
    {
        return thumb_small_url;
    }

    public String getPreview_url () { return preview_url; }

    public String getSpotify_url () { return spotify_url; }


    // setter
    public void setArtist_name(String artist_name) { this.artist_name = artist_name;}

    public void setTrack_name(String track_name)
    {
        this.track_name = track_name;
    }

    public void setAlbum_name(String album_name)
    {
        this.album_name = album_name;
    }

    public void setThumb_large_url(String thumb_large_url)
    {
        this.thumb_large_url = thumb_large_url;
    }

    public void setThumb_small_url(String thumb_small_url)
    {
        this.thumb_small_url = thumb_small_url;
    }

    public void setPreview_url(String preview_url)
    {
        this.preview_url = preview_url;
    }
    public void setSpotify_url(String spotify_url)
    {
        this.preview_url = spotify_url;
    }



    @Override
    public String toString()
    {
        return "TopTracksItem{" +
                "artist_name="+ artist_name+
                ". album_name='" + album_name + '\'' +
                ", track_name='" + track_name + '\'' +
                ", thumb_large_url='" + thumb_large_url + '\'' +
                ", thumb_small_url='" + thumb_small_url + '\'' +
                ", preview_url ='" + preview_url + '\'' +
                ", spotify_url ='" + spotify_url + '\'' +
                '}';
    }
}

/**************** NOT USED &&&**********************
    public class Tracks {
        public List<Track> tracks;
    }

    public class Track extends TrackSimple
    {
        public AlbumSimple album;
        public Map<String, String> external_ids;
        public Integer popularity;
    }


public class TrackSimple
{
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

public class AlbumSimple {
    public String album_type;
    public List<String> available_markets;
    public Map<String, String> external_urls;
    public String href;
    public String id;
    public List<Image> images;
    public String name;
    public String type;
    public String uri;
}
 */