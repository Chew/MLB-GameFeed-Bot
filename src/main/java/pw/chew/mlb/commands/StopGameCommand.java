package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import pw.chew.mlb.objects.ActiveGame;

import static pw.chew.mlb.commands.StartGameCommand.GAME_THREADS;

public class StopGameCommand extends Command {

    public StopGameCommand() {
        this.name = "stopgame";
        this.help = "Stops a game in the current channel";
        this.guildOnly = true;
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        for (ActiveGame game : GAME_THREADS.keySet()) {
            if (!game.channelId().equals(event.getTextChannel().getId())) {
                continue;
            }

            GAME_THREADS.get(game).interrupt();
            GAME_THREADS.remove(game);
            event.getChannel().sendMessage("Stopped game with gamePk: " + game.gamePk()).queue();
            return;
        }

        event.replyWarning("No active game in this channel, please start a game first.");
    }
}
