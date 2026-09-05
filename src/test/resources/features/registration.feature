@registration
Feature: New customer registration
  As a prospective ParaBank customer
  I want to open online access for myself
  So that I can bank without visiting a branch

  Background:
    Given a customer is on the ParaBank login page

  @smoke @regression
  Scenario: A new customer can register and is signed in immediately
    When a new customer registers with valid details
    Then the registration succeeds and the customer is signed in

  @regression
  Scenario: Registration is refused when the password confirmation does not match
    When a new customer registers with a mismatched password confirmation
    Then the registration is rejected with validation messages

  @regression
  Scenario: Registration is refused when nothing is filled in
    When a new customer submits the registration form with every field empty
    Then the registration is rejected with validation messages
