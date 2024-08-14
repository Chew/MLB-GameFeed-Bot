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
import org.json.JSONObject;
import pw.chew.chewbotcca.util.MiscUtil;
import pw.chew.chewbotcca.util.RestClient;
import pw.chew.mlb.objects.GameState;
import pw.chew.mlb.objects.ImageUtil;
import pw.chew.mlb.util.AutocompleteUtil;
import pw.chew.mlb.util.EmbedUtil;
import pw.chew.mlb.util.TeamEmoji;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
            Button.primary("gameinfo:boxscore:" + info.gamePk() + ":away:batters", "Box Score " + info.away().clubName()),
            Button.primary("gameinfo:boxscore:" + info.gamePk() + ":home:batters", "Box Score " + info.home().clubName())
        ));
    }

    public static void buildBoxScore(String gamePk, String homeOrAway, String type, ButtonInteractionEvent event) {
        GameState info = GameState.fromPk(gamePk);
        if (info.failed()) {
            event.replyEmbeds(EmbedUtil.failure("Failed to get game info")).queue();
            return;
        }

        JSONObject data = new JSONObject(RestClient.get("https://api.chew.pro/sports/mlb/%s/boxscore".formatted(gamePk)));
        List<JSONObject> batters = MiscUtil.toList(data.getJSONObject("teams").getJSONObject(homeOrAway).getJSONArray(type), JSONObject.class);

        String title = "Box Score for %s @ %s".formatted(info.away().clubName(), info.home().clubName());

        BoxScoreDataSets set = BoxScoreDataSets.valueOf(type.toUpperCase());

        String[][] values = new String[batters.size() + 1][set.length()];
        values[0] = set.headers;

        for(int i = 0; i < batters.size(); i++) {
            JSONObject batter = batters.get(i);
            JSONObject stats = batter.getJSONObject("stats");
            values[i + 1] = set.parseFromLine(batter.getString("name"), stats);
        }

        ImageUtil.GeneratedImage image = ImageUtil.createTable(values, set);
        if (image.failed()) {
            event.replyEmbeds(EmbedUtil.failure("Failed to create box score image")).queue();
            return;
        }

        ActionRow row = ActionRow.of(
            Button.primary("gameinfo:boxscore:" + gamePk + ":" + homeOrAway + ":batters", "Batters"),
            Button.primary("gameinfo:boxscore:" + gamePk + ":" + homeOrAway + ":pitchers", "Pitchers"),
            Button.primary("gameinfo:boxscore:" + gamePk + ":" + homeOrAway + ":bench", "Bench"),
            Button.primary("gameinfo:boxscore:" + gamePk + ":" + homeOrAway + ":bullpen", "Bullpen"),
            Button.primary("gameinfo:boxscore:" + gamePk + ":" + homeOrAway + ":info", "Info")
        );

        if (event.getButton().getLabel().contains("Box Score")) {
            event.reply(title)
                .addComponents(row)
                .addFiles(image.asFileUpload())
                .setEphemeral(true)
                .queue();
        } else {
            event.editMessage(title)
                .setComponents(row)
                .setFiles(image.asFileUpload())
                .queue();
        }
    }

    public enum BoxScoreDataSets {
        BATTERS(
            new String[]{"Batters", "AB", "R", "H", "RBI", "BB", "K", "AVG", "OPS"},
            new int[]{100, 30, 30, 30, 30, 30, 30, 50, 50}
        ),
        PITCHERS(
            new String[]{"Pitchers", "IP", "H", "R", "ER", "BB", "K", "ERA"},
            new int[]{100, 30, 30, 30, 30, 30, 30, 50}
        ),
        BENCH(
            new String[]{"Bench", "B", "POS", "AVG", "G", "R", "H", "HR", "RBI", "SB"},
            new int[]{100, 30, 30, 50, 30, 30, 30, 30, 30, 30}
        ),
        BULLPEN(
            new String[]{"Bullpen", "T", "ERA", "IP", "H", "BB", "K"},
            new int[]{100, 30, 50, 30, 30, 30, 30}
        );

        private final String[] headers;
        private final int[] widths;

        BoxScoreDataSets(String[] headers, int[] widths) {
            this.headers = headers;
            this.widths = widths;
        }

        public String[] headers() {
            return headers;
        }

        public int[] columnWidths() {
            return widths;
        }

        public String[] parseFromLine(String name, JSONObject stats) {
            String[] line = new String[headers.length];
            line[0] = name;
            for (int i = 1; i < headers.length; i++) {
                line[i] = stats.optString(headers[i].toLowerCase(Locale.ROOT), "");
            }

            return line;
        }

        public int totalWidth() {
            int total = 0;
            for (int width : widths) {
                total += width;
            }
            return total;
        }

        public int length() {
            return headers.length;
        }
    }
}
