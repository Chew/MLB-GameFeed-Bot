package pw.chew.mlb.objects;

import org.hibernate.Transaction;
import org.slf4j.LoggerFactory;
import pw.chew.chewbotcca.util.DatabaseHelper;
import pw.chew.mlb.models.Channel;

import java.util.HashMap;

public class ChannelConfig {
    static final HashMap<String, ChannelConfig> cache = new HashMap<>();
    final Channel data;
    public ChannelConfig(Channel input) {
        data = input;
    }

    /**
     * Returns the config for the specified channel.
     *
     * @param id the channel ID to get the config for
     * @param createIfNotExists whether to create the config if it doesn't exist
     * @return the config for the specified channel
     */
    public static ChannelConfig getConfig(String id, boolean createIfNotExists) {
        if (cache.containsKey(id)) {
            return cache.get(id);
        } else {
            return retrieveChannel(id, createIfNotExists);
        }
    }

    public static ChannelConfig getConfig(String id) {
        return getConfig(id, true);
    }

    /**
     * Get a channel from the cache, and only the cache.
     * Don't attempt retrieving it.
     * @param id the channel ID
     * @return a possibly null channel
     */
    public static ChannelConfig getChannelIfCached(String id) {
        return cache.get(id);
    }

    /**
     * Retrieve server info
     * @param channelId the server id
     * @return a server settings
     */
    public static ChannelConfig retrieveChannel(String channelId, boolean createIfNotExists) {
        long id = Long.parseLong(channelId);
        var session = DatabaseHelper.getSessionFactory().openSession();
        Channel channel = session.find(Channel.class, id);
        if (channel == null && !createIfNotExists) {
            return null;
        }
        if (channel == null) {
            Transaction trans = session.beginTransaction();
            channel = new Channel();
            channel.setId(id);
            session.save(channel);
            trans.commit();
        }
        session.close();
        ChannelConfig settings = new ChannelConfig(channel);
        cache.put(channelId, settings);
        LoggerFactory.getLogger(ChannelConfig.class).debug("Saving {} to channel cache", id);
        return settings;
    }

    /**
     * Whether this is the default configuration.
     *
     * @return whether this is the default configuration
     */
    public boolean isDefault() {
        return data.getGameAdvisories() &&
            data.getInPlayDelay() == 13 &&
            data.getNoPlayDelay() == 18 &&
            !data.getOnlyScoringPlays();
    }

    public Channel getChannel() {
        return data;
    }

    public void saveData() {
        var session = DatabaseHelper.getSessionFactory().openSession();
        Transaction trans = session.beginTransaction();
        session.update(data);
        trans.commit();
        session.close();
        cache.put(getId(), new ChannelConfig(data));
        LoggerFactory.getLogger(ChannelConfig.class).debug("Saved {} to database, and updating cache", getId());
    }

    public String getId() {
        return data.getId() + "";
    }

    public boolean onlyScoringPlays() {
        return data.getOnlyScoringPlays();
    }

    public boolean gameAdvisories() {
        return data.getGameAdvisories();
    }

    public int inPlayDelay() {
        return data.getInPlayDelay();
    }

    public int noPlayDelay() {
        return data.getNoPlayDelay();
    }

    public boolean showScoreOnOut3() {
        return data.getShowScoreOnOut3();
    }
}
