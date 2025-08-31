package moe.koseirin.nyanruaineo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/*
 * @author KoseiRin_
 * awa
 */
@Entity
@Setter
@Getter
public class ServerConfig {

    @Id
    @Column(columnDefinition="varchar(150)",nullable = false)
    private String id;

    @Column(columnDefinition = "Text")
    private String raw;
}
