package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import pw.chew.mlb.listeners.GameFeedHandler;
import pw.chew.mlb.objects.GameState;

import java.util.Map;

public class ScoreCommand extends SlashCommand {
    public ScoreCommand() {
        this.name = "score";
        this.help = "Shows the score and match-ups of the current game";
        this.descriptionLocalization = Map.of(
            DiscordLocale.ENGLISH_US, "Shows the score and match-ups of the current game",
            DiscordLocale.SPANISH, "Muestra la puntuación y los enfrentamientos del juego actual"
        );
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
                    
                    Pitching: %s
                    Batting: %s
                    
                    On Base:
                    %s
                    """,
            state.awayTeam(), state.awayScore(), state.homeScore(), state.homeTeam(),
            state.inningState(), state.inningOrdinal(), state.outs(),
            state.currentPitcher(), state.currentBatter(),
            state.currentBases()
        );
    }
}
