package com.ulms.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
    Optional<UserAuth> findByProviderAndProviderId(String provider, String providerId);
    Optional<UserAuth> findByUser_UserIdAndProvider(Long userId, String provider);
}
