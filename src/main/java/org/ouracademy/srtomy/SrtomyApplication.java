package org.ouracademy.srtomy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SrtomyApplication {

	public static void main(String[] args) {
		SpringApplication.run(SrtomyApplication.class, args);
	}

}
