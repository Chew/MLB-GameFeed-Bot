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

public class JDAListeners extends ListenerAdapter {
    private static final DB db = DBMaker.fileDB("games.db").fileMmapEnable().closeOnJvmShutdown().make();
    private static final HTreeMap<String, ActiveGame> gamesMap = db
        .hashMap("messages", Serializer.STRING, new ActiveGame.EntrySerializer())
        .createOrOpen();

    private final Logger logger = LoggerFactory.getLogger(JDAListeners.class);

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        logger.info("Stopping game threads (will resume on restart)");

//        for (ActiveGame game : GAME_THREADS.keySet()) {
//            gamesMap.put(game.gamePk(), game);
//            // Get the thread
//            Thread gameThread = GAME_THREADS.get(game);
//            // Stop the thread
//            gameThread.interrupt();
//            // Debug log
//            logger.debug("Stopped game thread for gamePk: " + game.gamePk());
//        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("Resuming game threads");

//        for (ActiveGame game : gamesMap.values()) {
//            // Start a new thread
//            Thread gameThread = new Thread(() -> new GameFeedHandler(game.gamePk()/*, game.gamePk(), event.getJDA().getTextChannelById(game.channelId()))*/));
//            GAME_THREADS.put(game, gameThread);
//            gameThread.start();
//            // Remove the game from the map
//            gamesMap.remove(game.gamePk());
//            // Debug log
//            logger.debug("Resumed game thread for gamePk: " + game.gamePk());
//        }
    }
}
