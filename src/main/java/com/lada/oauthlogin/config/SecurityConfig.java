package com.lada.oauthlogin.config;

import com.lada.oauthlogin.service.CustomOAuth2UserService;
import com.lada.oauthlogin.service.DelegatingOidcUserService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final DelegatingOidcUserService delegatingOidcUserService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, DelegatingOidcUserService delegatingOidcUserService) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.delegatingOidcUserService = delegatingOidcUserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Public vs protected routes
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html",
                                "/auth/google/verify",          // GIS posts the ID token here
                                "/css/**", "/js/**", "/images/**",
                                "/h2-console/**"               // dev only
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // Dev conveniences
                .headers(h -> h.frameOptions(f -> f.disable()))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/auth/google/verify", "/h2-console/**") // allow POST from your page
                        .disable()
                )

                // GitHub via Spring OAuth2 (keep working)
                .oauth2Login(oauth -> oauth
                        .loginPage("/")                              
                        .defaultSuccessUrl("/profile.html", true)
                        .userInfoEndpoint(u -> u
                                .userService(customOAuth2UserService)
                                .oidcUserService(delegatingOidcUserService)  
                        )
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/")
                )

                .sessionManagement(sm -> sm.sessionFixation(sf -> sf.migrateSession()))
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
