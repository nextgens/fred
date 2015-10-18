/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */
package freenet.node;

import junit.framework.TestCase;

import java.util.Arrays;

public class LocationTest extends TestCase {

    // Maximal acceptable difference to consider two doubles equal.
    private static final double EPSILON = 1e-12;

    // Just some valid non corner case locations.
    private static final double VALID_1 = 0.2;
    private static final double VALID_2 = 0.75;
        
    // Precalculated distances between valid locations.
    private static final double DIST_12 = 0.45;
    private static final double CHANGE_12 = -0.45;
    private static final double CHANGE_21 = 0.45;
    
    // Just some invalid locations.
    private static final double INVALID_1 = -1;
    private static final double INVALID_2 = 1.1;

    public void testIsValid() {
        // Simple cases.
        assertTrue(Location.isValid(VALID_1));
        assertTrue(Location.isValid(VALID_2));
        assertFalse(Location.isValid(INVALID_1));
        assertFalse(Location.isValid(INVALID_2));
        
        // Corner cases.
        assertTrue(Location.isValid(0.0));
        assertTrue(Location.isValid(1.0));
    }

    public void testEquals() {
        // Simple cases.
        assertTrue(Location.equals(VALID_1, VALID_1));
        assertTrue(Location.equals(VALID_2, VALID_2));
        assertFalse(Location.equals(VALID_1, VALID_2));
        assertFalse(Location.equals(VALID_2, VALID_1));

        // Cases with invalid locations.
        assertFalse(Location.equals(INVALID_1, VALID_1));
        assertFalse(Location.equals(INVALID_1, VALID_2));
        assertFalse(Location.equals(INVALID_2, VALID_1));
        assertFalse(Location.equals(INVALID_2, VALID_2));
        assertTrue(Location.equals(INVALID_1, INVALID_1));
        assertTrue(Location.equals(INVALID_2, INVALID_2));
        assertTrue(Location.equals(INVALID_1, INVALID_2));
        assertTrue(Location.equals(INVALID_2, INVALID_1));

        // Corner cases.
        assertTrue(Location.equals(0.0, 0.0));
        assertTrue(Location.equals(0.0, 1.0));
        assertTrue(Location.equals(1.0, 0.0));
        assertTrue(Location.equals(1.0, 1.0));
    }
    
    public void testDistance() {
        // Simple cases.
        assertEquals(DIST_12, Location.distance(VALID_1, VALID_2), EPSILON);
        assertEquals(DIST_12, Location.distance(VALID_2, VALID_1), EPSILON);
        
        // Corner case.
        assertEquals(0.5, Location.distance(VALID_1, Location.normalize(VALID_1 + 0.5)), EPSILON);
        assertEquals(0.5, Location.distance(VALID_1, Location.normalize(VALID_1 - 0.5)), EPSILON);
        assertEquals(0.5, Location.distance(VALID_2, Location.normalize(VALID_2 + 0.5)), EPSILON);
        assertEquals(0.5, Location.distance(VALID_2, Location.normalize(VALID_2 - 0.5)), EPSILON);
        
        // Identity.
        assertEquals(0.0, Location.distance(VALID_1, VALID_1));
        assertEquals(0.0, Location.distance(VALID_2, VALID_2));
    }
    
    public void testChange() {
        // Simple cases.
        assertEquals(CHANGE_12, Location.change(VALID_1, VALID_2), EPSILON);
        assertEquals(CHANGE_21, Location.change(VALID_2, VALID_1), EPSILON);
        
        // Maximal change is always positive.
        assertEquals(0.5, Location.change(VALID_1, Location.normalize(VALID_1 + 0.5)), EPSILON);
        assertEquals(0.5, Location.change(VALID_1, Location.normalize(VALID_1 - 0.5)), EPSILON);
        assertEquals(0.5, Location.change(VALID_2, Location.normalize(VALID_2 + 0.5)), EPSILON);
        assertEquals(0.5, Location.change(VALID_2, Location.normalize(VALID_2 - 0.5)), EPSILON);

        // Identity.
        assertEquals(0.0, Location.change(VALID_1, VALID_1));
        assertEquals(0.0, Location.change(VALID_2, VALID_2));
    }
    
    public void testNormalize() {
        // Simple cases.
        for (int i = 0; i < 5; i++) {
            assertEquals(VALID_1, Location.normalize(VALID_1 + i), EPSILON);
            assertEquals(VALID_1, Location.normalize(VALID_1 - i), EPSILON);
            assertEquals(VALID_2, Location.normalize(VALID_2 + i), EPSILON);
            assertEquals(VALID_2, Location.normalize(VALID_2 - i), EPSILON);
        }
        
        // Corner case.
        assertEquals(0.0, Location.normalize(1.0));
    }
    
    public void testDistanceAllowInvalid() {
        // Simple cases.
        assertEquals(DIST_12, Location.distanceAllowInvalid(VALID_1, VALID_2), EPSILON);
        assertEquals(DIST_12, Location.distanceAllowInvalid(VALID_2, VALID_1), EPSILON);
        
        // Corner case.
        assertEquals(0.5, Location.distanceAllowInvalid(VALID_1, Location.normalize(VALID_1 + 0.5)), EPSILON);
        assertEquals(0.5, Location.distanceAllowInvalid(VALID_1, Location.normalize(VALID_1 - 0.5)), EPSILON);
        assertEquals(0.5, Location.distanceAllowInvalid(VALID_2, Location.normalize(VALID_2 + 0.5)), EPSILON);
        assertEquals(0.5, Location.distanceAllowInvalid(VALID_2, Location.normalize(VALID_2 - 0.5)), EPSILON);
        
        // Identity.
        assertEquals(0.0, Location.distanceAllowInvalid(VALID_1, VALID_1));
        assertEquals(0.0, Location.distanceAllowInvalid(VALID_2, VALID_2));
        
        // Normal operation with invalid.
        assertEquals(2.0 - VALID_1, Location.distanceAllowInvalid(INVALID_1, VALID_1));
        assertEquals(2.0 - VALID_1, Location.distanceAllowInvalid(VALID_1, INVALID_1));
        assertEquals(2.0 - VALID_1, Location.distanceAllowInvalid(INVALID_2, VALID_1));
        assertEquals(2.0 - VALID_1, Location.distanceAllowInvalid(VALID_1, INVALID_2));
        assertEquals(2.0 - VALID_2, Location.distanceAllowInvalid(INVALID_1, VALID_2));
        assertEquals(2.0 - VALID_2, Location.distanceAllowInvalid(VALID_2, INVALID_1));
        assertEquals(2.0 - VALID_2, Location.distanceAllowInvalid(INVALID_2, VALID_2));
        assertEquals(2.0 - VALID_2, Location.distanceAllowInvalid(VALID_2, INVALID_2));
        
        // Identity of invalid.
        assertEquals(0.0, Location.distanceAllowInvalid(INVALID_1, INVALID_1));
        assertEquals(0.0, Location.distanceAllowInvalid(INVALID_1, INVALID_2));
        assertEquals(0.0, Location.distanceAllowInvalid(INVALID_2, INVALID_1));
        assertEquals(0.0, Location.distanceAllowInvalid(INVALID_2, INVALID_2));
    }

  double[] universe;

  public void setUp() {
    // All the tricky cases I can think of
    universe = new double[]{ 0.0, 0.1, 0.2, 0.21, 0.3, 0.35, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };
    // I know it's already sorted.
    Arrays.sort(universe);
  }

  // Given our universe, try all the corner cases for each values of the set...
  // As universe, target and exclude values successively
  // TODO: fix the exclude values; we don't do the match % Double.MIN_VALUE ...
  // so we might return "better" values than the legacy method!
  public void testsmallestDistance() throws Exception {
    for (double target:universe) {
      for (double exclude : universe) {

        assertTrue(
            smallestDistanceLegacy(universe, target, new double[]{exclude}) >=
            Location.smallestDistance(universe, target, new double[]{exclude}
            )
        );
      }
    }
  }

  /**
   * This is the old, legacy, obviously correct version of Location.smallestDistance()
   * It doesn't make any assumption on the input
   *
   * @param universe
   * @param target
   * @param exclusion
   * @return
   */
  public static double smallestDistanceLegacy(double[] universe, double target, double[] exclusion) {
    double diff = Double.MAX_VALUE;
    double loc = -1;
    for (double l : universe) {
      boolean ignoreLoc = false;
      for (double ex : exclusion) {
        if (Math.abs(l - ex) < Double.MIN_VALUE * 2) {
          ignoreLoc = true;
          break;
        }
      }
      if (ignoreLoc) {
        continue;
      }
      double newDiff = Location.distance(l, target);
      if (newDiff < diff) {
        loc = l;
        diff = newDiff;
      }
    }
    return loc;
  }
}
