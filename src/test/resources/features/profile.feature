@profile
Feature: Keeping contact details up to date
  As a ParaBank customer
  I want to change the details the bank holds for me
  So that the bank can reach me

  Background:
    Given a logged-in customer

  @regression
  Scenario: A customer changes their phone number
    When the customer updates their phone number
    Then the profile update is confirmed

  @regression
  Scenario: Required details cannot be blanked out
    When the customer clears every required profile field and submits
    Then every required field reports a validation message
    And the profile is not updated
