package org.yyc.ignite.operator.e2e.tests.service;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.stereotype.Service;

import static org.yyc.ignite.operator.e2e.tests.utils.BuildIgniteResourceUtils.NAMESPACES_FOR_TEST;

@Service
public class TestPreparationService {
    
    @BeforeAll
    public static void setupNamespace() {
        KubernetesClient kubernetesClient = new KubernetesClientBuilder().build();
        for (String namespace : NAMESPACES_FOR_TEST) {
            Namespace ns = new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(namespace) // replace with your desired namespace name
                    .endMetadata()
                    .build();
            kubernetesClient.namespaces().createOrReplace(ns);
        }
    }
}
