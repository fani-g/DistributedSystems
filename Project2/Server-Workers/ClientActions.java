import org.apache.commons.math3.linear.RealMatrix;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class ClientActions extends Thread {
    private Socket client;
    private int clientID;
    private ArrayList<POI> pois, best = new ArrayList<>();
    private int users, POIs;
    private RealMatrix X, Y;

    ClientActions(Socket connection, int idCounter) {
        this.client = connection;
        this.clientID = idCounter;
    }

    void initialize(final int users, final int POIs, final RealMatrix X, final RealMatrix Y, ArrayList<POI> POI_List) {
        this.users = users;
        this.POIs = POIs;
        this.X = X.copy();
        this.Y = Y.copy();
        this.pois = POI_List;
    }

    @Override
    public void run() {
        try {
            List<POI> newList = null;
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());

            out.writeInt(users);
            out.flush();
            int user = in.readInt();
            int reply = (user > users ? -1 : 0);

            if (reply == 0) {
                best = calculateBestLocalPOIsForUsers(user);

                String category = in.readUTF();
                System.out.println(category);
                double lat, lon;
                int distance, k_top;
                //Getting LatLng
                lat = in.readDouble();
                lon = in.readDouble();
                //User's radius
                distance = in.readInt();
                //Requested amount of POIs
                k_top = in.readInt();


                if (category.equalsIgnoreCase("ALL")){
                    //top_k POIs regardless of category.

                    System.out.println("Lat "+ lat + ", Lng " + lon + ", Radius " + distance + ", Requested "+ k_top);

                    best.sort((o1, o2) -> Double.compare(o2.getFrequency(), o1.getFrequency()));
                    int count = (int) (best.stream()
                            .filter(poi -> poi.distance(poi.getLatitude(),lat,poi.getLongitude(),lon)<= distance*1000)
                            .count());

                    System.out.println("Found: " + count);
                    System.out.println("Requested: "+ k_top);

                    if (count < k_top)
                        k_top = count;

                    System.out.println(k_top);

                    out.writeInt(k_top);
                    out.flush();

                    newList = best.stream()
                            .filter(poi -> poi.distance(poi.getLatitude(),lat,poi.getLongitude(),lon)<= distance*1000)
                            .limit(k_top)
                            .collect(Collectors.toList());

                    for (POI p : newList) {
                        out.writeInt(p.getID());
                        out.flush();
                        out.writeUTF(p.getPOI_name());
                        out.flush();
                        out.writeDouble(p.getLatitude());
                        out.flush();
                        out.writeDouble(p.getLongitude());
                        out.flush();
                        out.writeUTF(p.getPhotos());
                        out.flush();
                        out.writeUTF(p.getPOI_category_id());
                        out.flush();
                    }

                }else{
                    //if a category is selected.

                    boolean found = best.stream()
                            .anyMatch(poi -> poi.getPOI_category_id().equalsIgnoreCase(category));

                    if (found) {
                        out.writeInt(0);
                        out.flush();

                        System.out.println("Lat "+ lat + ", Lng " + lon + ", Radius " + distance + ", Requested "+ k_top);
                        best.sort((o1, o2) -> Double.compare(o2.getFrequency(), o1.getFrequency()));

                        int count = (int) (best.stream()
                                .filter(poi -> poi.getPOI_category_id().equalsIgnoreCase(category)))
                                .filter(poi -> poi.distance(poi.getLatitude(),lat,poi.getLongitude(),lon)<= distance*1000)
                                .count();
                        System.out.println("Found: " + count);
                        System.out.println("Requested: "+ k_top);

                        if (count < k_top)
                            k_top = count;

                        System.out.println(k_top);

                        out.writeInt(k_top);
                        out.flush();

                        newList = best.stream()
                                .filter(poi -> poi.getPOI_category_id().equalsIgnoreCase(category))
                                .filter(poi -> poi.distance(poi.getLatitude(),lat,poi.getLongitude(),lon)<= distance*1000)
                                .limit(k_top)
                                .collect(Collectors.toList());

                        for (POI p : newList) {
                            out.writeInt(p.getID());
                            out.flush();
                            out.writeUTF(p.getPOI_name());
                            out.flush();
                            out.writeDouble(p.getLatitude());
                            out.flush();
                            out.writeDouble(p.getLongitude());
                            out.flush();
                            out.writeUTF(p.getPhotos());
                            out.flush();
                            out.writeUTF(p.getPOI_category_id());
                            out.flush();
                        }
                    }else{
                        out.writeInt(-1);
                        out.flush();
                    }
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private ArrayList<POI> calculateBestLocalPOIsForUsers(int user) {
        ArrayList<POI> poi = new ArrayList<>();
        ArrayList<Double> arr = new ArrayList<>();
        int[] position = new int[POIs];

        //RealMatrix rui = calculate_x_u(u, YY).transpose().multiply(calculate_y_i(i, XX));
        for (int i = 0; i < POIs; i++) {
            RealMatrix rui = X.getRowMatrix(user).multiply(Y.getRowMatrix(i).transpose());
            arr.add(rui.getEntry(0, 0));
            position[i] = i;
            int j = 0;
            while (pois.get(j).getID() != i) {
                j++;
            }
            pois.get(j).setFrequency(arr.get(i));
            poi.add(pois.get(j));
        }

        return poi;
    }
}
