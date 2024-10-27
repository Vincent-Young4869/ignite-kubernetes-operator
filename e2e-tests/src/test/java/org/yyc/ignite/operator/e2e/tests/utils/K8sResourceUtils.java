package org.yyc.ignite.operator.e2e.tests.utils;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;

public class K8sResourceUtils {
    
    public static ConfigMap getConfigMapResource(KubernetesClient kubernetesClient, String name, String namespace) {
        return kubernetesClient.configMaps().inNamespace(namespace).withName(name).get();
    }
    
    public static StatefulSet getStatefulSetResource(KubernetesClient kubernetesClient, String name, String namespace) {
        return kubernetesClient.apps().statefulSets().inNamespace(namespace).withName(name).get();
    }
}
