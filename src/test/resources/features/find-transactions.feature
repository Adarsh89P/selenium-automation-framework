@transactions
Feature: Finding past transactions
  As a ParaBank customer
  I want to search my account history
  So that I can check a payment went where I expected

  Background:
    Given a logged-in customer

  @regression
  Scenario: A customer finds transactions by amount
    When the customer transfers "25.00" between their own accounts
    And the customer searches the debited account for transactions of "25.00"
    Then matching transactions are listed

  @regression
  Scenario: A search with no criteria is refused
    When the customer searches for transactions without entering any criteria
    Then a validation message explains what is missing
