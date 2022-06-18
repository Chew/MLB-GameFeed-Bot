package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.chew.chewbotcca.util.RestClient;
import pw.chew.mlb.objects.GameState;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StartGameCommand extends Command {
    private static ScheduledExecutorService generalThreadPool;
    private final Logger logger = LoggerFactory.getLogger(StartGameCommand.class);


    public StartGameCommand() {
        this.name = "startgame";
        this.help = "Starts a game";
        this.guildOnly = true;
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        String gamePk = event.getArgs();

        logger.debug("Starting game with gamePk: " + gamePk);

        if (gamePk.isEmpty()) {
            event.reply("Please provide a game id");
            return;
        }

        GameState currentState = new GameState(gamePk);
        List<String> postedAdvisories = currentState.gameAdvisories();

        event.reply("Starting game with gamePk: " + gamePk);

        // Start the bStats tracking thread
        while (!currentState.gameState().equals("Final")) {
            GameState recentState = new GameState(gamePk);

            if (recentState.gameState().equals("Final")) {
                currentState = recentState;
                break;
            }

            if (!currentState.inningState().equals(recentState.inningState())) {
                event.reply("Inning state changed to " + recentState.inningState() + " " + recentState.inningOrdinal());
            }

            logger.debug("At bat index: " + recentState.atBatIndex());
            if (!recentState.currentPlayDescription().equals(currentState.currentPlayDescription())) {
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

                if (recentState.outs() != currentState.outs()) {
                    int oldOuts = recentState.outs() - currentState.outs();
                    if (oldOuts < 0) {
                        oldOuts = recentState.outs();
                    }

                    embed.addField("Outs", recentState.outs() + " (+" + oldOuts + ")", true);

                    if (recentState.outs() == 3) {
                        embed.addField("Score", recentState.awayTeam() + " " + recentState.awayScore() + " - " + recentState.homeScore() + " " + recentState.homeTeam(), true);
                    }
                }

                event.getChannel().sendMessageEmbeds(embed.build()).queue();
            } else {
                logger.debug("No change in at bat index, or is not complete");
            }

            currentState = recentState;

            // Wait for 10 seconds before requesting the next game state
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Game is over!
        event.reply("The game is over. And as always, thanks for watching!");
    }
}
