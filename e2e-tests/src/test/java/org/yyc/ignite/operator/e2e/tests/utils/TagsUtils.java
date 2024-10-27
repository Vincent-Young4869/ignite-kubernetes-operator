package org.yyc.ignite.operator.e2e.tests.utils;

import io.cucumber.java.Scenario;

import java.util.Collection;

public class TagsUtils {
    private static final String RESOURCE_NAME_TAG_PREFIX = "@resourceName:";
    public static final String RESOURCE_NAME_NOT_FOUND_MESSAGE = String.format("Tag '%s' need be specified in scenarios", RESOURCE_NAME_TAG_PREFIX);
    
    public static String getResourceNameFromScenario(Scenario scenario) {
        Collection<String> tags = scenario.getSourceTagNames();
        for (String tagName : tags) {
            if (tagName.startsWith(RESOURCE_NAME_TAG_PREFIX)) {
                return tagName.substring(RESOURCE_NAME_TAG_PREFIX.length());
            }
        }
        return null;
    }
    
}
