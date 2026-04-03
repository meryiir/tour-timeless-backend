package com.tourisme.service;

import com.tourisme.dto.response.MapEmbedUrlResponse;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleMapsEmbedResolverService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "maps.app.goo.gl",
            "goo.gl",
            "www.google.com",
            "google.com",
            "maps.google.com",
            "www.maps.google.com"
    );

    private static final Pattern AT = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
    private static final Pattern DATA_3D4D = Pattern.compile("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)");
    private static final Pattern Q_NUM = Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)(?:&|#|$)");
    private static final Pattern LL = Pattern.compile("[?&]ll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)(?:&|#|$)");

    public MapEmbedUrlResponse resolve(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return new MapEmbedUrlResponse(null, null);
        }
        String trimmed = urlString.trim();
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException e) {
            return new MapEmbedUrlResponse(null, null);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return new MapEmbedUrlResponse(null, null);
        }
        String host = uri.getHost();
        if (host == null) {
            return new MapEmbedUrlResponse(null, null);
        }
        String hostLower = host.toLowerCase();
        boolean allowed = ALLOWED_HOSTS.contains(hostLower)
                || hostLower.endsWith(".goo.gl");
        if (!allowed) {
            return new MapEmbedUrlResponse(null, null);
        }

        try {
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "TourismeMapResolver/1.0")
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            String finalUrl = resp.uri() != null ? resp.uri().toString() : trimmed;
            String blob = finalUrl + "\n" + truncate(resp.body(), 96_000);
            Optional<String[]> coords = firstMatchingCoords(blob);
            if (coords.isEmpty()) {
                return new MapEmbedUrlResponse(null, finalUrl);
            }
            String[] c = coords.get();
            String embed = "https://www.google.com/maps?q=" + c[0] + "," + c[1] + "&z=11&output=embed";
            return new MapEmbedUrlResponse(embed, finalUrl);
        } catch (Exception e) {
            return new MapEmbedUrlResponse(null, null);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static Optional<String[]> firstMatchingCoords(String blob) {
        for (Pattern p : new Pattern[]{AT, DATA_3D4D, Q_NUM, LL}) {
            Matcher m = p.matcher(blob);
            if (m.find()) {
                return Optional.of(new String[]{m.group(1), m.group(2)});
            }
        }
        return Optional.empty();
    }
}
