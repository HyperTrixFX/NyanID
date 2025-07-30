package moe.takanashihoshino.nyaniduserserver.entity;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


@Entity
@Setter
@Getter
public class YggdrasilPlayer {

    @Id
    @Column(columnDefinition="varchar(38)",nullable = false)
    private String uuid;

    private int SkinTexturesType;//1=default ,0=slim

    @Column(columnDefinition="varchar(65)")
    private String SkinTexturesHash;

    @Column(columnDefinition="varchar(65)")
    private String CAPETexturesHash;
}
