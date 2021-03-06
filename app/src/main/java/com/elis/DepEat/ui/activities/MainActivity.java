package com.elis.DepEat.ui.activities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Explode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.elis.DepEat.R;
import com.elis.DepEat.backend.SharedPreferencesSettings;
import com.elis.DepEat.datamodels.Restaurant;
import com.elis.DepEat.services.RestController;
import com.elis.DepEat.ui.adapter.restaurantAdapter;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Response.Listener<String>, Response.ErrorListener{

    CoordinatorLayout mainCL;
    RecyclerView restaurantRv;
    FloatingActionButton changeVisualBtn;
    FloatingActionButton userBtn;
    FloatingActionButton cartBtn;
    RecyclerView.LayoutManager layoutManager;
    restaurantAdapter adapter;
    ArrayList<Restaurant> arrayList = new ArrayList<>();
    RestController restController;

    private boolean layoutController = false;
    private boolean isFABOpen;
    private final int LOGIN_REQUEST_CODE = 2001;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mainCL = findViewById(R.id.main_content);
        restaurantRv = findViewById(R.id.places_rw);
        changeVisualBtn = findViewById(R.id.changevisual_btn);
        userBtn = findViewById(R.id.user_btn);
        cartBtn = findViewById(R.id.checkout_btn);
        adapter = new restaurantAdapter(MainActivity.this, arrayList);
        checkLayout();
        restaurantRv.setAdapter(adapter);
        changeVisualBtn.setOnClickListener(this);
        userBtn.setOnClickListener(this);
        cartBtn.setOnClickListener(this);


        restaurantRv.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy){
                if (dy > 0){
                    userBtn.hide();
                    cartBtn.hide();
                    changeVisualBtn.hide();
                } else if (dy < 0) {
                    userBtn.show();
                    cartBtn.show();
                    changeVisualBtn.show();
                }
            }
        });

        restController = new RestController(this);
        restController.getRequest(Restaurant.ENDPOINT, this, this );


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.changevisual_btn) {

            if(layoutController){
                setLayoutManager();
                item.setIcon(R.drawable.ic_list_black_24dp);
            }else {
                setLayoutManager();
                item.setIcon(R.drawable.ic_grid_on_black_24dp);
            }
        }
        return true;
    }

    private void checkLayout(){
        layoutController = SharedPreferencesSettings.getBooleanFromPreferences(this, "layout");
        setLayoutManager();

    }

    private void setLayoutManager(){
        if(layoutController){
            adapter.setOrientation(1);
            layoutManager = new GridLayoutManager(this, 2);
            layoutController = false;
        } else {
            adapter.setOrientation(0);
            layoutManager = new LinearLayoutManager(this);
            layoutController = true;
        }
        SharedPreferencesSettings.setSharedPreferences(this,"layout", !layoutController);
        restaurantRv.setLayoutManager(layoutManager);
    }


    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.changevisual_btn){
            if(!isFABOpen){
                showFABMenu((FloatingActionButton) v);
            }else{
                closeFABMenu((FloatingActionButton)v);
            }
        } else if(v.getId() == R.id.user_btn){
            if(SharedPreferencesSettings.getBooleanFromPreferences(this,"loggedIn")){
                Intent intent = new Intent(this, AccountActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            }

        } else if(v.getId() == R.id.checkout_btn){
            if(SharedPreferencesSettings.getBooleanFromPreferences(this, "loggedIn")){
                Intent intent = new Intent(this, CheckoutActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivityForResult(intent, LOGIN_REQUEST_CODE);
                Toast.makeText(this,
                        R.string.login_check, Toast.LENGTH_LONG).show();
            }

        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(requestCode == LOGIN_REQUEST_CODE && resultCode == RESULT_OK){
            Intent intent = new Intent(this, CheckoutActivity.class);
            startActivity(intent);
        }
    }

    private void showFABMenu(FloatingActionButton v) {
        isFABOpen=true;
        v.setImageResource(R.drawable.ic_touch_app_black_24dp);
        userBtn.animate().translationY(-getResources().getDimension(R.dimen.standard_65)).setDuration(120);
        cartBtn.animate().translationY(-getResources().getDimension(R.dimen.standard_115)).setDuration(180);
        v.animate().rotation(90).setDuration(100);
        v.setImageResource(R.drawable.ic_close_black_24dp);

    }

    private void closeFABMenu(FloatingActionButton v){
        isFABOpen=false;
        v.setImageResource(R.drawable.ic_close_black_24dp);
        userBtn.animate().translationY(0).setDuration(120);
        cartBtn.animate().translationY(0).setDuration(180);
        v.setImageResource(R.drawable.ic_touch_app_black_24dp);
        v.animate().rotation(0).setDuration(100);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.e("RequestError",error.getMessage());
        Toast.makeText(this,error.getMessage(),Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResponse(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            for(int i = 0; i<jsonArray.length(); i++){
                arrayList.add(new Restaurant(jsonArray.getJSONObject(i)));
            }
            adapter.setData(arrayList);
        } catch (JSONException e) {
            Log.e("JSONError", e.getMessage());
        }

    }


}