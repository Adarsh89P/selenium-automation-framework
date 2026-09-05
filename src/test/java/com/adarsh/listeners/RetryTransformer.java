package com.adarsh.listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

/**
 * Attaches {@link RetryAnalyzer} to every test method in the suite.
 *
 * <p>The alternative - writing {@code @Test(retryAnalyzer = RetryAnalyzer.class)} on each test - is
 * a rule enforced by memory. The first scenario somebody adds without it silently loses retry
 * protection and, more importantly, loses the flaky-marking that goes with it, so the suite starts
 * reporting a genuine intermittent failure as a plain red. Doing it here means the guarantee holds
 * for tests that do not exist yet.
 *
 * <p>Registered through {@code META-INF/services/org.testng.ITestNGListener}, so a newly added
 * suite XML inherits it without anyone remembering to add a {@code <listeners>} block.
 *
 * <p>An explicit analyzer on a test is left alone: this sets a default, it does not overrule a
 * deliberate choice.
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(
            ITestAnnotation annotation,
            Class testClass,
            Constructor testConstructor,
            Method testMethod) {

        if (annotation.getRetryAnalyzerClass() == null
                || annotation.getRetryAnalyzerClass() == org.testng.internal.annotations.DisabledRetryAnalyzer.class) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
