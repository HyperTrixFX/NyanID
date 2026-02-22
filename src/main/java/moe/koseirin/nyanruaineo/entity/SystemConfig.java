package moe.koseirin.nyanruaineo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
/*
 * @author KoseiRin_
 * awa
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class SystemConfig {
    @Id
    private String configKey;

    private String configValue;



}
