package pw.chew.mlb.listeners;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import pw.chew.mlb.commands.PlanGameCommand;

public class InteractionHandler extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getComponentId().startsWith("plangame:refresh:")) {
            String gamePk = event.getComponentId().split(":")[2];
            event.getMessage().editMessage(PlanGameCommand.generateGameBlurb(gamePk)).queue((m) -> {
                event.reply("Refreshed!").setEphemeral(true).queue();
            });
        }
    }
}
