@billpay
Feature: Paying bills
  As a ParaBank customer
  I want to pay a third party from my account
  So that I can settle bills online

  Background:
    Given a logged-in customer

  @smoke @regression
  Scenario: A customer pays a bill to a new payee
    When the customer pays a bill of "30.00" to a new payee
    Then the bill payment is confirmed
    And the confirmation names the payee and the amount

  @regression
  Scenario: A payment is refused when the account confirmation does not match
    When the customer pays a bill with a mismatched account confirmation
    Then the bill payment is rejected
