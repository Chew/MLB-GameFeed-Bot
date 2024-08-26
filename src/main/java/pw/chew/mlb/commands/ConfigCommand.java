package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.utils.Checks;
import pw.chew.mlb.models.Channel;
import pw.chew.mlb.models.Server;
import pw.chew.mlb.objects.ChannelConfig;
import pw.chew.mlb.objects.ServerConfig;
import pw.chew.mlb.util.AutocompleteUtil;

import java.util.Arrays;

public class ConfigCommand extends SlashCommand {
    public ConfigCommand() {
        this.name = "config";
        this.help = "Configure MLB Bot";
        this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
        this.children = new SlashCommand[]{
            new ConfigChannelSubcommand(), // Channel
            new ConfigServerSubcommand()   // Server
        };
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        // Unused
    }

    public static class ConfigChannelSubcommand extends SlashCommand {
        public ConfigChannelSubcommand() {
            this.name = "channel";
            this.help = "Show or manages the config for this channel";
            this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
            this.options = Arrays.asList(
                new OptionData(OptionType.BOOLEAN, "only_scoring_plays", "Only show scoring plays"),
                new OptionData(OptionType.BOOLEAN, "game_advisories", "Show game advisories, e.g. pitching changes"),
                new OptionData(OptionType.INTEGER, "in_play_delay", "Delay for \"In Play\" (non strikeout/walk)"),
                new OptionData(OptionType.INTEGER, "no_play_delay", "Delay for strikeout/walk, usually appears quicker")
                /*, new OptionData(OptionType.BOOLEAN, "show_score_on_out_3", "Show score on out 3") */
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            // check if no arguments were provided
            if (event.getOptions().isEmpty()) {
                ChannelConfig config = ChannelConfig.getConfig(event.getChannel().getId());
                if (config == null) {
                    Channel defaults = new Channel();
                    config = new ChannelConfig(defaults);
                }

                String defaultMessage = "This channel is using default settings. Run this command with arguments to change them!";

                event.reply(config.isDefault() ? defaultMessage : "")
                    .addEmbeds(buildConfigEmbed(config, event.getChannel().getName())).setEphemeral(true).queue();
                return;
            }

            ChannelConfig current = ChannelConfig.getConfig(event.getChannel().getId(), true);
            Channel channel = current.getChannel();

            channel.setOnlyScoringPlays(event.optBoolean("only_scoring_plays", current.onlyScoringPlays()));
            channel.setGameAdvisories(event.optBoolean("game_advisories", current.gameAdvisories()));
            channel.setInPlayDelay(event.getOption("in_play_delay", current.inPlayDelay(), OptionMapping::getAsInt));
            channel.setNoPlayDelay(event.getOption("no_play_delay", current.noPlayDelay(), OptionMapping::getAsInt));
            //current.update("showScoreOnOut3", event.optBoolean("show_score_on_out_3", current.showScoreOnOut3()));

            current.saveData();

            event.reply("Configuration updated!").setEphemeral(true).queue();
        }

        public static MessageEmbed buildConfigEmbed(ChannelConfig config, String channelName) {
            return new EmbedBuilder()
                .setTitle("Configuration for " + channelName)
                .addField("Only Scoring Plays", config.onlyScoringPlays() + "", true)
                .addField("Game Advisories", config.gameAdvisories() + "", true)
                .addField("In Play Delay", config.inPlayDelay() + "", true)
                .addField("No Play (K/BB) Delay", config.noPlayDelay() + "", true)
                //.addField("Show Score on Out 3", config.showScoreOnOut3() + "", true)
                .build();
        }
    }

    public static class ConfigServerSubcommand extends SlashCommand {
        public ConfigServerSubcommand() {
            this.name = "server";
            this.help = "See or manage the config for this server";
            this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
            this.options = Arrays.asList(
                new OptionData(OptionType.STRING, "team", "What major league team your server should be associated with.", false, true)
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            // This command is only for servers
            Checks.notNull(event.getGuild(), "Server");

            // check if no arguments were provided
            if (event.getOptions().isEmpty()) {
                ServerConfig config = ServerConfig.getConfig(event.getGuild().getId(), false);
                if (config == null) {
                    event.reply("This server is currently not configured. Run this command with arguments to start!").setEphemeral(true).queue();
                } else {
                    event.replyEmbeds(buildConfigEmbed(config, event.getGuild())).setEphemeral(true).queue();
                }
            }

            ServerConfig current = ServerConfig.getConfig(event.getGuild().getId(), true);
            Server server = current.getServer();

            server.setTeamId(event.getOption("team", current.teamId(), OptionMapping::getAsInt));
            current.saveData();

            event.reply("Configuration updated!").setEphemeral(true).queue();
        }

        @Override
        public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
            event.replyChoices(AutocompleteUtil.getTeams("1", event.getFocusedOption().getValue())).queue();
        }

        public static MessageEmbed buildConfigEmbed(ServerConfig config, Guild server) {
            return new EmbedBuilder()
                .setTitle("Configuration for " + server.getName())
                .addField("Associated Team", config.teamName(), true)
                .build();
        }
    }
}
