# Selenium Framework

This project is a Selenium framework designed for automated testing of web applications. It provides a structured approach to writing and executing tests using Java and Selenium WebDriver.

## Project Structure

```
selenium-framework
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           ├── BaseTest.java
│   │   │           ├── pageobjects
│   │   │           │   └── HomePage.java
│   │   │           └── tests
│   │   │               └── LoginTest.java
│   │   └── resources
│   │       └── config.properties
│   └── test
│       ├── java
│       │   └── com
│       │       └── example
│       │           └── tests
│       │               └── ExampleTest.java
│       └── resources
│           └── test-data.yml
├── pom.xml
└── README.md
```

## Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd selenium-framework
   ```

2. **Install Dependencies**
   Ensure you have Maven installed. Run the following command to install the necessary dependencies:
   ```bash
   mvn install
   ```

3. **Configure WebDriver**
   Update the `src/main/resources/config.properties` file with your WebDriver settings and application URL.

4. **Run Tests**
   You can run the tests using Maven:
   ```bash
   mvn test
   ```

## Usage Guidelines

- Extend the `BaseTest` class for your test classes to inherit common setup and teardown methods.
- Use the `HomePage` class to interact with elements on the home page.
- Create test cases in the `LoginTest` class or other classes in the `tests` package.
- Utilize the `test-data.yml` file for parameterized or data-driven tests.

## Example

Here is a simple example of how to create a test case:

```java
public class SampleTest extends BaseTest {
    @Test
    public void exampleTest() {
        HomePage homePage = new HomePage(driver);
        homePage.performSomeAction();
        // Add assertions here
    }
}
```

## Contributing

Feel free to submit issues or pull requests to improve the framework.