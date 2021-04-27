import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.util.Arrays;

import static org.apache.commons.math3.linear.MatrixUtils.inverse;

public class Worker extends Thread {
    private static final double l = 0.1; //parameter for cost function
    private final int w_Port = 4000;
    private Socket requestSocket = null;
    private double memory;
    private int cores;
    private RealMatrix C, P, X, XX, Y, YY;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private int from, to, exit;

    private Worker(double freeMemory, int cores) {
        this.memory = freeMemory;
        this.cores = cores;
    }

    public static void main(String[] args) {
        OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double FreeMemory = (double) bean.getFreePhysicalMemorySize();
        int cores = bean.getAvailableProcessors();

        new Worker(FreeMemory, cores).start();
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        exit = 0;
        try {
            initialize();

            while (in.available() == 0) {
                sleep(1000);
            }

            System.out.println("Successfully connected to Master. \n- Received message from Master: \n" + in.readUTF() + "\n");

            while (exit != -1) {

                from = in.readInt();
                to = in.readInt();

                for (int j = from; j <= to; j++) {
                    X.setRowMatrix(j, calculate_x_u(j, YY));
                }

                System.out.println("Sending training results to master");
                RealMatrix sub = X.getSubMatrix(from, to, 0, X.getColumnDimension() - 1);
                out.writeObject(sub);
                out.flush();
                out.writeInt(from);
                out.flush();
                System.out.println("Training results for X sent!");

                //RealMatrix newX = (RealMatrix) in.readObject();
                X = null;
                X = ((RealMatrix) in.readObject()).copy();

                XX = preCalculateXX(X);
                System.out.println("Recalculating XX matrix");
                from = 0;
                from = in.readInt();
                to = in.readInt();

                for (int j = from; j <= to; j++) {
                    Y.setRowMatrix(j, calculate_y_i(j, XX));
                }

                System.out.println("Sending training results to master");
                RealMatrix subY = Y.getSubMatrix(from, to, 0, Y.getColumnDimension() - 1);
                out.writeObject(subY);
                out.flush();
                out.writeInt(from);
                out.flush();
                System.out.println("Training results for Y sent!");
                Y = null;
                Y = ((RealMatrix) in.readObject()).copy();
                YY = preCalculateYY(Y);

                exit = in.readInt();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }

    void printRealMatrix(RealMatrix M, String name) {

        System.out.print("\nPrinting Matrix " + name + " (");
        System.out.print(M.getRowDimension() + " x " + M.getColumnDimension() + ")\n");
        for (int i = 0; i < M.getRowDimension(); i++) {
            for (int j = 0; j < M.getColumnDimension(); j++) {
                if (M.getEntry(i, j) == 0)
                    System.out.print(" - ");
                else
                    System.out.print(M.getEntry(i, j) + " ");
            }
            System.out.println();
        }
        System.out.print("\n\n");
    }

    private void initialize() {
        try {
            requestSocket = new Socket("localhost", w_Port);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());


            System.out.println("Sending personal information: ");

            out.writeDouble(memory);
            out.flush();
            out.writeInt(cores);
            out.flush();

            System.out.println("Waiting for initial data: ");
            X = (RealMatrix) in.readObject();
            System.out.println("X matrix received");
            Y = (RealMatrix) in.readObject();
            System.out.println("Y matrix received");
            P = (RealMatrix) in.readObject();
            System.out.println("P matrix received");
            C = (RealMatrix) in.readObject();
            System.out.println("C matrix received");
            System.out.println();

            System.out.println("Now calculating XX and YY matrices");
            XX = preCalculateXX(X);
            YY = preCalculateYY(Y);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private RealMatrix preCalculateXX(RealMatrix X_matrix) {
        return (X_matrix.transpose()).multiply(X_matrix);
    }

    private RealMatrix preCalculateYY(RealMatrix Y_matrix) {
        return (Y_matrix.transpose()).multiply(Y_matrix);
    }

    //Pairnei orismata ton arithmo tis grammis u kai ton pinaka YY pou exei idi ypologistei apo prin
    private RealMatrix calculate_x_u(int u, RealMatrix YtransY) {
        RealMatrix Ytrans = Y.transpose(); //f*n
        RealMatrix CU = calculateCuMatrix(u, C); //n*n
        RealMatrix I = MatrixUtils.createRealIdentityMatrix(CU.getRowDimension()); //n*n
        RealMatrix I2 = MatrixUtils.createRealIdentityMatrix(X.getColumnDimension()); //f*f
        RealMatrix lI = I2.scalarMultiply(l); //f*f
        RealMatrix PU = P.getRowMatrix(u).transpose();//n*1

        RealMatrix Result = CU.subtract(I);
        Result = Ytrans.multiply(Result).multiply(Y);
        Result = YtransY.add(Result).add(lI);
        Result = inverse(Result);
        Result = Result.multiply(Ytrans).multiply(CU).multiply(PU);
        System.out.println("Training X row: " + u + "/" + to);
        return Result.transpose();
    }

    //Pairnei orismata ton arithmo tis grammis i kai ton pinaka XX pou exei idi ypologistei apo prin
    private RealMatrix calculate_y_i(int i, RealMatrix XtransX) {
        RealMatrix Xtrans = X.transpose();
        RealMatrix CI = calculateCiMatrix(i, C);
        RealMatrix I = MatrixUtils.createRealIdentityMatrix(CI.getRowDimension());
        RealMatrix I2 = MatrixUtils.createRealIdentityMatrix(Y.getColumnDimension());
        RealMatrix lI = I2.scalarMultiply(l);
        RealMatrix PI = P.getColumnMatrix(i);

        RealMatrix Result = CI.subtract(I);
        Result = Xtrans.multiply(Result).multiply(X);
        Result = XtransX.add(Result).add(lI);
        Result = inverse(Result);
        Result = Result.multiply(Xtrans).multiply(CI).multiply(PI);
        System.out.println("Training Y row: " + i + "/" + to);
        return Result.transpose();
    }

    //O Cu matrix einai enas pinakas pou vgainei an apo ton C paroume mia grammi(tin u grammi, p.x. tin u=4 grammi) ,
    //kai tin kanoume diagonio se enan allo mideniko pinaka
    private RealMatrix calculateCuMatrix(int u, RealMatrix C_matrix) {
        //get a row from C
        double[] diag_elems = C_matrix.getRow(u);


        //create and return the diagonal matrix
        return MatrixUtils.createRealDiagonalMatrix(diag_elems);
    }

    //O Ci matrix einai enas pinakas pou vgainei an apo ton C paroume mia stili(tin i stili, p.x. tin i=3 stili) ,
    //kai tin kanoume diagonio se enan allo mideniko pinaka
    private RealMatrix calculateCiMatrix(int it, RealMatrix C_matrix) {
        //get a column from C
        double[] diag_elems = C_matrix.getColumn(it);
        //create and return the diagonal matrix
        return MatrixUtils.createRealDiagonalMatrix(diag_elems);
    }
}
