package org.yyc.ignite.operator.utils;

import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;

import java.util.List;

public class DependentResourceUtils {
    public static <T extends CustomResource<?,?>> ObjectMetaBuilder fromPrimary(T primary, String component) {
        return  new ObjectMetaBuilder()
                .withNamespace(primary.getMetadata().getNamespace())
                .withManagedFields((List<ManagedFieldsEntry>)null)
                .addToLabels("component", component)
                .addToLabels("name", primary.getMetadata().getName())
                .withName(buildDependentResourceName(primary, component))
                .addToLabels("ManagedBy", Constants.OPERATOR_NAME);
    }
    
    public static <T extends CustomResource<?,?>> String buildDependentResourceName(T primary, String component) {
        return primary.getMetadata().getName() + "-" + component;
    }
}
