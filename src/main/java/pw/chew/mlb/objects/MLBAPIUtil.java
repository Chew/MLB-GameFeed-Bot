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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static List<Player> getLineup(String gamePk, String homeAway) {
        JSONObject data = new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1.1/game/%s/feed/live?language=en".formatted(gamePk)));
        JSONObject boxScore = data.getJSONObject("liveData")
            .getJSONObject("boxscore")
            .getJSONObject("teams");

        Map<Integer, Player> players = new HashMap<>();
        for (String team :new String[]{"home", "away"}) {
            JSONObject teamPlayers = boxScore.getJSONObject(team).getJSONObject("players");
            for (String item : teamPlayers.keySet()) {
                Player player = new Player(teamPlayers.getJSONObject(item));
                players.put(player.id(), player);
            }
        }

        JSONArray lineup = boxScore.getJSONObject(homeAway).getJSONArray("battingOrder");
        List<Player> lineupPlayers = new ArrayList<>();
        for (Object item : lineup) {
            // json array of int player ID
            int playerId = (int) item;
            Player player = players.get(playerId);
            lineupPlayers.add(player);
        }

        return lineupPlayers;
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
            choices.add(new Command.Choice("All Star Game", 160));
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

    public record Player(JSONObject raw) {
        public int id() {
            return raw.getJSONObject("person").getInt("id");
        }

        public String name() {
            return raw.getJSONObject("person").getString("fullName");
        }

        public String number() {
            return raw.getString("jerseyNumber");
        }

        public Position position() {
            return new Position(raw.getJSONObject("position"));
        }

        public boolean isPitcher() {
            return position().abbreviation().equals("P");
        }

        public String era() {
            return raw.getJSONObject("seasonStats").getJSONObject("pitching").getString("era");
        }

        public String avg() {
            return raw.getJSONObject("seasonStats").getJSONObject("batting").getString("avg");
        }

        public String ops() {
            return raw.getJSONObject("seasonStats").getJSONObject("batting").getString("ops");
        }

        public int homers() {
            return raw.getJSONObject("seasonStats").getJSONObject("batting").getInt("homeRuns");
        }

        public String friendlyString() {
            if (isPitcher()) {
                return "%s (%s ERA)".formatted(name(), era());
            } else {
                return "%s (%s) - %s AVG, %s OPS, %s HR".formatted(name(), position().abbreviation(), avg(), ops(), homers());
            }
        }
    }

    private record Position(JSONObject raw) {
        public String name() {
            return raw.getString("name");
        }

        public String abbreviation() {
            return raw.getString("abbreviation");
        }
    }
}
