package pt.ulisboa.tecnico.cmov.library;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class DiningPlace implements Serializable {

    private String name;
    private String address;
    private String campus;

    private byte[] thumbnail;
    private ArrayList<Dish> dishes;
    private String[] schedule;
    private String queueTime;


    public DiningPlace(String name, String address, Bitmap thumbnail, String[] schedule, String campus){
        this.name = name;
        this.address = address;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, stream);
        this.thumbnail = stream.toByteArray();

        this.dishes = new ArrayList<>();
        this.schedule = schedule;
        this.queueTime = "0";
        this.campus = campus;
    }

    public String getName() {
        return this.name;
    }

    public String getAddress() {
        return this.address;
    }

    public Bitmap getThumbnail(){ return BitmapFactory.decodeByteArray(this.thumbnail, 0, this.thumbnail.length); }
    public ArrayList<Dish> getDishes() { return this.dishes; }

    public Dish getDish(String dishName){

        for (Dish dish: this.dishes){
            if (dish.getName().equals(dishName)){
                return dish;
            }
        }

        return null;
    }

    public String getSchedule(int category) { return this.schedule[category]; }

    public String getQueueTime() { return this.queueTime; }

    public String getCampus() { return this.campus; }

    public void addDish(Dish dish){
        dish.setDiningPlace(this.name);
        this.dishes.add(dish);
    }
}