package com.tourisme.controller;

import com.tourisme.repository.ActivityRepository;
import com.tourisme.repository.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Public SEO endpoints: dynamic sitemap and robots.txt using {@code app.seo.site-url} / {@code SITE_PUBLIC_URL}.
 */
@RestController
@RequestMapping("/api/seo")
@RequiredArgsConstructor
public class SeoController {

    private final ActivityRepository activityRepository;
    private final DestinationRepository destinationRepository;

    @Value("${app.seo.site-url:http://localhost:5173}")
    private String siteUrl;

    private static final DateTimeFormatter W3C_DT = DateTimeFormatter.ISO_INSTANT;

    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public String sitemap() {
        String base = siteUrl.replaceAll("/$", "");
        String lastmod = W3C_DT.format(Instant.now());
        String[] langs = new String[] {"en", "fr", "es", "de"};

        List<String> chunks = new ArrayList<>();
        appendLangUrls(chunks, base, "/", lastmod, "1.0", langs);
        appendLangUrls(chunks, base, "/activities", lastmod, "0.9", langs);
        appendLangUrls(chunks, base, "/destinations", lastmod, "0.9", langs);
        appendLangUrls(chunks, base, "/about", lastmod, "0.7", langs);
        appendLangUrls(chunks, base, "/contact", lastmod, "0.7", langs);
        appendLangUrls(chunks, base, "/privacy", lastmod, "0.3", langs);
        appendLangUrls(chunks, base, "/terms", lastmod, "0.3", langs);

        for (String slug : activityRepository.findAllActiveSlugs()) {
            if (slug != null && !slug.isBlank()) {
                appendLangUrls(chunks, base, "/activities/" + slug.trim(), lastmod, "0.8", langs);
            }
        }
        for (String slug : destinationRepository.findAllSlugs()) {
            if (slug != null && !slug.isBlank()) {
                appendLangUrls(chunks, base, "/destinations/" + slug.trim(), lastmod, "0.8", langs);
            }
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String chunk : chunks) {
            xml.append(chunk);
        }
        xml.append("</urlset>");
        return xml.toString();
    }

    private static void appendLangUrls(
            List<String> out,
            String base,
            String path,
            String lastmod,
            String priority,
            String[] langs
    ) {
        for (String lang : langs) {
            String loc = base + path + (path.contains("?") ? "&" : "?") + "lang=" + lang;
            out.add("  <url>\n"
                    + "    <loc>" + xmlEscape(loc) + "</loc>\n"
                    + "    <lastmod>" + lastmod + "</lastmod>\n"
                    + "    <changefreq>weekly</changefreq>\n"
                    + "    <priority>" + priority + "</priority>\n"
                    + "  </url>\n");
        }
    }

    private static String xmlEscape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        String base = siteUrl.replaceAll("/$", "");
        String sitemapUrl = base + "/api/seo/sitemap.xml";
        return String.join("\n",
                "User-agent: *",
                "Allow: /",
                "Disallow: /admin",
                "Disallow: /login",
                "Disallow: /register",
                "Disallow: /forgot-password",
                "Disallow: /profile",
                "",
                "Sitemap: " + sitemapUrl,
                ""
        );
    }
}
