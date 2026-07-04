package com.maintainsoft;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@EnableEncryptableProperties
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@SpringBootApplication
public class MaintainsoftApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(MaintainsoftApplication.class);
		app.setApplicationStartup(new BufferingApplicationStartup(2048));
		app.run(args);
	}

	@Bean
	AuditorAware<String> auditorProvider(){
		return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
				.filter(Authentication::isAuthenticated)
				.map(Authentication::getName)
				.or(() -> Optional.of("system"));
	}

}
