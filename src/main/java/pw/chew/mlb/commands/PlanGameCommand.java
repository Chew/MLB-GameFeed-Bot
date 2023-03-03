package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.json.JSONArray;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;
import pw.chew.mlb.objects.MLBAPIUtil;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class PlanGameCommand extends SlashCommand {
    public PlanGameCommand() {
        this.name = "plangame";
        this.help = "Plans a game to be played. Makes a thread in text channels or a post in forum channels.";

        this.options = Arrays.asList(
            new OptionData(OptionType.STRING, "team", "The team to plan for", true)
                .setAutoComplete(true),
            new OptionData(OptionType.CHANNEL, "channel", "The channel to plan for", true)
                .setChannelTypes(ChannelType.TEXT, ChannelType.FORUM),
            new OptionData(OptionType.STRING, "date", "The date of the game. Select one from the list!", true)
                .setAutoComplete(true),
            new OptionData(OptionType.STRING, "sport", "The sport to plan a game for, Majors by default.", false)
                .setAutoComplete(true)
        );
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

        // get da info
        JSONObject game = new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1/schedule?language=en&gamePk=%s&hydrate=broadcasts(all),gameInfo,team,probablePitcher(all)&useLatestGames=true&fields=dates,date,games,gameDate,teams,away,probablePitcher,fullName,team,teamName,name,leagueRecord,wins,losses,pct,home,broadcasts,type,name,homeAway,isNational,callSign".formatted(gamePk)))
            .getJSONArray("dates")
            .getJSONObject(0)
            .getJSONArray("games")
            .getJSONObject(0);

        // Format "2023-02-26T20:05:00Z" to OffsetDateTime
        TemporalAccessor accessor = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(game.getString("gameDate"));
        OffsetDateTime dateTime = OffsetDateTime.from(accessor);

        // Convert to Eastern Time, then to this format: "Feb 26th"
        ZoneId eastern = ZoneId.of("America/New_York");
        String date = dateTime.atZoneSameInstant(eastern).format(DateTimeFormatter.ofPattern("MMM d"));

        // Teams
        JSONObject home = game.getJSONObject("teams").getJSONObject("home");
        JSONObject away = game.getJSONObject("teams").getJSONObject("away");
        
        JSONObject homeRecord = home.getJSONObject("leagueRecord");
        JSONObject awayRecord = away.getJSONObject("leagueRecord");
        
        String homeName = home.getJSONObject("team").getString("teamName");
        String awayName = away.getJSONObject("team").getString("teamName");

        String name = "%s @ %s - %s".formatted(awayName, homeName, date);

        // Get probable pitchers
        JSONObject fallback = new JSONObject().put("fullName", "TBD");
        String homePitcher = home.optJSONObject("probablePitcher", fallback).getString("fullName");
        String awayPitcher = away.optJSONObject("probablePitcher", fallback).getString("fullName");

        // Handle broadcast stuff
        List<String> tv = new ArrayList<>();
        List<String> radio = new ArrayList<>();
        JSONArray broadcasts = game.optJSONArray("broadcasts");
        if (broadcasts == null) broadcasts = new JSONArray();
        for (Object broadcastObj : broadcasts) {
            JSONObject broadcast = (JSONObject) broadcastObj;
            String team = broadcast.getString("homeAway").equals("away") ? awayName : homeName;
            switch (broadcast.getString("type")) {
                case "TV" -> {
                    if (broadcast.getString("name").contains("Bally Sports")) {
                        // use call sign
                        tv.add("%s - %s".formatted(team, broadcast.getString("callSign")));
                    } else {
                        tv.add("%s - %s".formatted(team, broadcast.getString("name")));
                    }
                }
                case "FM", "AM" -> radio.add("%s - %s".formatted(team, broadcast.getString("name")));
            }
        }

        // Go through radio and see if the teamName is twice, if so, merge them
        cleanDuplicates(tv);
        cleanDuplicates(radio);

        // if tv or radio are empty, put "No TV/Radio Broadcasts"
        if (tv.isEmpty()) tv.add("No TV Broadcasts");
        if (radio.isEmpty()) radio.add("No Radio Broadcasts");

        String response = """
            **%s** @ **%s**
            **Game Time**: %s
            
            **Probable Pitchers**
            %s: %s
            %s: %s
            
            **Records**
            %s: %s - %s
            %s: %s - %s
            
            :tv:
            %s
            :radio:
            %s
            
            Game Link: https://mlb.chew.pw/game/%s
            """.formatted(
                awayName, homeName, // teams
            TimeFormat.DATE_TIME_LONG.format(accessor), // game time
            awayName, awayPitcher, // away pitcher
            homeName, homePitcher, // home pitcher
            awayName, awayRecord.getInt("wins"), awayRecord.getInt("losses"), // away record
            homeName, homeRecord.getInt("wins"), homeRecord.getInt("losses"), // home record
            String.join("\n", tv), String.join("\n", radio), // tv and radio broadcasts
            gamePk // game pk
        );

        switch (channel.getType()) {
            case TEXT -> {
                channel.asTextChannel().createThreadChannel(name).queue(threadChannel -> {
                    threadChannel.sendMessage(response).queue(msg -> {
                        try {
                            msg.pin().queue();
                        } catch (InsufficientPermissionException ignored) {
                        }
                        event.reply("Planned game! " + threadChannel.getAsMention()).setEphemeral(true).queue();
                    });
                });
            }
            case FORUM -> {
                channel.asForumChannel().createForumPost(name, MessageCreateData.fromContent(response)).queue(forumPost -> {
                    try {
                        forumPost.getMessage().pin().queue();
                    } catch (InsufficientPermissionException ignored) {
                    }
                    event.reply("Planned game! " + forumPost.getThreadChannel().getAsMention()).setEphemeral(true).queue();
                });
            }
        }
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        switch (event.getFocusedOption().getName()) {
            case "team": {
                // get current value of sport
                String sport = event.getOption("sport", "1", OptionMapping::getAsString);
                String input = event.getFocusedOption().getValue();

                if (input.equals("")) {
                    event.replyChoices(MLBAPIUtil.getTeams(sport).asChoices()).queue();
                } else {
                    event.replyChoices(MLBAPIUtil.getTeams(sport).potentialChoices(input)).queue();
                }

                return;
            }
            case "sport": {
                event.replyChoices(MLBAPIUtil.getSports().asChoices()).queue();
                return;
            }
            case "date": {
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
                    c1.add(Calendar.DAY_OF_YEAR, -1); // yesterday

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

                // Only get 25 choices
                if (choices.size() > 25) {
                    choices = choices.subList(0, 25);
                }

                event.replyChoices(choices).queue();

                return;
            }
        }

        event.replyChoices().queue();
    }

    private void cleanDuplicates(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            String radioTeam = list.get(i).split(" - ")[0];
            for (int j = 0; j < list.size(); j++) {
                if (i == j) continue;
                String radioTeam2 = list.get(j).split(" - ")[0];
                if (radioTeam.equals(radioTeam2)) {
                    list.set(i, list.get(i) + ", " + list.get(j).split(" - ")[1]);
                    list.remove(j);
                }
            }
        }
    }
}
