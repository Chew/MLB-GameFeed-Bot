package pw.chew.mlb.listeners;

import com.jagrosh.jdautilities.commons.utils.TableBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.chew.mlb.objects.ActiveGame;
import pw.chew.mlb.objects.ChannelConfig;
import pw.chew.mlb.objects.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static pw.chew.mlb.MLBBot.jda;

public class GameFeedHandler {
    private static final Logger logger = LoggerFactory.getLogger(GameFeedHandler.class);
    public final static Map<String, Thread> GAME_THREADS = new HashMap<>();
    public final static List<ActiveGame> ACTIVE_GAMES = new ArrayList<>();

    /**
     * Adds a game to the active games list.
     * If no game is currently active, the game will be started, otherwise it will be added to the active games list.
     *
     * @param game The game to add to the active games list.
     */
    public static void addGame(ActiveGame game) {
        ACTIVE_GAMES.add(game);

        if (!GAME_THREADS.containsKey(game.gamePk())) {
            Thread thread = new Thread(() -> runGame(game.gamePk()), "Game-" + game.gamePk());

            GAME_THREADS.put(game.gamePk(), thread);
            thread.start();
            logger.info("Started game thread for gamePk: " + game.gamePk());
        }

        logger.info("Added game " + game.gamePk() + " with channel ID " + game.channelId() + " to the active games list");
    }

    /**
     * Stops (or "unsubscribes") a game from the active games list.
     *
     * @param game The game to stop from the active games list.
     */
    public static void stopGame(ActiveGame game) {
        int currentGames = 0;
        for (ActiveGame activeGame : ACTIVE_GAMES) {
            if (game.gamePk().equals(activeGame.gamePk())) {
                currentGames++;
            }
        }

        ACTIVE_GAMES.remove(game);

        // If this is the last game running, stop the thread
        if (currentGames == 1) {
            Thread thread = GAME_THREADS.get(game.gamePk());
            thread.interrupt();
            GAME_THREADS.remove(game.gamePk());
            logger.debug("Stopped game " + game.gamePk() + " thread");
        }

        logger.debug("Removed game " + game.gamePk() + " from the active games list");
    }

    /**
     * Stops a game with a provided text channel.
     * The game in the provided text channel will be stopped.
     *
     * @param channel The text channel to stop the game from.
     * @return The gamePk if the game was stopped, null if no game was found in the provided text channel.
     */
    public static String stopGame(GuildMessageChannel channel) {
        for (ActiveGame game : ACTIVE_GAMES) {
            if (game.channelId().equals(channel.getId())) {
                stopGame(game);
                return game.gamePk();
            }
        }

        return null;
    }

    /**
     * Returns the current game for the provided text channel.
     *
     * @param channel The text channel to get the current game from.
     * @return The current game for the provided text channel, null if no game is currently running in the provided text channel.
     */
    public static String currentGame(GuildMessageChannel channel) {
        for (ActiveGame game : ACTIVE_GAMES) {
            if (game.channelId().equals(channel.getId())) {
                return game.gamePk();
            }
        }

        return null;
    }

    /**
     * Runs a game. This should be run on a separate thread.
     *
     * @param gamePk The gamePk of the game to run.
     */
    private static void runGame(String gamePk) {
        logger.debug("Starting game with gamePk: " + gamePk);

        if (gamePk.isEmpty()) {
            return;
        }

        GameState currentState = new GameState(gamePk);
        List<JSONObject> postedAdvisories = currentState.gameAdvisories();

//        boolean canEdit = channel.getGuild().getSelfMember().hasPermission(channel, Permission.MANAGE_CHANNEL);

        // Start the looping thread until the game is stopped
        while (!currentState.gameState().equals("Final")) {
            GameState recentState = new GameState(gamePk);

            if (recentState.failed()) {
                logger.warn("Failed to get game state for gamePk: " + gamePk + "! Retrying in 3s...");
                try {
                    //noinspection BusyWait
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // Shutdown the thread
                    GAME_THREADS.remove(gamePk);
                    return;
                }
                continue;
            }

            // Check to see if the game has changed state
            if (recentState.gameState().equals("Final")) {
                currentState = recentState;
                break;
            }

            // Check for new changes in the description
            if (recentState.atBatIndex() >= 0 && !recentState.currentPlayDescription().equals(currentState.currentPlayDescription())) {
                logger.debug("New play description for gamePk " + gamePk + ": " + recentState.currentPlayDescription());

                boolean scoringPlay = recentState.homeScore() != currentState.homeScore() || recentState.awayScore() != currentState.awayScore();
                boolean hasOut = recentState.outs() != currentState.outs() && recentState.outs() > 0;

                EmbedBuilder embed = new EmbedBuilder()
                    .setDescription(recentState.currentPlayDescription());

                // Display Hit info if there is any. This only shows for balls that are in-play.
                String hitInfo = recentState.hitInfo();
                if (hitInfo != null) {
                    embed.addField("Hit Info", hitInfo, false);
                }

                // Check if score changed
                if (scoringPlay) {
                    boolean homeScored = recentState.homeScore() > currentState.homeScore();

                    embed.setTitle((homeScored ? recentState.homeTeam() : recentState.awayTeam()) + " scored!");
                    embed.addField("Score", recentState.awayTeam() + " " + recentState.awayScore() + " - " + recentState.homeScore() + " " + recentState.homeTeam(), true);
                }

                // Check if outs changed. Display if it did.
                if (hasOut) {
                    int oldOuts = recentState.outs() - currentState.outs();
                    if (oldOuts < 0) {
                        oldOuts = recentState.outs();
                    }

                    embed.addField("Outs", recentState.outs() + " (+" + oldOuts + ")", true);

                    if (recentState.outs() == 3 && !scoringPlay) {
                        embed.addField("Score", recentState.awayTeam() + " " + recentState.awayScore() + " - " + recentState.homeScore() + " " + recentState.homeTeam(), true);
                    }
                }

                if (scoringPlay) {
                    embed.setColor(0x427ee6);
                } else if (hasOut) {
                    embed.setColor(0xd23d33);
                } else if (recentState.currentPlayDescription().contains("walks")) {
                    embed.setColor(0x4fc94f);
                } else {
                    embed.setColor(0x979797);
                }

                // Send result
                sendPlay(embed.build(), gamePk, recentState.currentBallInPlay(), scoringPlay);
            }

            // Check for new advisories
            List<JSONObject> newAdvisories = recentState.gameAdvisories();
            if (newAdvisories.size() > postedAdvisories.size()) {
                int startIndex = postedAdvisories.size();
                List<MessageEmbed> queuedAdvisories = new ArrayList<>();
                for (int i = startIndex; i < newAdvisories.size(); i++) {
                    JSONObject advisory = newAdvisories.get(i);
                    JSONObject details = advisory.getJSONObject("details");

                    logger.debug("New advisory: " + advisory);

                    EmbedBuilder detailEmbed = new EmbedBuilder()
                        .setTitle(details.getString("event"))
                        .setDescription(details.getString("description"));

                    // Check if score changed
                    if (details.has("isScoringPlay") && details.getBoolean("isScoringPlay")) {
                        int homeScore = details.getInt("homeScore");
                        int awayScore = details.getInt("awayScore");
                        boolean homeScored = homeScore > awayScore;

                        detailEmbed.setAuthor((homeScored ? recentState.homeTeam() : recentState.awayTeam()) + " scored!");
                        detailEmbed.addField("Score", recentState.awayTeam() + " " + details.getInt("awayScore") + " - " + details.getInt("homeScore") + " " + recentState.homeTeam(), true);
                    }

                    queuedAdvisories.add(detailEmbed.build());
                }

                sendAdvisory(queuedAdvisories, gamePk);
            }

            // Check if the inning state changed
            if (!currentState.inningState().equals(recentState.inningState())) {
                // Ignore if the state is "Middle" or "End"
                if (!recentState.inningState().equals("Middle") && !recentState.inningState().equals("End")) {
                    EmbedBuilder inningEmbed = new EmbedBuilder()
                        .setTitle("Inning State Updated")
                        .setDescription(recentState.inningState() + " of the " + recentState.inningOrdinal());

                    sendMessages(inningEmbed.build(), gamePk);
                }
            }

            // Update the current states
            postedAdvisories = newAdvisories;
            currentState = recentState;

            // Wait for 10 seconds before requesting the next game state
            try {
                //noinspection BusyWait
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                // Shutdown the thread
                GAME_THREADS.remove(gamePk);
                return;
            }
        }

        // Build a scorecard embed
        EmbedBuilder scorecardEmbed = new EmbedBuilder();
        scorecardEmbed.setTitle("Scorecard");

        TableBuilder tableBuilder = new TableBuilder();
        List<String> headers = new ArrayList<>();
        headers.add("Team");

        JSONArray inningData = currentState.lineScore().getJSONArray("innings");
        int totalInnings = inningData.length();

        for (int i = 1; i <= totalInnings; i++) {
            headers.add(String.valueOf(i));
        }

        headers.add("R");
        headers.add("H");
        headers.add("E");
        headers.add("LOB");

        tableBuilder.addHeaders(headers.toArray(new String[0]));

        String[][] tableData = new String[totalInnings + 5][2];

        // Add team names
        tableData[0][0] = currentState.awayTeam();
        tableData[0][1] = currentState.homeTeam();

        for (int i = 0; i < totalInnings; i++) {
            JSONObject inning = inningData.getJSONObject(i);
            int awayScore = inning.getJSONObject("away").getInt("runs");
            String homeScore = inning.getJSONObject("home").optString("runs", "-");

            tableData[i+1] = new String[] {
                String.valueOf(awayScore),
                homeScore
            };
        }

        // Add runs, hits, errors, and leftOnBase to the last row
        JSONObject homeTotals = currentState.lineScore().getJSONObject("teams").getJSONObject("home");
        JSONObject awayTotals = currentState.lineScore().getJSONObject("teams").getJSONObject("away");

        int index = totalInnings + 1;
        for (String item : new String[]{"runs", "hits", "errors", "leftOnBase"}) {
            tableData[index] = new String[] {
                String.valueOf(awayTotals.getInt(item)),
                String.valueOf(homeTotals.getInt(item))
            };

            index++;
        }

        // Flip the rows and columns in the tableData matrix
        String[][] flippedTableData = new String[tableData[0].length][tableData.length];
        for (int i = 0; i < tableData.length; i++) {
            for (int j = 0; j < tableData[i].length; j++) {
                flippedTableData[j][i] = tableData[i][j];
            }
        }

        // Set values
        tableBuilder.setValues(flippedTableData);
        tableBuilder.setBorders(TableBuilder.Borders.HEADER_PLAIN);
        tableBuilder.codeblock(true);

        // Game is over!
        endGame(gamePk, tableBuilder.build());
    }

    /**
     * Sends a game advisory message to enabled channels.
     *
     * @param embeds The embeds to send.
     * @param gamePk The gamePk of the game.
     */
    public static void sendAdvisory(List<MessageEmbed> embeds, String gamePk) {
        for (ActiveGame game : getGames(gamePk)) {
            ChannelConfig config = ChannelConfig.getConfig(game.channelId());
            if (!config.gameAdvisories()) continue;

            GuildMessageChannel channel = canSafelySend(game);
            if (channel == null) continue;

            channel.sendMessageEmbeds(embeds).queue();
        }
    }

    public static void sendMessages(MessageEmbed message, String gamePk) {
        for (ActiveGame game : getGames(gamePk)) {
            GuildMessageChannel channel = canSafelySend(game);
            if (channel == null) continue;

            channel.sendMessageEmbeds(message).queue();
        }
    }

    /**
     * Sends a play message to enabled channels.
     *
     * @param message The message to send.
     * @param gamePk The gamePk of the game.
     * @param inPlay Whether the ball is in play.
     * @param isScoringPlay Whether the play is a scoring play.
     */
    public static void sendPlay(MessageEmbed message, String gamePk, boolean inPlay, boolean isScoringPlay) {
        for (ActiveGame game : getGames(gamePk)) {
            ChannelConfig config = ChannelConfig.getConfig(game.channelId());

            // If configured to only show scoring plays, ignore non-scoring plays
            if (config.onlyScoringPlays() && !isScoringPlay) continue;

            int delay = inPlay ? config.inPlayDelay() : config.noPlayDelay();

            GuildMessageChannel channel = canSafelySend(game);
            if (channel == null) continue;

            channel.sendMessageEmbeds(message).queueAfter(delay, TimeUnit.SECONDS);
        }
    }

    public static void endGame(String gamePk, String scorecard) {
        for (ActiveGame game : ACTIVE_GAMES) {
            // We only want to send the game over message to the channels that are watching the game
            if (!game.gamePk().equals(gamePk)) continue;

            GuildChannel gChan = jda.getGuildChannelById(game.channelId());
            if (gChan == null) continue;

            GuildMessageChannel channel = (GuildMessageChannel)gChan;
            try {
                channel.sendMessage("Game Over!\n**Final Scorecard**" + scorecard)
                    .setActionRow(Button.link("https://mlb.chew.pw/game/" + gamePk, "View Game"))
                    .queue();
            } catch (InsufficientPermissionException ignored) {
                logger.debug("Insufficient permissions to send message to channel " + game.channelId());
            }
        }

        // Remove the games from the active games list
        ACTIVE_GAMES.removeIf(game -> game.gamePk().equals(gamePk));

        // Remove the game thread
        GAME_THREADS.remove(gamePk);
    }

    /**
     * Checks to see if the bot can safely send messages to the channel.
     * This checks to make sure the channel exists, and if we can talk.
     *
     * @param game The game to check
     * @return The channel if we can safely send messages, null otherwise
     */
    private static GuildMessageChannel canSafelySend(ActiveGame game) {
        GuildChannel gChan = jda.getGuildChannelById(game.channelId());
        // channel is null for some reason, so we're going to remove the game from the active games list
        if (gChan == null) {
            logger.debug("Stopping game %s due to nonexistent channel %s".formatted(game.gamePk(), game.channelId()));
            stopGame(game);
            return null;
        }

        GuildMessageChannel channel = (GuildMessageChannel)gChan;
        // If we can't talk in the channel, we're going to remove the game from the active games list
        if (!channel.canTalk()) {
            logger.debug("Stopping game %s because we can't speak in %s".formatted(game.gamePk(), game.channelId()));
            stopGame(game);
            return null;
        }
        // Check to see if we have permission to send embeds
        if (!channel.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
            logger.debug("Stopping game %s because we can't send embeds to %s".formatted(game.gamePk(), game.channelId()));
            channel.sendMessage("Uh oh! I can't send embeds to this channel. This is required! Please give me the permission and start the game again.").queue();
            stopGame(game);
            return null;
        }

        return channel;
    }

    /**
     * Gets active games for the specified gamePk
     *
     * @param gamePk The gamePk to get active games for
     * @return A list of active games
     */
    public static List<ActiveGame> getGames(String gamePk) {
        List<ActiveGame> games = new ArrayList<>();

        for (ActiveGame game : ACTIVE_GAMES) {
            if (!game.gamePk().equals(gamePk)) continue;

            games.add(game);
        }

        return games;
    }
}
