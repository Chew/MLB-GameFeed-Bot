package pw.chew.mlb.models

import java.time.Instant
import javax.persistence.*

@Entity
@Table(name = "bets")
open class Bet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "user_id", nullable = false)
    open var userId: Long? = null

    @Column(name = "bet", nullable = false)
    open var bet: Int = 0

    @Column(name = "payout", nullable = false)
    open var payout: Int = 0

    @Column(name = "game_pk")
    open var gamePk: Int? = null

    @Column(name = "team_id")
    open var teamId: Int? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    open var kind: BetKind = BetKind.PENDING

    @Column(name = "reason", nullable = false, length = 128)
    open var reason: String = ""

    @Column(name = "created_at", nullable = false)
    open var createdAt: Instant = Instant.now()

    fun amount(): Int {
        return payout - bet
    }
}

enum class BetKind {
    AUTOMATED,
    PENDING,
    WIN,
    LOSS
}