package com.sistema.controle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ControleApplication {
	public static void main(String[] args) {
		// Auto-fix para URL do Render (converte usuario:senha@host para o formato que o Java aceita)
		String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
		if (dbUrl != null && dbUrl.contains("@")) {
			try {
				// Remove o "jdbc:" se existir para facilitar o parse
				String cleanUrl = dbUrl.replace("jdbc:", "");
				String userPass = cleanUrl.substring(cleanUrl.indexOf("//") + 2, cleanUrl.indexOf("@"));
				String hostPart = cleanUrl.substring(cleanUrl.indexOf("@") + 1);
				
				String user = userPass.split(":")[0];
				String pass = userPass.split(":")[1];
				
				System.setProperty("spring.datasource.url", "jdbc:postgresql://" + hostPart);
				System.setProperty("spring.datasource.username", user);
				System.setProperty("spring.datasource.password", pass);
				
				System.out.println("✅ Database URL auto-configurada com sucesso!");
			} catch (Exception e) {
				System.out.println("⚠️ Falha ao limpar URL: " + e.getMessage());
			}
		}

		SpringApplication.run(ControleApplication.class, args);
	}
}