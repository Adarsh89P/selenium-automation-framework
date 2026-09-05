@loans
Feature: Applying for a loan
  As a ParaBank customer
  I want to apply for a loan against an existing account
  So that I can borrow without visiting a branch

  Background:
    Given a logged-in customer

  @regression
  Scenario: A modest loan application is decided
    When the customer applies for a loan of "1000" with a down payment of "100"
    Then the loan application receives a decision
    And an approved loan opens a new account

  @regression
  Scenario: A loan far beyond the customer's means is refused
    When the customer applies for a loan of "100000000" with a down payment of "1"
    Then the loan application is denied
