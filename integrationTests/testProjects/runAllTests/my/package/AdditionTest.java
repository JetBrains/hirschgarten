import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AdditionTest {
    @Test
    public void testAddition() {
        System.out.println("AdditionTest stdout");
        System.err.println("AdditionTest stderr");
        assertEquals(4, 2 + 2);
    }
}
