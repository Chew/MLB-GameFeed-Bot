package pw.chew.mlb.listeners;

import org.json.JSONArray;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GameFeedHandler {
    private final int gamePk;

    public GameFeedHandler() {
        // Get today's date in EST as MM/DD/YYYY
        String today = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

        // Get today's schedule
        JSONObject data = new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1/schedule?language=en&sportId=1&date=" + today + "&hydrate=game,flags,team"));

        JSONArray games = data.getJSONArray("dates").getJSONObject(0).getJSONArray("games");

        // Iterate games until either the away or home team name is "Texas Rangers"
        for (int i = 0; i < games.length(); i++) {
            JSONObject game = games.getJSONObject(i);
            JSONObject homeTeam = game.getJSONObject("teams").getJSONObject("home");
            JSONObject awayTeam = game.getJSONObject("teams").getJSONObject("away");

            if (homeTeam.getString("name").equals("Texas Rangers") || awayTeam.getString("name").equals("Texas Rangers")) {
                // Get the game id
                gamePk = game.getInt("gamePk");
                return;
            }
        }

        // If we get here, no game was found
        gamePk = -1;
    }
}
