package org.yyc.ignite.operator.e2e.tests.utils;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.jetbrains.annotations.NotNull;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.spec.*;
import org.yyc.ignite.operator.api.type.K8sServiceTypeEnum;

import java.util.List;

public class BuildIgniteResourceUtils {
    public static final String DEFAULT_NAMESPACE = "e2e-test";
    public static final String TEST_NAMESPACE2 = "e2e-test-2";
    public static final String TEST_NAMESPACE3 = "e2e-test-3";
    public static final List<String> NAMESPACES_FOR_TEST = List.of(DEFAULT_NAMESPACE, TEST_NAMESPACE2, TEST_NAMESPACE3);
    
    public static IgniteResource buildDefaultIgniteResource(String name) {
        return buildIgniteResource(name, DEFAULT_NAMESPACE, 1, "120 * 1024 * 1024", true);
    }
    
    public static IgniteResource buildIgniteResource(String name, String namespace,
                                                     int replica, String relationDataRegionSize,
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
                + "-server -Xms700m -Xmx700m -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC "
                + "-XX:+DisableExplicitGC -XX:MetaspaceSize=200M -XX:MinMetaspaceFreeRatio=40 "
                + "-XX:MaxMetaspaceFreeRatio=60");
        igniteNodeSpec.setIgniteNodeCpu("1");
        igniteNodeSpec.setIgniteNodeMemory("1Gi");
        return igniteNodeSpec;
    }
    
    @NotNull
    private static K8sServiceSpec createK8sServiceSpec() {
        K8sServiceSpec k8sServiceSpec = new K8sServiceSpec();
        k8sServiceSpec.setType(K8sServiceTypeEnum.ClusterIP);
        return k8sServiceSpec;
    }
    
    @NotNull
    private static PersistenceSpec createPersistenceSpec(boolean enablePersistence) {
        PersistenceSpec persistenceSpec = new PersistenceSpec();
        if (!enablePersistence) {
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
    private static IgniteConfigMapSpec createIgniteConfigMapSpec(String relationDataRegionSize) {
        IgniteConfigMapSpec configMapSpec = new IgniteConfigMapSpec();
        configMapSpec.setDefaultDataRegionSize("110 * 1024 * 1024");
        configMapSpec.setRelationalDataRegionSize(relationDataRegionSize);
        return configMapSpec;
    }
}
