package org.yyc.ignite.operator.api.spec;

import lombok.Data;

@Data
public class IgniteConfigMapSpec {
    private String defaultDataRegionSize = "50 * 1024 * 1024";
    private String relationalDataRegionSize = "128 * 1024 * 1024";
    
    private String configXmlOverride = null;
}
