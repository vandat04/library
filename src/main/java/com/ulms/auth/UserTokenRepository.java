package com.ulms.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTokenRepository  extends JpaRepository<UserToken, Long>{
    Optional<UserToken> findByUser_UserIdAndTokenType(Long userId, Boolean tokenType);
}
