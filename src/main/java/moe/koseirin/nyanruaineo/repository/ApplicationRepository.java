package moe.koseirin.nyanruaineo.repository;

import moe.koseirin.nyanruaineo.entity.OAuthApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

/*
 * @author KoseiRin_
 * awa
 */
@Repository
public interface ApplicationRepository extends JpaRepository<OAuthApp,String>, Serializable {
}
