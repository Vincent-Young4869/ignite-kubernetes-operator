package org.yyc.ignite.operator.api.utils;

import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import org.yyc.ignite.operator.api.type.K8sMetadataLabelEnum;

import java.util.List;

public class DependentResourceUtils {
    public static <T extends CustomResource<?, ?>> ObjectMetaBuilder buildMetadataTemplate(T primary, String component) {
        return new ObjectMetaBuilder()
                .withNamespace(primary.getMetadata().getNamespace())
                .withManagedFields((List<ManagedFieldsEntry>) null)
                .addToLabels(K8sMetadataLabelEnum.COMPONENT.labelName(), component)
                .addToLabels(K8sMetadataLabelEnum.NAME.labelName(), primary.getMetadata().getName())
                .withName(buildDependentResourceName(primary, component))
                .addToLabels(K8sMetadataLabelEnum.MANAGED_BY.labelName(), Constants.OPERATOR_NAME);
    }
    
    public static <T extends CustomResource<?, ?>> String buildDependentResourceName(T primary, String component) {
        return primary.getMetadata().getName() + "-" + component;
    }
}
