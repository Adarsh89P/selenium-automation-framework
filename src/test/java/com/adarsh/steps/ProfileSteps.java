package com.adarsh.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.adarsh.core.ScenarioContext;
import com.adarsh.core.ScenarioContext.Key;
import com.adarsh.pages.AccountsOverviewPage;
import com.adarsh.pages.UpdateProfilePage;
import com.adarsh.utils.FakerUtils;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Step;
import org.assertj.core.api.SoftAssertions;

/** Update Contact Info. */
public class ProfileSteps {

    @When("the customer updates their phone number")
    @Step("Change the customer's phone number")
    public void updatePhoneNumber() {
        var page = new AccountsOverviewPage().goToUpdateProfile();
        ScenarioContext.put(Key.PROFILE_BEFORE, page.currentProfile());

        var newPhoneNumber = FakerUtils.numericPhone();
        ScenarioContext.put(Key.NEW_CUSTOMER, newPhoneNumber);
        page.changePhoneNumber(newPhoneNumber).submit();
    }

    @When("the customer clears every required profile field and submits")
    @Step("Submit the profile form with all required fields blank")
    public void clearRequiredFieldsAndSubmit() {
        new AccountsOverviewPage().goToUpdateProfile().clearRequiredFields().submit();
    }

    @Then("the profile update is confirmed")
    public void profileUpdateIsConfirmed() {
        assertThat(new UpdateProfilePage().isProfileUpdated())
                .as("the Profile Updated panel should be shown")
                .isTrue();
    }

    @Then("every required field reports a validation message")
    public void everyRequiredFieldReportsAValidationMessage() {
        var page = new UpdateProfilePage();

        // Soft assertions are the right tool here: the point of the scenario is that all six
        // messages appear, so failing on the first one would hide the other five.
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(page.firstNameError()).as("first name validation").isPresent();
            softly.assertThat(page.lastNameError()).as("last name validation").isPresent();
            softly.assertThat(page.streetError()).as("address validation").isPresent();
            softly.assertThat(page.cityError()).as("city validation").isPresent();
            softly.assertThat(page.stateError()).as("state validation").isPresent();
            softly.assertThat(page.zipCodeError()).as("zip code validation").isPresent();
        });
    }

    @Then("the profile is not updated")
    public void profileIsNotUpdated() {
        assertThat(new UpdateProfilePage().validationMessages())
                .as("a rejected profile update should surface at least one validation message")
                .isNotEmpty();
    }
}
