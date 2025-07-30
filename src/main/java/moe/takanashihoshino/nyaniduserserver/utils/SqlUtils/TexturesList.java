package moe.takanashihoshino.nyaniduserserver.utils.SqlUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Data
public class TexturesList  implements Serializable {

    @Id
    @Column(columnDefinition="varchar(150)",nullable = false)
    private String hash;

    private long create_time;

    @Column(columnDefinition="varchar(150)",nullable = false)
    private String uid;

    private Boolean type;// true skin ,false cape

    private int model;//0 slim ,1 default,
}
