@authentication
Feature: Customer authentication
  As a ParaBank customer
  I want my credentials to be checked before I see my money
  So that nobody else can reach my accounts

  Background:
    Given a customer is on the ParaBank login page

  @smoke @regression
  Scenario: A customer with valid credentials reaches their accounts
    When the customer logs in with valid credentials
    Then the accounts overview is displayed
    And the customer is greeted by name

  @regression
  Scenario Outline: Login is refused when the credentials are not usable
    When the customer logs in with username "<username>" and password "<password>"
    Then login is rejected with the message "<message>"

    Examples: rejected credentials
      | case                | username    | password    | message                                          |
      | unknown user        | nosuchuser  | demo        | The username and password could not be verified. |
      | wrong password      | john        | notthepass  | The username and password could not be verified. |
      | both fields empty   |             |             | Please enter a username and password.            |
      | username only       | john        |             | Please enter a username and password.            |
      | password only       |             | demo        | Please enter a username and password.            |

  @regression
  Scenario: Logging out ends the session
    When the customer logs in with valid credentials
    And the customer logs out
    Then the customer is returned to the login page

  # ParaBank sends no Cache-Control: no-store, so pressing back after logout paints a cached
  # copy of the overview. The cached pixels are not the security question - whether the server
  # still answers for that session is - so the scenario re-requests the data.
  @regression
  Scenario: The back button does not resurrect a closed session
    When the customer logs in with valid credentials
    And the customer logs out
    And the customer presses the browser back button
    And the customer tries to reload their accounts from the cached page
    Then the protected page is not shown

  @regression
  Scenario: An unauthenticated visitor cannot deep-link into an account
    When the customer navigates directly to the accounts overview
    Then the protected page is not shown

  # TODO: ParaBank exposes no way to lock an account, so the "locked account" case from the
  # test plan cannot be automated against this application. Covering it needs either an admin
  # endpoint that sets the lock flag or a different application under test.
  @wip
  Scenario: A locked account cannot be used to log in
    When the customer logs in with username "lockeduser" and password "demo"
    Then login is rejected with the message "Your account is locked."
