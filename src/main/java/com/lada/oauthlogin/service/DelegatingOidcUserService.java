package com.lada.oauthlogin.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DelegatingOidcUserService extends OidcUserService {

    private final CustomOAuth2UserService customOAuth2UserService;

    public DelegatingOidcUserService(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OAuth2User oauthUser = customOAuth2UserService.loadUser(userRequest);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        String nameAttrKey = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOidcUser(
                authorities,
                userRequest.getIdToken(),
                new OidcUserInfo(oauthUser.getAttributes()),
                nameAttrKey
        );
    }
}
