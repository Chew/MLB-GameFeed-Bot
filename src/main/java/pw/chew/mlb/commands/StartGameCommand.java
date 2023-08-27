package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;
import pw.chew.mlb.listeners.GameFeedHandler;
import pw.chew.mlb.objects.ActiveGame;
import pw.chew.mlb.objects.GameState;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static pw.chew.mlb.listeners.GameFeedHandler.ACTIVE_GAMES;

public class StartGameCommand extends SlashCommand {

    public StartGameCommand() {
        this.name = "startgame";
        this.help = "Starts a currently active MLB game";
        this.descriptionLocalization = Map.of(
            DiscordLocale.ENGLISH_US, "Starts a currently active MLB game",
            DiscordLocale.SPANISH, "Comienza un juego de MLB actualmente activo"
        );

        this.guildOnly = true;
        this.options = Collections.singletonList(
            new OptionData(OptionType.INTEGER, "game", "Which game to listen to", true)
                .setDescriptionLocalization(DiscordLocale.SPANISH, "A qué juego escuchar")
                .setAutoComplete(true)
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        String gamePk = event.getOption("game", "0", OptionMapping::getAsString);
        String startGame = startGame(gamePk, event.getChannel().getId());
        event.reply(startGame).setEphemeral(!startGame.contains("Starting game")).queue();
    }

    public static String startGame(String gamePk, String channelId) {
        for (ActiveGame game : ACTIVE_GAMES) {
            if (game.channelId().equals(channelId)) {
                return "This channel is already playing a game: " + game.gamePk() + ". Please wait for it to finish, or stop it with `/stopgame`.";
            }
        }

        // Start a new thread
        ActiveGame activeGame = new ActiveGame(gamePk, channelId);
        GameState currentState = new GameState(gamePk);

        // Refuse to start if the game is already over
        if (currentState.isFinal()) {
            return "This game is already over. Please start a different game.";
        }

        // We can only start games if the start time is less than 30 minutes away
        // E.g. if game starts at 2:30, and the time is 1:59, we cannot start the game
        // But, if it's 2:00, we can start the game
        // We can also start it if it's like, 2:56, who cares, maybe we forgot to start it
        if (OffsetDateTime.now().isBefore(currentState.officialDate().minusMinutes(30))) {
            return "This game is not yet ready to start. Please wait until the game is within 30 minutes of starting.";
        }

        GameFeedHandler.addGame(activeGame);

        return "Starting game with gamePk: " + gamePk + "\n" +
            currentState.awayTeam() + " @ " + currentState.homeTeam() + " at " +
            TimeFormat.DATE_TIME_SHORT.format(currentState.officialDate());
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // Build the current date String as MM/DD/YYYY
        String today = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

        // Retrieve the current games
        JSONObject gameResponse = new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1/schedule?language=en&sportId=1&date=" + today + "&hydrate=game,flags,team"));

        // Build games
        List<Command.Choice> choices = new ArrayList<>();

        JSONArray games = gameResponse.getJSONArray("dates").getJSONObject(0).getJSONArray("games");

        for (int i = 0; i < games.length(); i++) {
            JSONObject game = games.getJSONObject(i);

            String status = game.getJSONObject("status").getString("abstractGameState");

            if (status.equals("Live")) {
                String home = game.getJSONObject("teams").getJSONObject("home").getJSONObject("team").getString("clubName");
                String awa = game.getJSONObject("teams").getJSONObject("away").getJSONObject("team").getString("clubName");

                choices.add(new Command.Choice(String.format("%s @ %s", awa, home), game.getInt("gamePk")));
            }
        }

        event.replyChoices(choices).queue();
    }

    @Override
    protected void execute(CommandEvent event) {
        String gamePk = event.getArgs();

        for (ActiveGame game : ACTIVE_GAMES) {
            if (game.channelId().equals(event.getTextChannel().getId())) {
                event.reply("This channel is already playing a game: " + game.gamePk() + ". Please wait for it to finish or stop it.");
                return;
            }
        }

        // make sure the bot has proper perms
        if (!event.getSelfMember().hasPermission(event.getTextChannel(), Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
            event.reply("I do not have the proper permissions to start a game in this channel. I need: " + Permission.VIEW_CHANNEL + ", " + Permission.MESSAGE_SEND + ", " + Permission.MESSAGE_EMBED_LINKS);
            return;
        }

        // Start a new thread
        ActiveGame activeGame = new ActiveGame(gamePk, event.getTextChannel().getId());
        GameState currentState = new GameState(gamePk);

        event.getChannel().sendMessage("Starting game with gamePk: " + gamePk + "\n" +
            currentState.awayTeam() + " @ " + currentState.homeTeam() + " at " +
            TimeFormat.DATE_TIME_SHORT.format(currentState.officialDate())
        ).queue();

        GameFeedHandler.addGame(activeGame);
    }
}
