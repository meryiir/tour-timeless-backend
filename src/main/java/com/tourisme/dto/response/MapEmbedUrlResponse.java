package com.tourisme.dto.response;

/**
 * {@code embedUrl} uses Google's legacy {@code output=embed} form when coordinates can be
 * extracted from the resolved Maps URL (e.g. after following maps.app.goo.gl redirects).
 */
public record MapEmbedUrlResponse(String embedUrl, String resolvedUrl) {}
