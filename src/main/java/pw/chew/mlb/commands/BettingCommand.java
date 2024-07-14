package pw.chew.mlb.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.hibernate.Transaction;
import pw.chew.chewbotcca.util.DatabaseHelper;
import pw.chew.mlb.models.Bet;
import pw.chew.mlb.models.BetKind;
import pw.chew.mlb.models.Profile;
import pw.chew.mlb.objects.BetHelper;
import pw.chew.mlb.objects.GameBlurb;
import pw.chew.mlb.objects.GameState;
import pw.chew.mlb.util.AutocompleteUtil;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class BettingCommand extends SlashCommand {
    public BettingCommand() {
        this.name = "betting";
        this.help = "Bet on a team";
        this.children = new SlashCommand[]{
            new BettingProfileSubCommand(),
            new BettingBetSubCommand(),
            new BettingClaimSubCommand()
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
            Profile profile = BetHelper.retrieveProfile(event.getUser().getIdLong());
            List<Bet> bets = retrieveBets(event.getUser().getIdLong());

            // build embed
            event.replyEmbeds(buildProfileEmbed(event, profile, bets)).setEphemeral(true).queue();
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
                    bet.amount() > 0 ? "+" : "",
                    bet.amount(),
                    bet.getReason()
                ));
            }

            embed.addField("Recent Bets", String.join("\n", betString), false);

            return embed.build();
        }
    }

    public static class BettingClaimSubCommand extends SlashCommand {
        public BettingClaimSubCommand() {
            this.name = "claim";
            this.help = "Claim your free daily credits";
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            Bet recentDailyCredit = getRecentDailyCredit(event.getUser().getIdLong());
            long lastRecentDaily = 0;

            if (recentDailyCredit != null) {
                lastRecentDaily = recentDailyCredit.getCreatedAt().getEpochSecond();
            }

            long now = OffsetDateTime.now().toEpochSecond();
            long diff = now - lastRecentDaily;

            // must be less than 24 hours
            if (diff < 86400) {
                long canClaimAt = lastRecentDaily + 86400;

                event.reply("You already claimed your daily credits! You can claim again <t:" + canClaimAt + ":R>").setEphemeral(true).queue();
                return;
            }

            // add daily credit
            addDailyCredit(event.getUser().getIdLong());

            event.reply("You have claimed your daily credits!").setEphemeral(true).queue();
        }

        public Bet getRecentDailyCredit(long userId) {
            var session = DatabaseHelper.getSessionFactory().openSession();

            // get Bets where user_id == userId
            List<Bet> bets = session.createQuery("from Bet where userId = :userId and kind = :kind and reason = :reason order by createdAt desc", Bet.class)
                .setParameter("userId", userId)
                .setParameter("reason", "Daily Credits")
                .setParameter("kind", BetKind.AUTOMATED)
                .getResultList();

            session.close();

            if (bets.isEmpty()) {
                return null;
            }

            return bets.get(0);
        }

        public void addDailyCredit(long userId) {
            var session = DatabaseHelper.getSessionFactory().openSession();

            Bet bet = new Bet();
            bet.setKind(BetKind.AUTOMATED);
            bet.setBet(0);
            bet.setPayout(10);
            bet.setReason("Daily Credits");
            bet.setUserId(userId);

            Profile profile = BetHelper.retrieveProfile(userId);
            profile.setCredits(profile.getCredits() + 10);

            Transaction trans = session.beginTransaction();
            session.update(profile);
            session.save(bet);
            trans.commit();
        }
    }

    public static class BettingBetSubCommand extends SlashCommand {
        public BettingBetSubCommand() {
            this.name = "bet";
            this.help = "Bet on a team";
            this.options = List.of(
                new OptionData(OptionType.INTEGER, "team", "Which team to bet on", true, true),
                new OptionData(OptionType.INTEGER, "date", "Which game to bet on", true, true),
                new OptionData(OptionType.INTEGER, "amount", "How much to bet. 0 to remove bet.", true, false)
                    .setMinValue(0)
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            // options
            int teamId = (int) event.optLong("team", 0);
            int gamePk = (int) event.optLong("date", 0);
            int amount = (int) event.optLong("amount", 0);

            // Check the game status
            GameState state = GameState.fromPk(String.valueOf(gamePk));
            if (state.isFinal()) {
                event.reply("This game is already over!").setEphemeral(true).queue();
                return;
            }
            if (state.inning() > 4) {
                event.reply("Bets cannot be placed or changed past the 4th inning! To see your bet, run `/betting placed`").setEphemeral(true).queue();
                return;
            }

            // start session
            var session = DatabaseHelper.getSessionFactory().openSession();

            // check for a bet, check by user_id, game_pk, and team_id
            Bet bet = session.createQuery("from Bet where userId = :userId and gamePk = :gamePk and teamId = :teamId", Bet.class)
                .setParameter("userId", event.getUser().getIdLong())
                .setParameter("gamePk", gamePk)
                .setParameter("teamId", teamId)
                .uniqueResult();

            // Ensure we have enough credit to bet
            Profile user = BetHelper.retrieveProfile(event.getUser().getIdLong());

            // Get game blurb
            GameBlurb blurb = new GameBlurb(gamePk + "");

            // if bet exists, update it
            if (bet == null) {
                if (amount == 0) {
                    event.reply("You must bet at least 1 credit!").setEphemeral(true).queue();
                    return;
                }

                if (user.getCredits() < amount) {
                    event.reply("You don't have enough credits to bet that much! You only have " + user.getCredits() + " credits!").setEphemeral(true).queue();
                    return;
                }

                String team = blurb.away().id() == teamId ? blurb.away().name() : blurb.home().name();

                Bet newBet = new Bet();
                newBet.setUserId(event.getUser().getIdLong());
                newBet.setGamePk(gamePk);
                newBet.setTeamId(teamId);
                newBet.setBet(amount);
                newBet.setKind(BetKind.PENDING);
                newBet.setReason("Bet on " + team + " for " + blurb.name());

                user.setCredits(user.getCredits() - amount);

                Transaction trans = session.beginTransaction();
                session.save(newBet);
                session.update(user);
                trans.commit();

                event.reply("Bet placed!").setEphemeral(true).queue();
            } else if (amount == 0) {
                int currentBet = bet.getBet();

                Transaction trans = session.beginTransaction();
                session.delete(bet);

                user.setCredits(user.getCredits() + currentBet);
                session.update(user);

                trans.commit();

                event.reply("Bet removed!").setEphemeral(true).queue();
            } else {
                int currentBet = bet.getBet();
                int creditsToDeduct = amount - currentBet;
                if (amount > currentBet && creditsToDeduct > user.getCredits()) {
                    event.reply("You don't have enough credits to bet that much! You only have " + user.getCredits() + " credits!").queue();
                    return;
                }

                user.setCredits(user.getCredits() - creditsToDeduct);
                bet.setBet(amount);

                Transaction trans = session.beginTransaction();
                session.update(bet);
                session.update(user);
                trans.commit();

                event.reply("Bet updated!").setEphemeral(true).queue();
            }

            session.close();
        }

        @Override
        public void onAutoComplete(CommandAutoCompleteInteractionEvent event) {
            event.replyChoices(AutocompleteUtil.handleInput(event)).queue();
        }
    }

    public static class BettingPlacedSubCommand extends SlashCommand {
        public BettingPlacedSubCommand() {
            this.name = "placed";
            this.help = "View your placed bets";
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            List<Bet> bets = retrieveBets(event.getUser().getIdLong());

        }
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
