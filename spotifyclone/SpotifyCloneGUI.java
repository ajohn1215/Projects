package main.java.com.spotifyclone;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class SpotifyCloneGUI extends Application {

    private TextField searchField;
    private Button searchButton;
    private ListView<Song> searchResultsListView;
    private ListView<Song> playlistListView;
    private Button addToPlaylistButton;
    private Button playPreviewButton;
    private Button playPlaylistSongButton;
    private Label statusLabel;

    private MediaPlayer mediaPlayer;
    private ObservableList<Song> myPlaylist;

    @Override
    public void start(Stage stage) {
        searchField = new TextField();
        searchField.setPromptText("Search for songs...");
        searchButton = new Button("Search");
        searchButton.setOnAction(e -> searchSongs());

        HBox searchBox = new HBox(10, searchField, searchButton);
        searchBox.setPadding(new Insets(10));

        searchResultsListView = new ListView<>();
        searchResultsListView.setPrefHeight(200);

        addToPlaylistButton = new Button("Add to My Playlist");
        addToPlaylistButton.setOnAction(e -> addToPlaylist());

        VBox searchResultsBox = new VBox(10, new Label("Search Results:"), searchResultsListView, addToPlaylistButton);
        searchResultsBox.setPadding(new Insets(10));

        playlistListView = new ListView<>();
        playlistListView.setPrefHeight(200);
        myPlaylist = FXCollections.observableArrayList();
        playlistListView.setItems(myPlaylist);

        playPlaylistSongButton = new Button("Play Selected from Playlist");
        playPlaylistSongButton.setOnAction(e -> playSelectedPlaylistSong());

        VBox playlistBox = new VBox(10, new Label("My Playlist:"), playlistListView, playPlaylistSongButton);
        playlistBox.setPadding(new Insets(10));

        playPreviewButton = new Button("Play Preview from Search Results");
        playPreviewButton.setOnAction(e -> playSelectedSearchSong());

        statusLabel = new Label("Enter a search term and click Search.");

        HBox mainContent = new HBox(10, searchResultsBox, playlistBox);
        VBox root = new VBox(10, searchBox, mainContent, playPreviewButton, statusLabel);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 800, 500);
        stage.setScene(scene);
        stage.setTitle("Spotify Clone");
        stage.show();
    }

    private void searchSongs() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText("Please enter a search term.");
            return;
        }
        statusLabel.setText("Searching...");
        new Thread(() -> {
            try {
                List<Song> songs = ITunesAPIClient.searchSongs(query);
                Platform.runLater(() -> {
                    searchResultsListView.getItems().setAll(songs);
                    statusLabel.setText("Found " + songs.size() + " songs.");
                });
            } catch (IOException | InterruptedException ex) {
                Platform.runLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    private void addToPlaylist() {
        Song selected = searchResultsListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            myPlaylist.add(selected);
            statusLabel.setText("Added to playlist: " + selected.getSongName());
        } else {
            statusLabel.setText("Please select a song from search results.");
        }
    }

    private void playSelectedSearchSong() {
        Song selected = searchResultsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a song from search results.");
            return;
        }
        playSong(selected);
    }

    private void playSelectedPlaylistSong() {
        Song selected = playlistListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Please select a song from your playlist.");
            return;
        }
        playSong(selected);
    }

    private void playSong(Song song) {
        if (song.getPreviewUrl() == null || song.getPreviewUrl().isEmpty()) {
            statusLabel.setText("No preview available for this song.");
            return;
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        try {
            Media media = new Media(song.getPreviewUrl());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.play();
            statusLabel.setText("Playing: " + song.getSongName());
        } catch (Exception ex) {
            statusLabel.setText("Error playing song: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
