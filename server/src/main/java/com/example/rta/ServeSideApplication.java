package com.example.rta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ServeSideApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServeSideApplication.class, args);
	}

}
