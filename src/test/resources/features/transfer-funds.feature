@transfer
Feature: Transferring funds between accounts
  As a ParaBank customer
  I want to move money between my own accounts
  So that I can cover payments from the right place

  Background:
    Given a logged-in customer

  @smoke @regression
  Scenario: A customer moves money between two of their own accounts
    When the customer transfers "25.00" between their own accounts
    Then the transfer is confirmed
    And the confirmation shows the transferred amount and both accounts

  @regression
  Scenario Outline: Transfers are confirmed across a range of amounts
    When the customer transfers "<amount>" between their own accounts
    Then the transfer is confirmed

    Examples: boundary amounts
      | case              | amount |
      | smallest unit     | 0.01   |
      | typical amount    | 50.00  |
      | fractional cents  | 1.99   |
