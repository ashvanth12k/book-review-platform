package com.examly.springapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.HttpAuthenticationScheme;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.oas.annotations.EnableOpenApi;

import java.util.Collections;

@Configuration
@EnableOpenApi
public class SwaggerConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.examly.springapp"))
                .paths(PathSelectors.any())
                .build()
                .securitySchemes(Collections.singletonList(bearerScheme()))
                .securityContexts(Collections.singletonList(securityContext()));
    }

    private HttpAuthenticationScheme bearerScheme() {
        return HttpAuthenticationScheme.JWT_BEARER_BUILDER
                .name(SCHEME_NAME)
                .build();
    }

    private SecurityContext securityContext() {
        AuthorizationScope[] scopes = {
                new AuthorizationScope("global", "accessEverything")
        };
        return SecurityContext.builder()
                .securityReferences(Collections.singletonList(
                        new SecurityReference(SCHEME_NAME, scopes)
                ))
                .build();
    }
}
