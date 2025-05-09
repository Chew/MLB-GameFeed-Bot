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

import net.dv8tion.jda.api.entities.User;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * A collection of misc utilities used throughout the bot
 */
public class MiscUtil {
    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

    /**
     * Gets a random item from an array
     *
     * @param array the array
     * @return a random value from the array
     */
    @SafeVarargs
    public static <T> T getRandom(T... array) {
        return array[RANDOM.nextInt(array.length)];
    }

    /**
     * Sorts a given map of objects by their value
     *
     * @param map the map to sort
     * @param <K> key
     * @param <V> value (to sort by)
     * @return a sorted map
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * <a href="https://github.com/ChewMC/TransmuteIt/blob/2b86/src/pw/chew/transmuteit/DiscoveriesCommand.java#L174-L186">Source</a><br>
     * Capitalizes a String, e.g. "BRUH_MOMENT" -> "Bruh Moment"
     *
     * @param to the unformatted string
     * @return a formatting string
     * @author Chew
     */
    public static String capitalize(String to) {
        if (to.isBlank()) {
            return "";
        }
        String[] words = to.split("_");
        StringBuilder newword = new StringBuilder();
        for (String word : words) {
            String rest = word.substring(1).toLowerCase();
            String first = word.substring(0, 1).toUpperCase();
            newword.append(first).append(rest).append(" ");
        }
        return newword.toString().trim();
    }

    /**
     * Converts a given amount of bytes into friendlier data.<br>
     * Example: 2048 => 2 KB
     *
     * @param bytes the amount of bytes
     * @return the formatted string
     */
    public static String bytesToFriendly(long bytes) {
        // Find size of repo and list it
        int k = 1024;
        String[] measure = {"B", "KB", "MB", "GB", "TB"};
        double i;
        if (bytes == 0) {
            i = 0;
        } else {
            i = Math.floor(Math.log(bytes) / Math.log(k));
        }
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(bytes / Math.pow(k, i)) + " " + measure[(int) i + 1];
    }

    /**
     * Parse a given date with a given format
     *
     * @param date   The date string to parse
     * @param format The format to parse
     * @return the parsed date
     */
    public static OffsetDateTime dateParser(String date, String format) {
        return OffsetDateTime.parse(date, DateTimeFormatter.ofPattern(format));
    }

    /**
     * Parse a given date with a given format
     *
     * @param date   The date string to parse
     * @param format The format to parse
     * @return the parsed date
     */
    public static OffsetDateTime dateParser(String date, DateTimeFormatter format) {
        return OffsetDateTime.parse(date, format);
    }

    /**
     * Delimits a value into common "1,000,000.00" (US) format.
     *
     * @param value The value
     * @return A delimited string
     */
    public static String delimitNumber(long value) {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }

    /**
     * Formats a percentage based on a decimal value.
     * E.g. 0.05 => 5%
     *
     * @param value The value
     * @return A percentage
     */
    public static String formatPercent(float value) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(value * 100) + "%";
    }

    /**
     * Converts a JSONArray to a list of {@code <T>}.
     *
     * @param array The JSONArray
     * @param cast The class to cast to
     * @param <T> The type to cast to
     * @return A list of {@code <T>}
     */
    public static <T> List<T> toList(JSONArray array, Class<T> cast) {
        List<T> results = new ArrayList<>(array.length());
        for (Object element : array) {
            if (element == null || JSONObject.NULL.equals(element)) {
                results.add(null);
            } else {
                results.add(cast.cast(element));
            }
        }
        return results;
    }

    /**
     * Returns the tag of a user. If they have a discriminator, it'll be username#0000, otherwise, it'll be their username.
     *
     * @param user the user
     * @return the user's tag
     */
    public static String getTag(User user) {
        if (user.getDiscriminator().equals("0000")) {
            return user.getName();
        } else {
            return user.getAsTag();
        }
    }

    public static int asInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static long asLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
