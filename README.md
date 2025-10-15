# OAuth2 Integration with Google & GitHub (Spring Boot)

This project demonstrates how to implement OAuth2 login using **Google** and **GitHub** in a Spring Boot 3.x application using Spring Security and RESTful endpoints (no Thymeleaf).

---

## Architecture Overview

**Technologies Used:**
- Backend: Spring Boot 3.5+, Java 21
- OAuth2 Providers: Google, GitHub
- Security: Spring Security OAuth2 Client
- Database: H2 (in-memory, development mode)
- Frontend: Static HTML/CSS/JS (`/static` folder)
```
+--------------------+
|    User Browser    |
+--------------------+
          |
          v
+------------------------------+
| Static Frontend (HTML/CSS/JS)|
+------------------------------+
          |
          v
+-----------------------------------------+
| Spring Boot Backend (OAuth Login Demo)  |
|                                         |
|  +-----------------------------------+  |
|  | Spring Security OAuth2 Client     |  |
|  |  - Handles login redirects        |  |
|  |  - Fetches access tokens          |  |
|  |  - Loads user info (OIDC/OAuth2)  |  |
|  +----------------+------------------+  |
|                       |                 |
|                       v                 |
|  +-----------------------------------+  |
|  | CustomOAuth2UserService           |  |
|  |  - Maps user info to DB record    |  |
|  |  - Links AuthProvider entries     |  |
|  +----------------+------------------+  |
|                       |                 |
|                       v                 |
|  +-----------------------------------+  |
|  | DelegatingOidcUserService         |  |
|  |  - Wraps Google OIDC login        |  |
|  |  - Returns compliant OidcUser     |  |
|  +-----------------------------------+  |
+-----------------------------------------+
          |
          v
+------------------------------+
|     H2 In-Memory Database    |
|  ┌────────────────────────┐  |
|  │      users table       │  |
|  │ ─ id, email, name...   │  |
|  └────────────────────────┘  |
|  ┌────────────────────────┐  |
|  │  auth_providers table  │  |
|  │ ─ provider, sub, FK... │  |
|  └────────────────────────┘  |
+------------------------------+
```

**Authentication Flow:**
1. User clicks "Login with Google" or "Login with GitHub" from `index.html`
2. Redirects to the respective OAuth2 provider
3. After successful login, user lands on `/profile.html`
4. `/profile.html` uses JavaScript fetch to load `/profile-data`
5. User can edit profile and save changes via `/profile` (POST)

---

## OAuth2 Setup Instructions

You must register your application with **Google** and **GitHub** to obtain credentials.

### Google OAuth2 Configuration

1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Create an **OAuth2 Client ID** (Web Application)
3. Add these URIs:

**Authorized JavaScript Origins**
```
http://localhost:8080
```
```
http://127.0.0.1:8080
```

**Authorized Redirect URIs**
```
http://localhost:8080/login/oauth2/code/google
```
```
http://127.0.0.1:8080/login/oauth2/code/google
```

4. Copy your **Client ID** and **Client Secret** into `application.properties`

---

### GitHub OAuth App Configuration

1. Visit: [GitHub Developer Settings](https://github.com/settings/developers)
2. Create a **New OAuth App** with the following:

**Homepage URL**
```
http://localhost:8080
```

**Authorization Callback URL**
```
http://localhost:8080/login/oauth2/code/github
```

3. Copy your **Client ID** and **Client Secret** into `application.properties`

---

## Configuration: application.properties

Your config file should be placed at:

```
src/main/resources/application.properties
```

Paste and update with your own credentials:

```
spring.application.name=OAuth Login Demo
server.port=8080

# H2 Database (dev mode)
spring.datasource.url=jdbc:h2:mem:oauthdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true
spring.jpa.open-in-view=true

# Logging
logging.level.org.springframework.security=INFO
logging.level.org.hibernate.SQL=INFO

# --- Google OAuth2 ---
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=openid,profile,email
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration.google.client-name=Google
spring.security.oauth2.client.registration.google.provider=google
spring.security.oauth2.client.provider.google.user-name-attribute=sub
spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo

# --- GitHub OAuth2 ---
spring.security.oauth2.client.registration.github.client-id=YOUR_GITHUB_CLIENT_ID
spring.security.oauth2.client.registration.github.client-secret=YOUR_GITHUB_CLIENT_SECRET
spring.security.oauth2.client.registration.github.scope=read:user,user:email
spring.security.oauth2.client.registration.github.redirect-uri=http://localhost:8080/login/oauth2/code/github
spring.security.oauth2.client.registration.github.client-name=GitHub
```

---

## Additional Notes

- Static HTML is located in `src/main/resources/static/`
- H2 database resets on each application restart (for development only)
- `/profile-data` exposes user info via JSON
- `/profile` accepts POST requests to update name and bio
- CSRF is automatically handled by Spring Security

---

Lada, Nathan Xander  
IT342 - Systems Integration and Architecture 1 - G01
