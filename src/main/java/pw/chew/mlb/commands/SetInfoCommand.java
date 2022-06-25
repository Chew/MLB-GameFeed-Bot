package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.geysermc.discordbot.util.DicesCoefficient;
import org.json.JSONArray;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static pw.chew.mlb.MLBBot.TEAMS;

public class SetInfoCommand extends SlashCommand {

    public SetInfoCommand() {
        this.name = "setinfo";
        this.help = "Sets a voice channel name to a specified piece of info.";
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
        this.botPermissions = new Permission[]{Permission.MANAGE_CHANNEL};

        this.options = Arrays.asList(
            new OptionData(OptionType.CHANNEL, "channel", "The channel to set the info in.", true)
                .setChannelTypes(ChannelType.VOICE),
            new OptionData(OptionType.STRING, "team", "What team to grab info for.", true)
                .setAutoComplete(true),
            new OptionData(OptionType.STRING, "info", "The info to set the channel name to.", true)
                .addChoice("Standings (Team: WINS-LOSS, xth in division)", "standings")
                .addChoice("Next Game (Team: DATE: vs/@ OPP, hh:mm A/PM)", "nextgame")
        );
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        VoiceChannel channel = event.getOption("channel", OptionMapping::getAsVoiceChannel);
        String team = event.getOption("team", OptionMapping::getAsString);

        String info = "uh oh";

        JSONObject teamInfo = getTeamInfo(team);

        if (teamInfo == null) {
            event.reply("Could not find team " + team).setEphemeral(true).queue();
            return;
        }

        switch (event.getOption("info", "", OptionMapping::getAsString)) {
            case "standings" -> {

                String abbreviation = teamInfo.getString("abbreviation");
                int wins = 0;
                int losses = 0;
                int position = 0;
                String divisionAbbreviation = "";

                int division = teamInfo.getJSONObject("division").getInt("id");
                String divisionName = teamInfo.getJSONObject("division").getString("name");
                // Get the first letter of each word in the division name
                for (String word : divisionName.split(" ")) {
                    divisionAbbreviation += word.substring(0, 1);
                }

                JSONObject standings = new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1/standings?leagueId=103,104&season=2022&standingsTypes=regularSeason"));

                // Gotta find the team now...
                JSONArray records = standings.getJSONArray("records");
                for (int i = 0; i < records.length(); i++) {
                    JSONObject record = records.getJSONObject(i);
                    if (record.getJSONObject("division").getInt("id") != division) continue;

                    JSONArray teamRecords = record.getJSONArray("teamRecords");

                    for (int j = 0; j < teamRecords.length(); j++) {
                        JSONObject teamRecord = teamRecords.getJSONObject(j);
                        if (!teamRecord.getJSONObject("team").getString("name").equals(team)) {
                            continue;
                        }

                        position = j + 1;
                        wins = teamRecord.getJSONObject("leagueRecord").getInt("wins");
                        losses = teamRecord.getJSONObject("leagueRecord").getInt("losses");
                    }
                }

                // Ordinalize the position
                String ordinal = switch (position) {
                    case 1 -> "st";
                    case 2 -> "nd";
                    case 3 -> "rd";
                    default -> "th";
                };

                info = String.format("%s: %s-%s, %s%s in %s", abbreviation, wins, losses, position, ordinal, divisionAbbreviation);
            }
            case "nextgame" -> {
                int teamId = teamInfo.getInt("id");

                JSONObject teamSchedule = new JSONObject(RestClient.get(String.format("https://statsapi.mlb.com/api/v1/teams/%s?season=2022&hydrate=nextSchedule", teamId)));

                JSONObject nextGame = null;

                JSONArray upcomingGames = teamSchedule.getJSONArray("teams")
                    .getJSONObject(0)
                    .getJSONObject("nextGameSchedule")
                    .getJSONArray("dates");

                for (int i = 0; i < upcomingGames.length(); i++) {
                    JSONArray games = upcomingGames.getJSONObject(i).getJSONArray("games");
                    for (int j = 0; j < games.length(); j++) {
                        JSONObject game = games.getJSONObject(j);
                        if (game.getJSONObject("status").getString("abstractGameState").equals("Final")) continue;
                        if (nextGame != null) continue;

                        nextGame = game;

                        break;
                    }
                }

                if (nextGame == null) {
                    event.reply("Could not find a future game for " + team).setEphemeral(true).queue();
                    return;
                }

                int away = nextGame.getJSONObject("teams").getJSONObject("away").getJSONObject("team").getInt("id");
                int home = nextGame.getJSONObject("teams").getJSONObject("home").getJSONObject("team").getInt("id");

                boolean isHome = teamId == home;

                String opponent = getTeamInfo(nextGame.getJSONObject("teams").getJSONObject(isHome ? "away" : "home").getJSONObject("team").getString("name")).getString("abbreviation");

                OffsetDateTime gameTime = OffsetDateTime.parse(nextGame.getString("gameDate"));
                // Set the time zone to America/Chicago
                gameTime = gameTime.withOffsetSameInstant(OffsetDateTime.now().getOffset());
                // Convert the game time to MM/DD
                String gameDate = String.format("%s/%s", gameTime.getMonthValue(), gameTime.getDayOfMonth());
                // Get the time in Central Time, HH:mm A/PM
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
                String gameTimeString = gameTime.format(timeFormatter);

                info = String.format("%s: %s %s, %s", gameDate, isHome ? "vs" : "@", opponent, gameTimeString);
            }
        }

        String finalInfo = info;
        event.getGuild().getVoiceChannelById(channel.getId()).getManager().setName(info).queue(unused -> {
            event.reply("Set channel name to " + finalInfo).setEphemeral(true).queue();
        });
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
        // Get the query
        String query = event.getFocusedOption().getValue();

        // Get the providers
        List<String> teams = potentialTeams(query);

        event.replyChoices(teams.stream()
                .distinct()
                .map(team -> new Command.Choice(team, team))
                .limit(25)
                .toArray(Command.Choice[]::new))
            .queue();
    }

    private List<String> potentialTeams(String query) {
        List<String> potential = new ArrayList<>();

        // Collect the providers
        List<String> teams = new ArrayList<>();
        for (JSONObject json : TEAMS) {
            String name = json.getString("name");
            teams.add(name);
        }

        // Search the providers by an exact starting match
        for (String team : teams) {
            if (team.toLowerCase().startsWith(query.toLowerCase())) {
                potential.add(team);
            }
        }

        // Find the best similarity
        for (String team : teams) {
            double similar = DicesCoefficient.diceCoefficientOptimized(query.toLowerCase(), team.toLowerCase());
            if (similar > 0.2d) {
                potential.add(team);
            }
        }

        // Send a message if we don't know what team
        return potential;
    }

    private JSONObject getTeamInfo(String teamName) {
        for (JSONObject json : TEAMS) {
            if (json.getString("name").equals(teamName)) {
                return json;
            }
        }
        return null;
    }
}
