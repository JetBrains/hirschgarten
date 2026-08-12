import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SimpleTest {
    @Test
    void firstTest() {
        assertEquals(3, 1 + 1);
    }

    @Test
    void secondTest() {
        java.lang.Integer expected = 2;  // JVM classes should resolve correctly
        assertEquals(expected, 1 + 1);
    }
}
