package com.microservice.reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MicroserviceReservasApplication {
	public static void main(String[] args) {
		// 2. Add this line BEFORE SpringApplication.run
		// This forces Java to use the specific timezone string that PostgreSQL expects
		TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));

		SpringApplication.run(MicroserviceReservasApplication.class, args);
	}
}