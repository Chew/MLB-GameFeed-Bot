package pw.chew.mlb.util;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.ApplicationEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import pw.chew.chewbotcca.util.RestClient;

import java.util.ArrayList;
import java.util.List;

import static pw.chew.mlb.MLBBot.SEASON;

/**
 * Team Emoji. Pulls from the bot's emoji on Discord and matches them to teams.
 */
public record TeamEmoji(String name, String clubName, int id, Emoji emoji) {
    private static final List<TeamEmoji> cache = new ArrayList<>();

    public static void setupEmoji(JDA jda) {
        LoggerFactory.getLogger(TeamEmoji.class).debug("Setting up emojis...");

        // Retrieve Emoji from Discoed
        JSONArray teams = RestClient.get("https://statsapi.mlb.com/api/v1/teams?sportIds=1,11,12,13,14&season=%s&fields=teams,id,name,clubName,active".formatted(SEASON)).asJSONObject().getJSONArray("teams");
        List<ApplicationEmoji> emojis = jda.retrieveApplicationEmojis().complete();

        // iterate through emojis
        for (ApplicationEmoji emoji : emojis) {
            String emojiName = emoji.getName();

            if (!emojiName.startsWith("team_")) {
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
     * @deprecated use {@link #fromTeamId(int)} or {@link #fromTeamId(String)} instead
     */
    @Deprecated
    public static Emoji fromName(String input) {
        TeamEmoji unknownEmoji = cache.stream().filter(teamEmoji -> teamEmoji.name().equals("Unknown")).findFirst().orElseThrow();

        return cache.stream().filter(teamEmoji -> teamEmoji.name().equals(input)).findFirst().orElse(unknownEmoji).emoji();
    }

    /**
     * Gets an emoji based on the club name. This is the "Rangers" in "Texas Rangers"
     * <br>Due to some club names being the same in majors and minors (Like "Chicago Cubs" and "Iowa Cubs"),
     * this method is not recommended
     *
     * @param input the club name
     * @return the emoji
     * @deprecated use {@link #fromTeamId(int)} or {@link #fromTeamId(String)} instead
     */
    @Deprecated
    public static Emoji fromClubName(String input) {
        TeamEmoji unknownEmoji = cache.stream().filter(teamEmoji -> teamEmoji.name().equals("Unknown")).findFirst().orElseThrow();

        return cache.stream().filter(teamEmoji -> teamEmoji.clubName().equals(input)).findFirst().orElse(unknownEmoji).emoji();
    }

    /**
     * Gets an emoji based on the team ID. This is the "140" in "Texas Rangers"
     * @param id the team ID
     * @return the emoji or unknown if not found
     */
    public static Emoji fromTeamId(int id) {
        TeamEmoji unknownEmoji = cache.stream().filter(teamEmoji -> teamEmoji.name().equals("Unknown")).findFirst().orElseThrow();

        return cache.stream().filter(teamEmoji -> teamEmoji.id() == id).findFirst().orElse(unknownEmoji).emoji();
    }

    /**
     * Gets an emoji based on the team ID. This is the "140" in "Texas Rangers"
     * @param id the team ID
     * @return the emoji or unknown if not found
     */
    public static Emoji fromTeamId(String id) {
        return fromTeamId(Integer.parseInt(id));
    }
}
