/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yyc.ignite.operator.example;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.spec.*;
import org.yyc.ignite.operator.api.type.K8sServiceTypeEnum;

import java.util.Arrays;
import java.util.List;

/**
 * example of creating 3 ignite clusters concurrently (mock 3 users)
 */
public class CreateMultipleClusters {
    public static void main(String[] args) {
        IgniteResource resource1 = createIgniteResource("resource1", "ignite", 1, 120, false);
        IgniteResource resource2 = createIgniteResource("resource2", "ignite", 2, 110, true);
        IgniteResource resource3 = createIgniteResource("resource3", "yyc-test", 1, 100, false);
        List<IgniteResource> resources = Arrays.asList(resource1, resource2, resource3);
        
        // resources.parallelStream().forEach(CreateMultipleClusters::createResource);
        resources.parallelStream().forEach(CreateMultipleClusters::deleteResource);
    }
    
    private static void createResource(IgniteResource resource) {
        try (KubernetesClient kubernetesClient = new KubernetesClientBuilder().build()) {
            kubernetesClient.resource(resource).createOrReplace();
        }
    }
    
    private static void deleteResource(IgniteResource resource) {
        try (KubernetesClient kubernetesClient = new KubernetesClientBuilder().build()) {
            kubernetesClient.resource(resource).delete();
        }
    }
    
    @NotNull
    private static IgniteResource createIgniteResource(String name, String namespace,
                                                       int replica, int relationDataRegionSize,
                                                       boolean enablePvc) {
        IgniteResource igniteResource = new IgniteResource();
        igniteResource.setMetadata(new ObjectMetaBuilder()
                .withNamespace(namespace)
                .withName(name)
                .build());
        IgniteSpec spec = new IgniteSpec();
        spec.setReplicas(replica);
        
        spec.setIgniteNodeSpec(createNodeSpec());
        spec.setK8sServiceSpec(createK8sServiceSpec());
        spec.setPersistenceSpec(createPersistenceSpec(enablePvc));
        spec.setIgniteConfigMapSpec(createIgniteConfigMapSpec(relationDataRegionSize));
        
        igniteResource.setSpec(spec);
        return igniteResource;
    }
    
    @NotNull
    private static IgniteNodeSpec createNodeSpec() {
        IgniteNodeSpec igniteNodeSpec = new IgniteNodeSpec();
        igniteNodeSpec.setIgniteImage("gridgain/community");
        igniteNodeSpec.setIgniteVersion("8.8.42-openjdk17");
        igniteNodeSpec.setIgniteOptionalLibs("ignite-kubernetes,ignite-rest-http");
        igniteNodeSpec.setJvmOpts("-DIGNITE_WAL_MMAP=false -DIGNITE_WAIT_FOR_BACKUPS_ON_SHUTDOWN=true "
                + "-server -Xms1G -Xmx1G -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC "
                + "-XX:+DisableExplicitGC -XX:MetaspaceSize=200M -XX:MinMetaspaceFreeRatio=40 "
                + "-XX:MaxMetaspaceFreeRatio=60");
        igniteNodeSpec.setIgniteNodeCpu("1");
        igniteNodeSpec.setIgniteNodeMemory("2Gi");
        return igniteNodeSpec;
    }
    
    @NotNull
    private static K8sServiceSpec createK8sServiceSpec() {
        K8sServiceSpec k8sServiceSpec = new K8sServiceSpec();
        k8sServiceSpec.setType(K8sServiceTypeEnum.ClusterIP);
        return k8sServiceSpec;
    }
    
    @NotNull
    private static PersistenceSpec createPersistenceSpec(boolean enablePvc) {
        PersistenceSpec persistenceSpec = new PersistenceSpec();
        if (!enablePvc) {
            persistenceSpec.setPersistenceEnabled(false);
            return persistenceSpec;
        }
        persistenceSpec.setPersistenceEnabled(true);
        persistenceSpec.setDataVolumeSpec(
                VolumeSpec.builder()
                        .name("data-vol")
                        .accessModes(List.of("ReadWriteOnce"))
                        .mountPath("/opt/gridgain/work")
                        .storage("2Gi")
                        .build());
        return persistenceSpec;
    }
    
    @NotNull
    private static IgniteConfigMapSpec createIgniteConfigMapSpec(int relationDataRegionSize) {
        IgniteConfigMapSpec configMapSpec = new IgniteConfigMapSpec();
        configMapSpec.setDefaultDataRegionSize("110 * 1024 * 1024");
        configMapSpec.setRelationalDataRegionSize(relationDataRegionSize + " * 1024 * 1024");
        return configMapSpec;
    }
}
