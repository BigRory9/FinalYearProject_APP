package com.example.adaptingbackend;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3Object;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ViewTickets extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private final String accessKey = "AKIAJVL5I336SYABBB4A";
    private final String secretKey = "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";
    private LinearLayout mparent;
    private ArrayList<Ticket> ticketList = new ArrayList<Ticket>();
    private LayoutInflater layoutInflater;
    private RecyclerView recyclerView;
    private String id;
    private DrawerLayout drawer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tickets);
        id = SharedPrefManager.getUserID(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mparent = findViewById(R.id.mparent);
        layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                ViewTickets.this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(ViewTickets.this);

        new ViewTickets.JSONBackgroundWorker().execute();

    }

    public void startUp() {
        ArrayList<Ticket> validTickets = new ArrayList<Ticket>();
        try {
            Date todayDate = new Date();
            for (int i = 0; i < ticketList.size(); i++) {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
                String ticketDate = ticketList.get(i).getDate();
                Date date = inputFormat.parse(ticketDate);
                todayDate = inputFormat.parse(inputFormat.format(new Date()));
                System.out.println(date);
                System.out.println(todayDate);
                if (todayDate.before(date) || todayDate.equals(date)) {
                    validTickets.add(ticketList.get(i));
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        TicketAdapter adapter = new TicketAdapter(this, validTickets);

        //setting adapter to recyclerview
        recyclerView.setAdapter(adapter);
    }

    public void downloadPDF(View view) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    AWSCredentials creden = new BasicAWSCredentials("AKIAI5BANVNXM3EHHWMQ",
                            "vVsj1Kd+iQ0LKyOgSuS5PVM8vJ00fdGMll1jCc6r");
                    AmazonS3Client s3Client = new AmazonS3Client(creden);
                    S3Object object = s3Client.getObject(new GetObjectRequest(
                            "tickets-images-fare", "Image Number 161"));

                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            object.getObjectContent()));
                    Writer writer = null;


                    while (true) {
                        String line = reader.readLine();
                        if (line == null)
                            break;
                        writer.write(line + "\n");
                    }
                    writer.flush();
                    writer.close();
                    reader.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();


    }



    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        String email = SharedPrefManager.getEmail(this);

        if (id == R.id.nav_first_layout) {
            Intent i = new Intent(this, MainShop.class);
            startActivity(i);
        }
        else if (id == R.id.nav_view_orders) {
            Toast.makeText(this, "Attempting to view your Orders....",
                    Toast.LENGTH_LONG).show();
            Intent i = new Intent(this, ViewOrders.class);
            startActivity(i);
        }else if (id == R.id.nav_second_layout) {
            Toast.makeText(this, "Attempting to view your Tickets....",
                    Toast.LENGTH_LONG).show();
            Intent i = new Intent(this, ViewTickets.class);
            startActivity(i);
        }
        else if (id == R.id.nav_view_map) {
            Toast.makeText(this, "Attempting to view the Map...",
                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
        }else if (id == R.id.logout) {
            Toast.makeText(this, "Logging out now  user "+email, Toast.LENGTH_LONG).show();
            SharedPrefManager.saveEmail("",this);
            SharedPrefManager.saveOrderID("",this);
            SharedPrefManager.saveUserID("",this);
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
        }


        return false;
    }

    public ArrayList<Ticket> parseJSON(String JSON_STRING) {


        try {
            JSONObject jsonObject = new JSONObject(JSON_STRING);
            JSONArray jsonArray = jsonObject.getJSONArray("server_response");
            int count = 0;
            String id, arena, date, name, time = "", value;
            boolean type = true;
            while (count < jsonArray.length()) {
                JSONObject JO = jsonArray.getJSONObject(count);
                id = JO.getString("id");
                name = JO.getString("name");
                arena = JO.getString("arena");
                date = JO.getString("date");
                value = JO.getString("price");

                double price = Double.parseDouble(value);
                date = date.replace("\"", "");
                Ticket ticket = new Ticket(id, name, arena, date, price, time,"","");
                ticketList.add(ticket);

                count++;
            }
            startUp();
            return ticketList;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public class JSONBackgroundWorker extends AsyncTask<Void, Void, String> {
        String JSON_URL;
        String JSON_STRING;

        @Override
        protected void onPreExecute() {
           JSON_URL = "http://192.168.1.120//viewUsersTickets.php?id=" + id;
           // JSON_URL = "http://147.252.148.84//viewUsersTickets.php?id=" + id;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                URL url = new URL(JSON_URL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                InputStream inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();

                while ((JSON_STRING = bufferedReader.readLine()) != null) {
                    stringBuilder.append(JSON_STRING + "\n");
                }
                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();
                JSON_STRING = stringBuilder.toString().trim();
                return stringBuilder.toString().trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return " ERROR";
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {

            ticketList = parseJSON(result);

        }
    }


}