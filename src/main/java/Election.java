import gurobi.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class Election {

    // Model params
    /**
     * Testfile for democrats
     */
    public final String FILE_DEMOCRATS;
    /**
     * Testfile for republicans
     */
    public final String FILE_REPUBLICANS;
    /**
     * Number of voting districts
     */
    public static final int B = 10;
    /**
     * Minimum size of a district
     */
    public static int DISTRICT_SIZE;
    /**
     * Constant used in if-else construct
     */
    public static int M;

    // Results
    /**
     * Gurobi status after execution
     */
    public int status;
    /**
     * Democrat district win count
     */
    public int demVictoryCount;
    /**
     * Republican district win count
     */
    public int repVictoryCount;

    // Attributes to read input files
    BufferedReader inp;
    private int inpPos = 0;
    private String inpLine;

    public Election(String filenameDemocrats, String filenameRepublicans, int districtSize, int m) {
        // Set filenames
        FILE_DEMOCRATS = filenameDemocrats;
        FILE_REPUBLICANS = filenameRepublicans;

        // Set district size
        DISTRICT_SIZE = districtSize;

        // Set M
        M = m;
    }

    public static void main(String[] args) throws Exception {
        // Create election
        Election demElection = new Election("dem_tc.in", "rep_tc.in", 135, 200000000);

        // Create model maximized for democrat wins
        demElection.winCount(true, false, true);

        // Create model maximize for democrat wins
        Election repElection = new Election("dem_tc.in", "rep_tc.in", 135, 200000000);

        // Create model maximized for republican wins
        repElection.winCount(false, true, false);
    }

    public int winCount(boolean countDemocratWins, boolean countRepublicanWins, boolean optimizeDemocrats)
            throws Exception {
        // Read files
        // Load democrat votes
        loadDemocratFile();
        int width = Integer.parseInt(next());
        int height = Integer.parseInt(next());
        // d[y][x] = Number of democrats votes of cell with coordinates (x,y)
        int[][] d = getDemocratVotes(width, height);

        // Read republican votes
        loadRepublicanFile(width, height);
        // r[y][x] = Number of republican votes of cell with coordinates (x,y)
        int[][] r = getRepublicanVotes(width, height);

        // Generate GUROBI model
        GRBEnv env = new GRBEnv();
        GRBModel model = new GRBModel(env);
        String st = "";

        // Build model
        // VD[b] = win for democrats in district b
        GRBVar[] VD = new GRBVar[B];
        // VR[b] = win for republicans in district b
        GRBVar[] VR = new GRBVar[B];
        // D[b] = Number of democrat votes of cell with district b
        GRBLinExpr[] D = new GRBLinExpr[B];
        // R[b] = Number of republican votes of cell with district b
        GRBLinExpr[] R = new GRBLinExpr[B];
        // Constraint (B): Each label has to be used at least districtSize times
        GRBLinExpr[] districtSizeExpr = new GRBLinExpr[B];
        for (int b = 0; b < B; b++) {
            if (countDemocratWins) {
                // Add variable VD
                st = "VD_" + String.valueOf(b);
                VD[b] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
            }
            if (countRepublicanWins) {
                // Add variable VR
                st = "VR_" + String.valueOf(b);
                VR[b] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
            }

            // Initialize arrays
            D[b] = new GRBLinExpr();
            R[b] = new GRBLinExpr();
            districtSizeExpr[b] = new GRBLinExpr();
        }

        // C[y][x][b] = district of cell with coordinates (x,y)
        GRBVar[][][] C = new GRBVar[height][width][B];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                // Ignore inhabited area
                if (r[y][x] + d[y][x] == 0)
                    continue;

                for (int b = 0; b < B; b++) {
                    // Add variable C
                    st = "C_" + String.valueOf(x) + "_" + String.valueOf(y) + "_" + String.valueOf(b);
                    C[y][x][b] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, st);
                }

            }
        }

        // Add constraints
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                // Ignore inhabited area
                if (r[y][x] + d[y][x] == 0)
                    continue;

                // Add constraint (A): Each cell with coordinates (x,y) has to belong to exactly 1 district
                GRBLinExpr labelCountExpr = new GRBLinExpr();
                labelCountExpr.addTerms(null, C[y][x]);
                st = "con_A_" + String.valueOf(x) + "_" + String.valueOf(y);
                model.addConstr(labelCountExpr, GRB.EQUAL, 1.0, st);

                for (int b = 0; b < B; b++) {
                    // Set D
                    D[b].addTerm(d[y][x], C[y][x][b]);

                    // Set R
                    R[b].addTerm(r[y][x], C[y][x][b]);

                    // Constraint (B): Each label has to be used at least districtSize times
                    districtSizeExpr[b].addTerm(1, C[y][x][b]);
                }
            }
        }

        // Parts of greater than comparison
        GRBLinExpr middlePart;
        GRBLinExpr rightPart;
        // Objective: Sum victories VD/VR
        GRBLinExpr sumV = new GRBLinExpr();
        for (int b = 0; b < B; b++) {
            // Add constraint (B): Each label has to be used at least districtSize times
            st = "con_B_" + String.valueOf(b);
            model.addConstr(districtSizeExpr[b], GRB.GREATER_EQUAL, DISTRICT_SIZE, st);

            if (countDemocratWins) {
                // Set VD[b]
                // D[b] > R[b]
                middlePart = new GRBLinExpr();
                middlePart.add(R[b]);
                middlePart.multAdd(-1, D[b]);
                middlePart.addTerm(M, VD[b]);
                st = "con_VD1_" + String.valueOf(b);
                model.addConstr(0, GRB.LESS_EQUAL, middlePart, st);

                rightPart = new GRBLinExpr();
                rightPart.addConstant(M);
                rightPart.addConstant(-1);
                st = "con_VD2_" + String.valueOf(b);
                model.addConstr(middlePart, GRB.LESS_EQUAL, rightPart, st);
            }

            if (countRepublicanWins) {
                // Set VR[b]
                // R[b] > D[b]
                middlePart = new GRBLinExpr();
                middlePart.add(D[b]);
                middlePart.multAdd(-1, R[b]);
                middlePart.addTerm(M, VR[b]);
                st = "con_VR1_" + String.valueOf(b);
                model.addConstr(0, GRB.LESS_EQUAL, middlePart, st);

                rightPart = new GRBLinExpr();
                rightPart.addConstant(M);
                rightPart.addConstant(-1);
                st = "con_VR2_" + String.valueOf(b);
                model.addConstr(middlePart, GRB.LESS_EQUAL, rightPart, st);
            }

            // Objective: Sum victories VD/VR
            if (optimizeDemocrats && countDemocratWins) {
                sumV.addTerm(1, VD[b]);
            } else if (!optimizeDemocrats && countRepublicanWins) {
                sumV.addTerm(1, VR[b]);
            } else {
                // Illegal combination of var to set and var to optimize
                throw new IllegalStateException();
            }
        }

        // Objective
        model.setObjective(sumV, GRB.MAXIMIZE);

        // Optimize model
        long start = System.currentTimeMillis();
        model.optimize();
        long stop = System.currentTimeMillis();

        // Write model to file
        if (countRepublicanWins != countDemocratWins) {
            // Only in test cases both flags true
            if (optimizeDemocrats) {
                model.write("democrats.mps");
            } else {
                model.write("republicans.mps");
            }
        }

        // Get results
        status = model.get(GRB.IntAttr.Status);
        int maxDiff = Integer.MIN_VALUE;
        if (status == GRB.Status.OPTIMAL) {
            for (int b = 0; b < B; b++) {

                if (countDemocratWins) {
                    double demVictory = VD[b].get(GRB.DoubleAttr.X);
                if (demVictory > 0) {
                    demVictoryCount++;
                }
                }

                if (countRepublicanWins) {
                    double repVictory = VR[b].get(GRB.DoubleAttr.X);
                    if (repVictory > 0) {
                        repVictoryCount++;
                    }
                }
            }

            // Check model
            int[] labelCount = new int[B];
            int diff = 0;
            int[] democratsPerDistrict = new int[B];
            int[] republicansPerDistrict = new int[B];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    // Ignore inhabited area
                    if (r[y][x] + d[y][x] == 0)
                        continue;

                    int districtCounter = 0;
                    for (int b = 0; b < B; b++) {
                        // Number of districts a cell is assigned
                        districtCounter += C[y][x][b].get(GRB.DoubleAttr.X);

                        // Number of cells that use label b
                        if (C[y][x][b].get(GRB.DoubleAttr.X) == 1) {
                            labelCount[b] += 1;
                        }

                        // Get number of democrat votes per district
                        democratsPerDistrict[b] += d[y][x] * C[y][x][b].get(GRB.DoubleAttr.X);
                        // Get number of republican votes per district
                        republicansPerDistrict[b] += r[y][x] * C[y][x][b].get(GRB.DoubleAttr.X);

                        // Get max difference
                        if (optimizeDemocrats){
                            diff = Math.abs(republicansPerDistrict[b]- democratsPerDistrict[b]);
                        } else {
                            diff = Math.abs(democratsPerDistrict[b] - republicansPerDistrict[b]);
                        }
                        maxDiff = diff > maxDiff ? diff : maxDiff;
                    }

                    // Constraint (A): Each cell with coordinates (x,y) has to belong to exactly 1 district
                    assert (districtCounter == 1);


                }
            }

            for (int b = 0; b < B; b++) {
                //System.out.println("Label " + b + ", " + labelCount[b]);
                // Constraint (B): Each label has to be used at least districtSize times
                assert (labelCount[b] >= DISTRICT_SIZE);

                // Check if detected win corresponds with expected win
                if (democratsPerDistrict[b] > republicansPerDistrict[b]) {
                    if (countDemocratWins) assert (VD[b].get(GRB.DoubleAttr.X) == 1);
                    if (countRepublicanWins) assert (VR[b].get(GRB.DoubleAttr.X) == 0);
                }
                if (democratsPerDistrict[b] < republicansPerDistrict[b]) {
                    if (countDemocratWins) assert (VD[b].get(GRB.DoubleAttr.X) == 0);
                    if (countRepublicanWins) assert (VR[b].get(GRB.DoubleAttr.X) == 1);
                }
            }

            // There can not be more winner than 10
            assert (demVictoryCount + repVictoryCount <= 10);

        }

        double exec = (stop - start) / 1000d;
        System.out.println("--------------------------------------------------------------------------------------");
        System.out.println("                                    INFO");
        System.out.println("--------------------------------------------------------------------------------------");
        System.out.println("Maximal Difference between R/D: " + maxDiff);
        System.out.println("Gurobi exec (s): " + exec );
        System.out.println("");

        // Dispose of model and environment
        model.dispose();
        env.dispose();

        return demVictoryCount;
    }

    private int[][] getRepublicanVotes(int width, int height) throws Exception {
        int[][] r = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = Integer.parseInt(next());
                r[y][x] = value;
            }
        }
        return r;
    }

    private int[][] getDemocratVotes(int width, int height) throws Exception {
        int[][] d = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = Integer.parseInt(next());
                d[y][x] = value;
            }
        }
        return d;
    }

    private void loadRepublicanFile(int width, int height) throws Exception {
        inp = new BufferedReader(new FileReader(getFileFromResources(FILE_REPUBLICANS)));
        inpLine = inp.readLine();
        if (width != Integer.parseInt(next()) || height != Integer.parseInt(next()))
            throw new IllegalStateException("Files have not same number of cells");
    }

    private void loadDemocratFile() throws IOException {
        // Read democrats file
        inp = new BufferedReader(new FileReader(getFileFromResources(FILE_DEMOCRATS)));
        inpLine = inp.readLine();
    }

    private String next() throws Exception {
        int nextPos = inpLine.indexOf(' ', inpPos + 1);
        String token = inpLine.substring(inpPos, nextPos == -1 ? inpLine.length() : nextPos);
        if (nextPos == -1)
            inpLine = inp.readLine();
        inpPos = nextPos + 1;
        return token;
    }

    private File getFileFromResources(String fileName) {
        ClassLoader classLoader = this.getClass().getClassLoader();

        URL resource = classLoader.getResource(fileName);
        return new File(resource.getFile());
    }
}
