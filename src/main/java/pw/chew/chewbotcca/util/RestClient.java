/*
 * Copyright (C) 2024 Chewbotcca
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package pw.chew.chewbotcca.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Off brand RestClient based on the ruby gem of the same name
 */
public class RestClient {
    public static final String JSON = "application/json; charset=utf-8";
    private static final HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private static final Duration timeout = Duration.ofSeconds(30);
    private static final boolean debug = true;

    /// CACHING ///
    public static final Cache<URI, Response> requests = Caffeine.newBuilder()
        .maximumSize(10_000)
//        .refreshAfterWrite(Duration.ofSeconds(10))
        .expireAfterWrite(Duration.ofSeconds(10))
        .build();

    /**
     * Make a GET request
     *
     * @param url the url to get
     * @param headers Optional set of headers as "Header: Value" like "Authorization: Bearer bob"
     * @throws IllegalArgumentException If an invalid header is passed
     * @throws RuntimeException If the request fails
     * @return a String response
     */
    public static Response get(String url, String ...headers) {
        String userAgent = "MLB Bot/1.0 (JDA; +https://mlb.chew.pw/)";
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", userAgent)
            .timeout(timeout);

        for (String header : headers) {
            String[] details = header.split(":");
            if (details.length != 2) {
                throw new IllegalArgumentException("Invalid header syntax provided: " + header);
            }
            request.header(details[0].trim(), details[1].trim());
        }

        if (debug) LoggerFactory.getLogger(RestClient.class).debug("Making call to GET {}", url.split("\\?")[0]);
        return performRequest(request.build());
    }

    /**
     * Actually perform the request
     * @param request a request
     * @return a response
     */
    public static Response performRequest(HttpRequest request) {
        if (requests.getIfPresent(request.uri()) != null) {
            LoggerFactory.getLogger(RestClient.class).debug("Received response from cache");
            return requests.getIfPresent(request.uri());
        }

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            String body = response.body();
            if (debug) {
                LoggerFactory.getLogger(RestClient.class).debug("Received uncached response");
            }
            Response res = new Response(code, body);
            requests.put(request.uri(), res);
            return res;
        } catch (IOException | InterruptedException e) {
            // Rethrow exceptions as runtime
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * A response from a REST call
     */
    public record Response(int code, String response) {
        /**
         * Check to see if the request was successful.
         * Codes 200-299 are considered successful.
         * @return true if successful
         */
        public boolean success() {
            return code >= 200 && code < 300;
        }

        /**
         * Get the response as a String
         * @return a String
         */
        public String asString() {
            return response;
        }

        /**
         * Get the response as a JSONObject
         * @return a JSONObject
         */
        public JSONObject asJSONObject() {
            return new JSONObject(response);
        }

        /**
         * Get the response as a JSONArray
         * @return a JSONArray
         */
        public JSONArray asJSONArray() {
            return new JSONArray(response);
        }

        /**
         * Get the response as a String
         * @return a String
         */
        @Override
        public String toString() {
            return asString();
        }
    }
}