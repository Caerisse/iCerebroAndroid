package com.icerebro.icerebro.ui.tunnel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.icerebro.icerebro.R;
import com.icerebro.icerebro.data.model.ProxyPort;
import com.icerebro.icerebro.data.model.PubKey;
import com.icerebro.icerebro.rest.ServiceGenerator;
import com.icerebro.icerebro.rest.iCerebroService;
import com.icerebro.icerebro.ssh.JSCHTunnel;
import com.jcraft.jsch.JSchException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class TunnelActivity extends AppCompatActivity {
    private String TAG = "TunnelActivity";

    private SharedPreferences SP;
    private iCerebroService client;

    private Monitor monitor;

    private TextView title;
    private ScrollView monitor_scroll_view;
    private TextView monitor_text;
    private Button button;
    private ProgressBar progressBar;

    private String user = "caerisse";
    private int ssh_port = 7101;
    private int local_port = 8080;
    private String host;
    private int remote_port;
    private JSCHTunnel tunnel;

    private ExecutorService executor;
    private boolean running = false;
    private boolean stopping = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tunnel);

        SP = getSharedPreferences("sp", MODE_PRIVATE);

        String authToken = SP.getString("authToken",null);
        client = ServiceGenerator.createService(authToken);

        title = findViewById(R.id.title);
        monitor_scroll_view = findViewById(R.id.monitor);
        monitor_text = findViewById(R.id.monitor_text);
        button = findViewById(R.id.start_stop);
        button.setOnClickListener(start_stop);
        progressBar = findViewById(R.id.loading);

        monitor = new Monitor();
    }

    private View.OnClickListener start_stop = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (running && !stopping) {
                monitor.writeToMonitor("Stopping");
                stop();
            } else if (!running) {
                monitor.writeToMonitor("Starting");
                start();
            }
        }
    };

    public class Monitor {
        public void writeToMonitor(String line){
            Log.d("writeToMonitor", line);
            synchronized (this) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Todo append and rotate text as in terminal output
                        String new_text = monitor_text.getText() + "\n" + line;
                        monitor_text.setText(new_text);
                        monitor_scroll_view.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        }

        public void writeTitle(String new_title){
            Log.d("writeTitle", new_title);
            synchronized (this) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        title.setText(new_title);
                    }
                });
            }
        }

        public void writeButton(String text){
            Log.d("writeButton", text);
            synchronized (this) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.setText(text);
                    }
                });
            }
        }

        public void setProgressBarVisibility(int visibility) {
            Log.d("setProgressBarVis", "" + visibility);
            synchronized (this) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(visibility);
                    }
                });
            }
        }
    }

    private void start() {
        if ( isNotPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE) || isNotPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) ){
            requestPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE});
        }
        running = true;
        progressBar.setVisibility(View.VISIBLE);
        monitor.writeTitle("Starting");
        monitor.writeToMonitor("Getting or generating public key");
        tunnel = new JSCHTunnel();
        tunnel.setRootDir("iCerebro");
        File[] keys = tunnel.generateAuthKeys();
        String pubKey = "";
        try (BufferedReader br = new BufferedReader(new FileReader(keys[1].getAbsolutePath()))) {
            StringBuilder contentBuilder = new StringBuilder();
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
            pubKey = contentBuilder.toString();
            savePubKey(pubKey);
        } catch (IOException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
    }

    private void stop() {
        progressBar.setVisibility(View.VISIBLE);
        running = false;
        stopping = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        stop();
    }

    private boolean isNotPermissionGranted(String permission) {
        //Getting the permission status
        int result = ContextCompat.checkSelfPermission(this, permission);

        //If permission is granted returning true else false
        return result != PackageManager.PERMISSION_GRANTED;
    }

    //Requesting permission
    private void requestPermission(String[] permissions){
        for (String permission: permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                //If the user has denied the permission previously your code will come to this block
                //Here you can explain why you need this permission
                //Explain here why you need this permission
            }
        }
        //And finally ask for the permission
        ActivityCompat.requestPermissions(this, permissions, 1);
    }

    //This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //If permission is granted
        if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this,"Permission granted",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this,"You just denied the permission :(",Toast.LENGTH_LONG).show();
        }
    }

    public void savePubKey(String pub_key_string) {
        monitor.writeToMonitor("Sending public key to server");
        monitor.writeToMonitor("pub_key: " + pub_key_string);

        PubKey pubKey = new PubKey();
        pubKey.setKey(pub_key_string);

        Call<PubKey> call = client.savePubKey(pubKey);
        call.enqueue(new Callback<PubKey>() {
            @Override
            public void onResponse(@NotNull Call<PubKey> call, @NotNull Response<PubKey> response) {
                if (!response.isSuccessful() && response.errorBody() != null) {
                    monitor.writeToMonitor("ERROR sending public key to server");
                    try {
                        JSONObject error = new JSONObject(response.errorBody().string());
                        // TODO: handle error
                    } catch (IOException | JSONException e) {
                        Log.e(TAG,"savePubKey - onResponse: " +  e.getMessage());
                    }
                } else {
                    monitor.writeToMonitor("Sending public key to server -> OK");
                    getProxyPort();
                }
            }

            @Override
            public void onFailure(@NotNull Call<PubKey> call, @NotNull Throwable t) {
                Log.e(TAG,"savePubKey - onFailure: " + t.getMessage());
                monitor.writeToMonitor("ERROR sending public key to server");
            }
        });
    }

    public void getProxyPort() {
        monitor.writeToMonitor("Getting proxy port");
        Call<ProxyPort> call = client.getProxyPort();
        call.enqueue(new Callback<ProxyPort>() {
            @Override
            public void onResponse(@NotNull Call<ProxyPort> call, @NotNull Response<ProxyPort> response) {
                if (!response.isSuccessful() && response.errorBody() != null) {
                    monitor.writeToMonitor("ERROR getting proxy port");
                    try {
                        JSONObject error = new JSONObject(response.errorBody().string());
                        // TODO: handle error
                    } catch (IOException | JSONException e) {
                        Log.e(TAG,"getProxyPort - onResponse: " +  e.getMessage());
                    }
                } else {
                    if (response.body() != null) {
                        monitor.writeToMonitor("Getting proxy port -> OK");
                        remote_port = response.body().getPort();
                        host = response.body().getHost();
                        executor = Executors.newSingleThreadExecutor();
                        executor.execute(sshConnection);
                    } else {
                        Log.e(TAG,"getProxyPort - onResponse: body is null");
                        // TODO: handle error
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call<ProxyPort> call, @NotNull Throwable t) {
                Log.e(TAG,"getProxyPort - onFailure: " + t.getMessage());
            }
        });
    }

    Runnable sshConnection = () -> {
        try {
            monitor.writeToMonitor("Starting SSH tunnel");
            tunnel.connect(host, ssh_port, user, "0404", monitor);
            tunnel.startReverseDynamicPortForwarding(remote_port, host, local_port, monitor);
            monitor.writeButton("Stop");
            monitor.setProgressBarVisibility(View.GONE);
            monitor.writeToMonitor("SSH tunnel ready!");
            while (running) {
                monitor.setProgressBarVisibility(View.GONE);
//                Log.d(TAG, "running");
                Thread.sleep(10000);
            }
        } catch (JSchException e) {
            // TODO, if error is port is occupied inform server and get a new port
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            Log.e(TAG, Arrays.toString(e.getStackTrace()));
        } catch (InterruptedException e) {
            Log.d(TAG, "sshConnection thread interrupted");
        } finally {
            if (tunnel != null) {
                tunnel.stop();
                stopping = false;
                monitor.writeTitle("Inactive");
                monitor.writeButton("Start");
                monitor.writeToMonitor("Stopped");
                monitor.setProgressBarVisibility(View.GONE);
            }
        }
    };

}