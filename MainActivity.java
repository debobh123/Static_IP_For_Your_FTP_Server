package com.sandvik.newtraxdatabearerdev.gui.main;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sandvik.newtraxdatabearerdev.App;
import com.sandvik.newtraxdatabearerdev.AppSettings;
import com.sandvik.newtraxdatabearerdev.R;
import com.sandvik.newtraxdatabearerdev.WiFiDirectBroadcastReceiver;
import com.sandvik.newtraxdatabearerdev.gui.FsNotification;
import com.sandvik.newtraxdatabearerdev.gui.log.TransferLogController;
import com.sandvik.newtraxdatabearerdev.gui.log.TransferLogItem;
import com.sandvik.newtraxdatabearerdev.gui.log.TransferLogListView;
import com.sandvik.newtraxdatabearerdev.gui.settings.FsPreferenceActivity;
import com.sandvik.newtraxdatabearerdev.gui.settings.XSettingsActivity;
import com.sandvik.newtraxdatabearerdev.service.fdm.FDMservice;
import com.sandvik.newtraxdatabearerdev.service.fdm.RequestStartStopReceiver;
import com.sandvik.newtraxdatabearerdev.service.ftp.FsService;
import com.sandvik.newtraxdatabearerdev.storage.FileStorage;
import com.sandvik.newtraxdatabearerdev.storage.IFileStorageListener;
import com.sandvik.newtraxdatabearerdev.storage.OutboxFile;
import com.sandvik.newtraxdatabearerdev.util.Logger;
import com.sandvik.newtraxdatabearerdev.util.NetUtils;

import java.io.File;
import java.io.FileFilter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements IFileStorageListener {
    private final static String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_WIFI = 5;

    private static Handler handler;    // Handlers for runnable
    private static boolean isActive = false;

    private static MachineController machineList = null;

    private static TransferLogController transferLogCtrl = null;

    private TextView uiPendingReportsValue;
    private static boolean isCloudTransferActive = false;

    private static TextView connectionTypeLabel;

    private static ImageView cloudTransferStateIcon;
    public static FrameLayout logViewFrame;

    public static FloatingActionButton wifiDirectButton;
    public static Switch hotspotToggle;
    private boolean isHotspotEnabled = false;
    private boolean isWifiP2pEnabled = false;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private int connectedDeviceCount = 0;
    private IntentFilter intentFilter;
    Context mContext = App.getAppContext();
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001;
    private BroadcastReceiver receiver;


    static private final Thread.UncaughtExceptionHandler defaultExpHandler = Thread.getDefaultUncaughtExceptionHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] permissions = {Manifest.permission.NEARBY_WIFI_DEVICES, Manifest.permission.WRITE_SETTINGS, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.POST_NOTIFICATIONS};       //if (!ActivityCompat..shouldShowRequestPermissionRationale(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 121);
        //}


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //FileStorage.createInstance(getApplicationContext().getFilesDir());
            FileStorage.createInstance(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        } else {
            FileStorage.createInstance(Environment.getExternalStorageDirectory());
        }

        // Check if Wi-Fi is enabled
        if (!isWifiEnabled()) {
            // If Wi-Fi is not enabled, open the Wi-Fi settings screen
            openWifiSettings();
            return; // Stop the activity from further loading until Wi-Fi is enabled
        }


        //setContentView(R.layout.mainpage);
        setContentView(R.layout.sandvik_experimental_main_page);

        logViewFrame = findViewById(R.id.logViewFrame);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                Logger.e(TAG, "uncaughtException", e);
                Logger.scanLogs();
                defaultExpHandler.uncaughtException(thread, e);
            }
        });
        registerIntentListeners();
        Logger.i(TAG, "onCreate()");

        // prompt user to disable battery optimizations for databearer.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                //  Prompt the user to disable battery optimization
                Logger.i(TAG, "Battery optimizations are not disabled, prompting user.");
                Toast.makeText(this, R.string.battery_optimization_info_1, Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS));
                } catch (ActivityNotFoundException e) {
                    Logger.e(TAG, "This device does not support automated battery optimization disable, take user to settings page.", e);
                    try {
                        startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                        Toast.makeText(this, R.string.battery_optimization_info_2, Toast.LENGTH_LONG).show();
                    } catch (ActivityNotFoundException e2) {
                        Logger.e(TAG, "This device does not support any kind of battery optimization disabling, nothing we can do.", e2);
                        Toast.makeText(this, R.string.battery_optimization_info_3, Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Logger.i(TAG, "Battery optimizations already disabled.");
            }
        }



        if (machineList == null) {
            machineList = new MachineController();
        }
        if (transferLogCtrl == null) {
            transferLogCtrl = new TransferLogController();
            addExistingFilesToTransferLog();
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.setTitleTextColor(Color.WHITE);

        setSupportActionBar(toolbar);

        DataTransferListViewAdapter dataTransferViewAdapter = new DataTransferListViewAdapter(App.getAppContext());
        machineList.setListViewAdapter(this, dataTransferViewAdapter);
        ListView dataTransferList = findViewById(R.id.dataTransferListView);
        dataTransferList.setAdapter(dataTransferViewAdapter);

        TransferLogListView transferLogView = new TransferLogListView(App.getAppContext());
        transferLogCtrl.setParentView(this, transferLogView);
        ListView transferLogList = findViewById(R.id.logListView);
        transferLogList.setAdapter(transferLogView);


        /*WifiDirectFeature*/
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!initP2p()) {
                finish();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MainActivity.PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }
        hotspotToggle=findViewById(R.id.hotspotToggle);
        boolean isToggleButtonOn = hotspotToggle.isChecked();
        hotspotToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(!isWifiP2pEnabled){
                        Log.d("P2P-Check","error: cannot start hotspot. WifiP2p is not enabled\n");
                        return;
                    }
                    startWifiDirectAp();
                   /* EditText editText = findViewById(R.id.editSSID);
                    String ssid = "DIRECT-hs-" + AppSettings.getWiFiDirectSSID();


                    //editText = findViewById(R.id.editPassword);
                    String password = AppSettings.getWiFiDirectPassWord();
                    int band = WifiP2pConfig.GROUP_OWNER_BAND_AUTO;
                    if(AppSettings.getBandwidth()==AppSettings.WifiDirectBandwidth.Two){
                        band = WifiP2pConfig.GROUP_OWNER_BAND_2GHZ;
                    }else if(AppSettings.getBandwidth()==AppSettings.WifiDirectBandwidth.Five){
                        band = WifiP2pConfig.GROUP_OWNER_BAND_5GHZ;
                    }

                    WifiP2pConfig config = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        config = new WifiP2pConfig.Builder()
                                .setNetworkName(ssid)
                                .setPassphrase(password)
                                .enablePersistentMode(false)
                                .setGroupOperatingBand(band)
                                .build();
                    }

                    int finalBand = band;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        manager.createGroup(channel, config, new WifiP2pManager.ActionListener() {


                            @Override
                            public void onSuccess() {
                                Log.d("P2P-Check","hotspot started\n");
                                isHotspotEnabled = true;
                                Log.d("P2P-Check","------------------- Hotspot Info -------------------\n");
                                Log.d("P2P-Check","SSID: " + ssid + "\n");
                                Log.d("P2P-Check","Password: " + password + "\n");
                                Log.d("P2P-Check","Band: "+((finalBand==WifiP2pConfig.GROUP_OWNER_BAND_2GHZ)?"2.4":"5")+"GHz\n");
                                Log.d("P2P-Check","-----------------------------------------------------------\n");

                                Log.d("check ftp adrs",String.valueOf(NetUtils.getWifiDirectIpAddress()));
                                isHotspotEnabled = true;
                                startfdmService();
                                //App.getAppContext().sendBroadcast(new Intent(FsService.ACTION_START_FTPSERVER));
                                //startfdmService();



                            }

                            @Override
                            public void onFailure(int i) {
                                Log.d("P2P-Check","hotspot failed to start. reason: " + String.valueOf(i) + "\n");
                            }
                        });
                    }*/
                } else {
                    stopWifiDirectAp();
                   /* manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d("mainActivity P2P-Check","hotspot stopped\n");
                            isHotspotEnabled = false;
                            connectedDeviceCount = 0;
                            mContext.sendBroadcast(new Intent(FsService.ACTION_STOP_FTPSERVER));*/

                           //stopfdmService();
                           /* Handler handler = new Handler(); Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    startfdmService();
                                }
                            };
                            int delayMillis = 10000;  handler.postDelayed(runnable, delayMillis);*/

                            /*TextView textViewDevices = findViewById(R.id.textViewDevices);
                            textViewDevices.setText("");*/



                       /* }

                        @Override
                        public void onFailure(int i) {
                            Log.d("P2P-Check","hotspot failed to stop. reason: " + String.valueOf(i) + "\n");
                        }
                    });*/
                }
            }
        });


       /* wifiDirectButton = findViewById(R.id.wifiDirectFloatingActionButton);
        wifiDirectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //stopfdmService();
                Intent wifiDirectIntent = new Intent(MainActivity.this, WifiDirectActivity.class);
                startActivity(wifiDirectIntent);

            }
        });
*/


        uiPendingReportsValue = findViewById(R.id.pendingReportsValue);
        connectionTypeLabel = findViewById(R.id.connectionTypeHeader);

        FileStorage.instance().addListener(this);

        // Android 6.0 permission for write settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        cloudTransferStateIcon = findViewById(R.id.cloudConnStateIcon);

        handler = new Handler();

        Logger.d(TAG, "Started Sandvik DataMule ver. " + App.getVersion());
        //startfdmService();

        Button logButton = findViewById(R.id.logButton);
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logViewFrame.setVisibility(View.VISIBLE);

            }
        });

        setCloudTransferActive(isCloudTransferActive);
        refreshConnectionLabel();
        onMachineMetaDataChanged(null);

        isActive = true;
    }
 private void stopWifiDirectAp(){
        stopfdmService();
     manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
         @Override
         public void onSuccess() {
             Log.d("mainActivity P2P-Check","hotspot stopped\n");
             isHotspotEnabled = false;
             connectedDeviceCount = 0;
             mContext.sendBroadcast(new Intent(FsService.ACTION_STOP_FTPSERVER));
            startfdmService();
             //stopfdmService();
                           /* Handler handler = new Handler(); Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    startfdmService();
                                }
                            };
                            int delayMillis = 10000;  handler.postDelayed(runnable, delayMillis);*/

                            /*TextView textViewDevices = findViewById(R.id.textViewDevices);
                            textViewDevices.setText("");*/



         }

         @Override
         public void onFailure(int i) {
             Log.d("P2P-Check","hotspot failed to stop. reason: " + String.valueOf(i) + "\n");
         }
     });

 }
    private void startWifiDirectAp() {
        stopfdmService();
        EditText editText = findViewById(R.id.editSSID);
        String ssid = "DIRECT-hs-" + AppSettings.getWiFiDirectSSID();


        //editText = findViewById(R.id.editPassword);
        String password = AppSettings.getWiFiDirectPassWord();
        int band = WifiP2pConfig.GROUP_OWNER_BAND_AUTO;
        if(AppSettings.getBandwidth()==AppSettings.WifiDirectBandwidth.Two){
            band = WifiP2pConfig.GROUP_OWNER_BAND_2GHZ;
        }else if(AppSettings.getBandwidth()==AppSettings.WifiDirectBandwidth.Five){
            band = WifiP2pConfig.GROUP_OWNER_BAND_5GHZ;
        }

        WifiP2pConfig config = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            config = new WifiP2pConfig.Builder()
                    .setNetworkName(ssid)
                    .setPassphrase(password)
                    .enablePersistentMode(false)
                    .setGroupOperatingBand(band)
                    .build();
        }

        int finalBand = band;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            manager.createGroup(channel, config, new WifiP2pManager.ActionListener() {


                @Override
                public void onSuccess() {
                    Log.d("P2P-Check","hotspot started\n");
                    isHotspotEnabled = true;
                    Log.d("P2P-Check","------------------- Hotspot Info -------------------\n");
                    Log.d("P2P-Check","SSID: " + ssid + "\n");
                    Log.d("P2P-Check","Password: " + password + "\n");
                    Log.d("P2P-Check","Band: "+((finalBand==WifiP2pConfig.GROUP_OWNER_BAND_2GHZ)?"2.4":"5")+"GHz\n");
                    Log.d("P2P-Check","-----------------------------------------------------------\n");

                    Log.d("check ftp adrs",String.valueOf(NetUtils.getWifiDirectIpAddress()));
                    isHotspotEnabled = true;
                    startfdmService();
                    startServer();
                    //App.getAppContext().sendBroadcast(new Intent(FsService.ACTION_START_FTPSERVER));
                    //startfdmService();



                }

                @Override
                public void onFailure(int i) {
                    stopWifiDirectAp();
                    Log.d("P2P-Check","hotspot failed to start. reason: " + String.valueOf(i) + "\n");
                    startWifiDirectAp();
                }
            });
        }
    }

    private void registerIntentListeners() {
        // Registering FTP server intent listener
        BroadcastReceiver br = new RequestStartStopReceiver();
        IntentFilter ftpIntentFilter = new IntentFilter();
        ftpIntentFilter.addAction(FsService.ACTION_START_FTPSERVER);
        ftpIntentFilter.addAction(FsService.ACTION_STOP_FTPSERVER);
        this.registerReceiver(br, ftpIntentFilter);

        FsNotification fsn = new FsNotification();
        IntentFilter fsIntentFilter = new IntentFilter();
        fsIntentFilter.addAction(FsService.ACTION_STARTED);
        fsIntentFilter.addAction(FsService.ACTION_STOPPED);
        fsIntentFilter.addAction(FsService.ACTION_FAILEDTOSTART);
        this.registerReceiver(fsn, fsIntentFilter);
    }

    static public TransferLogController getTransferLogController() {
        return transferLogCtrl;
    }

    private void addExistingFilesToTransferLog() {

        // get all files from outbox and m2moutbox
        ArrayList<File> files = new ArrayList<File>();
        // get files from outbox
        new File(FileStorage.instance().getOutboxPath()).listFiles(new FileFilter() {
                                                                       @Override
                                                                       public boolean accept(File pathname) {
                                                                           if (pathname.isFile()) {
                                                                               files.add(pathname);
                                                                           }
                                                                           return false;
                                                                       }
                                                                   }
        );
        // get files from m2moutbox
        new File(FileStorage.instance().getM2MOutboxPath()).listFiles(new FileFilter() {
                                                                          @Override
                                                                          public boolean accept(File pathname) {
                                                                              if (pathname.isFile()) {
                                                                                  files.add(pathname);
                                                                              }
                                                                              return false;
                                                                          }
                                                                      }
        );

        // iterate received files in outbox and m2moutbox, add to transferlog with Received state.
        for (File file : files) {
            TransferLogItem i = transferLogCtrl.getOrAddFile(file.getName());
            if (i != null) {
                i.setState(TransferLogItem.State.Received);
            }
        }

        // iterate all files from Sent path and add to transferlog with Uploaded state.
        new File(FileStorage.instance().getSentFilesPath()).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isFile()) {
                    TransferLogItem i = transferLogCtrl.getOrAddFile(pathname.getName());
                    if (i != null) {
                        i.setState(TransferLogItem.State.Uploaded);
                    }
                }
                return false;
            }
        });

        transferLogCtrl.sortFiles();
        transferLogCtrl.cleanOldFiles();

    }

    @Override
    public void onBackPressed() {
        if (logViewFrame.getVisibility() == View.VISIBLE) {
            logViewFrame.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Logger.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
            //resume tasks needing this permission
        }
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION:
                if  (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Fine location permission is not granted!");
                    finish();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onMachineMetaDataChanged(String machineSerial) {
        try {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    uiPendingReportsValue.setText(String.valueOf(FileStorage.instance().getPendingReportCount()));
                }
            });
        } catch (Exception ignored) {
        } // silently ignore, this may happen occasionally if Activity is closed at the same time we execute this.

        setCloudTransferActive(isCloudTransferActive);
    }

    public static void refreshConnectionLabel() {
        try {
            if (!isActive) return;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (AppSettings.getServerType() == AppSettings.ServerType.Optimine) {
                        connectionTypeLabel.setText(R.string.optimine_connection);
                    } else {
                        connectionTypeLabel.setText(R.string.iothub_connection);
                    }
                }
            });
        } catch (Exception ignored) {
        } // silently ignore, this may happen occasionally if Activity is closed at the same time we execute this.
    }

    public static void unableToChangeWifiState() {
        Logger.d(TAG, "Unable to change Wifi state");
        MainActivity.showWarning("You must allow DataBearer to change system settings.");
    }

    public static void noValidClock() {
        Logger.d(TAG, "No valid clock");
        MainActivity.showWarning("Clock time/date is invalid. Connect internet to synchronize or set manually.");
    }

    private static void showWarning(String msg) {
        try {
            if (!isActive) return;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(App.getAppContext(),
                            msg, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
        } catch (Exception ignored) {
        } // silently ignore, this may happen occasionally if Activity is closed at the same time we execute this.

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                hotspotToggle=findViewById(R.id.hotspotToggle);
                boolean isToggleButtonOn = hotspotToggle.isChecked();
                Intent intent = new Intent(this, XSettingsActivity.class);
                intent.putExtra("toggleState", isToggleButtonOn);
                this.startActivity(intent);
                break;
            case R.id.action_mule_logs:
                openDocumentsDirectory();

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void openDocumentsDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("*/*");
        startActivityForResult(intent, 0);
    }

    public static void setCloudTransferActive(boolean active) {
        isCloudTransferActive = active;
        try {
            if (!isActive) return;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (isCloudTransferActive) {
                        cloudTransferStateIcon.setImageResource(R.drawable.sendreceive_100x100);
                        cloudTransferStateIcon.setVisibility(View.VISIBLE);
                    } else {
                        if (FileStorage.instance().getPendingReportCount() > 0) {
                            /*cloudTransferStateIcon.setImageResource(R.drawable.pending_black_100x100);
                            cloudTransferStateIcon.setVisibility(View.VISIBLE);*/
                        } else {
                            /*cloudTransferStateIcon.setVisibility(View.INVISIBLE);*/
                        }
                    }
                }
            });
        } catch (Exception ignored) {
        } // silently ignore, this may happen occasionally if Activity is closed at the same time we execute this.
    }

    private static void refreshMachineListUI() {
        try {
            if (!isActive) return;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    machineList.refreshUI();
                }
            });
        } catch (Exception ignored) {
        } // silently ignore, this may happen occasionally if Activity is closed at the same time we execute this.
    }

    public static void refreshTransferLogUI() {
        try {
            if (!isActive) return;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    transferLogCtrl.refreshUI();
                }
            });
        } catch (Exception ignored) {
        } // silently ignore, this may happen occasionally if Activity is closed at the same time we execute this.
    }

    public static void ftpDeviceConnected(String ip) {
        MachineState m = machineList.addMachineIP(ip);
        m.setConnectionState(MachineState.ConnectionState.Idle);
        refreshMachineListUI();
    }

    public static void ftpDeviceDisconnected(String ip) {
        String serial = machineList.getMachineSerial(ip);
        if (serial != null) {
            MachineState m = machineList.getMachine(serial);
            if (m != null) {
                m.setConnectionState(MachineState.ConnectionState.Disconnected);
                refreshMachineListUI();
            }
        }
    }

    public static void ftpFileReceiveStarted(String ip, String filename) {
        /*int l = filename.lastIndexOf('.');
        String ext = null;
        if (l > 0) {
            ext = filename.substring(l+1);

        }

        if (ext.equals("gz")) {*/
            OutboxFile.OutboxFileMetaData fileInfo = OutboxFile.parseFileName(filename);
            if (fileInfo.IsValid) {
                MachineState m = machineList.associateMachineSerial(ip, fileInfo.DeviceName);
                m.setConnectionState(MachineState.ConnectionState.Receiving);
                refreshMachineListUI();

                TransferLogItem i = transferLogCtrl.getOrAddFile(filename);
                if (i != null) {
                    i.setState(TransferLogItem.State.FtpDownload);
                    refreshTransferLogUI();
                }
            }
        /*}else if (ext.equals("raw")) {
            OutboxFile.OutboxFileMetaData fileInfo = OutboxFile.parseFileNameMet(filename);
            if (fileInfo.IsValid) {
                MachineState m = machineList.associateMachineSerial(ip, fileInfo.DeviceName);
                m.setConnectionState(MachineState.ConnectionState.Receiving);
                refreshMachineListUI();

                TransferLogItem i = transferLogCtrl.getOrAddFile(filename);
                if (i != null) {
                    i.setState(TransferLogItem.State.FtpDownload);
                    refreshTransferLogUI();
                }
            }
        }*/
    }

    public static void ftpFileReceiveCompleted(String ip, String filename, boolean succeeded) {
       /* int l = filename.lastIndexOf('.');
        String ext = null;
        if (l > 0) {
            ext = filename.substring(l+1);

        }

        if (ext.equals("gz")) {

        */
            OutboxFile.OutboxFileMetaData fileInfo = OutboxFile.parseFileName(filename);
            if (fileInfo.IsValid) {
                MachineState m = machineList.associateMachineSerial(ip, fileInfo.DeviceName);
                m.setConnectionState(MachineState.ConnectionState.Idle);
                refreshMachineListUI();

                TransferLogItem i = transferLogCtrl.getOrAddFile(filename);
                if (i != null) {
                    i.setState(TransferLogItem.State.Received);
                    refreshTransferLogUI();
                }
            }
       /* }else if(ext.equals("raw")){
            OutboxFile.OutboxFileMetaData fileInfo = OutboxFile.parseFileNameMet(filename);
            if (fileInfo.IsValid) {
                MachineState m = machineList.associateMachineSerial(ip, fileInfo.DeviceName);
                m.setConnectionState(MachineState.ConnectionState.Idle);
                refreshMachineListUI();

                TransferLogItem i = transferLogCtrl.getOrAddFile(filename);
                if (i != null) {
                    i.setState(TransferLogItem.State.Received);
                    refreshTransferLogUI();
                }
            }
        }*/
    }

    private void updateRunningState() throws UnknownHostException {
        Resources res = getResources();
        if (FsService.isRunning()) {
            // Fill in the FTP server address
            InetAddress address = InetAddress.getByName(NetUtils.getWifiDirectIpAddress());
            if (address == null) {
                Logger.v(TAG, "Unable to retrieve wifi ip address");
                return;
            }
            String ipText = "ftp://" + address.getHostAddress() + ":"
                    + AppSettings.getPortNumber() + "/";
            String summary = res.getString(R.string.running_summary_started, ipText);
        }
    }

    /**
     * This receiver will check FTPServer.ACTION* messages and will update the button,
     * running_state, if the server is running and will also display at what url the
     * server is running.
     */
    private final BroadcastReceiver mFsActionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.v(TAG, "action received: " + intent.getAction());
            // remove all pending callbacks
            handler.removeCallbacksAndMessages(null);
            // action will be ACTION_STARTED or ACTION_STOPPED
            //updateRunningState();
            // or it might be ACTION_FAILEDTOSTART
            if (intent.getAction().equals(FsService.ACTION_FAILEDTOSTART)) {
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
        try {
            updateRunningState();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            updateRunningState();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Logger.d(TAG, "onResume: Registering the FTP server actions");
        IntentFilter filter = new IntentFilter();
        filter.addAction(FsService.ACTION_STARTED);
        filter.addAction(FsService.ACTION_STOPPED);
        filter.addAction(FsService.ACTION_FAILEDTOSTART);
        registerReceiver(mFsActionsReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //startfdmService();
        Logger.v(TAG, "onPause: Unregistering the FTPServer actions");
        unregisterReceiver(mFsActionsReceiver);
    }

    @Override
    protected void onDestroy() {
        Logger.i(TAG, "onDestroy()");
        isActive = false;
        FileStorage.instance().removeListener(this);
        //startfdmService();
        Logger.scanLogs();
        super.onDestroy();

    }

    @Override
    protected void onStop() {
        super.onStop();
        //startfdmService();
    }

    private void startfdmService() {

        if (GetRunningServices()) {
            Logger.d(TAG, "Service State: Running");
            return;
        }
        // Start the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(getBaseContext(), FDMservice.class));
        } else {
            startService(new Intent(getBaseContext(), FDMservice.class));
        }
        Logger.d(TAG, "Service State: Running");
    }

    private void stopfdmService() {
        // We are already running the babyapp service
        if (GetRunningServices()) {
            Logger.d(TAG, "Service State: Running");
            stopService(new Intent(getBaseContext(), FDMservice.class));
        }
    }

    private boolean GetRunningServices() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals("com.sandvik.newdatabearerdev.service.fdm.FDMservice")) {
                Logger.d(TAG, "FDMservice is already running");
                return true;
            }
        }
        return false;
    }

    /*WifiDirect Methods*/
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean initP2p() {
        // Device capability definition check
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.");
            return false;
        }
        // Hardware capability check
        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager == null) {
            Log.e(TAG, "Cannot get Wi-Fi system service.");
            return false;
        }
        if (!wifiManager.isP2pSupported()) {
            Log.e(TAG, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.");
            return false;
        }
        manager = (WifiP2pManager) mContext.getApplicationContext().getSystemService(WIFI_P2P_SERVICE);
        if (manager == null) {
            Log.e(TAG, "Cannot get Wi-Fi Direct system service.");
            return false;
        }
        channel = manager.initialize(this, getMainLooper(), null);
        if (channel == null) {
            Log.e(TAG, "Cannot initialize Wi-Fi Direct.");
            return false;
        }
        return true;
    }

    private boolean isWifiEnabled() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    private void openWifiSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
        startActivityForResult(intent, REQUEST_ENABLE_WIFI);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_WIFI) {

            if (!isWifiEnabled()) {

            } else {
                //setContentView(R.layout.mainpage);

            }
        }
    }

    private void startServer() {
        sendBroadcast(new Intent(FsService.ACTION_START_FTPSERVER));
    }

    private void stopServer() {
        sendBroadcast(new Intent(FsService.ACTION_STOP_FTPSERVER));
    }

}
