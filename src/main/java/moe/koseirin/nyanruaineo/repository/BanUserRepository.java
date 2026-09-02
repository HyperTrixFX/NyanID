package moe.koseirin.nyanruaineo.repository;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.transaction.Transactional;
import moe.koseirin.nyanruaineo.entity.BanUserList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BanUserRepository extends JpaRepository<BanUserList, String>, Serializable {

    @Query(value = "SELECT a FROM BanUserList a WHERE a.uid = ?1 AND a.isActive = true AND a.Type= 4 or a.Type = 5 or a.Type = 0")
    BanUserList LEVE450TRUE(String uid);

    @Query(value = "SELECT a.BanID FROM BanUserList a WHERE a.uid = ?1 AND a.isActive = true")
    String findBanIDByUid(String uid);

    @Query(value = "SELECT COUNT(*) AS nums FROM BanUserList WHERE uid = ?1")
    int COUNTByUid(String uid);

    @Query("SELECT b FROM BanUserList b WHERE b.uid = ?1")
    Page<BanUserList> searchByUid(String keyword, Pageable pageable);

    /**
     * 查询某个目标仍然生效的游戏登录封禁（type 5/6），过期时间已过的不算。
     * {@code targetType} 为 0 时，历史数据里 {@code TargetType IS NULL} 的旧行也视为 UID 封禁。
     */
    @Query("SELECT b FROM BanUserList b WHERE b.isActive = true AND (b.Type = 5 OR b.Type = 20) " +
            "AND (b.ExpireTime IS NULL OR b.ExpireTime > ?3) " +
            "AND b.uid = ?1 " +
            "AND (b.TargetType = ?2 OR (b.TargetType IS NULL AND ?2 = 0)) " +
            "ORDER BY b.BanTime DESC")
    List<BanUserList> findActiveGameBans(String target, int targetType, LocalDateTime now);

    /** 批量把已过期的封禁置为失效（自动解封），单条 UPDATE，低资源占用。 */
    @Modifying
    @Transactional
    @Query("UPDATE BanUserList b SET b.isActive = false WHERE b.isActive = true AND b.ExpireTime IS NOT NULL AND b.ExpireTime <= ?1")
    int deactivateExpired(LocalDateTime now);
}
