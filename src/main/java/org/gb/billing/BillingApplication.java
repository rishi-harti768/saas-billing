package org.gb.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@SpringBootApplication
public class BillingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillingApplication.class, args);
	}

	/**
	 * Provide a dummy JavaMailSender when email is disabled.
	 * This prevents ApplicationContext loading failures in tests.
	 */
	@Bean
	@ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "false", matchIfMissing = true)
	public JavaMailSender dummyMailSender() {
		return new JavaMailSenderImpl();
	}
}
