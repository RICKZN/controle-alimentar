package com.sistema.controle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ControleApplication {

	public static void main(String[] args) {
		String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
		
		System.out.println("🔍 Verificando configuração de banco...");

		if (dbUrl != null && dbUrl.contains("@")) {
			try {
				// Formato esperado: postgres://user:pass@host:port/db
				String cleanUrl = dbUrl.replace("jdbc:", "");
				int doubleSlash = cleanUrl.indexOf("//");
				int atSymbol = cleanUrl.indexOf("@");
				
				if (doubleSlash != -1 && atSymbol != -1) {
					String userPass = cleanUrl.substring(doubleSlash + 2, atSymbol);
					String hostPart = cleanUrl.substring(atSymbol + 1);
					
					if (userPass.contains(":")) {
						String user = userPass.split(":")[0];
						String pass = userPass.split(":")[1];
						
						
						String finalUrl = "jdbc:postgresql://" + hostPart;
						if (!finalUrl.contains("sslmode")) {
							finalUrl += (finalUrl.contains("?") ? "&" : "?") + "sslmode=require";
						}
						
						System.setProperty("spring.datasource.url", finalUrl);
						System.setProperty("spring.datasource.username", user);
						System.setProperty("spring.datasource.password", pass);
						System.out.println("🚀 Conectando ao Banco de Dados de Alta Performance: " + hostPart.split(":")[0]);
					}
				}
			} catch (Exception e) {
				System.out.println("⚠️ Aviso: Erro ao processar URL do banco: " + e.getMessage());
			}
		} else {
			System.out.println("ℹ️ Usando URL direta (sem arroba ou vazia)");
		}

		try {
			SpringApplication.run(ControleApplication.class, args);
			System.out.println("\n==========================================");
			System.out.println("🚀 SISTEMA ONLINE NO RENDER!");
			System.out.println("==========================================");
		} catch (Exception e) {
			System.err.println("❌ ERRO CRÍTICO NA INICIALIZAÇÃO:");
			e.printStackTrace();
		}
	}
}
