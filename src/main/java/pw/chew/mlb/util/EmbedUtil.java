package pw.chew.mlb.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class EmbedUtil {
    private EmbedUtil() {}

    /**
     * Returns a simple failure embed with the given message
     *
     * @param message The message to display
     * @return The failure embed
     */
    public static MessageEmbed failure(String message) {
        return new EmbedBuilder()
            .setTitle("Time Out!")
            .setDescription(message)
            .setColor(0xd23d33)
            .build();
    }
}
