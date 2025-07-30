package moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository;

import jakarta.transaction.Transactional;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Yggdrasil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface YggdrasilRepository extends JpaRepository<Yggdrasil, String>, Serializable {

    @Query(value = "SELECT y.uuid  FROM Yggdrasil y WHERE y.nyanuid = ?1 OR y.uuid = ?1")
    String GetPlayerUUID(String uid);

    @Query(value = "SELECT y.playername  FROM Yggdrasil y WHERE y.nyanuid = ?1 OR y.uuid = ?1")
    String GetPlayerNAME(String uid);

    @Query(value = "SELECT y.useSkin  FROM Yggdrasil y WHERE y.uuid = ?1")
    Boolean getUseSkin(String uid);

    @Query(value = "SELECT y.useCAPE  FROM Yggdrasil y WHERE y.uuid = ?1")
    Boolean getUseCAPE(String uid);

    @Query(value = "SELECT y  FROM Yggdrasil y WHERE y.nyanuid = ?1 OR y.uuid = ?1")
    Yggdrasil YggdrasilPlayer(String uid);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Yggdrasil y SET y.playername = ?1 where y.nyanuid = ?2")
    void UpdatePlayerName(String n, String uid);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Yggdrasil y SET  y.useSkin = ?1 where y.nyanuid = ?2 OR y.uuid = ?2")
    void UpdateUseSkin(Boolean b, String uid);
    @Modifying
    @Transactional
    @Query(value = "UPDATE Yggdrasil y SET  y.useCAPE = ?1 where y.nyanuid = ?2 OR y.uuid = ?2")
    void UpdateUseCAPE(Boolean b, String uid);

}
