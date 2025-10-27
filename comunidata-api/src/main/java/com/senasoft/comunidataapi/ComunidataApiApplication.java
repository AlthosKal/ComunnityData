package com.senasoft.comunidataapi;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ComunidataApiApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        // Configuración de la base de datos MongoDB
        System.setProperty("DATABASE_URL", dotenv.get("DATABASE_URL"));

        // Conexión al modelo de OpenAI
        System.setProperty("OPENAI_KEY", dotenv.get("OPENAI_KEY"));
        System.setProperty("OPENAI_MODEL", dotenv.get("OPENAI_MODEL"));

        // URLs del frontend
        System.setProperty("WEB_URL", dotenv.get("WEB_URL"));
        System.setProperty("MOBILE_URL", dotenv.get("MOBILE_URL"));

        // Configuración de IBM Watsonx
        System.setProperty("IBM_WATSONX_API_KEY", dotenv.get("USER_IBM_API_KEY"));
        System.setProperty("IBM_WATSONX_PROJECT_ID", dotenv.get("PROJECT_IBM_ID"));

        SpringApplication.run(ComunidataApiApplication.class, args);
    }
}
