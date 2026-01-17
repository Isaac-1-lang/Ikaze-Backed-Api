package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EntityScan("com.ecommerce.entity")
@EnableJpaRepositories("com.ecommerce.repository")
public class EcomAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcomAppApplication.class, args);
	}

}
