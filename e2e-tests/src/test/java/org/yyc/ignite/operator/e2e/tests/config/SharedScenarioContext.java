package org.yyc.ignite.operator.e2e.tests.config;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.yyc.ignite.operator.api.customresource.IgniteResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.yyc.ignite.operator.e2e.tests.utils.BuildIgniteResourceUtils.buildIgniteResource;

@Getter
@Component
public class SharedScenarioContext {
    private final List<IgniteResource> testResources;
    
    private static enum ScenarioDataTableColumns {
        RESOURCE_NAME("resourceName"),
        NAMESPACE("namespace"),
        REPLICA("replica"),
        DATA_REGION_MEMORY("memory"),
        ENABLE_PERSISTENCE("enablePersistence");
        private final String columnName;
        ScenarioDataTableColumns(String columnName) {
            this.columnName = columnName;
        }
    }
    
    public SharedScenarioContext() {
        testResources = new ArrayList<>();
    }
    
    public void addTestResources(List<Map<String, String>> configurations) {
        for (Map<String, String> config : configurations) {
            IgniteResource resource = buildIgniteResource(config.get(ScenarioDataTableColumns.RESOURCE_NAME.columnName),
                    config.get(ScenarioDataTableColumns.NAMESPACE.columnName),
                    Integer.parseInt(config.get(ScenarioDataTableColumns.REPLICA.columnName)),
                    config.get(ScenarioDataTableColumns.DATA_REGION_MEMORY.columnName),
                    Boolean.parseBoolean(config.get(ScenarioDataTableColumns.ENABLE_PERSISTENCE.columnName)));
            testResources.add(resource);
        }
    }
}
