package moe.koseirin.nyanruaineo.repository;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.transaction.Transactional;
import moe.koseirin.nyanruaineo.entity.UserDevices;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.time.LocalDateTime;


@Repository
public interface UserDevicesRepository extends JpaRepository<UserDevices, String>, Serializable {

    @Query(value = "SELECT uid FROM UserDevices WHERE Token = ?1  AND IsActive = true")
    String findUidByToken(String Token);

    @Query(value = "SELECT uid FROM UserDevices WHERE Session = ?1  AND IsActive = true")
    String findUidBySession(String Session);

    @Query(value = "SELECT u FROM UserDevices u WHERE u.ClientId = ?1  AND u.IsActive = true")
    UserDevices getByINFO(String info);

    @Query(value = "SELECT ClientId FROM UserDevices WHERE Token = ?1")
    String findClientIdByToken(String Token);

    @Query(value = "SELECT Token FROM UserDevices WHERE Session = ?1")
    String findTokenBySession(String Session);

    @Query(value = "SELECT ClientId FROM UserDevices WHERE Session = ?1  AND IsActive = true")
    String findClientIdBySession(String Session);


    @Query(value = "SELECT CreateTime FROM UserDevices WHERE Session = ?1  AND IsActive = true")
    LocalDateTime findTimeBySession(String Session);

    @Query(value = "SELECT IsActive FROM UserDevices WHERE Token = ?1  ")
    Boolean getActive(String info);


    @Transactional
    @Modifying
    @Query("UPDATE  UserDevices u SET u.CreateTime = ?1,u.IsActive = true  WHERE  u.Token = ?2 AND u.IsActive = false ")
    void UpdateCreateTime(LocalDateTime cutoffDateTime,String Token);

    @Transactional
    @Modifying
    @Query("UPDATE  UserDevices u SET u.Token = ?2  WHERE  u.Token = ?1")
    void UpdateAccessToken(String old,String n);
    @Transactional
    @Modifying
    @Query(value = "delete UserDevices WHERE Session = ?1")
    void deleteBySession(String Session);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserDevices u WHERE  u.CreateTime < ?1 AND u.HardwareID is null AND u.IsActive is false")
    int deleteByCreateTimeBefore(LocalDateTime cutoffDateTime);

    @Transactional
    @Modifying
    @Query("UPDATE  UserDevices u SET u.IsActive = false  WHERE  u.CreateTime < ?1 AND u.HardwareID is null")
    void SetNosDevices(LocalDateTime cutoffDateTime);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserDevices u WHERE  u.uid = ?1 AND u.DeviceName = ?2")
    void LogOut(String uid,String name);

    @Transactional
    @Modifying
    @Query("UPDATE  UserDevices u SET u.IsActive = false  WHERE  u.uid = ?1 OR u.Token =?1")
    void SetActiveIsFalse(String info);
    @Query("SELECT u FROM UserDevices u WHERE u.uid = ?1")
    Page<UserDevices> searchByUid(String keyword,Pageable pageable);
}
