package pw.chew.mlb.objects;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.geysermc.discordbot.util.DicesCoefficient;
import org.json.JSONArray;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static pw.chew.mlb.MLBBot.TEAMS;

public class MLBAPIUtil {
    /// CACHING ///
    public static final Cache<String, Sports> sportsCache = Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofDays(1)).build();
    public static final Cache<String, Teams> teamsCache = Caffeine.newBuilder().maximumSize(1).expireAfterWrite(Duration.ofDays(1)).build();

    // Prevent instantiation
    private MLBAPIUtil() {
    }

    public static Sports getSports() {
        if (sportsCache.getIfPresent("all") != null) {
            return sportsCache.getIfPresent("all");
        }
        Sports sports = new Sports(new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1/sports?fields=")).getJSONArray("sports"));
        sportsCache.put("all", sports);
        return sports;
    }

    public static Teams getTeams() {
        return getTeams("1");
    }

    public static Teams getTeams(String sportId) {
        if (teamsCache.getIfPresent(sportId) != null) {
            return teamsCache.getIfPresent(sportId);
        }
        Teams teams = new Teams(new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1/teams?sportIds=%s&season=2023".formatted(sportId))).getJSONArray("teams"));
        teamsCache.put(sportId, teams);
        return teams;
    }

    public record Sports(JSONArray raw) {
        public List<Command.Choice> asChoices() {
            List<Command.Choice> choices = new ArrayList<>();
            for (int i = 0; i < raw.length(); i++) {
                JSONObject sport = raw.getJSONObject(i);
                choices.add(new Command.Choice(sport.getString("name"), sport.getInt("id")));
            }
            return choices;
        }
    }

    public record Teams(JSONArray raw) {
        public List<Command.Choice> asChoices() {
            List<Command.Choice> choices = new ArrayList<>();
            for (int i = 0; i < raw.length(); i++) {
                JSONObject team = raw.getJSONObject(i);
                choices.add(new Command.Choice(team.getString("name"), team.getInt("id")));
            }
            // only up to 25 choices
            return choices.subList(0, Math.min(choices.size(), 25));
        }

        public List<Command.Choice> potentialChoices(String query) {
            List<Command.Choice> potential = new ArrayList<>();

            // Search the teams by an exact starting match
            for (Object teamObj : raw) {
                JSONObject team = ((JSONObject) teamObj);
                if (team.getString("name").toLowerCase().startsWith(query.toLowerCase())) {
                    potential.add(new Command.Choice(team.getString("name"), team.getInt("id")));
                }
            }

            // Find the best similarity
            for (Object teamObj : raw) {
                JSONObject team = ((JSONObject) teamObj);
                double similar = DicesCoefficient.diceCoefficientOptimized(query.toLowerCase(), team.getString("name").toLowerCase());
                if (similar > 0.2d) {
                    potential.add(new Command.Choice(team.getString("name"), team.getInt("id")));
                }
            }

            // Send a message if we don't know what team
            return potential;
        }
    }
}
