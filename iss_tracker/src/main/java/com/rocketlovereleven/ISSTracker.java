package com.rocketlovereleven;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.json.JSONException;
import org.json.JSONObject;

public class ISSTracker extends JFrame {
    private final JLabel mapLabel;
    private BufferedImage mapImage, issMarker;
    private double currentLat = 0, currentLon = 0;
    private double targetLat = 0, targetLon = 0;
    private final LinkedList<Point.Double> trail = new LinkedList<>();
    private String lastUpdTime = "Loading...";

    public ISSTracker() {
        //Window Configurations
        setTitle("ISS Tracker JE");
        setSize(900, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        //Load Icon
        try {
            setIconImage(ImageIO.read(getClass().getResource("iss.png")));
            issMarker = (ImageIO.read(getClass().getResource("iss.png")));
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(null, "Error Loading ISS Icon :(", "Error", JOptionPane.ERROR_MESSAGE);
        }

        //Load Base World Map
        try {
            URI mapUri = new URI("https://upload.wikimedia.org/wikipedia/commons/8/83/Equirectangular_projection_SW.jpg");
            mapImage = ImageIO.read(mapUri.toURL());
        } catch (URISyntaxException | IOException e) {
            JOptionPane.showMessageDialog(null, "Invalid map URI :(", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        mapLabel = new JLabel();
        mapLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(mapLabel, BorderLayout.CENTER);

        //Refresh ISS Target Position Every 10s
        new Timer(10000, e -> updateISSPosition()).start();

        //Animation Timer (60 FPS)
        new Timer(17, e -> animateAndDraw()).start();

        updateISSPosition();
    }

    private void updateISSPosition() {
        try {
            URI api = new URI("http://api.open-notify.org/iss-now.json");
            HttpURLConnection c = (HttpURLConnection) api.toURL().openConnection();
            c.setRequestMethod("GET");

            try (BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                StringBuilder re = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) re.append(line);
                
                JSONObject json = new JSONObject(re.toString());
                JSONObject pos = json.getJSONObject("iss_position");

                targetLat = pos.getDouble("latitude");
                targetLon = pos.getDouble("longitude");

                //Save Timestamp in UTC
                Instant now = Instant.now();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);
                lastUpdTime = fmt.format(now);
            }
        } catch (URISyntaxException | IOException | JSONException e) {
            JOptionPane.showMessageDialog(null, "Error fetching ISS position Data :(", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void animateAndDraw() {
        //Smooth motion
        currentLat += (targetLat - currentLat) * 0.05;
        currentLon += (targetLon - currentLon) * 0.05;

        //Trail
        trail.add(new Point.Double(currentLat, currentLon));
        if (trail.size() > 50) trail.removeFirst();

        //Draw to Frame
        BufferedImage frame = new BufferedImage(mapImage.getWidth(), mapImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = frame.createGraphics();
        g.drawImage(mapImage, 0, 0, null);

        //Draw Day/Night shading
        drawNightOverlay(g, frame.getWidth(), frame.getHeight());

        //Draw Trail
        g.setColor(new Color(255, 80, 80, 180));
        Point prev = null;
        for (Point.Double p : trail) {
            int x = (int) ((p.y + 180) * (mapImage.getHeight() / 360.0));
            int y = (int) ((90 - p.x) * (mapImage.getHeight() / 180.0));
            if (prev != null) g.drawLine(prev.x, prev.y, x, y);
            prev = new Point(x, y);
        }

        //Draw ISS
        int x = (int) ((currentLon + 180) * (mapImage.getWidth() / 360.0));
        int y = (int) ((90 - currentLat) * (mapImage.getHeight() / 180.0));
        g.drawImage(issMarker, x - 12, y - 12, 24, 24, null);

        //Draw Last Upddate time
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 16));
        g.drawString("Last update: " + lastUpdTime, 10, 20);

        g.dispose();

        //Resize And Show
        Image scaled = frame.getScaledInstance(mapLabel.getWidth(), mapLabel.getHeight(), Image.SCALE_SMOOTH);
        mapLabel.setIcon(new ImageIcon(scaled));
    }

    private void drawNightOverlay(Graphics2D g, int width, int height) {
        //Approximate Sun Position
        long now = System.currentTimeMillis();
        double fractionalDay = (now % 86400000L) / 86400000.0;
        double subSolarLon = (fractionalDay * 360) - 180;

        //Draw a Semi-Transparent dark overlay for night side :3
        GradientPaint gp = new GradientPaint((float) ((subSolarLon + 180) / 360.0 * width), 0, new Color(0, 0, 0, 0), (float) ((subSolarLon + 180) / 360.0 * width + width / 2f), 0, new Color(0, 0, 0, 150));
        g.setPaint(gp);
        g.fillRect(0, 0, width, height);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ISSTracker app = new ISSTracker();
            app.setVisible(true);
        });
    }
}