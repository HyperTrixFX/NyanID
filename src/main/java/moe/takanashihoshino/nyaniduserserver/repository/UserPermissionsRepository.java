package moe.takanashihoshino.nyaniduserserver.repository;


/*
 * @author KoseiRin_
 * awa
 */

import moe.takanashihoshino.nyaniduserserver.entity.UserPermissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface UserPermissionsRepository extends JpaRepository<UserPermissions,String>, Serializable {

    @Query(value = "SELECT u.UserGroup FROM UserPermissions u WHERE u.uid = ?1 AND u.Action = ?2 AND u.AdminKey = ?3")
    String getUserGroup(String uid,String Action,String KEY);


    @Query(value = "SELECT u FROM UserPermissions u WHERE u.uid = ?1")
    UserPermissions getByUid(String uid);
}
