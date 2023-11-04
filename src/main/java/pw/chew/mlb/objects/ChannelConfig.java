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
        Channel server = session.find(Channel.class, id);
        if (server == null && !createIfNotExists) {
            return null;
        }
        if (server == null) {
            Transaction trans = session.beginTransaction();
            server = new Channel();
            server.setId(id);
            session.save(server);
            trans.commit();
        }
        session.close();
        ChannelConfig settings = new ChannelConfig(server);
        cache.put(channelId, settings);
        LoggerFactory.getLogger(ChannelConfig.class).debug("Saving " + id + " to channel cache");
        return settings;
    }

    public static ChannelConfig retrieveChannel(String channelId) {
        return retrieveChannel(channelId, false);
    }

    public void update(String key, boolean val) {
        data.setBoolean(key, val);
    }

    public void update(String key, int val) {
        data.setInt(key, val);
    }

    public void saveData() {
        var session = DatabaseHelper.getSessionFactory().openSession();
        session.beginTransaction();
        session.update(data);
        session.getTransaction().commit();
        session.close();
        cache.put(getId(), new ChannelConfig(data));
        LoggerFactory.getLogger(ChannelConfig.class).debug("Saved " + getId() + " to database, and updating cache");
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
