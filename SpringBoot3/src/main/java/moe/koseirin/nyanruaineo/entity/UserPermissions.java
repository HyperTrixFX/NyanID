package moe.koseirin.nyanruaineo.entity;


/*
 * @author KoseiRin_
 * awa
 */


/*
 * @author KoseiRin_
 * awa
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
public class UserPermissions {

    @Id
    @Column(columnDefinition="varchar(32)",nullable = false)
    private String uid;

    @Column(columnDefinition="varchar(15)",nullable = false)
    private String AccessKey;

    @Column(nullable = false)
    private int Level;

    @Column(columnDefinition="varchar(15)",nullable = false)
    private String UserGroup;
}
