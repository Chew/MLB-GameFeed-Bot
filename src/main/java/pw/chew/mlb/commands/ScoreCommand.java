package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import pw.chew.mlb.listeners.GameFeedHandler;
import pw.chew.mlb.objects.GameState;

public class ScoreCommand extends SlashCommand {
    public ScoreCommand() {
        this.name = "score";
        this.help = "Shows the score of the current game";
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        String currentGame = GameFeedHandler.currentGame(event.getGuildChannel());
        if (currentGame == null) {
            event.reply("No active game in this channel, please start a game first.").setEphemeral(true).queue();
        } else {
            event.reply(buildScore(currentGame)).setEphemeral(true).queue();
        }
    }

    public String buildScore(String gamePk) {
        GameState state = new GameState(gamePk);

        return String.format("""
                    Score: %s %s - %s %s
                    Inning: %s %s, %s out(s)
                    """,
            state.awayTeam(), state.awayScore(), state.homeScore(), state.homeTeam(),
            state.inningState(), state.inningOrdinal(), state.outs());
    }
}
