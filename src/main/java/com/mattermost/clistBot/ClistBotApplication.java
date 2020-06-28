package com.mattermost.clistBot;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@Configuration
@ComponentScan(basePackages = {"com.mattermost.clistBot"})
@EnableScheduling
@Slf4j
@NoArgsConstructor
@RestController
@EnableJpaRepositories(basePackages="com.mattermost.clistBot.domain.infrastructure")
@EnableTransactionManagement
@EntityScan(basePackages="com.mattermost.clistBot.domain")
@SpringBootApplication
@EnableAutoConfiguration
public class ClistBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClistBotApplication.class, args);
	}

	@GetMapping("/")
	public String hello() {
		return "ClistBot is running";
	}
}
