package com.example.adaptingbackend;

import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.os.Handler;


public class DoorServo extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    //98:D3:31:90:82:9A
    private final String DEVICE_ADDRESS = "00:14:03:05:59:BB"; //MAC Address of Bluetooth Module
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private BluetoothDevice device;
    private BluetoothSocket socket;

    private OutputStream outputStream;
    private InputStream inputStream;
    private DrawerLayout drawer;

    Thread thread;
    byte buffer[];

    boolean stopThread;
    boolean connected = false;
    String command;

    Button lock_state_btn, bluetooth_connect_btn;

    TextView lock_state_text;

    ImageView lock_state_img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_door_servo);

        Toolbar toolbar = findViewById(R.id.toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                com.example.adaptingbackend.DoorServo.this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(com.example.adaptingbackend.DoorServo.this);

        lock_state_btn = (Button) findViewById(R.id.lock_state_btn);
        bluetooth_connect_btn = (Button) findViewById(R.id.bluetooth_connect_btn);

        lock_state_text = (TextView) findViewById(R.id.lock_state_text);

        lock_state_img = (ImageView) findViewById(R.id.lock_state_img);

        bluetooth_connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){

                if(BTinit())
                {
                    BTconnect();
                    beginListenForData();

                    // The code below sends the number 3 to the Arduino asking it to send the current state of the door lock so the lock state icon can be updated accordingly

                    command = "3";

                    try
                    {
                        outputStream.write(command.getBytes());
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                }
            }
        });

        lock_state_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){

                if(connected == false)
                {
                    Toast.makeText(getApplicationContext(), "Please establish a connection with the bluetooth servo door lock first", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    command = "1";

                    try
                    {
                        Toast.makeText(getApplicationContext(), "Sending Message to Arduino Board", Toast.LENGTH_SHORT).show();
                        outputStream.write(command.getBytes()); // Sends the number 1 to the Arduino. For a detailed look at how the resulting command is handled, please see the Arduino Source Code
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    void beginListenForData() // begins listening for any incoming data from the Arduino
    {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];

        Thread thread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        int byteCount = inputStream.available();

                        if(byteCount > 0)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string = new String(rawBytes, "UTF-8");

                            handler.post(new Runnable()
                            {
                                public void run()
                                {
                                    if(string.equals("3"))
                                    {
                                        lock_state_text.setText("Lock State: UNLOCKED");
                                        lock_state_img.setImageResource(R.drawable.unlocked_icon);

                                    }
                                    else if(string.equals("4"))
                                    {
                                        lock_state_text.setText("Lock State: LOCKED");
                                        lock_state_img.setImageResource(R.drawable.locked_icon);
                                    }
                                }
                            });
                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }

    //Initializes bluetooth module
    public boolean BTinit()
    {
        boolean found = false;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null) //Checks if the device supports bluetooth
        {
            Toast.makeText(getApplicationContext(), "Device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
        }

        if(!bluetoothAdapter.isEnabled()) //Checks if bluetooth is enabled. If not, the program will ask permission from the user to enable it
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter,0);

            try
            {
                Thread.sleep(1000);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        if(bondedDevices.isEmpty()) //Checks for paired bluetooth devices
        {
            Toast.makeText(getApplicationContext(), "Please pair the device first", Toast.LENGTH_SHORT).show();
        }
        else
        {
            for(BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(DEVICE_ADDRESS))
                {
                    device = iterator;
                    found = true;
                    break;
                }
            }
        }

        return found;
    }

    public boolean BTconnect()
    {

        try
        {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID); //Creates a socket to handle the outgoing connection
            socket.connect();

            Toast.makeText(getApplicationContext(),
                    "Connection to bluetooth device successful", Toast.LENGTH_LONG).show();
            connected = true;
        }
        catch(IOException e)
        {
            e.printStackTrace();
            connected = false;
        }

        if(connected)
        {
            try
            {
                outputStream = socket.getOutputStream(); //gets the output stream of the socket
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            try
            {
                inputStream = socket.getInputStream(); //gets the input stream of the socket
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return connected;
    }

    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

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
            String email =SharedPrefManager.getEmail(this);
            Toast.makeText(this, "Logging out now  user "+email, Toast.LENGTH_LONG).show();
            SharedPrefManager.saveEmail("",this);
            SharedPrefManager.saveOrderID("",this);
            SharedPrefManager.saveUserID("",this);
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
        }


        return false;
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
