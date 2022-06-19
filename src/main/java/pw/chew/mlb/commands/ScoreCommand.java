package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import pw.chew.mlb.objects.ActiveGame;
import pw.chew.mlb.objects.GameState;

import static pw.chew.mlb.commands.StartGameCommand.GAME_THREADS;

public class ScoreCommand extends Command {
    public ScoreCommand() {
        this.name = "score";
        this.help = "Shows the score of the current game";
    }

    @Override
    protected void execute(CommandEvent event) {
        for (ActiveGame game : GAME_THREADS.keySet()) {
            if (!game.channelId().equals(event.getTextChannel().getId())) {
                continue;
            }

            GameState state = new GameState(game.gamePk());

            event.getChannel().sendMessage(String.format("""
                    Score: %s %s - %s %s
                    Inning: %s %s, %s out(s)
                    """,
                state.awayTeam(), state.awayScore(), state.homeScore(), state.homeTeam(),
                state.inningState(), state.inningOrdinal(), state.outs())).queue();
            return;
        }

        event.replyWarning("No active game in this channel, please start a game first.");
    }
}
