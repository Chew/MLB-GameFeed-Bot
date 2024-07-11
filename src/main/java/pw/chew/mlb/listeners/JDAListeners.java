package pw.chew.mlb.listeners;

import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pw.chew.mlb.objects.ActiveGame;

import static pw.chew.mlb.listeners.GameFeedHandler.GAME_THREADS;

public class JDAListeners extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(JDAListeners.class);

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        logger.info("Stopping game threads (will resume on restart)");

        for (Thread thread : GAME_THREADS.values()) {
            thread.interrupt();
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("Resuming game threads");

        for (ActiveGame game : GameFeedHandler.allGames()) {
            // Start the game
            GameFeedHandler.addGame(game, false);
            // Debug log
            logger.debug("Resumed game with gamePk: " + game.gamePk());
        }
    }
}
