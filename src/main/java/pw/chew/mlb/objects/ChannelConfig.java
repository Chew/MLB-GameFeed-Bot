package pw.chew.mlb.objects;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import pw.chew.mlb.commands.ConfigCommand;

import java.io.IOException;
import java.io.Serializable;

/**
 * A record class for storing channel configs.
 * @param onlyScoringPlays Whether to only show scoring plays
 * @param gameAdvisories Whether to show game advisories
 * @param inPlayDelay The amount of time in seconds to delay an in-play ball (non strikeout/walk) before showing it. Default 13.
 * @param noPlayDelay The amount of time in seconds to delay a strikeout/walk before showing it. Default 18.
 * @param showScoreOnOut3 Whether to show the score when the third out is recorded. Default true.
 */
public record ChannelConfig(boolean onlyScoringPlays, boolean gameAdvisories, int inPlayDelay, int noPlayDelay, boolean showScoreOnOut3)
    implements Serializable {
    public ChannelConfig() {
        this(false, true, 13, 18, true);
    }

    /**
     * Returns the config for the specified channel.
     *
     * @param channelId the channel ID to get the config for
     * @return the config for the specified channel
     */
    public static ChannelConfig getConfig(String channelId) {
        ChannelConfig config = ConfigCommand.channelsMap.get(channelId);
        if (config == null) {
            config = new ChannelConfig();
        }
        return config;
    }

    public static class EntrySerializer implements Serializer<ChannelConfig>, Serializable {
        @Override
        public void serialize(@NotNull DataOutput2 out, @NotNull ChannelConfig value) throws IOException {
            out.writeBoolean(value.onlyScoringPlays());
            out.writeBoolean(value.gameAdvisories());
            out.writeInt(value.inPlayDelay());
            out.writeInt(value.noPlayDelay());
            out.writeBoolean(value.showScoreOnOut3());
        }

        @Override
        public ChannelConfig deserialize(@NotNull DataInput2 input, int available) throws IOException {
            return new ChannelConfig(input.readBoolean(), input.readBoolean(), input.readInt(), input.readInt(), input.readBoolean());
        }
    }
}
