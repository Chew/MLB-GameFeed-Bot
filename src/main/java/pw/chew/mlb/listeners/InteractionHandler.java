package pw.chew.mlb.listeners;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import pw.chew.mlb.commands.PlanGameCommand;
import pw.chew.mlb.commands.StartGameCommand;

public class InteractionHandler extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getComponentId().startsWith("plangame:")) {
            String action = event.getComponentId().split(":")[1];
            String gamePk = event.getComponentId().split(":")[2];
            switch (action) {
                case "refresh" -> event.getMessage()
                    .editMessage(PlanGameCommand.generateGameBlurb(gamePk))
                    .setActionRow(PlanGameCommand.buildButtons(gamePk))
                    .queue((m) -> event.reply("Refreshed!").setEphemeral(true).queue());
                case "start" -> {
                    String startGame = StartGameCommand.startGame(gamePk, event.getChannel().getId());
                    event.reply(startGame).setEphemeral(!startGame.contains("Starting game")).queue();
                }
            }
        }
    }
}
