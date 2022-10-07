package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import pw.chew.mlb.listeners.GameFeedHandler;

public class StopGameCommand extends SlashCommand {

    public StopGameCommand() {
        this.name = "stopgame";
        this.help = "Stops a game in the current channel";
        this.guildOnly = true;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        String stoppedGame = GameFeedHandler.stopGame(event.getGuildChannel());
        if (stoppedGame == null) {
            event.reply("No active game in this channel, please start a game first.").queue();
        } else {
            event.reply("Stopped game " + stoppedGame).queue();
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        String stoppedGame = GameFeedHandler.stopGame(event.getGuildChannel());
        if (stoppedGame == null) {
            event.replyWarning("No active game in this channel, please start a game first.");
        } else {
            event.replySuccess("Stopped game " + stoppedGame);
        }
    }
}
