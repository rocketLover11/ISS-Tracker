package com.rocketlover11;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.io.BufferedReader; 
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class App extends Application {
    private WebEngine we;

    @Override public void start(Stage stage) {
        stage.setTitle("ISS Tracker");

        WebView wv = new WebView();
        we = wv.getEngine();

        String map = """
        <html>
        <head>
          <meta charset="utf-8">
          <title>ISS Tracker</title>
          <link rel="stylesheet" href="https://unpkg.com/leaflet/dist/leaflet.css" />
          <script src="https://unpkg.com/leaflet/dist/leaflet.js"></script>
        </head>
        <body style="margin:0;">
          <div id="map" style="width:100vw;height:100vh;"></div>
          <script>
            var map = L.map('map').setView([0, 0], 2);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
              maxZoom: 6,
              attribution: '© OpenStreetMap contributors'
            }).addTo(map);

            var marker = L.marker([0, 0]).addTo(map).bindPopup("ISS Position");

            function updateMarker(lat, lon) {
              marker.setLatLng([lat, lon]);
              map.panTo([lat, lon]);
            }
          </script>
        </body>
        </html>
        """;

        we.loadContent(map);

        stage.setScene(new Scene(wv, 800, 600));
        stage.show();

        new Thread(() -> {
            while(true) {
                try {
                    double[] pos = getISSPosition();

                    javafx.application.Platform.runLater(() -> we.executeScript("updateMarker(" + pos[0] + ", " + pos[1] + ");"));
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private double[] getISSPosition() throws Exception {
        URL api = URI.create("http://api.open-notify.org/iss-now.json").toURL();

        HttpURLConnection conn = (HttpURLConnection) api.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder re = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) re.append(line);
        in.close();

        JSONObject json = new JSONObject(re.toString());
        JSONObject pos = json.getJSONObject("iss_position");
        double lat = pos.getDouble("latitude");
        double lon = pos.getDouble("longitude");
        return new double[]{lat, lon};
    }

    public static void main(String[] args) {
        launch();
    }
}