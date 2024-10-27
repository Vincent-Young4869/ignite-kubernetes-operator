package org.yyc.ignite.operator.e2e.tests.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

@SpringBootConfiguration
@ComponentScan("org.yyc.ignite.operator.e2e.tests")
@TestPropertySource(locations = "classpath:test.properties")
public class SpringConfig {
    
    @Bean
    public KubernetesClient kubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
