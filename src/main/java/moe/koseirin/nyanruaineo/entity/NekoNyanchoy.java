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

//mongodb
@Entity
@Getter
@Setter
public class NekoNyanchoy {

    @Id
    @Column(columnDefinition="varchar(32)",nullable = false)
    private String UserUID;

    private String likability;

    private String TotalCatFood;

    @Column(columnDefinition="text")
    private String Data;

}
