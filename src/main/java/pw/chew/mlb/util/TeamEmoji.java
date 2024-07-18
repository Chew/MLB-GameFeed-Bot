package pw.chew.mlb.util;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import pw.chew.chewbotcca.util.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Emoii come from a server, so it may not render if the bot is not in that server!
 * TODO: A better way to handle these, this is a mess.
 */
public record TeamEmoji(String name, String clubName, int id, Emoji emoji) {
    private static final List<TeamEmoji> cache = new ArrayList<>();

    public static void setupEmoji(JDA jda) {
        LoggerFactory.getLogger(TeamEmoji.class).debug("Setting up emojis...");

        // Retrieve Emoji from Discoed
        JSONArray teams = new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1/teams?sportIds=1,11,12,13,14&season=2024&fields=teams,id,name,clubName,active")).getJSONArray("teams");
        // TODO: Convert this to native JDA calls when possible
        JSONArray emojis = new JSONObject(RestClient.get("https://discord.com/api/v10/applications/%s/emojis".formatted(jda.getSelfUser().getId()), jda.getToken())).getJSONArray("items");

        // iterate through emojis
        for (Object emojiObj : emojis) {
            JSONObject emojiJsonObj = (JSONObject) emojiObj;
            DataObject emojiData = DataObject.fromJson(emojiJsonObj.toString());
            Emoji emoji = Emoji.fromData(emojiData);

            String emojiName = emoji.getName();

            // skip _icon, TODO: Make it check for _[number]
            if (emojiName.endsWith("_icon")) {
                continue;
            }

            // we need everything after the FINAL _
            String emojiTeamId = emojiName.substring(emojiName.lastIndexOf("_") + 1);
            int emojiTeamIdInt = Integer.parseInt(emojiTeamId);

            // check hardcoded first
            switch (emojiTeamIdInt) {
                case 159 -> cache.add(new TeamEmoji("American League All-Stars", "American", 159, emoji));
                case 160 -> cache.add(new TeamEmoji("National League All-Stars", "National", 160, emoji));
                case 0 -> cache.add(new TeamEmoji("Unknown", "Unknown", 0, emoji));
            }

            // iterate through teams
            for (Object teamObj : teams) {
                JSONObject teamJsonObj = (JSONObject) teamObj;
                if (teamJsonObj.getInt("id") == emojiTeamIdInt) {
                    TeamEmoji teamEmoji = new TeamEmoji(teamJsonObj.getString("name"), teamJsonObj.getString("clubName"), emojiTeamIdInt, emoji);
                    cache.add(teamEmoji);
                    break;
                }
            }
        }

        LoggerFactory.getLogger(TeamEmoji.class).debug("Set up {} emoji!", cache.size());
    }

    /**
     * Gets an emoji based on the team name. This is the FULL name, e.g Texas Rangers
     *
     * @return the emoji
     */
    public static Emoji fromName(String input) {
        TeamEmoji unknownEmoji = cache.stream().filter(teamEmoji -> teamEmoji.name().equals("Unknown")).findFirst().orElseThrow();

        return cache.stream().filter(teamEmoji -> teamEmoji.name().equals(input)).findFirst().orElse(unknownEmoji).emoji();
    }

    /**
     * Gets an emoji based on the club name. This is the "Rangers" in "Texas Rangers"
     * @param input the club name
     * @return the emoji
     */
    public static Emoji fromClubName(String input) {
        TeamEmoji unknownEmoji = cache.stream().filter(teamEmoji -> teamEmoji.name().equals("Unknown")).findFirst().orElseThrow();

        return cache.stream().filter(teamEmoji -> teamEmoji.clubName().equals(input)).findFirst().orElse(unknownEmoji).emoji();
    }
}
