package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.json.JSONArray;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;
import pw.chew.mlb.objects.GameBlurb;
import pw.chew.mlb.objects.ImageUtil;
import pw.chew.mlb.objects.MLBAPIUtil;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanGameCommand extends SlashCommand {
    public PlanGameCommand() {
        this.name = "plangame";
        this.help = "Plans a game to be played. Makes a thread in text channels or a post in forum channels.";
        this.descriptionLocalization = Map.of(
            DiscordLocale.ENGLISH_US, "Plans a game to be played. Makes a thread in text channels or a post in forum channels.",
            DiscordLocale.SPANISH, "Planifica un juego para ser jugado."
        );

        this.options = Arrays.asList(
            new OptionData(OptionType.STRING, "team", "The team to plan for", true, true)
                .setDescriptionLocalization(DiscordLocale.SPANISH, "El equipo para planificar"),
            new OptionData(OptionType.CHANNEL, "channel", "The channel to plan for", true)
                .setDescriptionLocalization(DiscordLocale.SPANISH, "El canal para planificar")
                .setChannelTypes(ChannelType.TEXT, ChannelType.FORUM),
            new OptionData(OptionType.STRING, "date", "The date of the game. Select one from the list!", true, true)
                .setDescriptionLocalization(DiscordLocale.SPANISH, "La fecha del juego. ¡Seleccione uno de la lista!"),
            new OptionData(OptionType.STRING, "sport", "The sport to plan a game for, Majors by default.", false, true)
                .setDescriptionLocalization(DiscordLocale.SPANISH, "El deporte para planificar un juego, Majors de forma predeterminada."),
            new OptionData(OptionType.BOOLEAN, "thread", "Whether to make a thread or not. Defaults to true, required true for forums.", false)
                .setDescriptionLocalization(DiscordLocale.SPANISH, "Si hacer un hilo o no. De forma predeterminada es verdadero, se requiere verdadero para los foros."),
            new OptionData(OptionType.BOOLEAN, "event", "Whether to additionally create an event with all the information. Defaults to false.", false)
                .setDescriptionLocalization(DiscordLocale.SPANISH, "Si crear adicionalmente un evento con toda la información. De forma predeterminada es falso.")
        );
    }

    public static MessageEmbed generateGameBlurb(String gamePk) {
        GameBlurb blurb = new GameBlurb(gamePk);
        return blurb.blurb();
    }

    private static MessageEmbed generateGameBlurb(String gamePk, JSONObject game) {
        GameBlurb blurb = new GameBlurb(gamePk, game);
        return blurb.blurb();
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        OptionMapping channelMapping = event.getOption("channel");
        if (channelMapping == null) {
            event.reply("You must specify a channel to plan for!").setEphemeral(true).queue();
            return;
        }
        GuildChannelUnion channel = channelMapping.getAsChannel();

        String gamePk = event.optString("date", "1");
        GameBlurb blurb = new GameBlurb(gamePk);

        boolean makeThread = event.optBoolean("thread", false);
        boolean makeEvent = event.optBoolean("event", false);

        event.deferReply(true).queue(interactionHook -> handle(interactionHook, gamePk, channel, blurb, makeThread, makeEvent));
    }

    public void handle(InteractionHook event, String gamePk, GuildChannelUnion channel, GameBlurb blurb, boolean makeThread, boolean makeEvent) {
        if (makeEvent) {
            // async pathfinding???
            new Thread(() -> {
                String name = blurb.name();

                OffsetDateTime start = blurb.time();
                if (start.isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
                    start = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(15);
                }

                var ev = channel.getGuild().createScheduledEvent(name, blurb.ballpark(), start, start.plus(4, ChronoUnit.HOURS))
                    .setDescription(blurb.blurbText());

                var matchupBanner = ImageUtil.matchUpBanner(blurb.away().id(), blurb.home().id());
                if (matchupBanner != null) ev = ev.setImage(matchupBanner.asIcon());

                try {
                    ev.queue();
                } catch (InsufficientPermissionException e) {
                    // oh well
                }
            }).start();
        }

        switch (channel.getType()) {
            case TEXT -> {
                if (!makeThread) {
                    channel.asTextChannel().sendMessageEmbeds(blurb.blurb()).setActionRow(buildButtons(gamePk, blurb)).queue(message -> {
                        event.editOriginal("Planned game! " + message.getJumpUrl()).queue();
                    });
                    return;
                }

                channel.asTextChannel().createThreadChannel(blurb.name()).queue(threadChannel -> {
                    threadChannel.sendMessageEmbeds(blurb.blurb()).setActionRow(buildButtons(gamePk, blurb)).queue(msg -> {
                        try {
                            msg.pin().queue();
                        } catch (InsufficientPermissionException ignored) {
                        }
                        event.editOriginal("Planned game! " + threadChannel.getAsMention()).queue();
                    });
                });
            }
            case FORUM ->
                channel.asForumChannel().createForumPost(blurb.name(), MessageCreateData.fromEmbeds(blurb.blurb())).setActionRow(buildButtons(gamePk, blurb)).queue(forumPost -> {
                    try {
                        forumPost.getMessage().pin().queue();
                    } catch (InsufficientPermissionException ignored) {
                    }
                    event.editOriginal("Planned game! " + forumPost.getThreadChannel().getAsMention()).queue();
                });
        }
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        switch (event.getFocusedOption().getName()) {
            case "team" -> {
                // get current value of sport
                String sport = event.getOption("sport", "1", OptionMapping::getAsString);
                String input = event.getFocusedOption().getValue();

                List<Command.Choice> choices;
                if (input.isBlank()) {
                    choices = MLBAPIUtil.getTeams(sport).asChoices();
                } else {
                    choices = MLBAPIUtil.getTeams(sport).potentialChoices(input);
                }

                // Ensure no duplicates and no more than 25 choices
                choices = choices.stream().distinct().limit(25).toList();

                event.replyChoices(choices).queue();
                return;
            }
            case "sport" -> {
                event.replyChoices(MLBAPIUtil.getSports().asChoices()).queue();
                return;
            }
            case "date" -> {
                int teamId = event.getOption("team", -1, OptionMapping::getAsInt);
                String sport = event.getOption("sport", "1", OptionMapping::getAsString);

                if (teamId == -1) {
                    event.replyChoices(new Command.Choice("Please select a team first!", -1)).queue();
                    return;
                }

                JSONArray games = new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1/schedule?lang=en&sportId=%S&season=2023&teamId=%S&fields=dates,date,games,gamePk,teams,away,team,teamName,id&hydrate=team".formatted(sport, teamId)))
                    .getJSONArray("dates");

                List<Command.Choice> choices = new ArrayList<>();
                for (int i = 0; i < games.length(); i++) {
                    // Formatted as YYYY-MM-DD
                    String date = games.getJSONObject(i).getString("date");

                    Calendar c1 = Calendar.getInstance(); // today
                    // yesterday
                    c1.add(Calendar.DATE, -1);

                    Calendar c2 = Calendar.getInstance();
                    c2.set(
                        Integer.parseInt(date.split("-")[0]),
                        Integer.parseInt(date.split("-")[1]) - 1,
                        Integer.parseInt(date.split("-")[2])
                    );

                    if (c2.before(c1)) {
                        continue;
                    }

                    JSONArray dayGames = games.getJSONObject(i).getJSONArray("games");
                    for (int j = 0; j < dayGames.length(); j++) {
                        JSONObject game = dayGames.getJSONObject(j);

                        // find if we're home or away
                        JSONObject away = game.getJSONObject("teams").getJSONObject("away").getJSONObject("team");
                        JSONObject home = game.getJSONObject("teams").getJSONObject("home").getJSONObject("team");

                        boolean isAway = away.getInt("id") == teamId;
                        String opponent = isAway ? home.getString("teamName") : away.getString("teamName");

                        String name = "%s %s - %s%s".formatted(isAway ? "@" : "vs", opponent, date, dayGames.length() > 1 ? " (Game %d)".formatted(j + 1) : "");
                        choices.add(new Command.Choice(name, game.getInt("gamePk")));
                    }
                }

                // Ensure no more than 25 choices, and no duplicates
                choices = choices.stream().distinct().limit(25).toList();

                event.replyChoices(choices).queue();

                return;
            }
        }

        event.replyChoices().queue();
    }

    public static List<Button> buildButtons(String gamePk, GameBlurb blurb) {
        return List.of(
            Button.success("plangame:start:"+gamePk, "Start"),
            Button.secondary("plangame:refresh:"+gamePk, "Refresh"),
            Button.primary("plangame:lineup:"+gamePk+":away", blurb.away().name() + " Lineup"),
            Button.primary("plangame:lineup:"+gamePk+":home", blurb.home().name() + " Lineup")
        );
    }

    public static void cleanDuplicates(List<String> list) {
        // map of team name -> list of broadcasts
        Map<String, List<String>> teamBroadcasts = new HashMap<>();
        for (String s : list) {
            String[] split = s.split(" - ");
            String team = split[0];
            String broadcast = split[1];

            if (!teamBroadcasts.containsKey(team)) teamBroadcasts.put(team, new ArrayList<>());
            teamBroadcasts.get(team).add(broadcast);
        }
        list.clear();
        for (Map.Entry<String, List<String>> entry : teamBroadcasts.entrySet()) {
            list.add("%s - %s".formatted(entry.getKey(), String.join(", ", entry.getValue())));
        }
    }
}
