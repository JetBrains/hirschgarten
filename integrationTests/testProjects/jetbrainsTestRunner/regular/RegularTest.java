import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RegularTest {
  @Test
  public void regularFail() {
    int actual = 2 + 3;
    assertEquals(4, actual);
  }

  @Test
  public void regularPass() {
    int actual = 2 + 2;
    assertEquals(4, actual);
  }
}
