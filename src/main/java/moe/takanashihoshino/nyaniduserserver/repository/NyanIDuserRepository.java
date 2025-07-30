package moe.takanashihoshino.nyaniduserserver.repository;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.transaction.Transactional;
import moe.takanashihoshino.nyaniduserserver.entity.NyanIDuser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface NyanIDuserRepository extends JpaRepository<NyanIDuser, String>, Serializable {

    @Query(value = "SELECT n FROM NyanIDuser n where n.uid = ?1")
    NyanIDuser getUser(String uid);

    @Query(value = "SELECT u.IsGIFAvatar FROM NyanIDuser u Where u.uid = ?1")
    Boolean IsGIFAvatar(String uid);

    @Query(value = "SELECT u.GIFAvatarID FROM NyanIDuser u Where u.uid = ?1")
    int GIFAvatarID(String uid);

    @Query(value = "SELECT u.EnableGIFAvatar FROM NyanIDuser u Where u.uid = ?1")
    Boolean EnableGIFAvatar(String uid);

    @Modifying
    @Transactional
    @Query(value = "update NyanIDuser set EnableGIFAvatar = ?1  where uid = ?2")
    void UpdateEnableGIFAvatar(Boolean d,String uid);

    @Modifying
    @Transactional
    @Query(value = "update NyanIDuser set Description = ?1  where uid = ?2")
    void SetDescriptionByUid(String Description,String uid);



    @Modifying
    @Transactional
    @Query(value = "update NyanIDuser set nickname = ?1  where uid = ?2")
    void UpdateNickname(String nickname,String uid);



}
