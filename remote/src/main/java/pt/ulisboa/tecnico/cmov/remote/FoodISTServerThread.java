package pt.ulisboa.tecnico.cmov.remote;

import java.io.*;
import java.net.*;

import pt.ulisboa.tecnico.cmov.library.Dish;
import pt.ulisboa.tecnico.cmov.library.DishImage;

public class FoodISTServerThread implements Runnable {

    private Socket socket;
    private State state;

    public FoodISTServerThread(Socket socket, State state) {
        this.socket = socket;
        this.state = state;
    }

    public void run() {
        try {

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            ObjectInputStream inputStream = new ObjectInputStream(in);
            ObjectOutputStream outputStream = new ObjectOutputStream(out);
            String command;

            command = (String) inputStream.readObject();

            switch (command){

                case "loadState":
                    outputStream.writeObject(this.state.getDishes());
                    System.out.println("LoadState: State sent to client");
                    break;

                case "addDish":
                    state.addDish( (Dish) inputStream.readObject() );
                    System.out.println("AddDish: Dish added to server");
                    break;

                case "addDishImage":
                    state.addDishImage((DishImage) inputStream.readObject());
                    System.out.println("AddDishImage: Added dish image to server");
                    break;

                case "addRating":
                    String diningOptionName = (String) inputStream.readObject();
                    String dishName = (String) inputStream.readObject();
                    String username = (String) inputStream.readObject();
                    float rating = (float) inputStream.readObject();
                    state.addRating(diningOptionName, dishName, username,rating);
                    System.out.println("AddRating: Added rating to server");
                    break;

                case "authenticate":
                    String usernameAuthenticate = (String) inputStream.readObject();
                    String passwordAuthenticate = (String) inputStream.readObject();
                    boolean isAuthenticated = this.state.authenticate(usernameAuthenticate, passwordAuthenticate);
                    outputStream.writeObject(isAuthenticated);
                    System.out.println("Authenticate: Authenticated client in the server");
                    break;


                case "createAccount":
                    String newUsername = (String) inputStream.readObject();
                    String newPassword = (String) inputStream.readObject();
                    boolean isCreated = this.state.createAccount(newUsername, newPassword);
                    System.out.println("CreateAccount: Created account added to server");
                    outputStream.writeObject(isCreated);
                    break;

                case "joiningQueue":
                    String usernameIn = (String) inputStream.readObject();
                    String beaconNameIn = (String) inputStream.readObject();
                    Long timestampIn = (Long) inputStream.readObject();
                    this.state.joinQueue(usernameIn, beaconNameIn, timestampIn.longValue());
                    System.out.println("JoiningQueue: Client " + usernameIn + " joined a queue");
                    break;

                case "leavingQueue":
                    String usernameOut = (String) inputStream.readObject();
                    String beaconNameOut = (String) inputStream.readObject();
                    Long timestampOut = (Long) inputStream.readObject();
                    this.state.leaveQueue(usernameOut, beaconNameOut, timestampOut.longValue());
                    System.out.println("LeavingQueue: Client " + usernameOut + " left a queue");
                    break;
            }

        } catch (Exception ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
