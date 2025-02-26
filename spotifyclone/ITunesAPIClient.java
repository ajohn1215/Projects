package main.java.com.spotifyclone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ITunesAPIClient {
    private static final String ITUNES_SEARCH_URL = "https://itunes.apple.com/search?term=%s&entity=song&limit=25";

    public static List<Song> searchSongs(String query) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format(ITUNES_SEARCH_URL, encodedQuery);
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        List<Song> songs = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        JsonNode results = root.path("results");
        for (JsonNode node : results) {
            String trackName = node.path("trackName").asText();
            String artistName = node.path("artistName").asText();
            int trackTimeMillis = node.path("trackTimeMillis").asInt();
            String previewUrl = node.path("previewUrl").asText();
            // Convert trackTimeMillis to mm:ss format:
            int totalSeconds = trackTimeMillis / 1000;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            String duration = String.format("%d:%02d", minutes, seconds);
            
            Song song = new Song(trackName, artistName, duration, previewUrl);
            songs.add(song);
        }
        return songs;
    }
}
