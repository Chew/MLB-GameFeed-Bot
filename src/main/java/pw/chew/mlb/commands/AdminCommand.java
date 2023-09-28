package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.EmbedPaginator;
import com.jagrosh.jdautilities.menu.Paginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.slf4j.LoggerFactory;
import pw.chew.mlb.MLBBot;
import pw.chew.mlb.listeners.GameFeedHandler;
import pw.chew.mlb.objects.ActiveGame;
import pw.chew.mlb.objects.ChannelConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static pw.chew.mlb.MLBBot.jda;

public class AdminCommand extends Command {
    public AdminCommand() {
        this.name = "admin";
        this.help = "Admin commands";
        this.hidden = true;
        this.ownerCommand = true;
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event) {
        String args = event.getArgs();

        if (args.startsWith("games")) {
            activeGames(event);
        } else if (args.startsWith("config")) {
            config(event);
        } else if (args.startsWith("stats")) {
            botStats(event);
        } else if (args.startsWith("export")) {
            export(event);
        }
    }

    private void export(CommandEvent event) {
        List<String> lines = new ArrayList<>();
        var servers = event.getJDA().getGuilds();
        lines.add("id,memberCount,name,joinedAt");
        for (Guild server : servers) {
            lines.add("%s,%s,%s,%s".formatted(server.getId(), server.getMemberCount(), server.getName(), server.getSelfMember().getTimeJoined().toEpochSecond()));
        }

        // export to file
        String fileName = "servers-%s.csv".formatted(System.currentTimeMillis());
        File file = new File(fileName);

        // write to file
        try {
            java.nio.file.Files.write(file.toPath(), lines);
            event.reply("Exported to file: " + file.getAbsolutePath());
        } catch (Exception e) {
            event.reply("Error writing to file: " + e.getMessage());
        }
    }

    private void config(CommandEvent event) {
        var config = new EmbedPaginator.Builder()
            .waitOnSinglePage(false)
            .setFinalAction(m -> {
                try {
                    m.clearReactions().queue();
                } catch(PermissionException ignored) { }
            })
            .setEventWaiter(MLBBot.waiter)
            .setTimeout(1, TimeUnit.MINUTES)
            .clearItems();

        for (String key : ConfigCommand.channelsMap.keySet()) {
            ChannelConfig channelConfig = ConfigCommand.channelsMap.get(key);
            if (channelConfig == null) continue;
            config.addItems(ConfigCommand.ConfigGetSubCommand.buildConfigEmbed(channelConfig, key));
        }

        config.setText("There are %s total configs".formatted(ConfigCommand.channelsMap.size())).build().display(event.getChannel());
    }

    public void activeGames(CommandEvent event) {
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
            LoggerFactory.getLogger(this.getClass()).debug("Found active game {}", game);
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
                LoggerFactory.getLogger(this.getClass()).debug("Game {} is in channel {}", key, channel);
                activeGames.addItems(channel.getGuild().getName() + " - " + channel.getAsMention() + " (" + key + ")");
            }
        }

        activeGames.build().paginate(event.getChannel(), 1);
    }

    public void botStats(CommandEvent event) {
        long serverCount = event.getJDA().getGuilds().size();
        long activeGames = GameFeedHandler.ACTIVE_GAMES.size();
        long activeThreads = GameFeedHandler.GAME_THREADS.size();

        // Store a list that can only have unique items
        List<String> activeServers = new ArrayList<>();
        for (ActiveGame game : GameFeedHandler.ACTIVE_GAMES) {
            GuildChannel channel = jda.getGuildChannelById(game.channelId());
            if (channel == null) continue;

            if (!activeServers.contains(channel.getGuild().getId())) {
                activeServers.add(channel.getGuild().getId());
            }
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("Bot Stats")
            .addField("Servers", String.valueOf(serverCount), true)
            .addField("Active Games", String.valueOf(activeGames), true)
            .addField("Active Threads", String.valueOf(activeThreads), true)
            .addField("Active Servers", String.valueOf(activeServers.size()), true)
            ;

        event.reply(embed.build());
    }
}
