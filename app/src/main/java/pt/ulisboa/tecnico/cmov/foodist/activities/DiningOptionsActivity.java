package pt.ulisboa.tecnico.cmov.foodist.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;

import pt.ulisboa.tecnico.cmov.library.DiningPlace;
import pt.ulisboa.tecnico.cmov.foodist.adapters.DiningOptionAdapter;
import pt.ulisboa.tecnico.cmov.foodist.GlobalState;
import pt.ulisboa.tecnico.cmov.foodist.R;

public class DiningOptionsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private GlobalState globalState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dining_options);

        this.setTitle("FoodIST - Campus");
        this.globalState = (GlobalState) getApplication();

        Spinner spinner = findViewById(R.id.chooseCampusSpinner);

        String[] campuses = this.globalState.getCampuses();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, campuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setSelection(0);
        spinner.setOnItemSelectedListener(this);

        ListView listOfDiningPlaces = (ListView) findViewById(R.id.listOfDiningPlaces);
        listOfDiningPlaces.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                DiningPlace item = (DiningPlace) parent.getItemAtPosition(position);
                Intent intent = new Intent(DiningOptionsActivity.this, DiningPlaceActivity.class);
                intent.putExtra("diningOptionName", item.getName());
                intent.putExtra("campus", item.getCampus());
                startActivity(intent);
            }

        });

        populateActivity(spinner.getSelectedItem().toString());

    }

    public void optionsButtonOnClick(View view) {

        Intent intent = new Intent(DiningOptionsActivity.this, UserProfileActivity.class);
        startActivity(intent);
    }

    public void populateActivity(String campus){

        ListView listOfDiningPlaces = (ListView) findViewById(R.id.listOfDiningPlaces);
        ArrayList<DiningPlace> diningPlaces = this.globalState.getDiningOptions(campus);

        DiningOptionAdapter diningOptionAdapter = new DiningOptionAdapter(getApplicationContext(), R.layout.list_row_dining_option, diningPlaces);
        listOfDiningPlaces.setAdapter(diningOptionAdapter);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        populateActivity(parent.getSelectedItem().toString());

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}