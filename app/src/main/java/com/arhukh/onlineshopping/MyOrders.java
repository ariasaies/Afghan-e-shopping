package com.arhukh.onlineshopping;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class MyOrders extends AppCompatActivity {
    private ProductClient client;
    private Helper helper;
    private Button btnRetry;
    private TextView txtNoInternet;
    private RecyclerView recyclerView;
    private String pId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_orders);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // initializing views
        btnRetry = (Button) findViewById(R.id.btn_retry);
        txtNoInternet = (TextView) findViewById(R.id.no_internet);
        recyclerView = (RecyclerView) findViewById(R.id.recycler);

        // helper class
        helper = new Helper();

        // checking the internet connection
        if (!helper.isNetworkAvailable(this)) {
            helper.toast(this, "No Internet Connection");
            recyclerView.setVisibility(View.GONE);
            btnRetry.setVisibility(View.VISIBLE);
            txtNoInternet.setVisibility(View.VISIBLE);
            btnRetry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MyOrders.this, MyOrders.class);
                    finish();
                    startActivity(intent);
                }
            });
        } else {
            // show progress bar
            helper.showProgressBar(this, "Loading...");
            // calling method fetchProducts()
            fetchProducts();
        }
    }

    private void fetchProducts() {

        client = new ProductClient();
        client.myOrders(helper.getToken(MyOrders.this), new JsonHttpResponseHandler() {
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("response", response.toString());

                helper.hideProgressDialog();

                try {
                    JSONArray products = response.getJSONArray("orders");
                    final ArrayList<Product> names = new ArrayList<Product>();
                    for(int i=0; i<products.length(); i++){
                        JSONObject inner = products.getJSONObject(i);
                        String id = inner.getString("id");
                        String name = inner.getString("title");
                        String image = inner.getString("imagePath");
                        String price = inner.getString("price");
                        String description = inner.getString("description");
                        names.add(new Product(id, name, image, price, description));
                    }

                    final RecyclerAdapter adapter = new RecyclerAdapter(MyOrders.this, names);
                    recyclerView.setAdapter(adapter);

                    LinearLayoutManager gridLayoutManager = new LinearLayoutManager(MyOrders.this);
                    gridLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                    recyclerView.setLayoutManager(gridLayoutManager);
                    recyclerView.setItemAnimator(new DefaultItemAnimator());

                    recyclerView.addOnItemTouchListener(
                            new RecyclerItemClickListener(MyOrders.this, recyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
                                @Override public void onItemClick(View view, int position) {

                                }
                                @Override public void onLongItemClick(View view, int position) {
                                    registerForContextMenu(view);
                                    pId = names.get(position).getId();
                                }
                            })
                    );


                } catch (JSONException e) {
                    // Invalid JSON format, show appropriate error.
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.d("responsemessage",throwable.toString());

                helper.hideProgressDialog();
                recyclerView.setVisibility(View.GONE);
                btnRetry.setVisibility(View.VISIBLE);
                txtNoInternet.setVisibility(View.VISIBLE);
                txtNoInternet.setText("Unable to connect to the server");
                btnRetry.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MyOrders.this, MyOrders.class);
                        finish();
                        startActivity(intent);
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);

                helper.hideProgressDialog();
                recyclerView.setVisibility(View.GONE);
                btnRetry.setVisibility(View.VISIBLE);
                txtNoInternet.setVisibility(View.VISIBLE);
                txtNoInternet.setText("Unable to connect to the server");
                btnRetry.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MyOrders.this, MyOrders.class);
                        finish();
                        startActivity(intent);
                    }
                });
            }
        });
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.cancel:
                Log.d("product_id", pId);
                client.cancelOrder(helper.getToken(getApplicationContext()), pId, new JsonHttpResponseHandler(){
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        super.onSuccess(statusCode, headers, response);
                        try {
                            helper.toast(getApplicationContext(), response.getString("message"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        fetchProducts();
                    }
                });
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
