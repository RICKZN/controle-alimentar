package com.sistema.controle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ControleApplication {
	public static void main(String[] args) {
		// Auto-fix para URL do Render (postgres:// para jdbc:postgresql://)
		String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
		if (dbUrl != null && dbUrl.startsWith("postgres://")) {
			String jdbcUrl = dbUrl.replace("postgres://", "jdbc:postgresql://");
			System.setProperty("spring.datasource.url", jdbcUrl);
		}
		SpringApplication.run(ControleApplication.class, args);
	}
}