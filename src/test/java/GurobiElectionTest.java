import gurobi.GRB;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class GurobiElectionTest {

    /**
     * Testcase 1:
     * Not enough districts
     */
    @Test public void testcase_1() throws Exception {
        // Setup
        Election election = new Election("dem_tc_1.in", "rep_tc_1.in", 1, 2);

        // Run election
        int democratWinCount = election.winCount(true, true, true);

        // Check result
        assertTrue(election.status == GRB.Status.INFEASIBLE);
        assertEquals(0, election.demVictoryCount);
        assertEquals(0, election.repVictoryCount);
    }

    /**
     * Testcase 2:
     * Same number of equal districts leading to a draw
     */
    @Test public void testcase_2() throws Exception {
        // Setup
        Election election = new Election("dem_tc_2.in", "rep_tc_2.in", 1, 2);

        // Run election
        int democratWinCount = election.winCount(true, true, true);

        // Check result
        assertTrue(election.status == GRB.Status.OPTIMAL);
        assertEquals(0, election.demVictoryCount);
        assertEquals(0, election.repVictoryCount);
    }

    /**
     * Testcase 3:
     * One equal district leads to a 9:0 victory for democrats
     */
    @Test public void testcase_3() throws Exception {
        // Setup
        Election election = new Election("dem_tc_3.in", "rep_tc_3.in", 1, 2);

        // Run election
        int democratWinCount = election.winCount(true, true, true);

        // Check result
        assertTrue(election.status == GRB.Status.OPTIMAL);
        assertEquals(9, election.demVictoryCount);
        assertEquals(0, election.repVictoryCount);
    }

    /**
     * Testcase 4:
     * All districts won by democrats optimized by democrats
     */
    @Test public void testcase_4() throws Exception {
        // Setup
        Election election = new Election("dem_tc_4.in", "rep_tc_4.in", 1, 2);

        // Run election
        int democratWinCount = election.winCount(true, true, true);

        // Check result
        assertTrue(election.status == GRB.Status.OPTIMAL);
        assertEquals(10, election.demVictoryCount);
        assertEquals(0, election.repVictoryCount);
    }

    /**
     * Testcase 5:
     * All districts won by democrats optimized by republicans
     */
    @Test public void testcase_5() throws Exception {
        // Setup
        Election election = new Election("dem_tc_5.in", "rep_tc_5.in", 1, 2);

        // Run election
        int democratWinCount = election.winCount(true, true, false);

        // Check result
        assertTrue(election.status == GRB.Status.OPTIMAL);
        assertEquals(10, election.demVictoryCount);
        assertEquals(0, election.repVictoryCount);
    }

    /**
     * Testcase 6:
     * All districts won by republicans optimized by democrats
     */
    @Test public void testcase_6() throws Exception {
        // Setup
        Election election = new Election("dem_tc_6.in", "rep_tc_6.in", 1, 2);

        // Run election
        int democratWinCount = election.winCount(true, true, true);

        // Check result
        assertTrue(election.status == GRB.Status.OPTIMAL);
        assertEquals(0, election.demVictoryCount);
        assertEquals(10, election.repVictoryCount);
    }

    /**
     * Testcase 7:
     * All districts won by republicans optimized by republicans
     */
    @Test public void testcase_7() throws Exception {
        // Setup
        Election election = new Election("dem_tc_7.in", "rep_tc_7.in", 1, 2);

        // Run election
        int democratWinCount = election.winCount(true, true, false);

        // Check result
        assertTrue(election.status == GRB.Status.OPTIMAL);
        assertEquals(0, election.demVictoryCount);
        assertEquals(10, election.repVictoryCount);
    }

    /**
     * Testcase 8:
     * 5 district won by republicans, 5 by democrats, optimized by democrats
     */
    @Test public void testcase_8() throws Exception {
        // Setup
        Election election = new Election("dem_tc_8.in", "rep_tc_8.in", 1, 2);

        // Run election
        int democratWinCount = election.winCount(true, true, true);

        // Check result
        assertTrue(election.status == GRB.Status.OPTIMAL);
        assertEquals(5, election.demVictoryCount);
        assertEquals(5, election.repVictoryCount);
    }

    /**
     * Testcase 9:
     * 5 district won by republicans, 5 by democrats, optimized by republicans
     */
    @Test public void testcase_9() throws Exception {
        // Setup
        Election election = new Election("dem_tc_9.in", "rep_tc_9.in", 1, 2);

        // Run election
        int democratWinCount = election.winCount(true, true, true);

        // Check result
        assertTrue(election.status == GRB.Status.OPTIMAL);
        assertEquals(5, election.demVictoryCount);
        assertEquals(5, election.repVictoryCount);
    }

    /**
     * Testcase 10:
     * Small M leads to infeasable solution
     */
    @Test public void testcase_10() throws Exception {
        // Setup using small m
        Election election = new Election("dem_tc_10.in", "rep_tc_10.in", 1, 1);

        // Run election
        int democratWinCount = election.winCount(true, true, true);

        // Check result
        assertTrue(election.status == GRB.Status.INFEASIBLE);

        // Setup using correct m
        election = new Election("dem_tc_10.in", "rep_tc_10.in", 1, 2);

        // Run election
        democratWinCount = election.winCount(true, true, true);

        // Check result
        assertTrue(election.status == GRB.Status.OPTIMAL);
        assertEquals(5, election.demVictoryCount);
        assertEquals(5, election.repVictoryCount);
    }

    /**
     * Testcase 11:
     * Optmimize Democtrats using main input files
     */
    @Test public void testcase_11() throws Exception {
        // Setup
        Election election = new Election("dem_tc_11.in", "rep_tc_11.in", 135, 200000000);

        // Run election
        int democratWinCount = election.winCount(true, true, true);

        // Check result
        assertTrue(election.status == GRB.Status.OPTIMAL);
        assertEquals(10, election.demVictoryCount);
        assertEquals(0, election.repVictoryCount);
    }

    /**
     * Testcase 12
     * Optmimize Republicans using main input files
     */
    @Test public void testcase_12() throws Exception {
        // Setup
        Election election = new Election("dem_tc_12.in", "rep_tc_12.in", 135, 200000000);

        // Run election
        int democratWinCount = election.winCount(true, true, false);

        // Check result
        assertTrue(election.status == GRB.Status.OPTIMAL);
        assertEquals(3, election.demVictoryCount);
        assertEquals(7, election.repVictoryCount);
    }

}
