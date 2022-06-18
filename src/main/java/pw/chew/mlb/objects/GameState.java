package pw.chew.mlb.objects;

import org.json.JSONArray;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record GameState(JSONObject gameData) {
    public GameState(String gamePk) {
        // Do an initial request to get the game state
        this(new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1.1/game/:id/feed/live?language=en&fields=gameData,status,detailedState,abstractGameState,liveData,plays,allPlays,result,description,awayScore,homeScore,event,about,isComplete,count,balls,strikes,outs,playEvents,details,description,event,eventType,hitData,launchSpeed,launchAngle,totalDistance,trajectory,hardness,isPitch,atBatIndex,linescore,currentInning,currentInningOrdinal,inningState,linescore,teams,home,name,clubName,runs,away,runs"
            .replace(":id", gamePk))));
    }

    public JSONObject lineScore() {
        return gameData.getJSONObject("liveData").getJSONObject("linescore");
    }

    public String gameState() {
        return gameData.getJSONObject("gameData").getJSONObject("status").getString("abstractGameState");
    }

    public String homeTeam() {
        return gameData().getJSONObject("gameData").getJSONObject("teams").getJSONObject("home").getString("clubName");
    }

    public String awayTeam() {
        return gameData().getJSONObject("gameData").getJSONObject("teams").getJSONObject("away").getString("clubName");
    }

    public int inning() {
        return lineScore().getInt("currentInning");
    }

    public String inningOrdinal() {
        return lineScore().getString("currentInningOrdinal");
    }

    public String inningState() {
        return lineScore().getString("inningState");
    }

    public JSONArray plays() {
        return gameData.getJSONObject("liveData").getJSONObject("plays").getJSONArray("allPlays");
    }

    public JSONObject currentPlay() {
        for (int i = plays().length() - 1; i >= 0; i--) {
            JSONObject play = plays().getJSONObject(i);
            if (play.getJSONObject("about").getBoolean("isComplete")) {
                return play;
            }
        }
        return null;
    }

    public String currentPlayDescription() {
        return currentPlay().getJSONObject("result").getString("description");
    }

    public int outs() {
        return currentPlay().getJSONObject("count").getInt("outs");
    }

    public String hitInfo() {
        JSONObject currentPlay = currentPlay();
        if (currentPlay == null) {
            return null;
        }

        JSONArray playEvents = currentPlay.getJSONArray("playEvents");
        for (Object eventObj : playEvents) {
            JSONObject event = (JSONObject) eventObj;

            if (!event.has("hitData")) {
                continue;
            }

            JSONObject hitData = event.getJSONObject("hitData");

            return String.format("Ball left the bat at a speed of %s mph at a %s° angle, and travelled %s feet.",
                hitData.getFloat("launchSpeed"),
                hitData.getFloat("launchAngle"),
                hitData.getFloat("totalDistance"));
        }

        return null;
    }

    public int atBatIndex() {
        JSONObject currentPlay = currentPlay();

        if (currentPlay == null) {
            return -1;
        }

        return currentPlay.getJSONObject("about").getInt("atBatIndex") - 1;
    }

    public int awayScore() {
        return lineScore().getJSONObject("teams").getJSONObject("away").getInt("runs");
    }

    public int homeScore() {
        return lineScore().getJSONObject("teams").getJSONObject("home").getInt("runs");
    }

    public List<String> gameAdvisories() {
        List<String> advisories = new ArrayList<>();

        for (Object playObj : plays()) {
            JSONObject play = (JSONObject) playObj;

            JSONArray playEvents = play.getJSONArray("playEvents");
            for (Object eventObj : playEvents) {
                JSONObject event = (JSONObject) eventObj;

                boolean isPitch = event.getBoolean("isPitch");

                if (!isPitch) {
                    advisories.add(event.getJSONObject("details").getString("description"));
                }
            }
        }

        return advisories;
    }
}
