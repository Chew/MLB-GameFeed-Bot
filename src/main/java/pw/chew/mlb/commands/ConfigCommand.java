package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import pw.chew.mlb.objects.ChannelConfig;

import java.util.Arrays;

public class ConfigCommand extends SlashCommand {
    public ConfigCommand() {
        this.name = "config";
        this.help = "Configure MLB Bot for this channel";
        this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
        this.children = new SlashCommand[]{new ConfigGetSubCommand(), new ConfigSetSubCommand()};
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        // Unused
    }

    public static class ConfigGetSubCommand extends SlashCommand {
        public ConfigGetSubCommand() {
            this.name = "get";
            this.help = "Get the current configuration for this channel";
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            ChannelConfig config = ChannelConfig.getConfig(event.getChannel().getId(), false);
            if (config == null) {
                event.reply("This channel has not been configured, and is using default settings. Run `/config set` to get started.").setEphemeral(true).queue();
                return;
            }

            event.replyEmbeds(buildConfigEmbed(config, event.getChannel().getName())).setEphemeral(true).queue();
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

    public static class ConfigSetSubCommand extends SlashCommand {
        public ConfigSetSubCommand() {
            this.name = "set";
            this.help = "Set the configuration for this channel";
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
            ChannelConfig current = ChannelConfig.getConfig(event.getChannel().getId(), true);

            current.update("OnlyScoringPlays", event.optBoolean("only_scoring_plays", current.onlyScoringPlays()));
            current.update("GameAdvisories", event.optBoolean("game_advisories", current.gameAdvisories()));
            current.update("InPlayDelay", event.getOption("in_play_delay", current.inPlayDelay(), OptionMapping::getAsInt));
            current.update("NoPlayDelay", event.getOption("no_play_delay", current.noPlayDelay(), OptionMapping::getAsInt));
            //current.update("showScoreOnOut3", event.optBoolean("show_score_on_out_3", current.showScoreOnOut3()));

            current.saveData();

            event.reply("Configuration updated!").setEphemeral(true).queue();
        }
    }
}
