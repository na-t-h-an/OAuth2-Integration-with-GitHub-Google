package com.lada.oauthlogin.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        // Invalidate HTTP session
        request.getSession().invalidate();

        // Let Spring Security handle logoutss
        request.logout();

        // Redirect to homepage or login screen
        return "redirect:/";
    }
}