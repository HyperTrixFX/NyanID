package moe.takanashihoshino.nyaniduserserver.repository;

/*
 * @author KoseiRin_
 * awa
 */

import moe.takanashihoshino.nyaniduserserver.entity.UserOAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface UserOAuthRepository extends JpaRepository<UserOAuth, Integer>, Serializable {
}
