package com.tourisme.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Convenience SEO endpoints at the site root.
 * <p>
 * Some crawlers and hosting setups expect these resources at:
 * {@code /robots.txt} and {@code /sitemap.xml}.
 */
@RestController
@RequiredArgsConstructor
public class PublicSeoController {

    private final SeoController seoController;

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return seoController.robots();
    }

    @GetMapping(value = "/sitemap.xml", produces = "application/xml")
    public String sitemap() {
        return seoController.sitemap();
    }
}

