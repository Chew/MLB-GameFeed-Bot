package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import org.slf4j.LoggerFactory;
import pw.chew.mlb.listeners.GameFeedHandler;

import static pw.chew.mlb.MLBBot.jda;

public class ShutdownCommand extends Command {
    public ShutdownCommand() {
        this.name = "shutdown";
        this.help = "Shuts down the bot";
        this.guildOnly = false;
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().contains("--now")) {
            event.getChannel().sendMessage("Bye bye!").queue(m -> shutdown());
        } else {
            if (GameFeedHandler.GAME_THREADS.isEmpty()) {
                event.getChannel().sendMessage("Bye bye!").queue(m -> shutdown());
            } else {
                GameFeedHandler.shutdownOnFinish = true;
                event.getChannel().sendMessage("Waiting for all active games to stop before shutting down. Add `--now` to shut down now safely.").queue();
            }
        }
    }

    public static void shutdown() {
        LoggerFactory.getLogger(ShutdownCommand.class).info("Shutting down...");
        jda.shutdown();
    }
}
