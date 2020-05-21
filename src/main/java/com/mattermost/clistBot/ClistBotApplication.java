package com.mattermost.clistBot;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;


@Configuration
@ComponentScan(basePackages = {"com.mattermost.clistBot"})
@EnableScheduling
@Slf4j
@NoArgsConstructor
@SpringBootApplication
public class ClistBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClistBotApplication.class, args);
	}

}
