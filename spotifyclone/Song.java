package main.java.com.spotifyclone;

public class Song implements Comparable<Song> {
    private String songName;
    private String artistName;
    private String duration; // formatted as mm:ss
    private String previewUrl; // URL for a preview clip

    public Song(String songName, String artistName, String duration, String previewUrl) {
        this.songName = songName;
        this.artistName = artistName;
        this.duration = duration;
        this.previewUrl = previewUrl;
    }

    public String getSongName() {
        return songName;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getDuration() {
        return duration;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    @Override
    public int compareTo(Song other) {
        int result = songName.compareTo(other.songName);
        return (result != 0) ? result : artistName.compareTo(other.artistName);
    }

    @Override
    public String toString() {
        return songName + " by " + artistName + " (" + duration + ")";
    }
}
