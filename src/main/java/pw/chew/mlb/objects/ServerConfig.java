package pw.chew.mlb.objects;

import org.hibernate.Transaction;
import org.slf4j.LoggerFactory;
import pw.chew.chewbotcca.util.DatabaseHelper;
import pw.chew.mlb.MLBBot;
import pw.chew.mlb.models.Server;

import java.util.HashMap;

public class ServerConfig {
    static final HashMap<String, ServerConfig> cache = new HashMap<>();
    final Server data;
    public ServerConfig(Server input) {
        data = input;
    }

    /**
     * Returns the config for the specified server.
     *
     * @param id the server ID to get the config for
     * @param createIfNotExists whether to create the config if it doesn't exist
     * @return the config for the specified server
     */
    public static ServerConfig getConfig(String id, boolean createIfNotExists) {
        if (cache.containsKey(id)) {
            return cache.get(id);
        } else {
            return retrieveServer(id, createIfNotExists);
        }
    }

    /**
     * Get a server from the cache, and only the cache.
     * Don't attempt retrieving it.
     * @param id the channel ID
     * @return a possibly null channel
     */
    public static ServerConfig getServerIfCached(String id) {
        return cache.get(id);
    }

    /**
     * Retrieve server info
     * @param serverId the server id
     * @return a server settings
     */
    public static ServerConfig retrieveServer(String serverId, boolean createIfNotExists) {
        long id = Long.parseLong(serverId);
        var session = DatabaseHelper.getSessionFactory().openSession();
        Server server = session.find(Server.class, id);
        if (server == null && !createIfNotExists) {
            return null;
        }
        if (server == null) {
            Transaction trans = session.beginTransaction();
            server = new Server();
            server.setId(id);
            session.save(server);
            trans.commit();
        }
        session.close();
        ServerConfig settings = new ServerConfig(server);
        cache.put(serverId, settings);
        LoggerFactory.getLogger(ServerConfig.class).debug("Saving {} to channel cache", id);
        return settings;
    }

    public Server getServer() {
        return data;
    }

    public void saveData() {
        var session = DatabaseHelper.getSessionFactory().openSession();
        session.beginTransaction();
        session.update(data);
        session.getTransaction().commit();
        session.close();
        cache.put(getId(), new ServerConfig(data));
        LoggerFactory.getLogger(ServerConfig.class).debug("Saved server with ID {} to database, and updating cache", getId());
    }

    public String getId() {
        return data.getId() + "";
    }

    public Integer teamId() {
        return data.getTeamId();
    }

    public String teamName() {
        var mlbteam = MLBBot.TEAMS.stream()
            .filter(team -> team.getInt("id") == teamId())
            .findFirst();

        if (mlbteam.isPresent()) {
            return mlbteam.get().getString("name");
        } else {
            return "No team set";
        }
    }
}
