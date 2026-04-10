package moe.koseirin.nyanruaineo.repository;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String>, Serializable    {
}