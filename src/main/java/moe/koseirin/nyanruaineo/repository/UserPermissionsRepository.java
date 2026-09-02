package moe.koseirin.nyanruaineo.repository;


/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.entity.UserPermissions;
import moe.koseirin.nyanruaineo.entity.UserPermissionsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;

@Repository
public interface UserPermissionsRepository extends JpaRepository<UserPermissions, UserPermissionsId>, Serializable {

    List<UserPermissions> findByUid(String uid);

    boolean existsByUidAndPermission(String uid, String permission);

    void deleteByUidAndPermission(String uid, String permission);

    long deleteByUid(String uid);
}
