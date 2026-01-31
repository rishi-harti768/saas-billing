package org.gb.billing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI billingOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("SaaS Billing Engine API")
                        .description("Enterprise SaaS Billing API with Multi-tenancy and Subscription Management")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}
