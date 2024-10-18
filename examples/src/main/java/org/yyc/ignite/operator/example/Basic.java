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
import org.yyc.ignite.operator.api.spec.*;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.spec.IgniteSpec;
import org.yyc.ignite.operator.api.type.K8sServiceTypeEnum;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * example of creating an ignite cluster using k8s api (with operator running)
 */
public class Basic {
    public static void main(String[] args) {
        IgniteResource igniteResource = new IgniteResource();
        igniteResource.setMetadata(new ObjectMetaBuilder()
                .withNamespace("ignite")
                .withName("test")
                .build());
        IgniteSpec spec = new IgniteSpec();
        spec.setReplicas(1);
        
        spec.setIgniteNodeSpec(createNodeSpec());
        spec.setK8sServiceSpec(createK8sServiceSpec());
        spec.setPersistenceSpec(createPersistenceSpec());
        spec.setIgniteConfigMapSpec(createIgniteConfigMapSpec());
        
        igniteResource.setSpec(spec);
        try (KubernetesClient kubernetesClient = new KubernetesClientBuilder().build()) {
            kubernetesClient.resource(igniteResource).createOrReplace();
        }
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
        igniteNodeSpec.setIgniteNodeMemory("3Gi");
        return igniteNodeSpec;
    }
    
    @NotNull
    private static K8sServiceSpec createK8sServiceSpec() {
        K8sServiceSpec k8sServiceSpec = new K8sServiceSpec();
        k8sServiceSpec.setType(K8sServiceTypeEnum.ClusterIP);
        return k8sServiceSpec;
    }
    
    @NotNull
    private static PersistenceSpec createPersistenceSpec() {
        PersistenceSpec persistenceSpec = new PersistenceSpec();
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
    private static IgniteConfigMapSpec createIgniteConfigMapSpec() {
        IgniteConfigMapSpec configMapSpec = new IgniteConfigMapSpec();
        configMapSpec.setDefaultDataRegionSize("110 * 1024 * 1024");
        configMapSpec.setRelationalDataRegionSize("120 * 1024 * 1024");
        return configMapSpec;
    }
}
