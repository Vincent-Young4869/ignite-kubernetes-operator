package org.yyc.ignite.operator.e2e.tests.config;

import io.cucumber.spring.CucumberContextConfiguration;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

// @CucumberContextConfiguration
// @SpringBootTest
@SpringBootConfiguration
@ComponentScan("org.yyc.ignite.operator.e2e.tests")
@TestPropertySource(locations = "classpath:test.properties")
public class SpringConfig {
    
    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
