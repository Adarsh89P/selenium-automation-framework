@accounts
Feature: Account overview and opening new accounts
  As a ParaBank customer
  I want to see what I hold and open further accounts
  So that I can organise my money

  Background:
    Given a logged-in customer

  @smoke @regression
  Scenario: The overview lists the customer's accounts with balances
    Then every listed account shows a balance

  @regression
  Scenario Outline: A customer can open a further account of either type
    When the customer opens a new <type> account
    Then the new account is created with an account number
    And the new account appears in the accounts overview

    Examples: account types
      | type     |
      | CHECKING |
      | SAVINGS  |
