package org.yyc.ignite.operator.api.utils;

import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import org.apache.commons.codec.digest.DigestUtils;
import org.yyc.ignite.operator.api.type.K8sMetadataLabelEnum;

import java.util.List;

public class DependentResourceUtils {
    public static <T extends CustomResource<?, ?>> ObjectMetaBuilder newK8sMetadataBuilder(T primary, String component) {
        return new ObjectMetaBuilder()
                .withName(buildDependentResourceName(primary, component))
                .withNamespace(primary.getMetadata().getNamespace())
                .withManagedFields((List<ManagedFieldsEntry>) null)
                .addToLabels(K8sMetadataLabelEnum.COMPONENT.labelName(), component)
                .addToLabels(K8sMetadataLabelEnum.NAME.labelName(), primary.getMetadata().getName())
                .addToLabels(K8sMetadataLabelEnum.MANAGED_BY.labelName(), Constants.OPERATOR_NAME);
    }
    
    public static <T extends CustomResource<?, ?>> String buildDependentResourceName(T primary, String component) {
        return primary.getMetadata().getName() + "-" + component;
    }
    
    public static String sha256Hex(String data) {
        return DigestUtils.sha256Hex(data);
    }
}
