package pw.chew.mlb.models

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "servers")
open class Server {
    @Id
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    /**
     * The team ID associated with this server
     */
    @Column(name = "teamId")
    open var teamId: Int? = null

    fun get(info: String) {
        // Dynamically call the method based on the info
        this.javaClass.getMethod("get${info}").invoke(this)
    }
}