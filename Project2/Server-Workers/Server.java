import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class Server {
    private static final int w_Port = 4000 , c_Port = 4001;
    private static final int m = 835; //m is number of all USERS (Y axis)
    private static final int n = 1692; //n is number of all POIs (X axis)
    private static final int f = 20; //(=k) rank of the factorization
    private static final int a = 40; //rate of increase in matrix C
    private static final double l = 0.1; //parameter for cost function
    private static ArrayList<POI> POI_List = new ArrayList<>();
    private ArrayList<WorkerActions> workerActions = new ArrayList<>();
    private OpenMapRealMatrix R;
    private RealMatrix P;
    private RealMatrix C;
    private RealMatrix X, Y, rui;
    private int idCounter = 0;
    private Socket connection = null;
    private ServerSocket server = null;

    private static OpenMapRealMatrix readFile(FileInputStream f) {
        OpenMapRealMatrix matrix = new OpenMapRealMatrix(m, n);
        BufferedReader br = new BufferedReader(new InputStreamReader(f));
        // Vector<POI> points = new Vector<POI>();

        //POI data;
        String line;
        String splitBy = ",";
        try {
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(splitBy);
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].startsWith(" ")) {
                        tokens[i] = tokens[i].substring(1);
                    }
                }
                //System.out.println(Integer.parseInt(tokens[0]) + " " + Integer.parseInt(tokens[1])+" "+Integer.parseInt(tokens[2]));
                matrix.setEntry(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return matrix;
    }

    public static void main(String[] args) throws IOException {
        try {
            JSONObject obj = (JSONObject) new JSONParser().parse(new FileReader("POIs.json"));
            JSONObject ele;
            for (int i = 0; (ele = (JSONObject) obj.get(Integer.toString(i))) != null; i++) {
                String POI = (String) ele.get("POI");
                double latitude = (double) ele.get("latitude");
                double longitude = (double) ele.get("longitude");
                String photos = (String) ele.get("photos");
                String POI_category = (String) ele.get("POI_category_id");
                String POI_Name = (String) ele.get("POI_name");
                POI_List.add(new POI(i, POI, latitude, longitude, photos, POI_category, POI_Name));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Scanner scan = new Scanner(System.in);
        System.out.println("Amount of workers: ");
        int workers = scan.nextInt();
        System.out.println("How many training epochs do you want? ");
        int epochs = scan.nextInt();
        new Server().openServer(workers, epochs);
    }

    private void initialize() throws FileNotFoundException {

        R = new OpenMapRealMatrix(m, n); //Matrix R(m x n): observations
        P = new OpenMapRealMatrix(m, n); //Matrix P(m x n): binary values of R
        C = MatrixUtils.createRealMatrix(m, n); //Matrix C(m x n): C =(1 + aR), a=40
        X = MatrixUtils.createRealMatrix(m, f); //Matrix X(m x f): user factors
        Y = MatrixUtils.createRealMatrix(n, f); //Matrix Y(n x f): item factors

        RandomGenerator r = new JDKRandomGenerator();
        r.setSeed(1);

        //Inserting real values into R
        R = readFile(new FileInputStream("input2.csv"));
        calculatePMatrix(R); //Inserting values into P
        calculateCMatrix(a, R); //Inserting values into C

        //Inserting into X
        for (int i = 0; i < X.getRowDimension(); i++) {
            for (int j = 0; j < X.getColumnDimension(); j++) {
                X.setEntry(i, j, r.nextDouble());
            }
        }
        //Inserting into Y
        for (int i = 0; i < Y.getRowDimension(); i++) {
            for (int j = 0; j < Y.getColumnDimension(); j++) {
                Y.setEntry(i, j, r.nextDouble());
            }
        }
    }

    private void calculateCMatrix(int a, RealMatrix R) {
        for (int i = 0; i < C.getRowDimension(); i++) {
            for (int j = 0; j < C.getColumnDimension(); j++) {
                C.setEntry(i, j, 1.0 + R.getEntry(i, j) * a);
            }
        }
    }

    private void calculatePMatrix(RealMatrix R) {
        for (int i = 0; i < P.getRowDimension(); i++) {
            for (int j = 0; j < P.getColumnDimension(); j++) {
                if (R.getEntry(i, j) > 0) {
                    P.setEntry(i, j, 1);
                }
            }
        }
    }

    private double calculateError() {
        //Mean square error
        double Left = 0;
        for (int i = 0; i < n; i++) {
            for (int u = 0; u < m; u++) {
                Double Cui = C.getEntry(u,i);
                Double Pui = P.getEntry(u,i);
                RealVector xu = X.getRowVector(u);
                RealVector yi = Y.getRowVector(i);
                double XYT = xu.dotProduct(yi);
                double temp = Pui - XYT;
                temp = Math.pow(temp,2.0);
                double temp2 = Cui * temp;
                Left += temp2;
            }
        }
        //regularization term
        double Sxu = 0;
        for (int u = 0; u < m; u++) {
            Sxu = Sxu + Math.pow(X.getRowMatrix(u).getFrobeniusNorm(), 2.0);
        }
        double Syi = 0;
        for (int i = 0; i < n; i++) {
            Syi = Syi + Math.pow(Y.getRowMatrix(i).getFrobeniusNorm(), 2.0);
        }

        double Right = l * (Sxu + Syi);

        return (Left+Right); //return (Mean square error) + regularization term

    }

    private void sortWorkers() {
        workerActions.sort((o1, o2) -> Double.compare(o2.mem, o1.mem));
    }
    private void openServer(final int workers, final int epochs) {
        try {
            initialize();
            server = new ServerSocket(w_Port);

            while (workerActions.size() < workers) {
                System.out.println("Waiting for connections ...");
                WorkerActions w;
                try {
                    connection = server.accept();
                    System.out.println("New connection accepted");
                    w = new WorkerActions(connection, idCounter);

                    w.initialize();

                    w.distributeMatrixToWorkers(X);
                    w.distributeMatrixToWorkers(Y);
                    w.distributeMatrixToWorkers(P);
                    w.distributeMatrixToWorkers(C);

                    workerActions.add(w);
                    idCounter++;

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            workerActions.get(0).initializeMatrices(X, Y);
            System.out.println();
            sortWorkers();

            for (WorkerActions w : workerActions) {
                w.laughterIsGood();
            }

            int epochCounter = 0;
            double error = Math.pow(2,63)-1;
            double cost = calculateError();
            double abs = Math.abs(error-cost);
            while (epochCounter < epochs || abs < Math.pow(10,-3)) {
                System.out.println(Math.abs(error-cost));
                error = cost;
                System.out.println("Training X matrix... (times: " + (epochCounter + 1) + "/" + epochs + ")");

                int from = 0, to;

                //Distributing work evenly for X matrix
                System.out.println("Distributing work for the X matrix training");
                for (WorkerActions w : workerActions) {
                    to = from + X.getRowDimension() / workerActions.size() - 1;
                    if (X.getRowDimension() - to < workerActions.size()) to += (X.getRowDimension() - to) - 1;
                    w.sendWork(from, to);
                    from = to + 1;
                }

                System.out.println("Waiting for results");
                for (WorkerActions w : workerActions) {
                    w.concatMatrixX();
                }

                X = workerActions.get(0).getResultsX().copy();

                for (WorkerActions w : workerActions) {
                    w.distributeMatrixToWorkers(X);
                }
                from = 0;
                //Distributing work evenly for Y matrix
                System.out.println("Distributing work for the Y matrix training");
                for (WorkerActions w : workerActions) {
                    to = from + Y.getRowDimension() / workerActions.size() - 1;
                    if (Y.getRowDimension() - to < workerActions.size()) to += (Y.getRowDimension() - to) - 1;
                    w.sendWork(from, to);
                    from = to + 1;
                }
                System.out.println("Waiting for results");
                for (WorkerActions w : workerActions) {
                    w.concatMatrixY();
                }

                Y = workerActions.get(0).getResultsY().copy();

                for (WorkerActions w : workerActions) {
                    w.distributeMatrixToWorkers(Y);
                }
                cost = calculateError();

                System.out.println("Cost Function for epoch " + (epochCounter+1) + " : " + cost);
                System.out.println();

                epochCounter++;

                for (WorkerActions w : workerActions) {
                    w.sendExitMessage(0);
                }

                abs = Math.abs(error-cost);
            }
            for (WorkerActions w : workerActions) {
                w.sendExitMessage(-1);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try{
            idCounter =0;
            server = new ServerSocket(c_Port);
            while (true) {
                System.out.println("Server is now ready to accept Client connections");
                connection = server.accept();
                ClientActions cA = new ClientActions(connection, idCounter);
                System.out.println("Got new Client connection" + cA.getId());
                ++idCounter;
                cA.initialize(m, n, X, Y, POI_List);
                Thread t = new Thread(cA);
                t.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

