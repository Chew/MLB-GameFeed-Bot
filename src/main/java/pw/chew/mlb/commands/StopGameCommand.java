package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.utils.TimeFormat;
import pw.chew.mlb.listeners.GameFeedHandler;
import pw.chew.mlb.objects.GameState;
import pw.chew.mlb.util.EmbedUtil;

import java.util.Map;

public class StopGameCommand extends SlashCommand {

    public StopGameCommand() {
        this.name = "stopgame";
        this.help = "Stops a game in the current channel";
        this.descriptionLocalization = Map.of(
            DiscordLocale.ENGLISH_US, "Stops a game in the current channel",
            DiscordLocale.SPANISH, "Detiene un juego en el canal actual"
        );

        this.guildOnly = true;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        String stoppedGame = GameFeedHandler.stopGame(event.getGuildChannel());
        if (stoppedGame == null) {
            event.replyEmbeds(EmbedUtil.failure("There is no active game in this channel. Please start a game first.")).setEphemeral(true).queue();
        } else {
            GameState state = GameState.fromPk(stoppedGame);

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Stopped Game **%s @ %s**".formatted(state.away().clubName(), state.home().clubName()))
                .setDescription("Game Date: " + TimeFormat.DATE_LONG.format(state.officialDate()))
                .setColor(0xd23d33)
                .setFooter("Game PK: %s".formatted(stoppedGame));

            event.replyEmbeds(embed.build()).queue();
        }
    }
}
