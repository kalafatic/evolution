package eu.kalafatic.evolution.view.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class EvoTableViewerHelperTest {

    @Test
    public void testMoveRowInListUpAndDown() {
        List<String> items = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));

        // Move B up by -1
        boolean movedUp = EvoTableViewerHelper.moveRowInList(items, "B", -1, null);
        Assert.assertTrue(movedUp);
        Assert.assertEquals(Arrays.asList("B", "A", "C", "D"), items);

        // Move B down by +1
        boolean movedDown = EvoTableViewerHelper.moveRowInList(items, "B", 1, null);
        Assert.assertTrue(movedDown);
        Assert.assertEquals(Arrays.asList("A", "B", "C", "D"), items);
    }

    @Test
    public void testMoveRowInListBoundaryGuards() {
        List<String> items = new ArrayList<>(Arrays.asList("A", "B"));

        // Cannot move top element up
        boolean movedTopUp = EvoTableViewerHelper.moveRowInList(items, "A", -1, null);
        Assert.assertFalse(movedTopUp);

        // Cannot move bottom element down
        boolean movedBottomDown = EvoTableViewerHelper.moveRowInList(items, "B", 1, null);
        Assert.assertFalse(movedBottomDown);

        // Element not in list
        boolean movedMissing = EvoTableViewerHelper.moveRowInList(items, "C", 1, null);
        Assert.assertFalse(movedMissing);
    }

    @Test
    public void testMoveRowWithCallback() {
        List<String> items = new ArrayList<>(Arrays.asList("X", "Y", "Z"));
        boolean[] callbackExecuted = { false };

        boolean moved = EvoTableViewerHelper.moveRowInList(items, "Z", -1, list -> {
            callbackExecuted[0] = true;
        });

        Assert.assertTrue(moved);
        Assert.assertTrue(callbackExecuted[0]);
        Assert.assertEquals(Arrays.asList("X", "Z", "Y"), items);
    }
}
