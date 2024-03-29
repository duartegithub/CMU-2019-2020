package pt.ulisboa.tecnico.cmov.foodist;

import android.app.Application;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.Messenger;
import android.widget.Toast;

import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import pt.inesc.termite.wifidirect.SimWifiP2pBroadcast;
import pt.inesc.termite.wifidirect.SimWifiP2pManager;
import pt.inesc.termite.wifidirect.service.SimWifiP2pService;

import pt.ulisboa.tecnico.cmov.foodist.asynctasks.ClientAuthenticator;
import pt.ulisboa.tecnico.cmov.foodist.asynctasks.CreateAccount;
import pt.ulisboa.tecnico.cmov.foodist.asynctasks.GetWalkTime;
import pt.ulisboa.tecnico.cmov.foodist.asynctasks.StateLoader;
import pt.ulisboa.tecnico.cmov.library.DiningPlace;
import pt.ulisboa.tecnico.cmov.library.Dish;
import pt.ulisboa.tecnico.cmov.library.DishImage;
import pt.ulisboa.tecnico.cmov.library.DishesView;

public class GlobalState extends Application {

    private String username;
    private String password;
    private Bitmap profilePicture;
    private String[] campuses;
    private Map<String,String> campusCoordinates;
    private boolean loggedIn;
    private String[] categories;
    private Map<String, ArrayList<DiningPlace>> diningOptions;
    private int actualCategory;
    private String userCoordinates;

    private Map<String, Boolean> preferences;

    private SimWifiP2pManager mManager = null;
    private SimWifiP2pManager.Channel mChannel = null;
    private boolean mBound = false;
    private SimWifiP2pBroadcastReceiver mReceiver;
    private String lastKnownBeacon;

    private boolean shouldSeeWarning;

    private Cache cache;

    public GlobalState(){
        this.categories = new String[] {"Student", "Researcher", "Professor", "Staff", "General Public"};
        this.actualCategory = 0;
        this.loggedIn = false;

        this.preferences = new HashMap<>();
        this.preferences.put("Vegetarian", true);
        this.preferences.put("Vegan", true);
        this.preferences.put("Fish", true);
        this.preferences.put("Meat", true);

        this.shouldSeeWarning = true;

        this.cache = new Cache();
    }

    public void login(String username, String password) {
        this.username = username;
        this.password = password;

        ClientAuthenticator clientAuthenticator = new ClientAuthenticator(this);

        try{
            this.loggedIn = (boolean) clientAuthenticator.execute().get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(this.diningOptions == null) {
            populate();

            StateLoader stateLoader = new StateLoader(this);
            stateLoader.execute();
        }

        GetWalkTime getWalkTime = new GetWalkTime(this);
        getWalkTime.execute();
        prepareWiFiDirect();

        this.shouldSeeWarning = true;

    }

    public void logWithoutAccount(){

        if(this.diningOptions == null) {
            populate();

            StateLoader stateLoader = new StateLoader(this);
            stateLoader.execute();
        }

        prepareWiFiDirect();

        this.shouldSeeWarning = false;
    }

    public synchronized void setState(ArrayList<DishesView> dishesViews){
        if (dishesViews != null){
            for(DishesView dishesView: dishesViews){
                DiningPlace diningPlace = getDiningOption(dishesView.getCampus(), dishesView.getDiningPlace());
                diningPlace.setDishes(dishesView.getDishes());
                diningPlace.setQueueTime(dishesView.getQueueTime());
            }
        }
    }

    public boolean createAccount(String username, String password){
        CreateAccount createAccount = new CreateAccount(username, password);
        boolean isCreated = false;

        try{
             isCreated = (boolean) createAccount.execute().get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isCreated){
            this.username = username;
            this.password = password;
            this.loggedIn = true;
            this.preferences.put("Vegetarian", true);
            this.preferences.put("Vegan", true);
            this.preferences.put("Fish", true);
            this.preferences.put("Meat", true);

        } else {
            Toast.makeText(getApplicationContext(), "That username is already taken...", Toast.LENGTH_LONG).show();
        }

        return isCreated;
    }

    public void setUsername(String username) { this.username = username; }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setActualCategory(String actualCategory){

        for(int i=0; i<categories.length; i++){
            if (categories[i] == actualCategory) {
                this.actualCategory = i;
            }
        }
    }

    public void setUserCoordinates(String location){
        System.out.println(location);
        this.userCoordinates = location;
    }

    public String getUserCoordinates(){
        return this.userCoordinates;
    }

    public int getNearestCampus(){

        while(userCoordinates == null){}

        double userLat = Double.parseDouble(this.userCoordinates.split(",")[0]);
        double userLong = Double.parseDouble(this.userCoordinates.split(",")[1]);

        double alamedaLat = Double.parseDouble(this.campusCoordinates.get("Alameda").split(",")[0]);
        double alamedaLong = Double.parseDouble(this.campusCoordinates.get("Alameda").split(",")[1]);

        double tagusLat = Double.parseDouble(this.campusCoordinates.get("Taguspark").split(",")[0]);
        double tagusLong = Double.parseDouble(this.campusCoordinates.get("Taguspark").split(",")[1]);

        double ctnLat = Double.parseDouble(this.campusCoordinates.get("CTN").split(",")[0]);
        double ctnLong = Double.parseDouble(this.campusCoordinates.get("CTN").split(",")[1]);

        double distAlameda = distance(userLat, alamedaLat, userLong, alamedaLong);
        double distTagus = distance(userLat, tagusLat, userLong, tagusLong);
        double distCTN = distance(userLat, ctnLat, userLong, ctnLong);

        if(distAlameda >= 8 && distTagus >= 8 && distCTN >= 8){
            return 3;
        }

        if(distCTN > distTagus && distCTN > distAlameda){
            return 2;
        }else if(distAlameda > distTagus && distAlameda > distCTN){
            return 1;
        }else{
            return 0;
        }

    }

    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2) {

        final int R = 6371;

        Double latDistance = (lat2-lat1) * Math.PI / 180;
        Double lonDistance = (lon2-lon1) * Math.PI / 180;
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    public void prepareWiFiDirect(){

        // LIKE IN LAB 4: register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_NETWORK_MEMBERSHIP_CHANGED_ACTION);
        filter.addAction(SimWifiP2pBroadcast.WIFI_P2P_GROUP_OWNERSHIP_CHANGED_ACTION);
        setmReceiver(new SimWifiP2pBroadcastReceiver(this));
        registerReceiver(mReceiver, filter);

        // LIKE IN LAB 4: Bind WiFi Direct
        Intent intent = new Intent(getBaseContext(), SimWifiP2pService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;

    }

    public void setNotificationChannel(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "FoodISTChannel";
            String description = "Channel for FoodIST notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("FoodIST", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public SimWifiP2pBroadcastReceiver getMReceiver(){ return this.mReceiver; }

    public SimWifiP2pManager getMManager(){ return this.mManager; }

    public SimWifiP2pManager.Channel getMChannel(){ return this.mChannel; }

    public ServiceConnection getMConnection(){ return this.mConnection; }

    public boolean getMbound(){ return this.mBound; }

    public boolean setMBound(boolean mBound){ return this.mBound = mBound; }

    public void setmReceiver(SimWifiP2pBroadcastReceiver mReceiver){ this.mReceiver = mReceiver;}

    public String getLastKnownBeacon(){ return this.lastKnownBeacon; }

    public void setLastKnownBeacon(String lastKnownBeacon){ this.lastKnownBeacon = lastKnownBeacon; }

    private ServiceConnection mConnection = new ServiceConnection() {
        // LIKE IN LAB 4: callbacks for service binding, passed to bindService()

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mManager = new SimWifiP2pManager(new Messenger(service));
            mChannel = mManager.initialize(getApplicationContext(), getMainLooper(), null);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mManager = null;
            mChannel = null;
            mBound = false;
        }
    };

    public DiningPlace getDiningOption(String campus, String diningOptionName){

        for (DiningPlace diningPlace : this.diningOptions.get(campus)) {
            if(diningPlace.getName().equals(diningOptionName)){
                return diningPlace;
            }
        }

        return null;
    }

    public int getDiningOptionIndex(String campus, String diningOptionName){
        int index = 0;

        for (DiningPlace diningPlace : this.diningOptions.get(campus)) {
            if(diningPlace.getName().equals(diningOptionName)){
                return index;
            }
            index++;
        }

        return index;
    }

    public Dish getDish(String campus, String diningOptionName, String dishName){

        for (DiningPlace diningPlace : this.diningOptions.get(campus)) {
            if(diningPlace.getName().equals(diningOptionName)){
                for (Dish dish: diningPlace.getDishes()) {
                    if(dish.getName().equals(dishName)){
                        return dish;
                    }
                }
            }
        }

        return null;
    }

    public int getDishIndex(String campus, String diningOptionName, String dishName){
        int index = 0;

        for (DiningPlace diningPlace : this.diningOptions.get(campus)) {
            if(diningPlace.getName().equals(diningOptionName)){
                for (Dish dish: diningPlace.getDishes()) {
                    if(dish.getName().equals(dishName)){
                        return index;
                    }
                    index++;
                }
            }
        }

        return index;
    }

    public int getDishBasedOnPreferenceIndex(String campus, String diningOptionName, String dishName){
        int index = 0;

        for (DiningPlace diningPlace : this.diningOptions.get(campus)) {
            if(diningPlace.getName().equals(diningOptionName)){
                for (Dish dish: diningPlace.getDishesBasedOnPreference(this.preferences)) {
                    if(dish.getName().equals(dishName)){
                        return index;
                    }
                    index++;
                }
            }
        }

        return index;
    }

    public String[] getDiningOptionNames(String campus){
        String[] result = new String[diningOptions.get(campus).size()];
        int index = 0;

        for(DiningPlace diningPlace : this.diningOptions.get(campus)) {
            result[index] = diningPlace.getName();
            index++;
        }

        return result;
    }

    public String[] getDishNames(DiningPlace diningPlace){
        String[] result = new String[diningPlace.getDishes().size()];
        int index = 0;

        for(Dish dish: diningPlace.getDishes()) {
            result[index] = dish.getName();
            index++;
        }

        return result;
    }

    public String[] getDishNamesByPreference(DiningPlace diningPlace){
        String[] result = new String[diningPlace.getDishesBasedOnPreference(this.preferences).size()];
        int index = 0;

        for(Dish dish: diningPlace.getDishesBasedOnPreference(this.preferences)) {
            result[index] = dish.getName();
            index++;
        }

        return result;
    }

    public String[] getCategories(){
        return this.categories;
    }

    public String getUsername(){
        return this.username;
    }

    public String getPassword(){
        return this.password;
    }

    public int getActualCategoryIndex(){
        return this.actualCategory;
    }

    public String getActualCategory(){
        return this.categories[this.actualCategory];
    }

    public Map<String, Boolean> getPreferences() {
        return this.preferences;
    }

    public void setPreferences(Map<String, Boolean> preferences){
        this.preferences = preferences;
    }

    public boolean getShouldSeeWarning(){

        if(!this.preferences.get("Vegetarian") || !this.preferences.get("Vegan") || !this.preferences.get("Meat") || !this.preferences.get("Fish")){
            return this.shouldSeeWarning;
        }

        return false;
    }

    public void changeShouldSeeWarning(boolean shouldSeeWarning){
        this.shouldSeeWarning = shouldSeeWarning;
    }

    public ArrayList<DiningPlace> getDiningOptions(String campus) { return this.diningOptions.get(campus); }

    public DishImage getDishImage(String campus, String diningOptionName, String dishName, int imageId){
        Dish dish = getDish(campus, diningOptionName, dishName);
        return dish.getDishImage(imageId);
    }

    public Bitmap getProfilePicture() { return this.profilePicture; }

    public String[] getCampuses(){ return this.campuses; }

    public void setProfilePicture(Bitmap profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void addDiningOption(DiningPlace diningPlace){
        this.diningOptions.get(diningPlace.getCampus()).add(diningPlace);
    }

    public void addDish(String campus, String diningOptionName, Dish dish){
        getDiningOption(campus, diningOptionName).addDish(dish);
    }

    public void addDishImage(Dish dish, DishImage newDishImage){
        dish.addImage(newDishImage);
    }

    public Cache getCache(){
        return this.cache;
    }

    public boolean isLoggedIn(){
        return this.loggedIn;
    }

    public Bitmap customBitMapper(int imageId) {

        try {
            return BitmapFactory.decodeResource(getResources(), imageId);
        } catch (Exception e) {
            return null;
        }
    }

    public void setLoggedIn(boolean loggedIn){
        this.loggedIn = loggedIn;
    }

    public void populate(){
        //TO DO: TAKING TOO LONG, SOLVE.
        this.diningOptions = new HashMap<>();
        this.diningOptions.put("Alameda", new ArrayList<DiningPlace>());
        this.diningOptions.put("Taguspark", new ArrayList<DiningPlace>());
        this.diningOptions.put("CTN", new ArrayList<DiningPlace>());
        this.campuses = new String[] {"Alameda", "Taguspark", "CTN"};

        String[] schedule1 = new String[] {"9:00-17:00", "9:00-17:00", "9:00-17:00", "9:00-17:00", "9:00-17:00"};
        String[] schedule2 = new String[] {"12:00-15:00", "12:00-15:00", "12:00-15:00", "12:00-15:00", "12:00-15:00"};
        String[] schedule3 = new String[] {"8:00-19:00","8:00-19:00","8:00-19:00","8:00-19:00","8:00-19:00"};
        String[] schedule4 = new String[] {"8:00-22:00", "8:00-22:00", "8:00-22:00", "8:00-22:00", "8:00-22:00"};
        String[] schedule5 = new String[] {"7:00-19:00", "7:00-19:00", "7:00-19:00", "7:00-19:00", "7:00-19:00"};
        String[] schedule6 = new String[] {"9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00", "9:00-21:00"};
        String[] schedule7 = new String[] {"13:30-15:00", "12:00-15:00", "12:00-15:00", "12:00-15:00", "13:30-15:00"};
        String[] schedule8 = new String[] {"9:00-12:00, 14:00-17:00", "9:00-17:00", "9:00-17:00", "9:00-17:00", "9:00-12:00, 14:00-17:00"};
        String[] schedule9 = new String[] {"13:30-14:00", "12:00-14:00", "12:00-14:00", "12:00-14:00", "13:30-14:00"};
        String[] schedule10 = new String[] {"8:30-12:00, 15:30-16:30", "8:30-12:00, 15:30-16:30", "8:30-12:00, 15:30-16:30", "8:30-12:00, 15:30-16:30", "8:30-12:00, 15:30-16:30"};
        addDiningOption(new DiningPlace("Red Bar", "Av. Prof. Dr. Cavaco Silva 13", customBitMapper(R.drawable.redbar), schedule4, "Taguspark", 38.736578,-9.302192));
        addDiningOption(new DiningPlace("Green Bar", "Av. Prof. Dr. Cavaco Silva 13", customBitMapper(R.drawable.greenbar), schedule5, "Taguspark", 38.738004, -9.303058));
        addDiningOption(new DiningPlace("Tagus Cafeteria", "Av. Prof. Dr. Cavaco Silva 13", customBitMapper(R.drawable.bolo1), schedule2, "Taguspark", 38.737802, -9.303223));
        addDiningOption(new DiningPlace("Central Bar", "Av. Rovisco Pais 1", customBitMapper(R.drawable.bolo2), schedule1, "Alameda", 38.736606, -9.139532));
        addDiningOption(new DiningPlace("Civil Bar", "Av. Rovisco Pais 1", customBitMapper(R.drawable.bolo3), schedule1, "Alameda", 38.736988, -9.139955));
        addDiningOption(new DiningPlace("Sena - Pastelaria e Restaurante", "Av. Rovisco Pais 1", customBitMapper(R.drawable.sena), schedule3, "Alameda", 38.737677, -9.138672));
        addDiningOption(new DiningPlace("Civil Cafeteria", "Av. Rovisco Pais 1", customBitMapper(R.drawable.bolo4), schedule2, "Alameda", 38.737650, -9.140384));
        addDiningOption(new DiningPlace("Mechy Bar", "Av. Rovisco Pais 1", customBitMapper(R.drawable.bolo5), schedule1, "Alameda", 38.737247, -9.137434));
        addDiningOption(new DiningPlace("AEIST Bar", "Av. Rovisco Pais 1", customBitMapper(R.drawable.bolo6), schedule1, "Alameda", 	38.736542, -9.137226));
        addDiningOption(new DiningPlace("AEIST Esplanade", "Av. Rovisco Pais 1", customBitMapper(R.drawable.bolo7), schedule1, "Alameda", 38.736318, -9.137820));
        addDiningOption(new DiningPlace("Chemy Bar", "Av. Rovisco Pais 1", customBitMapper(R.drawable.bolo8), schedule1, "Alameda", 38.736240, -9.138302));
        addDiningOption(new DiningPlace("SAS Cafeteria", "Av. Rovisco Pais 1", customBitMapper(R.drawable.bolo9), schedule6, "Alameda", 38.736571, -9.137036));
        addDiningOption(new DiningPlace("Math Cafeteria", "Av. Rovisco Pais 1", customBitMapper(R.drawable.bolo10), schedule7, "Alameda", 38.735508, -9.139645));
        addDiningOption(new DiningPlace("Complex Bar", "Av. Rovisco Pais 1", customBitMapper(R.drawable.bolo12), schedule8, "Alameda", 38.736050, -9.140156));
        addDiningOption(new DiningPlace("CTN Cafeteria", "RW74+2F Bobadela", customBitMapper(R.drawable.bolo3), schedule9, "CTN", 38.812522, -9.093773));
        addDiningOption(new DiningPlace("CTN Bar", "RW74+2F Bobadela", customBitMapper(R.drawable.bolo6), schedule10, "CTN", 38.812522, -9.093773));

        this.campusCoordinates = new HashMap<>();
        this.campusCoordinates.put("Alameda","38.736796,-9.138670");
        this.campusCoordinates.put("Taguspark","38.737333,-9.302568");
        this.campusCoordinates.put("CTN", "38.812522,-9.093773");

    }

}
