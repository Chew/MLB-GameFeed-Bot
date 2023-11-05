package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.hibernate.Transaction;
import pw.chew.chewbotcca.util.DatabaseHelper;
import pw.chew.mlb.models.Bet;
import pw.chew.mlb.models.Profile;

import java.util.ArrayList;
import java.util.List;

public class BettingCommand extends SlashCommand {
    public BettingCommand() {
        this.name = "betting";
        this.help = "Bet on a team";
        this.children = new SlashCommand[]{
            new BettingProfileSubCommand()
        };
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        // unused for children
    }

    public static class BettingProfileSubCommand extends SlashCommand {
        public BettingProfileSubCommand() {
            this.name = "profile";
            this.help = "View your betting profile";
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            // try to get profile
            Profile profile = retrieveProfile(event.getUser().getIdLong());
            List<Bet> bets = retrieveBets(event.getUser().getIdLong());

            // build embed
            event.replyEmbeds(buildProfileEmbed(event, profile, bets)).queue();
        }

        private MessageEmbed buildProfileEmbed(SlashCommandEvent event, Profile profile, List<Bet> bets) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Betting Profile for " + event.getUser().getName());
            embed.addField("Credits", profile.getCredits() + "", true);

            if (event.getMember() != null) {
                embed.setColor(event.getMember().getColor());
            }

            // get 5 most recent bets
            List<String> betString = new ArrayList<>();
            for (int i = 0; i < 5 && i < bets.size(); i++) {
                Bet bet = bets.get(i);
                betString.add("%s | %s%s - %s".formatted(
                    TimeFormat.DATE_SHORT.format(bet.getCreatedAt()),
                    bet.getSuccessful() ? "+" : "-",
                    bet.getAmount(),
                    bet.getReason()
                ));
            }

            embed.addField("Recent Bets", String.join("\n", betString), false);

            return embed.build();
        }
    }

    public static Profile retrieveProfile(long userId) {
        var session = DatabaseHelper.getSessionFactory().openSession();
        Profile profile = session.find(Profile.class, userId);
        if (profile == null) {
            Transaction trans = session.beginTransaction();
            profile = new Profile();
            profile.setId(userId);
            session.save(profile);

            // add initial betting credits
            Bet bet = new Bet();
            bet.setAmount(100);
            bet.setAutomated(true);
            bet.setSuccessful(true);
            bet.setReason("Initial betting credits");
            bet.setUserId(userId);
            session.save(bet);

            trans.commit();
        }
        session.close();
        return profile;
    }

    public static List<Bet> retrieveBets(long userId) {
        var session = DatabaseHelper.getSessionFactory().openSession();

        // get Bets where user_id == userId
        List<Bet> bets = session.createQuery("from Bet where userId = :userId order by createdAt desc", Bet.class)
            .setParameter("userId", userId)
            .getResultList();

        session.close();

        return bets;
    }
}
