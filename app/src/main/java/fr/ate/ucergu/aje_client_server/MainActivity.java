package fr.ate.ucergu.aje_client_server;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;
import android.widget.VideoView;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView mTextMessage,status;
    Button listen,send,listeDevices,downloadVideo;
    ListView listView;
    VideoView vidView;
    EditText writeMsg;
    DownloadManager downloadManager;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    ArrayList<BluetoothDevice> btArray = new ArrayList<>();
    SendRecive sendRecive;
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECIVED=5;

    int REQUEST_ENABLE_BLUETOOTH=1;

    private static final String APP_NAME = "AJE_Client_Server";
    private static final UUID MY_UUID= UUID.fromString("b219b08d-18a2-4a23-8bfe-bd0ea8ec8b81");

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //request for user de enable th bluetooth
      if(!bluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);
          Toast.makeText(MainActivity.this, "Bluetooth ACTIVATED !",
                  Toast.LENGTH_LONG).show();
        }
        //donwload the Video
        downloadVideo = (Button) findViewById(R.id.downloadVideo);
        downloadVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                Uri uri = Uri.parse("https://ia800201.us.archive.org/22/items/ksnn_compilation_master_the_internet/ksnn_compilation_master_the_internet_512kb.mp4");
                DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                Long reference = downloadManager.enqueue(request);

            }
        });
        implementListners();
        vidView = findViewById(R.id.video);
        MediaController vidControl = new MediaController(this);
        // Set it to use the VideoView instance as its anchor.
        vidControl.setAnchorView(vidView);
        // Set it as the media controller for the VideoView object.
        vidView.setMediaController(vidControl);

        // Prepare the URI for the endpoint.
        String vidAddress = "android.resource://" + getPackageName() + "/" + R.raw.video;
        Uri vidUri = Uri.parse(vidAddress);
        // Parse the address string as a URI so that we can pass it to the VideoView object.
        vidView.setVideoURI(vidUri);
        // Start playback.
        vidView.start();


    }

    //method to find all bluetooth devices
    private void implementListners(){
        listView = findViewById(R.id.btList);
        //Find and show Devices
        listeDevices = findViewById(R.id.listDevices);
        listeDevices.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                List<String> deviceName = new ArrayList<String>();
                for(BluetoothDevice bt : pairedDevices) {
                    deviceName.add(bt.getName());
                    btArray.add(bt);
                }
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                        MainActivity.this,android.R.layout.simple_list_item_1,deviceName);

                //show devices
                listView.setAdapter(arrayAdapter);

            }
        });
        listen = findViewById(R.id.listen);
        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();

        }
        });

    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            status = findViewById(R.id.status);
            ClientClass clientClass = new ClientClass(btArray.get(position));
            clientClass.start();
            status.setText("Connecting...");
        }
    });
    send = findViewById(R.id.send);
    send.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            writeMsg = findViewById(R.id.textSend);
            String string = String.valueOf(writeMsg.getText());
            sendRecive.write(string.getBytes());
        }
    });
    }

    Handler handler  = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            status = findViewById(R.id.status);
            switch (msg.what) {
                case STATE_LISTENING:
                    status.setText("Listning");
                   // Log.e("MESSAGE", "LISTNING");
                    break;

                case STATE_CONNECTING:
                    status.setText("Connecting");
                    //Log.e("MESSAGE", "CONNECTING");
                    break;

                case STATE_CONNECTED:
                    status.setText("Connected");
                    //Log.e("MESSAGE", "CONNECTED");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection failed");
                    //Log.e("MESSAGE", "CONNECTION FAILD");
                    break;
                case STATE_MESSAGE_RECIVED:
                    //Log.e("MESSAGE", "MESSAGE RECIVED");
                    status.setText("Message recived");
                    byte[] readBuff=(byte[])msg.obj;
                    String tempMesg = new String(readBuff,0,msg.arg1);
                    mTextMessage = findViewById(R.id.viewMessage);
                    mTextMessage.setText(tempMesg);

                    break;
            }
            return  true;
        }
    });

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



      /////////////////////////SERVER CLASS/////////////////////////////

    private  class ServerClass extends Thread{
        private BluetoothServerSocket serverSocket;

        public ServerClass(){
            try {
                serverSocket=bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            BluetoothSocket socket = null;
            while(socket==null){
                try {
                    Message message = Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket=serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);

                }
                if(socket!=null){
                    Message message = Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);
                    sendRecive = new SendRecive(socket);
                    sendRecive.start();
                    break;
                }
            }
        }
    }

    private class ClientClass extends Thread{
        private BluetoothDevice device;
        private  BluetoothSocket socket;

        public  ClientClass(BluetoothDevice device1){
            device=device1;

            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){

            try {
                socket.connect();
                Message message = Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);
                sendRecive = new SendRecive(socket);
                sendRecive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }
    private class SendRecive extends Thread{

       private final BluetoothSocket bluetoothSocket;
       private final InputStream inputStream;
       private final OutputStream outputStream;

       public  SendRecive(BluetoothSocket socket){
           bluetoothSocket = socket;
           InputStream tempIn = null;
           OutputStream tempOut = null;

           try {
               tempIn=bluetoothSocket.getInputStream();
           } catch (IOException e) {
               e.printStackTrace();
           }
           try {
               tempOut=bluetoothSocket.getOutputStream();
           } catch (IOException e) {
               e.printStackTrace();
           }
            inputStream = tempIn;
           outputStream = tempOut;

       }

       public  void run(){
           byte[] buffer = new byte[1024];
           int bytes;

           while (true){
               try {
                   bytes=inputStream.read(buffer);
                   handler.obtainMessage(STATE_MESSAGE_RECIVED,bytes,-1,buffer).sendToTarget();
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
       }
       public void write(byte[] bytes){
           try {

               outputStream.write(bytes);
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
    }
}
