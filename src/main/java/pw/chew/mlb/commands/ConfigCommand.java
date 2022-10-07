package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import pw.chew.mlb.objects.ChannelConfig;

import java.util.Arrays;

public class ConfigCommand extends SlashCommand {
    private static final DB db = DBMaker.fileDB("channels.db").fileMmapEnable().closeOnJvmShutdown().make();
    public static final HTreeMap<String, ChannelConfig> channelsMap = db
        .hashMap("channels", Serializer.STRING, new ChannelConfig.EntrySerializer())
        .createOrOpen();

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
            ChannelConfig config = channelsMap.get(event.getChannel().getId());
            if (config == null) {
                event.reply("This channel is has not been configured, and is using default settings. Run `/config set` to get started.").setEphemeral(true).queue();
                return;
            }

            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Configuration for " + event.getChannel().getName())
                .addField("Only Scoring Plays", config.onlyScoringPlays() + "", true)
                .addField("Game Advisories", config.gameAdvisories() + "", true)
                .addField("In Play Delay", config.inPlayDelay() + "", true)
                .addField("No Play (K/BB) Delay", config.noPlayDelay() + "", true)
                //.addField("Show Score on Out 3", config.showScoreOnOut3() + "", true)
                ;

            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        }
    }

    public static class ConfigSetSubCommand extends SlashCommand {
        public ConfigSetSubCommand() {
            this.name = "set";
            this.help = "Set the configuration for this channel";
            this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
            this.options = Arrays.asList(
                new OptionData(OptionType.BOOLEAN, "only_scoring_plays", "Only show scoring plays"),
                new OptionData(OptionType.BOOLEAN, "game_advisories", "Show game advisories"),
                new OptionData(OptionType.INTEGER, "reach_delay", "Delay for reach (non strikeout/walk)"),
                new OptionData(OptionType.INTEGER, "k_or_bb_delay", "Delay for strikeout/walk")
                /*, new OptionData(OptionType.BOOLEAN, "show_score_on_out_3", "Show score on out 3") */
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            ChannelConfig current = channelsMap.get(event.getChannel().getId());
            if (current == null) {
                current = new ChannelConfig();
            }

            ChannelConfig config = new ChannelConfig(
                event.optBoolean("only_scoring_plays", current.onlyScoringPlays()),
                event.optBoolean("game_advisories", current.gameAdvisories()),
                event.getOption("reach_delay", current.inPlayDelay(), OptionMapping::getAsInt),
                event.getOption("k_or_bb_delay", current.noPlayDelay(), OptionMapping::getAsInt),
                event.optBoolean("show_score_on_out_3", current.showScoreOnOut3())
            );

            channelsMap.put(event.getChannel().getId(), config);

            event.reply("Configuration updated!").setEphemeral(true).queue();
        }
    }
}
