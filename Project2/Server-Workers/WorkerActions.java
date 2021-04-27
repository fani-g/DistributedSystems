import org.apache.commons.math3.linear.RealMatrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

class WorkerActions {
    static RealMatrix X, Y;
    double mem;
    ObjectOutputStream out = null;
    ObjectInputStream in = null;
    private int ID;
    private int cores;
    private Socket worker;

    WorkerActions(Socket worker, int id) {
        this.worker = worker;
        this.ID = id;
    }

    void distributeMatrixToWorkers(RealMatrix M) {
        try {
            out.writeObject(M);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
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

    String getWorkerInfo() {
        return ID + ", " + mem + ", " + cores;
    }

    void sendWork(final int from, final int to) throws IOException {
        out.writeInt(from);
        out.flush();
        out.writeInt(to);
        out.flush();
    }

    void concatMatrixX() throws IOException, ClassNotFoundException {
        RealMatrix sub = (RealMatrix) in.readObject();
        int from = in.readInt();
        X.setSubMatrix(sub.getData(), from, 0);
    }

    void concatMatrixY() throws IOException, ClassNotFoundException {
        RealMatrix sub = (RealMatrix) in.readObject();
        int from = in.readInt();
        Y.setSubMatrix(sub.getData(), from, 0);
    }

    RealMatrix getResultsX() {
        return X;
    }

    RealMatrix getResultsY() {
        return Y;
    }

    void initializeMatrices(RealMatrix x, RealMatrix y) {
        X = x;
        Y = y;
    }

    void initialize() {
        try {
            in = new ObjectInputStream(worker.getInputStream());
            out = new ObjectOutputStream(worker.getOutputStream());
            this.mem = in.readDouble();
            this.cores = in.readInt();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void laughterIsGood() throws IOException {
        out.writeUTF("Execute Order 66");
        out.flush();
    }

    public void sendExitMessage(int i) throws IOException {
        out.writeInt(i);
        out.flush();
    }
}