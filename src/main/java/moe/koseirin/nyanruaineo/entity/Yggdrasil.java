package moe.koseirin.nyanruaineo.entity;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Setter
@Getter
public class Yggdrasil {

    @Id
    @Column(columnDefinition="varchar(36)",nullable = false)
    private String uuid;

    private Boolean useSkin;

    private Boolean useCAPE;

    @Column(columnDefinition="varchar(20)",nullable = false)
    private String playername;

    @Column(columnDefinition="varchar(32)",nullable = false)
    private String nyanuid;

    private int type;

}
