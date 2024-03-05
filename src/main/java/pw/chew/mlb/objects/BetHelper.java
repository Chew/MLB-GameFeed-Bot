package pw.chew.mlb.objects;

import org.hibernate.Transaction;
import org.slf4j.LoggerFactory;
import pw.chew.chewbotcca.util.DatabaseHelper;
import pw.chew.mlb.listeners.GameFeedHandler;
import pw.chew.mlb.models.Bet;
import pw.chew.mlb.models.BetKind;

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
}
