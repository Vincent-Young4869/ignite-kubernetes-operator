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

package org.example;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.customresource.IgniteSpec;

/**
 * client code for ../basic.yaml.
 */
public class Basic {
    public static void main(String[] args) {
        IgniteResource igniteResource = new IgniteResource();
        igniteResource.setMetadata(new ObjectMetaBuilder()
                .withNamespace("ignite")
                .withName("test")
                .build());
        IgniteSpec spec = new IgniteSpec();
        spec.setIgniteImage("gridgain/community");
        spec.setIgniteVersion("8.8.42-openjdk17");
        spec.setIgniteOptionalLibs("ignite-kubernetes,ignite-rest-http");
        spec.setJvmOpts("-DIGNITE_WAL_MMAP=false -DIGNITE_WAIT_FOR_BACKUPS_ON_SHUTDOWN=true "
                + "-server -Xms1G -Xmx1G -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC "
                + "-XX:+DisableExplicitGC -XX:MetaspaceSize=200M -XX:MinMetaspaceFreeRatio=40 "
                + "-XX:MaxMetaspaceFreeRatio=60");
        
        try (KubernetesClient kubernetesClient = new KubernetesClientBuilder().build()) {
            kubernetesClient.resource(igniteResource).createOrReplace();
        }
    }
}
