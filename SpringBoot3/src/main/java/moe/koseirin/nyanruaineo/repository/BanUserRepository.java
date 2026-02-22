package moe.koseirin.nyanruaineo.repository;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.entity.BanUserList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface BanUserRepository extends JpaRepository<BanUserList, String>,Serializable {

    @Query(value = "SELECT a FROM BanUserList a WHERE a.uid = ?1 AND a.isActive = true AND a.Type= 4 or a.Type = 5 or a.Type = 0")
    BanUserList LEVE450TRUE(String uid);

    @Query(value = "SELECT a.BanID FROM BanUserList a WHERE a.uid = ?1 AND a.isActive = true")
    String findBanIDByUid(String uid);

    @Query(value = "SELECT COUNT(*) AS nums FROM BanUserList WHERE uid = ?1")
    int COUNTByUid(String uid);

    @Query("SELECT b FROM BanUserList b WHERE b.uid = ?1")
    Page<BanUserList> searchByUid(String keyword, Pageable pageable);
}
