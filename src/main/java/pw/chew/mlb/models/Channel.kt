package pw.chew.mlb.models

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "channels")
open class Channel {
    @Id
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    /**
     * Whether to only show scoring plays
     */
    @Column(name = "onlyScoringPlays")
    open var onlyScoringPlays: Boolean = false

    /**
     * Whether to show game advisories
     */
    @Column(name = "gameAdvisories")
    open var gameAdvisories: Boolean = true

    /**
     * The amount of time in seconds to delay an in-play ball (non strikeout/walk) before showing it. Default 13.
     */
    @Column(name = "inPlayDelay")
    open var inPlayDelay: Int = 13

    /**
     * The amount of time in seconds to delay a strikeout/walk before showing it. Default 18.
     */
    @Column(name = "noPlayDelay")
    open var noPlayDelay: Int = 18

    /**
     * Whether to show the score when the third out is recorded. Default true.
     */
    @Column(name = "showScoreOnOut3")
    open var showScoreOnOut3: Boolean = true

    fun setBoolean(info: String, newValue: Boolean) {
        // Dynamically call the method based on the info
        this.javaClass.getMethod("set${info}", Boolean::class.java).invoke(this, newValue)
    }

    fun setInt(info: String, newValue: Int) {
        // Dynamically call the method based on the info
        this.javaClass.getMethod("set${info}", Int::class.java).invoke(this, newValue)
    }

    fun get(info: String) {
        // Dynamically call the method based on the info
        this.javaClass.getMethod("get${info}").invoke(this)
    }
}