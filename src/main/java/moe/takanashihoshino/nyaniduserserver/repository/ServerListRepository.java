package moe.takanashihoshino.nyaniduserserver.repository;

/*
 * @author KoseiRin_
 * awa
 */

import moe.takanashihoshino.nyaniduserserver.entity.ServerList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.io.Serializable;


@Repository
public interface ServerListRepository extends JpaRepository<ServerList, String>, Serializable {


    @Query(value = "SELECT Token FROM ServerList WHERE ServerUid = ?1")
    String findTokenByServerUid(String ServerUid);

    @Query(value = "SELECT SKey FROM ServerList WHERE ServerUid = ?1")
    String findSKeyByServerUid(String ServerUid);



}
