package com.microservice.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MicroserviceReservasApplication {

	public static void main(String[] args) {
		// 1. SETEAMOS EL RELOJ ANTES DE QUE ARRANQUE NADA
		// Usamos GMT-03:00 para que PostgreSQL lo entienda sin diccionarios
		TimeZone.setDefault(TimeZone.getTimeZone("GMT-03:00"));

		// 2. AHORA SÍ, LEVANTAMOS SPRING BOOT
		SpringApplication.run(MicroserviceReservasApplication.class, args);
	}
}