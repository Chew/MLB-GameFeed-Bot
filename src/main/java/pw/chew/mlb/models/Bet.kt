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

    @Column(name = "amount", nullable = false)
    open var amount: Int = 0

    @Column(name = "successful", nullable = false)
    open var successful: Boolean = false

    @Column(name = "automated", nullable = false)
    open var automated: Boolean = false

    @Column(name = "reason", nullable = false, length = 128)
    open var reason: String = ""

    @Column(name = "created_at", nullable = false)
    open var createdAt: Instant = Instant.now()
}
