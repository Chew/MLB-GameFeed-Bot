package pw.chew.mlb;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.chew.chewbotcca.util.DatabaseHelper;
import pw.chew.chewbotcca.util.RestClient;
import pw.chew.mlb.commands.AdminCommand;
import pw.chew.mlb.commands.BettingCommand;
import pw.chew.mlb.commands.ConfigCommand;
import pw.chew.mlb.commands.PlanGameCommand;
import pw.chew.mlb.commands.ScoreCommand;
import pw.chew.mlb.commands.SetInfoCommand;
import pw.chew.mlb.commands.ShutdownCommand;
import pw.chew.mlb.commands.StartGameCommand;
import pw.chew.mlb.commands.StopGameCommand;
import pw.chew.mlb.listeners.InteractionHandler;
import pw.chew.mlb.listeners.JDAListeners;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MLBBot {
    private static final Logger logger = LoggerFactory.getLogger(MLBBot.class);
    public static JDA jda;
    public static final List<JSONObject> TEAMS = new ArrayList<>();
    public static final EventWaiter waiter = new EventWaiter();

    public static void main(String[] args) throws IOException {
        // Load properties into the PropertiesManager
        Properties prop = new Properties();
        prop.load(new FileInputStream("bot.properties"));

        // Initialize Database for storage
        logger.info("Connecting to database...");
        DatabaseHelper.openConnection();
        logger.info("Connected!");

        // Initialize the waiter and client
        CommandClientBuilder client = new CommandClientBuilder();

        // Set the client settings
        client.setActivity(Activity.customStatus("Thank you everyone for an amazing season. Lot of new features planned for 2024. Stay tuned :)"));
        client.setOwnerId("476488167042580481");
        client.setPrefix("woody!");

        client.useHelpBuilder(false);

        client.addCommands(new ShutdownCommand(), new AdminCommand());
        client.addSlashCommands(new StartGameCommand(), new StopGameCommand(), new ScoreCommand(), new SetInfoCommand(), new ConfigCommand(),
            new PlanGameCommand(), new BettingCommand());

        //client.forceGuildOnly("148195924567392257");

        // Finalize the command client
        CommandClient commandClient = client.build();

        // Register JDA
        jda = JDABuilder.createDefault(prop.getProperty("token"))
            .setStatus(OnlineStatus.ONLINE)
            .setActivity(Activity.playing("Booting..."))
            .addEventListeners(
                waiter, commandClient // JDA-Chewtils stuff
                , new JDAListeners()
                , new InteractionHandler()
            ).build();

        RestClient.setClient(jda.getHttpClient());

        // Load teams
        JSONObject teams = new JSONObject(RestClient.get("https://statsapi.mlb.com/api/v1/teams?sportIds=1&season=2023"));

        for (int i = 0; i < teams.getJSONArray("teams").length(); i++) {
            TEAMS.add(teams.getJSONArray("teams").getJSONObject(i));
        }
    }
}
