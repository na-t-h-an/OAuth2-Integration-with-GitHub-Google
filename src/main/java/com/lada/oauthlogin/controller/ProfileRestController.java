package com.lada.oauthlogin.controller;

import com.lada.oauthlogin.model.User;
import com.lada.oauthlogin.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ProfileRestController {

    private final UserRepository userRepository;

    public ProfileRestController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile-data")
    public Map<String, Object> me(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return Map.of("error", "Not authenticated");
        }

        Object principal = auth.getPrincipal();
        String email = null;
        String name = null;
        String picture = null;
        String provider = "unknown";

        Map<String, Object> response = new HashMap<>();

        if (principal instanceof OAuth2User oauthUser) {
            email = oauthUser.getAttribute("email");
            name = oauthUser.getAttribute("name");
            picture = oauthUser.getAttribute("picture");

            // --- Fallbacks for GitHub ---
            if (email == null) email = oauthUser.getAttribute("login");
            if (picture == null) picture = oauthUser.getAttribute("avatar_url");
            if (name == null) name = oauthUser.getAttribute("login");

            // --- Determine provider (from attribute or fallback detection) ---
            provider = oauthUser.getAttribute("provider");
            if (provider == null) {
                if (oauthUser.getAttribute("login") != null && oauthUser.getAttribute("avatar_url") != null) {
                    provider = "github";
                } else if (oauthUser.getAttribute("email") != null && oauthUser.getAttribute("picture") != null) {
                    provider = "google";
                }
            }

            response.put("provider", provider);
        }

        if (email == null) {
            return Map.of("error", "Email missing in authentication principal");
        }

        email = email.trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        // --- Automatically create user if not existing ---
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setDisplayName(name);
            user.setAvatarUrl(picture);
            user.setBio("");
            userRepository.save(user);
        }

        response.put("email", user.getEmail());
        response.put("displayName", user.getDisplayName());
        response.put("avatarUrl", user.getAvatarUrl());
        response.put("bio", user.getBio());

        return response;
    }

    @PostMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates, Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = extractEmail(auth.getPrincipal());
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email not found"));
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        String displayName = updates.get("displayName");
        String bio = updates.get("bio");
        boolean dirty = false;

        if (displayName != null && !displayName.trim().isEmpty() &&
                !displayName.trim().equals(user.getDisplayName())) {
            user.setDisplayName(displayName.trim());
            dirty = true;
        }

        if (bio != null && !bio.equals(user.getBio())) {
            user.setBio(bio.trim());
            dirty = true;
        }

        if (dirty) {
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of(
                "status", "updated",
                "displayName", user.getDisplayName(),
                "bio", user.getBio()
        ));
    }

    private String extractEmail(Object principal) {
        if (principal instanceof Map<?, ?> map) {
            return (String) map.get("email");
        } else if (principal instanceof OAuth2User oauthUser) {
            return oauthUser.getAttribute("email");
        }
        return null;
    }
}
