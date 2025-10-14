package com.lada.oauthlogin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OAuthLoginDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(OAuthLoginDemoApplication.class, args);
		System.out.println("=========================");
		System.out.println("Successfully started!");
		System.out.println("=========================");
	}

}
