package moe.koseirin.nyanruaineo.repository;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.entity.ServerList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.io.Serializable;


@Repository
public interface ServerListRepository extends JpaRepository<ServerList, String>, Serializable {


    @Query(value = "SELECT Token FROM ServerList WHERE ServerUid = ?1")
    String findTokenByServerUid(String ServerUid);

    @Query(value = "SELECT SecretKey FROM ServerList WHERE ServerUid = ?1")
    String findSKeyByServerUid(String ServerUid);



}
