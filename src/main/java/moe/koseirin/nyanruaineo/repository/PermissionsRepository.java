package moe.koseirin.nyanruaineo.repository;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface PermissionsRepository extends JpaRepository<Permission, UUID>, Serializable {

    Optional<Permission> findFirstByName(String name);

    Optional<Permission> findFirstByCode(int code);
}
