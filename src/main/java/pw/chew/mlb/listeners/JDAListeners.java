package pw.chew.mlb.listeners;

import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.chew.mlb.objects.ActiveGame;

import static pw.chew.mlb.listeners.GameFeedHandler.ACTIVE_GAMES;
import static pw.chew.mlb.listeners.GameFeedHandler.GAME_THREADS;

public class JDAListeners extends ListenerAdapter {
    private static final DB db = DBMaker.fileDB("games.db").fileMmapEnable().closeOnJvmShutdown().make();
    private static final HTreeMap<String, ActiveGame> gamesMap = db
        .hashMap("games", Serializer.STRING, new ActiveGame.EntrySerializer())
        .createOrOpen();

    private final Logger logger = LoggerFactory.getLogger(JDAListeners.class);

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        logger.info("Stopping game threads (will resume on restart)");

        for (ActiveGame game : ACTIVE_GAMES) {
            gamesMap.put(game.gamePk(), game);
            // Debug log
            logger.debug("Stored game for gamePk: " + game.gamePk());
        }

        for (Thread thread : GAME_THREADS.values()) {
            thread.interrupt();
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("Resuming game threads");

        for (ActiveGame game : gamesMap.values()) {
            // Start the game
            GameFeedHandler.addGame(game);
            // Remove the game from the map
            gamesMap.remove(game.gamePk());
            // Debug log
            logger.debug("Resumed game with gamePk: " + game.gamePk());
        }
    }
}
