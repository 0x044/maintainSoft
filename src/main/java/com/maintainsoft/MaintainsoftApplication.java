package com.maintainsoft;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableEncryptableProperties
@SpringBootApplication
public class MaintainsoftApplication {

	public static void main(String[] args) {
		SpringApplication.run(MaintainsoftApplication.class, args);
	}

}
