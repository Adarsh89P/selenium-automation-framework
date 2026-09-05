package com.adarsh.core;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ExecutionTargetTest {

    @Test
    public void mapsSupportedTargetsCaseInsensitively() {
        Assert.assertEquals(ExecutionTarget.from("local"), ExecutionTarget.LOCAL);
        Assert.assertEquals(ExecutionTarget.from(" GRID "), ExecutionTarget.GRID);
        Assert.assertEquals(ExecutionTarget.from("Cloud"), ExecutionTarget.CLOUD);
    }

    @Test
    public void missingTargetDefaultsToLocal() {
        Assert.assertEquals(ExecutionTarget.from(null), ExecutionTarget.LOCAL);
        Assert.assertEquals(ExecutionTarget.from(""), ExecutionTarget.LOCAL);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void rejectsUnsupportedTarget() {
        ExecutionTarget.from("remote");
    }
}
