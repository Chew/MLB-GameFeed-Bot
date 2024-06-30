package pw.chew.mlb.listeners;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import pw.chew.mlb.commands.PlanGameCommand;
import pw.chew.mlb.commands.StartGameCommand;
import pw.chew.mlb.util.EmbedUtil;
import pw.chew.mlb.util.MLBAPIUtil;

import java.util.ArrayList;
import java.util.List;

public class InteractionHandler extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getComponentId().startsWith("plangame:")) {
            String action = event.getComponentId().split(":")[1];
            String gamePk = event.getComponentId().split(":")[2];
            switch (action) {
                case "refresh" -> event.deferEdit().queue(e -> e.editOriginalEmbeds(PlanGameCommand.generateGameBlurb(gamePk)).queue());
                case "start" -> {
                    try {
                        MessageEmbed startGame = StartGameCommand.startGame(gamePk, event.getGuildChannel(), event.getUser());
                        event.replyEmbeds(startGame).queue();
                    } catch (IllegalStateException e) {
                        event.replyEmbeds(EmbedUtil.failure(e.getMessage())).setEphemeral(true).queue();
                    }
                }
                case "lineup" -> {
                    String awayHome = event.getComponentId().split(":")[3];
                    var lineup = MLBAPIUtil.getLineup(gamePk, awayHome);

                    List<String> friendly = new ArrayList<>();

                    friendly.add("# " + event.getButton().getLabel());
                    friendly.add("The following is the lineup for this team. It is subject to change at any time.");
                    friendly.add("## Batting Order");

                    var battingOrder = lineup.get("Batting Order");

                    if (battingOrder.isEmpty()) {
                        friendly.add("The batting order is currently not available. Please try again closer to the scheduled game time.");
                    } else {
                        for (var player : battingOrder) {
                            friendly.add(player.friendlyString());
                        }
                    }

                    // add a string before the next-to-last element. e.g. "a", "b", "c" <-- between b and c
                    friendly.add("## Probable Pitcher");

                    var probablePitcher = lineup.get("Probable Pitcher");

                    if (probablePitcher.isEmpty()) {
                        friendly.add("The probable pitcher is currently not available. Please try again closer to the scheduled game time.");
                    } else {
                        friendly.add(probablePitcher.get(0).friendlyString());
                    }

                    event.reply(String.join("\n", friendly)).setEphemeral(true).queue();
                }
            }
        }
    }
}
