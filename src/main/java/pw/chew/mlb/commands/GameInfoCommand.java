package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import pw.chew.mlb.objects.GameState;
import pw.chew.mlb.objects.ImageUtil;
import pw.chew.mlb.util.AutocompleteUtil;
import pw.chew.mlb.util.EmbedUtil;
import pw.chew.mlb.util.TeamEmoji;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A command to get information about a specific game
 * <br>
 * This was partially inspired by <a href="https://github.com/AlecM33/mlb-gameday-bot">AlecM33's Discord bot</a>
 */
public class GameInfoCommand extends SlashCommand {
    public GameInfoCommand() {
        this.name = "gameinfo";
        this.help = "Get information about a specific game";
        this.guildOnly = false;

        this.options = Collections.singletonList(
            new OptionData(OptionType.STRING, "game", "Pick a game from today to view info for.", true, true)
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        String gamePk = event.optString("game", "");
        GameState info = GameState.fromPk(gamePk);
        if (info.failed()) {
            event.replyEmbeds(EmbedUtil.failure("Failed to get game info")).setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(buildGameInfoEmbed(info))
            .setComponents(buildButtons(info))
            .setEphemeral(true).queue();
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        event.replyChoices(AutocompleteUtil.getTodayGames(true)).queue();
    }

    public MessageEmbed buildGameInfoEmbed(String gamePk) {
        GameState info = GameState.fromPk(gamePk);
        return buildGameInfoEmbed(info);
    }

    public MessageEmbed buildGameInfoEmbed(GameState info) {
        if (info.failed()) {
            return EmbedUtil.failure("Failed to get game info");
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("Game Info for %s @ %s".formatted(info.away().clubName(), info.home().clubName()))
            .addField("Attendance", info.friendlyAttendance(), true)
            .addField("Weather", info.weather(), true)
            ;

        return embed.build();
    }

    public List<ActionRow> buildButtons(String gamePk) {
        GameState info = GameState.fromPk(gamePk);
        return buildButtons(info);
    }

    public static List<ActionRow> buildButtons(GameState info) {
        return Arrays.asList(ActionRow.of(
            Button.primary("plangame:lineup:" + info.gamePk() + ":away", info.away().clubName() + " Lineup").withEmoji(TeamEmoji.fromTeamId(info.away().id())),
            Button.primary("plangame:lineup:" + info.gamePk() + ":home", info.home().clubName() + " Lineup").withEmoji(TeamEmoji.fromTeamId(info.home().id()))
        ), ActionRow.of(
            // box score buttons
            Button.primary("gameinfo:boxscore:" + info.gamePk(), "Box Score TEX")
        ));
    }

    public static void buildBoxScore(String gamePk, ButtonInteractionEvent event) {
        GameState info = GameState.fromPk(gamePk);
        if (info.failed()) {
            event.replyEmbeds(EmbedUtil.failure("Failed to get game info")).queue();
            return;
        }

        String title = "Box Score for %s @ %s".formatted(info.away().clubName(), info.home().clubName());


        String[][] values = new String[2][9];

        values[0] = new String[] { "Batters", "AB", "R", "H", "RBI", "BB", "K", "AVG", "OPS" };



        values[1] = new String[] { "Player 1", "0", "0", "0", "0", "0", "0", "0.000", "0.000" };


        ImageUtil.GeneratedImage image = ImageUtil.createTable(values);
        if (image == null) {
            event.replyEmbeds(EmbedUtil.failure("Failed to create box score image")).queue();
            return;
        }

        event.reply("Box Score for %s @ %s".formatted(info.away().clubName(), info.home().clubName()))
            .addFiles(image.asFileUpload())
            .setEphemeral(true)
            .queue();
    }
}
