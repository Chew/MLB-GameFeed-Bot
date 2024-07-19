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
            addAutomatedBet(userId, 100, "Initial betting credits", session);

            // set initial credits in the profile
            profile.setCredits(100);

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
        boolean nullSession = false;
        if (session == null) {
            session = DatabaseHelper.getSessionFactory().openSession();
            nullSession = true;
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

        // Commit transaction and close session if we opened to begin with
        if (nullSession) {
            trans.commit();
            session.close();
        }

        return bet;
    }

    /**
     * Adds the daily credit to the user.
     *
     * @param userId
     */
    public static void addDailyCredit(long userId) {
        var session = DatabaseHelper.getSessionFactory().openSession();

        // Add daily credits
        addAutomatedBet(userId, 10, "Daily Credits", session);

        Profile profile = BetHelper.retrieveProfile(userId);
        profile.setCredits(profile.getCredits() + 10);

        Transaction trans = session.beginTransaction();
        session.update(profile);
        trans.commit();
        session.close();
    }

    public static Bet getRecentDailyCredit(long userId) {
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
