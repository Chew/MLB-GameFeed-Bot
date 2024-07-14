package pw.chew.mlb.objects;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import pw.chew.chewbotcca.util.DatabaseHelper;
import pw.chew.mlb.listeners.GameFeedHandler;
import pw.chew.mlb.models.Bet;
import pw.chew.mlb.models.BetKind;
import pw.chew.mlb.models.Profile;

import java.util.ArrayList;
import java.util.List;

public class BetHelper {
    public static void awardWinners(String gamePk, int winningTeam) {
        var session = DatabaseHelper.getSessionFactory().openSession();

        // get all sessions where bet's gamePk is the gamePk
        var bets = session.createQuery("from Bet where gamePk = :gamePk", Bet.class)
            .setParameter("gamePk", Integer.parseInt(gamePk))
            .getResultList();

        LoggerFactory.getLogger(BetHelper.class).debug("Bets for game {}: {}", gamePk, bets.size());

        LoggerFactory.getLogger(BetHelper.class).debug("Winning team: {}", winningTeam);
        int totalPoints = 0;
        int winningPoints = 0;

        List<Bet> winningBets = new ArrayList<>();
        List<Bet> losingBets = new ArrayList<>();

        for (Bet bet : bets) {
            totalPoints += bet.amount();
            Integer teamId = bet.getTeamId();
            if (teamId == null) {
                continue;
            }

            if (teamId == winningTeam) {
                winningBets.add(bet);
                winningPoints += bet.amount();
            } else {
                losingBets.add(bet);
            }
        }

        Transaction trans = session.beginTransaction();

        // first let's set all the losers and make sure they lose their points
        for (Bet bet : losingBets) {
            bet.setPayout(0);
            bet.setKind(BetKind.LOSS);
        }

        // now we give winners their points
        for (Bet bet : winningBets) {
            bet.setPayout((int) Math.floor((double) bet.amount() / winningPoints * totalPoints));
            bet.setKind(BetKind.WIN);
        }

        LoggerFactory.getLogger(GameFeedHandler.class).debug("Awarding winners: {}", winningBets);

        // save all the bets
        trans.commit();
    }

    /**
     * Retrieves a profile for the provided user id.
     * If there is no profile present, it will create a profile and award a bet.
     *
     * @param userId the user id
     * @return the profile
     */
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
            bet.setKind(BetKind.AUTOMATED);
            bet.setBet(0);
            bet.setPayout(100);
            bet.setReason("Initial betting credits");
            bet.setUserId(userId);
            session.save(bet);

            trans.commit();
        }
        session.close();
        return profile;
    }

    /**
     * Adds an automated bet for the user.
     *
     * @param userId the user id
     * @param amount the amount to add to the account
     * @param reason the reason for the bet
     * @param session the session to use, null if to open a new session. if a session is provided, the transaction won't commit
     * @return the bet
     */
    public static Bet addAutomatedBet(long userId, int amount, String reason, @Nullable Session session) {
        // open session and transaction
        if (session == null) {
            session = DatabaseHelper.getSessionFactory().openSession();
        }
        Transaction trans = session.beginTransaction();

        // add initial betting credits
        Bet bet = new Bet();
        bet.setKind(BetKind.AUTOMATED);
        bet.setBet(0); // 0 bet because the user didn't do anything
        bet.setPayout(amount);
        bet.setReason(reason);
        bet.setUserId(userId);

        session.save(bet);
        trans.commit();

        return bet;
    }
}
