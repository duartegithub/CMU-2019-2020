package pt.ulisboa.tecnico.cmov.foodist;

import android.app.Application;

public class GlobalState extends Application {

    private String username;
    private String password;
    private boolean logedIn;
    private String[] categories;
    private int actualCategory;

    public GlobalState(){
        this.categories = new String[] {"Student", "Researcher", "Professor", "Staff", "General Public"};
        this.actualCategory = 0;
        this.logedIn = false;
        // TO DO fetch everything from MySQLite database
    }

    public void login(String username, String password){
        // TO DO authentification
        this.username = username;
        this.password = password;
        this.logedIn = true;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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

    public boolean isLogedIn(){
        return this.logedIn;
    }
}
