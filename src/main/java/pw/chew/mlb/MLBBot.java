package pw.chew.mlb;

import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.chew.chewbotcca.util.RestClient;
import pw.chew.mlb.commands.ScoreCommand;
import pw.chew.mlb.commands.ShutdownCommand;
import pw.chew.mlb.commands.StartGameCommand;
import pw.chew.mlb.commands.StopGameCommand;
import pw.chew.mlb.listeners.JDAListeners;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class MLBBot {
    private static final Logger logger = LoggerFactory.getLogger(MLBBot.class);

    public static void main(String[] args) throws LoginException, IOException {
        // Load properties into the PropertiesManager
        Properties prop = new Properties();
        prop.load(new FileInputStream("bot.properties"));

        // Initialize the waiter and client
        EventWaiter waiter = new EventWaiter();
        CommandClientBuilder client = new CommandClientBuilder();

        // Set the client settings
        client.useDefaultGame();
        client.setOwnerId("476488167042580481");
        client.setPrefix("woody!");

        client.useHelpBuilder(false);

        client.addCommands(new StartGameCommand(), new ShutdownCommand(), new StopGameCommand(), new ScoreCommand());

        // Finalize the command client
        CommandClient commandClient = client.build();

        // Register JDA
        JDA jda = JDABuilder.createDefault(prop.getProperty("token"))
            .setStatus(OnlineStatus.ONLINE)
            .setActivity(Activity.playing("Booting..."))
            .addEventListeners(
                waiter, commandClient // JDA-Chewtils stuff
                , new JDAListeners()
            ).build();

        RestClient.setClient(jda.getHttpClient());
    }
}
