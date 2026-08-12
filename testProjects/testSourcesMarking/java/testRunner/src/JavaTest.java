package testRunner;

import org.junit.Test;
import org.junit.Assert;

public class JavaTest {
  @Test
  public void addTest() {
    Assert.assertEquals(4, new TestHelper().sum(2, 2));
  }
}
