package moe.takanashihoshino.nyaniduserserver.entity;

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
public class TexturesList{

    @Id
    @Column(columnDefinition="varchar(150)",nullable = false)
    private String hash;

    private long create_time;

    @Column(columnDefinition="varchar(32)",nullable = false)
    private String uid;

    private Boolean type;// true skin ,false cape

    private int model;//0 slim ,1 default,
}
