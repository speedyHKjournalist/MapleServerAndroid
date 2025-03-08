package tools;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RandomizerTest {

    @Test
    void randShouldIncludeEntireRange() {
        Map<Integer, Integer> rands = new HashMap<>();

        final int rounds = 100_000;
        for (int i = 0; i < rounds; i++) {
            int randomValue = Randomizer.rand(-5, 5);
            rands.compute(randomValue, (k, v) -> v == null ? 0 : v + 1);
        }

        assertFalse(rands.containsKey(-6));
        assertTrue(rands.containsKey(-5));
        assertTrue(rands.containsKey(-4));
        assertTrue(rands.containsKey(-3));
        assertTrue(rands.containsKey(-2));
        assertTrue(rands.containsKey(-1));
        assertTrue(rands.containsKey(0));
        assertTrue(rands.containsKey(1));
        assertTrue(rands.containsKey(2));
        assertTrue(rands.containsKey(3));
        assertTrue(rands.containsKey(4));
        assertTrue(rands.containsKey(5));
        assertFalse(rands.containsKey(6));
    }
}