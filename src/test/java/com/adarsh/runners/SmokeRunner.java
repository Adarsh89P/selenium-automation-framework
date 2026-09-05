package com.adarsh.runners;

import io.cucumber.testng.CucumberOptions;

/**
 * The gate that runs on every pull request: the shortest set of scenarios that proves the
 * application is worth testing further.
 *
 * <p>The tag expression is overridable with {@code -Dcucumber.filter.tags}, which is how the
 * nightly matrix reuses this runner for other slices.
 */
@CucumberOptions(tags = "@smoke and not @wip")
public class SmokeRunner extends BaseCucumberRunner {
    // Configuration only: everything else is inherited from BaseCucumberRunner.
}
