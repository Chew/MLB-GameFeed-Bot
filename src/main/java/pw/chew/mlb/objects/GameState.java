package pw.chew.mlb.objects;

import org.json.JSONArray;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public record GameState(JSONObject gameData) {
    public GameState(String gamePk) {
        // Do an initial request to get the game state
        this(new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1.1/game/:id/feed/live?language=en&fields=gameData,game,pk,datetime,dateTime,status,detailedState,abstractGameState,liveData,plays,allPlays,result,description,awayScore,homeScore,event,about,isComplete,count,balls,strikes,outs,playEvents,details,isInPlay,isScoringPlay,description,event,eventType,hitData,launchSpeed,launchAngle,totalDistance,trajectory,hardness,isPitch,atBatIndex,playId,linescore,currentInning,currentInningOrdinal,inningState,linescore,teams,home,name,clubName,abbreviation,runs,away,runs,innings,num,home,runs,away,runs,teams,home,runs,hits,errors,leftOnBase,away,runs,hits,errors,leftOnBase"
            .replace(":id", gamePk))));
    }

    public boolean failed() {
        return !gameData().has("gameData");
    }

    public JSONObject lineScore() {
        return gameData().getJSONObject("liveData").getJSONObject("linescore");
    }

    public String gameState() {
        return gameData().getJSONObject("gameData").getJSONObject("status").getString("abstractGameState");
    }

    public String homeTeam() {
        return gameData().getJSONObject("gameData").getJSONObject("teams").getJSONObject("home").getString("clubName");
    }

    public String awayTeam() {
        return gameData().getJSONObject("gameData").getJSONObject("teams").getJSONObject("away").getString("clubName");
    }

    public OffsetDateTime officialDate() {
        String officialDate = gameData().getJSONObject("gameData").getJSONObject("datetime").getString("dateTime");

        // The format is 1901-04-19T09:33:00Z, convert it to a Java OffsetDateTime
        return OffsetDateTime.parse(officialDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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
        JSONObject play = currentPlay();

        if (play == null) {
            return "";
        }

        return play.getJSONObject("result").getString("description");
    }

    public int outs() {
        JSONObject play = currentPlay();

        if (play == null) {
            return 0;
        }

        return play.getJSONObject("count").getInt("outs");
    }

    public JSONObject currentHit() {
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

            // If there's no launchSpeed, launchAngle, or totalDistance for the hitData, return null
            if (!hitData.has("launchSpeed") || !hitData.has("launchAngle") || !hitData.has("totalDistance")) {
                return null;
            }

            return event;
        }

        return null;
    }

    public String hitInfo() {
        JSONObject event = currentHit();

        if (event == null) {
            return null;
        } else {
            JSONObject hitData = event.getJSONObject("hitData");

            return String.format("Ball left the bat at a speed of %s mph at a %s° angle, and travelled %s feet.",
                hitData.getFloat("launchSpeed"),
                hitData.getFloat("launchAngle"),
                hitData.getFloat("totalDistance"));
        }
    }

    public boolean potentialHomer() {
        JSONObject event = currentHit();

        if (event == null) {
            return false;
        } else {
            JSONObject hitData = event.getJSONObject("hitData");
            return hitData.getFloat("totalDistance") >= 300.0;
        }
    }

    public JSONObject homerAtParks() {
        JSONObject hitData = currentHit();

        if (hitData == null) {
            return null;
        }

        String playId = hitData.getString("playId");

        return new JSONObject(RestClient.get("https://baseballsavant.mlb.com/gamefeed/x-parks/%s/%s?".formatted(
            gameData().getJSONObject("gameData").getJSONObject("game").getInt("pk"), playId
        )));
    }

    public String homerDescription() {
        JSONObject homers = homerAtParks();

        if (homers == null) {
            return null;
        }

        JSONArray hrs = homers.getJSONArray("hr");
        JSONArray not = homers.getJSONArray("not");

        int ballparks = hrs.toList().size();

        // was it a homer or not? this determines if we say "would've been a homer" versus "would also be a homer"
        boolean isHomer = currentPlayDescription().contains("homers") || currentPlayDescription().contains("grand slam");

        // now we see if it was away/home. if it's away, we'll see if it was a homer at their home field
        boolean awayTeam = inningState().equals("Top");
        String awayAbbrev = gameData().getJSONObject("gameData").getJSONObject("teams").getJSONObject("away").getString("abbreviation");
        boolean awayBpHomer = false;
        String awayBallpark = null;

        for (Object bpObj : hrs) {
            JSONObject bp = (JSONObject) bpObj;
            if (bp.getString("team_abbrev").equals(awayAbbrev)) {
                awayBpHomer = true;
                awayBallpark = bp.getString("name");
            }
        }
        if (awayBallpark == null) {
            for (Object bpObj : not) {
                JSONObject bp = (JSONObject) bpObj;
                if (bp.getString("team_abbrev").equals(awayAbbrev)) {
                    awayBallpark = bp.getString("name");
                }
            }
        }

        return "This %s a homer at %s / 30 ballparks%s.".formatted(
            isHomer ? "would also be" : "would've been", ballparks,
            awayTeam ? (", %s %s".formatted(awayBpHomer ? "including" : "but not", awayBallpark)) : ""
        );
    }

    public int atBatIndex() {
        JSONObject currentPlay = currentPlay();

        if (currentPlay == null) {
            return -1;
        }

        return currentPlay.getJSONObject("about").getInt("atBatIndex");
    }

    public boolean currentBallInPlay() {
        JSONObject currentPlay = currentPlay();

        if (currentPlay == null) {
            return false;
        }

        JSONArray events = currentPlay.getJSONArray("playEvents");

        for (int i = 0; i < events.length(); i++) {
            JSONObject playEvent = events.getJSONObject(i);

            // Some pitches don't have isInPlay
            if (!playEvent.getJSONObject("details").has("isInPlay")) {
                continue;
            }

            if (playEvent.getJSONObject("details").getBoolean("isInPlay")) {
                return true;
            }
        }

        return false;
    }

    public int awayScore() {
        return lineScore().getJSONObject("teams").getJSONObject("away").getInt("runs");
    }

    public int homeScore() {
        return lineScore().getJSONObject("teams").getJSONObject("home").getInt("runs");
    }

    public List<JSONObject> gameAdvisories() {
        List<JSONObject> advisories = new ArrayList<>();

        for (Object playObj : plays()) {
            JSONObject play = (JSONObject) playObj;

            JSONArray playEvents = play.getJSONArray("playEvents");
            for (Object eventObj : playEvents) {
                JSONObject event = (JSONObject) eventObj;

                boolean isPitch = event.getBoolean("isPitch");

                if (isPitch) {
                    continue;
                }

                JSONObject details = event.getJSONObject("details");

                if (!details.has("event") || !details.has("eventType")) {
                    continue;
                }

                advisories.add(event);
            }
        }

        return advisories;
    }

    /**
     * Topic friendly state. With the format:
     * <br>[InningState InningOrdinal] AwayTeam AwayScore - HomeScore HomeTeam
     * @return The topic friendly state
     */
    public String topicState() {
        if (gameState().equals("Final")) {
            return String.format("Final: %s %s - %s %s",
                awayTeam(), awayScore(), homeScore(), homeTeam());
        } else {
            return String.format("[%s %s] %s %s - %s %s",
                inningState(),
                inningOrdinal(),
                awayTeam(),
                awayScore(),
                homeScore(),
                homeTeam());
        }
    }
}
