package com.adarsh.runners;

import io.cucumber.testng.CucumberOptions;

/** The full nightly suite: everything except work in progress. */
@CucumberOptions(tags = "@regression and not @wip")
public class RegressionRunner extends BaseCucumberRunner {
    // Configuration only: everything else is inherited from BaseCucumberRunner.
}
