package moe.takanashihoshino.nyaniduserserver.repository;

/*
 * @author KoseiRin_
 * awa
 */

import moe.takanashihoshino.nyaniduserserver.entity.TexturesList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;


@Repository
public interface TexturesListRepository extends JpaRepository<TexturesList, String>, Serializable {
}
