package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.TableBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.chew.mlb.objects.ActiveGame;
import pw.chew.mlb.objects.GameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StartGameCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(StartGameCommand.class);
    public final static Map<ActiveGame, Thread> GAME_THREADS = new HashMap<>();

    public StartGameCommand() {
        this.name = "startgame";
        this.help = "Starts a game";
        this.guildOnly = true;
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        String gamePk = event.getArgs();

        // Start a new thread
        ActiveGame activeGame = new ActiveGame(gamePk, event.getTextChannel().getId());
        Thread gameThread = new Thread(() -> runGame(activeGame, gamePk, event.getTextChannel()));
        GAME_THREADS.put(activeGame, gameThread);
        gameThread.start();
    }

    public static void runGame(ActiveGame game, String gamePk, TextChannel channel) {
        logger.debug("Starting game with gamePk: " + gamePk);

        if (gamePk.isEmpty()) {
            channel.sendMessage("Please provide a game id").queue();
            return;
        }

        GameState currentState = new GameState(gamePk);
        List<String> postedAdvisories = currentState.gameAdvisories();

        channel.sendMessage("Starting game with gamePk: " + gamePk).queue();

        // Start the bStats tracking thread
        while (!currentState.gameState().equals("Final")) {
            GameState recentState = new GameState(gamePk);

            if (recentState.gameState().equals("Final")) {
                currentState = recentState;
                break;
            }

            if (recentState.atBatIndex() >= 0 && !recentState.currentPlayDescription().equals(currentState.currentPlayDescription())) {
                EmbedBuilder embed = new EmbedBuilder()
                    .setDescription(recentState.currentPlayDescription())
                    ;

                if (recentState.hitInfo() != null) {
                    embed.addField("Hit Info", recentState.hitInfo(), false);
                }

                // Check if score changed
                if (recentState.homeScore() != currentState.homeScore() || recentState.awayScore() != currentState.awayScore()) {
                    boolean homeScored = recentState.homeScore() > currentState.homeScore();

                    embed.setTitle((homeScored ? recentState.homeTeam() : recentState.awayTeam()) + " scored!");
                    embed.addField("Score", recentState.awayTeam() + " " + recentState.awayScore() + " - " + recentState.homeScore() + " " + recentState.homeTeam(), true);
                }

                if (recentState.outs() != currentState.outs() && recentState.outs() > 0) {
                    int oldOuts = recentState.outs() - currentState.outs();
                    if (oldOuts < 0) {
                        oldOuts = recentState.outs();
                    }

                    embed.addField("Outs", recentState.outs() + " (+" + oldOuts + ")", true);

                    if (recentState.outs() == 3) {
                        embed.addField("Score", recentState.awayTeam() + " " + recentState.awayScore() + " - " + recentState.homeScore() + " " + recentState.homeTeam(), true);
                    }
                }

                channel.sendMessageEmbeds(embed.build()).queue();
            }

            // Check for new advisories
            List<String> newAdvisories = recentState.gameAdvisories();
            if (newAdvisories.size() > postedAdvisories.size()) {
                int startIndex = postedAdvisories.size();
                for (int i = startIndex; i < newAdvisories.size(); i++) {
                    String[] details = newAdvisories.get(i).split("≠");

                    EmbedBuilder detailEmbed = new EmbedBuilder()
                        .setTitle(details[0].trim())
                        .setDescription(details[1].trim());

                    channel.sendMessageEmbeds(detailEmbed.build()).queue();
                }
            }

            // Check if the inning state changed
            if (!currentState.inningState().equals(recentState.inningState())) {
                // Ignore if the state is "Middle" or "End"
                if (!recentState.inningState().equals("Middle") && !recentState.inningState().equals("End")) {
                    EmbedBuilder inningEmbed = new EmbedBuilder()
                        .setTitle("Inning State Updated")
                        .setDescription(recentState.inningState() + " of the " + recentState.inningOrdinal());

                    channel.sendMessageEmbeds(inningEmbed.build()).queue();
                }
            }

            // Update the current states
            postedAdvisories = newAdvisories;
            currentState = recentState;

            // Wait for 10 seconds before requesting the next game state
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                // Shutdown the thread
                GAME_THREADS.remove(game);
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

//        tableBuilder.addRowNames(currentState.awayTeam(), currentState.homeTeam());

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

        tableData[totalInnings+1] = new String[] {
            String.valueOf(awayTotals.getInt("runs")),
            String.valueOf(homeTotals.getInt("runs"))
        };

        tableData[totalInnings+2] = new String[] {
            String.valueOf(awayTotals.getInt("hits")),
            String.valueOf(homeTotals.getInt("hits"))
        };

        tableData[totalInnings+3] = new String[] {
            String.valueOf(awayTotals.getInt("errors")),
            String.valueOf(homeTotals.getInt("errors"))
        };

        tableData[totalInnings+4] = new String[] {
            String.valueOf(awayTotals.getInt("leftOnBase")),
            String.valueOf(homeTotals.getInt("leftOnBase"))
        };

        // Flip the rows and columns in the tableData matrix
        String[][] flippedTableData = new String[tableData[0].length][tableData.length];
        for (int i = 0; i < tableData.length; i++) {
            for (int j = 0; j < tableData[i].length; j++) {
                flippedTableData[j][i] = tableData[i][j];
            }
        }

        tableBuilder.setValues(flippedTableData);
        tableBuilder.setBorders(TableBuilder.Borders.HEADER_PLAIN);
        tableBuilder.codeblock(true);

        // Print out the matrix to the console
        for (String[] row : flippedTableData) {
            System.out.println(Arrays.toString(row));
        }

        // Game is over!
        channel.sendMessage("Game Over!\n**Final Scorecard**" + tableBuilder.build())
            .setActionRow(Button.link("https://mlb.chew.pw/game/" + gamePk, "View Game"))
            .queue();

        // Remove the game thread
        GAME_THREADS.remove(game);
    }
}
