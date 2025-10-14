package com.lada.oauthlogin.service;

import com.lada.oauthlogin.model.AuthProvider;
import com.lada.oauthlogin.model.User;
import com.lada.oauthlogin.repository.AuthProviderRepository;
import com.lada.oauthlogin.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;

    public CustomOAuth2UserService(UserRepository userRepository,
                                   AuthProviderRepository authProviderRepository) {
        this.userRepository = userRepository;
        this.authProviderRepository = authProviderRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User delegate = super.loadUser(userRequest);
        Map<String, Object> attrs = new HashMap<>(delegate.getAttributes());

        String regId = userRequest.getClientRegistration().getRegistrationId(); // "google" or "github"
        String idAttr = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        log.info(">>> OAuth2 login via {}", regId);
        log.debug("Raw attributes: {}", attrs);

        String providerUserId = str(attrs.get(idAttr));
        if (providerUserId == null) {
            throw new OAuth2AuthenticationException("Missing provider user ID");
        }

        // Extract profile fields
        String email = null;
        String displayName = null;
        String avatarUrl = null;

        if ("google".equalsIgnoreCase(regId)) {
            email = str(attrs.get("email"));
            displayName = str(attrs.get("name"));
            avatarUrl = str(attrs.get("picture"));

        } else if ("github".equalsIgnoreCase(regId)) {
            String login = str(attrs.get("login")); // GitHub username
            if (login == null) {
                throw new OAuth2AuthenticationException("GitHub login (username) not found");
            }

            displayName = firstNonNull(str(attrs.get("name")), login);
            avatarUrl = str(attrs.get("avatar_url"));

            email = login.toLowerCase();
        }

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Login identifier not available from " + regId);
        }

        email = email.toLowerCase();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Manila"));

        // 1. Load or create user by this "email" or GitHub username
        User user = userRepository.findByEmail(email).orElse(null);
        boolean isNewUser = false;

        if (user == null) {
            log.info("Creating new user: {}", email);
            user = new User();
            user.setEmail(email);
            user.setDisplayName(displayName);
            user.setAvatarUrl(avatarUrl);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            user = userRepository.saveAndFlush(user);
            isNewUser = true;
        }

        // 2. Link provider if not already linked
        AuthProvider.Provider providerEnum;
        try {
            providerEnum = AuthProvider.Provider.valueOf(regId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("Unknown provider: " + regId);
        }

        AuthProvider existing = authProviderRepository
                .findByProviderAndProviderUserId(providerEnum, providerUserId)
                .orElse(null);

        if (existing == null) {
            log.info("Linking provider {} to user {}", regId, email);
            AuthProvider ap = new AuthProvider();
            ap.setProvider(providerEnum);
            ap.setProviderUserId(providerUserId);
            ap.setProviderEmail(email);
            ap.setUser(user);
            authProviderRepository.saveAndFlush(ap);
        }

        // 3. Update missing fields if needed
        boolean dirty = false;

        if ((user.getDisplayName() == null || user.getDisplayName().isBlank()) && displayName != null) {
            user.setDisplayName(displayName);
            dirty = true;
        }

        if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
            dirty = true;
        }

        if (dirty && !isNewUser) {
            user.setUpdatedAt(now);
            userRepository.saveAndFlush(user);
        }

        // 4. Add "email" and "provider" to attributes for frontend access
        attrs.put("email", email);
        attrs.put("provider", regId);

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attrs,
                idAttr
        );
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }
}
