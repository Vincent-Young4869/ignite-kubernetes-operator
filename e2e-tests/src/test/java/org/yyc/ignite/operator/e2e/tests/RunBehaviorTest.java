package org.yyc.ignite.operator.e2e.tests;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {
                "html:build/cucumber-html-report",
                "json:build/cucumber.json"
        },
        features = "src/test/resources/org.yyc.ignite.operator.e2e.tests.features",
        glue = "org.yyc.ignite.operator.e2e.tests.steps"
)
public class RunBehaviorTest {
}

