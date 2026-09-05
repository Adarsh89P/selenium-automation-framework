package com.adarsh.runners;

import io.cucumber.testng.CucumberOptions;

/**
 * Scenarios that exercise Selenium 4 specific capabilities (relative locators, BiDi, new-window
 * handling, shadow DOM). Kept in its own suite because these need a Chromium-family browser and
 * are not part of the release gate.
 */
@CucumberOptions(tags = "@advanced and not @wip")
public class AdvancedRunner extends BaseCucumberRunner {
    // Configuration only: everything else is inherited from BaseCucumberRunner.
}
