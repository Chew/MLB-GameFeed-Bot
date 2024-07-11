package pw.chew.mlb.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around MLB's game data to make it easier to access.
 *
 * @param gameData The game data from the MLB API
 */
public record GameState(JSONObject gameData) {
    /**
     * Retrieves the latest game data for the provided game PK (ID)
     *
     * @param gamePk The gamePk of the game to get the state of
     * @return The game state
     */
    @NotNull
    public static GameState fromPk(String gamePk) {
        String res = RestClient.get("https://statsapi.mlb.com/api/v1.1/game/:id/feed/live?language=en&fields=gameData,game,pk,datetime,dateTime,status,detailedState,abstractGameState,liveData,plays,allPlays,result,description,awayScore,homeScore,event,about,isComplete,count,balls,strikes,outs,playEvents,details,isInPlay,isScoringPlay,description,event,eventType,hitData,launchSpeed,launchAngle,totalDistance,trajectory,hardness,isPitch,atBatIndex,playId,currentPlay,count,outs,matchup,batter,fullName,pitcher,fullName,postOnFirst,fullName,postOnSecond,postOnThird,fullName,linescore,currentInning,currentInningOrdinal,inningState,linescore,teams,home,name,clubName,abbreviation,runs,away,runs,innings,num,home,runs,away,runs,teams,home,runs,hits,errors,leftOnBase,away,runs,hits,errors,leftOnBase,decisions,winner,fullName,id,loser,save,boxscore,teams,away,home,players,stats,pitching,note"
            .replace(":id", gamePk));

        try {
            JSONObject json = new JSONObject(res);

            return new GameState(json);
        } catch (JSONException e) {
            return new GameState(new JSONObject());
        }
    }

    /**
     * Whether the request was failed.
     * @return true if the request failed, false otherwise
     */
    public boolean failed() {
        return !gameData().has("gameData");
    }

    /**
     * Gets the current line score object.
     *
     * @return The line score object
     */
    public JSONObject lineScore() {
        return gameData().getJSONObject("liveData").getJSONObject("linescore");
    }

    /**
     * Gets the current game state. E.g. "Final", "Scheduled" etc.
     *
     * @return The game state
     */
    public String gameState() {
        return gameData().getJSONObject("gameData").getJSONObject("status").getString("abstractGameState");
    }

    /**
     * Check if a game is canceled.
     * Only minor league and spring training games get canceled, postponement is not true for this method.
     *
     * @return true if the game is canceled, false otherwise
     */
    public boolean isCancelled() {
        return gameData().getJSONObject("gameData").getJSONObject("status").getString("detailedState").equals("Cancelled");
    }

    public boolean isSuspended() {
        return gameData().getJSONObject("gameData").getJSONObject("status").getString("detailedState").contains("Suspended");
    }

    /**
     * Whether a game is final.
     * @return true if the game is final, false otherwise
     */
    public boolean isFinal() {
        return gameState().equals("Final");
    }

    /**
     * Gets the TeamInfo record for this game's away team. The provided object contains info from this JSON object.
     *
     * @return the TeamInfo record for this game's away team
     */
    public TeamInfo away() {
        var gameData = gameData().getJSONObject("gameData").getJSONObject("teams").getJSONObject("away");
        var lineData = lineScore().getJSONObject("teams").getJSONObject("away");

        return new TeamInfo(gameData, lineData);
    }

    /**
     * Gets the TeamInfo record for this game's home team.
     *
     * @return the TeamInfo record for this game's home team
     */
    public TeamInfo home() {
        var gameData = gameData().getJSONObject("gameData").getJSONObject("teams").getJSONObject("home");
        var lineData = lineScore().getJSONObject("teams").getJSONObject("home");

        return new TeamInfo(gameData, lineData);
    }

    /**
     * The official date and time of the game. E.g., when the game is officially slated to start.
     * @return The official date and time of the game
     */
    public OffsetDateTime officialDate() {
        String officialDate = gameData().getJSONObject("gameData").getJSONObject("datetime").getString("dateTime");

        // The format is 1901-04-19T09:33:00Z, convert it to a Java OffsetDateTime
        return OffsetDateTime.parse(officialDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * The current inning of the game.
     *
     * @return The current inning of the game
     */
    public int inning() {
        return lineScore().optInt("currentInning", 0);
    }

    /**
     * The current inning as an ordinal, e.g. "4th"
     *
     * @return The current inning as an ordinal
     */
    public String inningOrdinal() {
        return lineScore().getString("currentInningOrdinal");
    }

    /**
     * The current inning state. Usually "Top" or "Bottom" but can also be "Mid" or "End"
     *
     * @return The current inning state
     */
    public String inningState() {
        return lineScore().getString("inningState");
    }

    /**
     * All the plays of the game.
     *
     * @return all the plays of the game
     */
    public JSONArray plays() {
        return gameData.getJSONObject("liveData").getJSONObject("plays").getJSONArray("allPlays");
    }

    /**
     * The current play of the game.
     *
     * @return The current play of the game
     */
    public JSONObject currentPlay() {
        return gameData.getJSONObject("liveData").getJSONObject("plays").getJSONObject("currentPlay");
    }

    /**
     * The current pitcher's full name. E,g, "Jacob deGrom"
     *
     * @return the current pitcher's full name.
     */
    public String currentPitcher() {
        return currentPlay().getJSONObject("matchup").getJSONObject("pitcher").getString("fullName");
    }

    /**
     * The current batter's full name. E.g., "Mike Trout"
     *
     * @return the current batter's full name
     */
    public String currentBatter() {
        return currentPlay().getJSONObject("matchup").getJSONObject("batter").getString("fullName");
    }

    /**
     * Gets who is currently on a base. E.g., "1st: Mike Trout".
     * Can also be "No one is on base." if no one is on base.
     *
     * @return who is currently on a base
     */
    public String currentBases() {
        List<String> bases = new ArrayList<>();
        if (currentPlay().getJSONObject("matchup").has("postOnFirst")) {
            bases.add("1st: " + currentPlay().getJSONObject("matchup").getJSONObject("postOnFirst").getString("fullName"));
        }
        if (currentPlay().getJSONObject("matchup").has("postOnSecond")) {
            bases.add("2nd: " + currentPlay().getJSONObject("matchup").getJSONObject("postOnSecond").getString("fullName"));
        }
        if (currentPlay().getJSONObject("matchup").has("postOnThird")) {
            bases.add("3rd: " + currentPlay().getJSONObject("matchup").getJSONObject("postOnThird").getString("fullName"));
        }

        if (bases.isEmpty()) {
            return "No one is on base.";
        }

        return String.join("\n", bases);
    }

    /**
     * Gets the last completed play. Might be null if no plays have been completed.
     *
     * @return the last completed play
     */
    @Nullable
    public JSONObject lastCompletedPlay() {
        for (int i = plays().length() - 1; i >= 0; i--) {
            JSONObject play = plays().getJSONObject(i);
            if (play.getJSONObject("about").getBoolean("isComplete")) {
                return play;
            }
        }
        return null;
    }

    /**
     * Gets the current play description. If there has not been a play yet, returns an empty string.
     *
     * @return the current play description
     */
    @NotNull
    public String currentPlayDescription() {
        JSONObject play = lastCompletedPlay();

        if (play == null) {
            return "";
        }

        return play.getJSONObject("result").getString("description");
    }

    /**
     * Gets the current number of outs this inning.
     *
     * @return the current number of outs this inning
     */
    public int outs() {
        JSONObject play = currentPlay();

        if (play == null) {
            return 0;
        }

        return play.getJSONObject("count").getInt("outs");
    }

    /**
     * Gets the current hit. If there is no hit, or there is no statcast data, returns null.
     *
     * @return the current hit
     */
    @Nullable
    public JSONObject currentHit() {
        JSONObject currentPlay = lastCompletedPlay();
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

    /**
     * Gets the current hit info as a string. E.g., "Ball left the bat at a speed of 100 mph at a 45° angle, and travelled 400 feet."
     * Will be null if there is no hit data for the current play.
     *
     * @return the current hit info as a string
     */
    @Nullable
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

    /**
     * Checks to see if a hit is a potential homer. Only true if the hit was more than 300 feet
     *
     * @return true if the hit is a potential homer, false otherwise
     */
    public boolean potentialHomer() {
        JSONObject event = currentHit();

        if (event == null) {
            return false;
        } else {
            JSONObject hitData = event.getJSONObject("hitData");
            return hitData.getFloat("totalDistance") >= 300.0;
        }
    }

    /**
     * Gets the current homer at parks data. If there is no hit data, returns null.
     * Data may take up to 30 seconds to be available.
     *
     * @return the current homer at parks data
     */
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

    /**
     * Gets the current homer description with data from Baseball Savant. If there is no hit data, returns null.
     * If statcast returns an error, a generic error message is returned.
     *
     * @return the current homer description
     */
    @Nullable
    public String homerDescription() {
        JSONObject homers = homerAtParks();

        if (homers == null) {
            return null;
        }

        if (homers.has("error")) {
            return "Failed to retrieve homer data. Thanks MLB!";
        }

        JSONArray hrs = homers.getJSONArray("hr");
        JSONArray not = homers.getJSONArray("not");

        int ballparks = hrs.toList().size();

        // was it a homer or not? this determines if we say "would've been a homer" versus "would also be a homer"
        boolean isHomer = currentPlayDescription().contains("homers") || currentPlayDescription().contains("grand slam");

        // now we see if it was away/home. if it's away, we'll see if it was a homer at their home field
        String awayAbbrev = away().abbreviation();
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
            ballparks > 0 ? (", %s %s".formatted(awayBpHomer ? "including" : "but not", awayBallpark)) : ""
        );
    }

    /**
     * Gets the current "at bat" index. Basically just how many people have been up to bat so far.
     * -1 if no plays have been completed.
     *
     * @return the current "at bat" index
     */
    public int atBatIndex() {
        JSONObject currentPlay = lastCompletedPlay();

        if (currentPlay == null) {
            return -1;
        }

        return currentPlay.getJSONObject("about").getInt("atBatIndex");
    }

    /**
     * Checks if the current ball is in play. What is defined as "in play" is up to MLB's API.
     *
     * @return true if the current ball is in play, false otherwise
     */
    public boolean currentBallInPlay() {
        JSONObject currentPlay = lastCompletedPlay();

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

    /**
     * Gets a list of every game advisory so far.
     * Advisories are everything from "Mound Visits" to defense changes.
     *
     * @return a list of every game advisory so far
     */
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
     * <br>Example: [Top 4th] Dodgers 2 - 1 Yankees
     * @return The topic friendly state
     */
    public String topicState() {
        if (gameState().equals("Final")) {
            return String.format("Final: %s %s - %s %s",
                away().clubName(), away().runs(), home().runs(), home().clubName());
        } else {
            return String.format("[%s %s] %s %s - %s %s",
                inningState(),
                inningOrdinal(),
                away().clubName(),
                away().runs(),
                home().runs(),
                home().clubName());
        }
    }

    /**
     * Builds the decisions of the game.
     * Decisions are who was the winning pitcher, losing pitcher, and who got the save.
     *
     * @return The decisions of the game
     */
    public String decisions() {
        JSONObject decisions = gameData().getJSONObject("liveData").getJSONObject("decisions");

        List<String> response = new ArrayList<>();

        JSONObject awayPlayers = gameData().getJSONObject("liveData").getJSONObject("boxscore").getJSONObject("teams").getJSONObject("away").getJSONObject("players");
        JSONObject homePlayers = gameData().getJSONObject("liveData").getJSONObject("boxscore").getJSONObject("teams").getJSONObject("home").getJSONObject("players");

        for (String key : decisions.keySet()) {
            JSONObject decision = decisions.getJSONObject(key);
            int id = decision.getInt("id");
            String name = decision.getString("fullName");

            JSONObject player;
            if (awayPlayers.has("ID" + id)) {
                player = awayPlayers.getJSONObject("ID" + id);
            } else {
                player = homePlayers.getJSONObject("ID" + id);
            }

            String note = player.getJSONObject("stats").getJSONObject("pitching").getString("note");

            // Capitalize the key
            String keyCapitalized = key.substring(0, 1).toUpperCase() + key.substring(1);

            response.add("%s: %s %s".formatted(keyCapitalized, name, note));
        }

        return String.join("\n", response);
    }

    public int winningTeam() {
        JSONObject scores = gameData().getJSONObject("liveData").getJSONObject("linescore").getJSONObject("teams");
        int homeRuns = scores.getJSONObject("home").getInt("runs");
        int awayRuns = scores.getJSONObject("away").getInt("runs");

        JSONObject homeTeam = gameData().getJSONObject("gameData").getJSONObject("teams").getJSONObject("home");
        JSONObject awayTeam = gameData().getJSONObject("gameData").getJSONObject("teams").getJSONObject("away");

        return (homeRuns > awayRuns ? homeTeam : awayTeam).getInt("id");
    }

    /**
     * Builds a summary of the game. This includes the score, who won/is winning, and the current inning.
     * Present tense if the game is ongoing, past tense if the game is over.
     *
     * @return the summary of the game
     */
    public String summary() {
        int homeRuns = home().runs();
        int awayRuns = away().runs();

        String winning = homeRuns > awayRuns ? home().clubName() : away().clubName();
        String losing = homeRuns > awayRuns ? away().clubName() : home().clubName();

        String score = homeRuns > awayRuns ? "%s - %s".formatted(homeRuns, awayRuns) : "%s - %s".formatted(awayRuns, homeRuns);

        if (isFinal()) {
            if (awayRuns == homeRuns) {
                return "Well, that's odd; the %s tied the %s, %s.".formatted(away().clubName(), home().clubName(), score);
            }

            return "The %s beat the %s, %s.".formatted(winning, losing, score);
        } else {
            String currentInning = "the %s of the %s".formatted(lineScore().getString("inningState"), lineScore().getString("currentInningOrdinal"));

            if (awayRuns == homeRuns) {
                return "The %s are tied with the %s %s at %s.".formatted(away().clubName(), home().clubName(), score, currentInning);
            } else {
                return "The %s are leading the %s, %s in %s.".formatted(winning, losing, score, currentInning);
            }
        }
    }

    /**
     * Wrapper around the game data's teams object.
     *
     * @param gameData data from MLB API at gameData > teams > home or away
     * @param lineData data from MLB API at liveData > linescore > teams > home or away
     */
    public record TeamInfo(JSONObject gameData, JSONObject lineData) {
        /**
         * The team's ID on MLB's API.
         *
         * @return the team's ID
         */
        public int id() {
            return gameData().getInt("id");
        }

        /**
         * The team's full name on MLB's API.
         * E.g. "Los Angeles Dodgers"
         *
         * @return the team's full name
         */
        public String name() {
            return gameData().getString("name");
        }

        /**
         * The team's abbreviation on MLB's API.
         * E.g. "LAD"
         *
         * @return the team's abbreviation
         */
        public String abbreviation() {
            return gameData().getString("abbreviation");
        }

        /**
         * Gets the Club Name of the team.
         * E.g. "Rangers"
         *
         * @return the club name of the team
         */
        public String clubName() {
            return gameData().getString("clubName");
        }

        /**
         * Gets the current runs of the team.
         *
         * @return the current runs of the team
         */
        public int runs() {
            return lineData().getInt("runs");
        }
    }
}
