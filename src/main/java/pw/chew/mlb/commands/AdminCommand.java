package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.Paginator;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.slf4j.LoggerFactory;
import pw.chew.mlb.MLBBot;
import pw.chew.mlb.listeners.GameFeedHandler;
import pw.chew.mlb.objects.ActiveGame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AdminCommand extends Command {
    public AdminCommand() {
        this.name = "admin";
        this.help = "Admin commands";
        this.hidden = true;
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        var activeGames = new Paginator.Builder().setColumns(1)
            .setItemsPerPage(10)
            .showPageNumbers(true)
            .waitOnSinglePage(false)
            .useNumberedItems(false)
            .setFinalAction(m -> {
                try {
                    m.clearReactions().queue();
                } catch(PermissionException ignored) { }
            })
            .setEventWaiter(MLBBot.waiter)
            .setTimeout(1, TimeUnit.MINUTES)
            .clearItems();

        Map<String, List<GuildChannel>> channelMap = new HashMap<>();
        LoggerFactory.getLogger(this.getClass()).debug("There are {} active games", GameFeedHandler.ACTIVE_GAMES.size());
        for (ActiveGame game : GameFeedHandler.ACTIVE_GAMES) {
            GuildChannel channel = event.getJDA().getGuildChannelById(game.channelId());
            var channels = channelMap.get(game.gamePk());
            if (channels == null) {
                channels = new ArrayList<>();
            }
            channels.add(channel);
            channelMap.put(game.gamePk(), channels);
        }

        for (String key : channelMap.keySet()) {
            var channels = channelMap.get(key);
            for (GuildChannel channel : channels) {
                LoggerFactory.getLogger(this.getClass()).debug("Game {} is in channel {}", key, channel.getName());
                activeGames.addItems(channel.getGuild().getName() + " - " + channel.getAsMention() + " (" + key + ")");
            }
        }

        activeGames.build().paginate(event.getChannel(), 1);
    }
}
