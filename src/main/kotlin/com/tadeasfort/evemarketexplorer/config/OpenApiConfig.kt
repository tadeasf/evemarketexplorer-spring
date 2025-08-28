package com.tadeasfort.evemarketexplorer.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .addServersItem(
                Server()
                    .url("https://api.market.tundragon.space")
                    .description("Production server")
            )
            .info(
                Info()
                    .title("EVE Market Explorer API")
                    .description("REST API for exploring EVE Online market data")
                    .version("1.0.0")
                    .contact(
                        Contact()
                            .name("EVE Market Explorer Team")
                            .email("business@tadeasfort.com")
                    )
                    .license(
                        License()
                            .name("MIT")
                            .url("https://opensource.org/licenses/MIT")
                    )
            )
    }
}