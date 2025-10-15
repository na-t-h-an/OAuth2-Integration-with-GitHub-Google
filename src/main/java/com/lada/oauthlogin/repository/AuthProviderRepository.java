package com.lada.oauthlogin.repository;

import com.lada.oauthlogin.model.AuthProvider;
import com.lada.oauthlogin.model.AuthProvider.Provider;
import com.lada.oauthlogin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthProviderRepository extends JpaRepository<AuthProvider, Long> {
    Optional<AuthProvider> findByProviderAndProviderUserId(Provider provider, String providerUserId);
    Optional<AuthProvider> findByProviderAndUser(AuthProvider.Provider provider, User user);
}
