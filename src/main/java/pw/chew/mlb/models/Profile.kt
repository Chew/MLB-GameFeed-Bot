package pw.chew.mlb.models

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "profiles")
open class Profile {
    @Id
    @Column(name = "id", nullable = false)
    open var id: Long? = null

    @Column(name = "credits", nullable = false)
    open var credits: Int = 100
}
