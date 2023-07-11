package pw.chew.mlb.objects;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import pw.chew.chewbotcca.util.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static pw.chew.mlb.commands.PlanGameCommand.cleanDuplicates;

public record GameBlurb(String gamePk, JSONObject data) {
    public GameBlurb(String gamePk) {
        this(gamePk, new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1/schedule?language=en&gamePk=%s&hydrate=broadcasts(all),gameInfo,team,probablePitcher(all)&useLatestGames=true&fields=dates,date,games,gameDate,teams,away,probablePitcher,fullName,team,teamName,id,name,leagueRecord,wins,losses,pct,home,venue,name,broadcasts,type,name,homeAway,isNational,callSign".formatted(gamePk)))
            .getJSONArray("dates")
            .getJSONObject(0)
            .getJSONArray("games")
            .getJSONObject(0));
    }

    public String name() {
        // Convert to Eastern Time, then to this format: "Feb 26th"
        ZoneId eastern = ZoneId.of("America/New_York");
        String date = time().atZoneSameInstant(eastern).format(DateTimeFormatter.ofPattern("MMM d"));

        return "%s @ %s - %s".formatted(away().name(), home().name(), date);
    }

    public MLBTeam home() {
        return new MLBTeam(this.data.getJSONObject("teams").getJSONObject("home"));
    }

    public MLBTeam away() {
        return new MLBTeam(this.data.getJSONObject("teams").getJSONObject("away"));
    }

    public List<String> broadcasts(String... types) {
        // Handle broadcast stuff
        List<String> broadList = new ArrayList<>();
        JSONArray broadcasts = this.data.optJSONArray("broadcasts");
        if (broadcasts == null) broadcasts = new JSONArray();
        for (Object broadcastObj : broadcasts) {
            JSONObject broadcast = (JSONObject) broadcastObj;
            String team = broadcast.getString("homeAway").equals("away") ? away().name() : home().name();

            if (types.length > 0 && !Arrays.asList(types).contains(broadcast.getString("type"))) continue;
            switch (broadcast.getString("type")) {
                case "TV" -> {
                    if (broadcast.getString("name").contains("Bally Sports")) {
                        // use call sign
                        broadList.add("%s - %s".formatted(team, broadcast.getString("callSign")));
                    } else {
                        broadList.add("%s - %s".formatted(team, broadcast.getString("name")));
                    }
                }
                case "FM", "AM" -> broadList.add("%s - %s".formatted(team, broadcast.getString("name")));
            }
        }

        // Go through radio and see if the teamName is twice, if so, merge them
        cleanDuplicates(broadList);

        return broadList;
    }

    public MessageEmbed blurb() {
        // Handle broadcast stuff
        List<String> tv = broadcasts("TV");
        List<String> radio = broadcasts("FM", "AM");

        // if tv or radio are empty, put "No TV/Radio Broadcasts"
        if (tv.isEmpty()) tv.add("No TV Broadcasts");
        if (radio.isEmpty()) radio.add("No Radio Broadcasts");

        return new EmbedBuilder()
            .setTitle("**%s** @ **%s**".formatted(away().name(), home().name()), "https://mlb.chew.pw/game/%s".formatted(gamePk))
            .setDescription("**Game Time**: %s".formatted(TimeFormat.DATE_TIME_LONG.format(time())))
            .addField("Probable Pitchers", "%s: %s\n%s: %s".formatted(away().name(), away().probablePitcher(), home().name(), home().probablePitcher()), true)
            .addField("Records", "%s: %s - %s\n%s: %s - %s".formatted(
                away().name(), away().wins(), away().losses(), // away record
                home().name(), home().wins(), home().losses() // home record
            ), true)
            .addField(":tv: Broadcasts", String.join("\n", tv), true)
            .addField(":radio: Broadcasts", String.join("\n", radio), true).build();
    }

    public String blurbText() {
        MessageEmbed blurb = blurb();
        return blurb.getTitle() + "\n" +
            blurb.getDescription() + "\n" +
            "\n" +
            blurb().getFields().stream().map(field -> "**" + field.getName() + "**\n" +  field.getValue()).collect(Collectors.joining("\n\n")) + "\n\n" +
            "Game Link: " + blurb.getUrl();
    }

    // TODO: Handle when not at home ballpark, e.g. Mexico or London
    public String ballpark() {
        // teams > home > team > venue > name
        return data.getJSONObject("teams").getJSONObject("home").getJSONObject("team").getJSONObject("venue").getString("name");
    }

    public OffsetDateTime time() {
        // Format "2023-02-26T20:05:00Z" to OffsetDateTime
        TemporalAccessor accessor = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(data.getString("gameDate"));
        return OffsetDateTime.from(accessor);
    }
}