/*
 * Copyright (C) 2022 Chewbotcca
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.time.Duration;

// Off brand RestClient based on the ruby gem of the same name
public class RestClient {
    private static OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String userAgent = "MLB Bot/1.0 (JDA; +https://mlb.chew.pw/)";

    /// CACHING ///
    public static final Cache<String, String> requests = Caffeine.newBuilder()
        .maximumSize(10_000)
//        .refreshAfterWrite(Duration.ofSeconds(10))
        .expireAfterWrite(Duration.ofSeconds(10))
        .build();

    /**
     * Sets the OkHttp client to use with this RestClient
     *
     * @param client the new client
     */
    public static void setClient(OkHttpClient client) {
        RestClient.client = client;
    }

    /**
     * Make an authenticated GET request
     * @param url the url to get
     * @param authKey an auth key, include Bearer or Bot when necessary
     * @return a response
     */
    public static String get(String url, String authKey) {
        var requestBuilder = new Request.Builder()
            .url(url)
            .get()
            .addHeader("User-Agent", userAgent);

        if (authKey != null) {
            requestBuilder.addHeader("Authorization", authKey);
        }

        Request request = requestBuilder.build();

        if (!url.contains("?")) {
            url = url + "?";
        }

        // Only get the URL up to the first '?'
        String key = url.substring(0, url.indexOf('?'));

        LoggerFactory.getLogger(RestClient.class).debug("Making call to GET " + key);
        if (requests.getIfPresent(url) != null) {
            LoggerFactory.getLogger(RestClient.class).debug("Received response from cache for " + key);
            return requests.getIfPresent(url);
        }
        return performRequest(request);
    }

    /**
     * Make a GET request
     * @param url the url to get
     * @return a response
     */
    public static String get(String url) {
        return get(url, null);
    }

    /**
     * Actually perform the request
     * @param request a request
     * @return a response
     */
    public static String performRequest(Request request) {
        try (Response response = client.newCall(request).execute()) {
            String body;
            ResponseBody responseBody = response.body();
            if(responseBody == null) {
                body = "{}";
            } else {
                body = responseBody.string();
            }
            LoggerFactory.getLogger(RestClient.class).debug("Received uncached response");
            requests.put(request.url().toString(), body);
            return body;
        } catch (SSLHandshakeException e) {
            LoggerFactory.getLogger(RestClient.class).warn("Call to " + request.url() + " failed with SSLHandshakeException!");
            e.printStackTrace();
            return "{error: 'SSLHandshakeException'}";
        } catch (IOException e) {
            LoggerFactory.getLogger(RestClient.class).warn("Call to " + request.url() + " failed with IOException!");
            e.printStackTrace();
            return "{error: 'IOException'}";
        }
    }
}
