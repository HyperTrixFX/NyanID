package moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository;

import jakarta.transaction.Transactional;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.YggdrasilPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface YggdrasilPlayerRepository extends JpaRepository<YggdrasilPlayer,String>, Serializable {

    @Query(value = "SELECT y.SkinTexturesHash FROM  YggdrasilPlayer y WHERE y.uuid = ?1")
    String getSkinTexturesHash(String uuid);

    @Query(value = "SELECT y.CAPETexturesHash FROM  YggdrasilPlayer y WHERE y.uuid = ?1")
    String getCAPETexturesHash(String uuid);

    @Query(value = "SELECT y.SkinTexturesType FROM  YggdrasilPlayer y WHERE y.uuid = ?1")
    int getSkinTexturesType(String uuid);

    @Modifying
    @Transactional
    @Query(value = "UPDATE YggdrasilPlayer y SET  y.SkinTexturesType = ?1  where y.uuid = ?2")
    void UpdateSkinTexturesType(int b, String u);
    @Modifying
    @Transactional
    @Query(value = "UPDATE YggdrasilPlayer y SET  y.SkinTexturesHash = ?1  where y.uuid = ?2")
    void UpdateSkinTexturesHash(String h, String u);
    @Modifying
    @Transactional
    @Query(value = "UPDATE YggdrasilPlayer y SET  y.CAPETexturesHash = ?1  where y.uuid = ?2")
    void UpdateSkinCAPETexturesHash(String h, String u);

}
