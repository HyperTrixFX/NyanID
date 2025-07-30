package moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository;

import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.TexturesList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;


@Repository
public interface TexturesListRepository extends JpaRepository<TexturesList, String>, Serializable {
}
