package com.sandvik.newtraxdatabearerdev.gui.main;

import static com.sandvik.newtraxdatabearerdev.gui.main.MainActivity.logViewFrame;
import static com.sandvik.newtraxdatabearerdev.gui.main.MainActivity.refreshConnectionLabel;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.sandvik.newtraxdatabearerdev.App;
import com.sandvik.newtraxdatabearerdev.AppSettings;
import com.sandvik.newtraxdatabearerdev.R;
import com.sandvik.newtraxdatabearerdev.WiFiDirectBroadcastReceiver;
import com.sandvik.newtraxdatabearerdev.gui.log.TransferLogController;
import com.sandvik.newtraxdatabearerdev.gui.log.TransferLogItem;
import com.sandvik.newtraxdatabearerdev.service.fdm.FDMservice;
import com.sandvik.newtraxdatabearerdev.service.ftp.FsService;
import com.sandvik.newtraxdatabearerdev.storage.FileStorage;
import com.sandvik.newtraxdatabearerdev.storage.IFileStorageListener;
import com.sandvik.newtraxdatabearerdev.storage.OutboxFile;
import com.sandvik.newtraxdatabearerdev.util.Logger;
import com.sandvik.newtraxdatabearerdev.util.NetUtils;

import java.io.File;
import java.io.FileFilter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class WifiDirectActivity extends AppCompatActivity  {

   /* public static final String TAG = "WifiDirect";
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private boolean isWifiP2pEnabled = false;
    private boolean isHotspotEnabled = false;
    private int connectedDeviceCount = 0;
    private static TransferLogController transferLogCtrl = null;
    private static MachineController machineList = null;
    private static boolean isCloudTransferActive = false;
    private static ImageView cloudTransferStateIcon;
    private static Handler handler;
    private static boolean isActive = false;

    Context mContext = App.getAppContext();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //FileStorage.createInstance(getApplicationContext().getFilesDir());
            FileStorage.createInstance(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        } else {
            FileStorage.createInstance(Environment.getExternalStorageDirectory());
        }
        setContentView(R.layout.activity_wifi_direct);
        TextView textView = findViewById(R.id.textViewLog);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView = findViewById(R.id.textViewDevices);
        textView.setMovementMethod(new ScrollingMovementMethod());



        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (!initP2p()) {
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    WifiDirectActivity.PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }
        FileStorage.instance().addListener(this);

        if (machineList == null) {
            machineList = new MachineController();
        }
        if (transferLogCtrl == null) {
            transferLogCtrl = new TransferLogController();
            addExistingFilesToTransferLog();
        }
        setCloudTransferActive(isCloudTransferActive);
        refreshConnectionLabel();
        onMachineMetaDataChanged(null);


        handler = new Handler();


    }
    @Override
    public void onBackPressed() {
        if (logViewFrame.getVisibility() == View.VISIBLE) {
            logViewFrame.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION:
                if  (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Fine location permission is not granted!");
                    finish();
                }
                break;
        }
    }

    *//* register the broadcast receiver with the intent values to be matched *//*
    @Override
    protected void onResume() {
        super.onResume();
        //receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
        updateRunningState();

        Logger.d(TAG, "onResume: Registering the FTP server actions");
        IntentFilter filter = new IntentFilter();
        filter.addAction(FsService.ACTION_STARTED);
        filter.addAction(FsService.ACTION_STOPPED);
        filter.addAction(FsService.ACTION_FAILEDTOSTART);
        registerReceiver(mFsActionsReceiver, filter);
    }
    *//* unregister the broadcast receiver *//*
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        startfdmService();
        Logger.v(TAG, "onPause: Unregistering the FTPServer actions");
        unregisterReceiver(mFsActionsReceiver);
    }

    @Override
    protected void onDestroy() {
        Logger.i(TAG, "onDestroy()");
        isActive = false;
        FileStorage.instance().removeListener(this);
        startfdmService();
        Logger.scanLogs();
        super.onDestroy();

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

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void onButtonStartTapped(View view){
        //startfdmService();
        if(!isWifiP2pEnabled){
            outputLog("error: cannot start hotspot. WifiP2p is not enabled\n");
            return;
        }

        EditText editText = findViewById(R.id.editSSID);
        String ssid = "DIRECT-hs-" + editText.getText().toString();


        editText = findViewById(R.id.editPassword);
        String password = editText.getText().toString();
        int band = WifiP2pConfig.GROUP_OWNER_BAND_AUTO;
        if(((RadioButton) findViewById(R.id.radioButton2G)).isChecked()){
            band = WifiP2pConfig.GROUP_OWNER_BAND_2GHZ;
        }else if(((RadioButton) findViewById(R.id.radioButton5G)).isChecked()){
            band = WifiP2pConfig.GROUP_OWNER_BAND_5GHZ;
        }

        WifiP2pConfig config = new WifiP2pConfig.Builder()
                .setNetworkName(ssid)
                .setPassphrase(password)
                .enablePersistentMode(false)
                .setGroupOperatingBand(band)
                .build();

        int finalBand = band;
        manager.createGroup(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                outputLog("hotspot started\n");
                isHotspotEnabled = true;
                outputLog("------------------- Hotspot Info -------------------\n");
                outputLog("SSID: " + ssid + "\n");
                outputLog("Password: " + password + "\n");
                outputLog("Band: "+((finalBand==WifiP2pConfig.GROUP_OWNER_BAND_2GHZ)?"2.4":"5")+"GHz\n");
                outputLog("-----------------------------------------------------------\n");

                Log.d("check ftp adrs",String.valueOf(NetUtils.getWifiDirectIpAddress()));
                startfdmService();



            }

            @Override
            public void onFailure(int i) {
                outputLog("hotspot failed to start. reason: " + String.valueOf(i) + "\n");
            }
        });
    }

    public void onButtonStopTapped(View view){

        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                outputLog("hotspot stopped\n");
                isHotspotEnabled = false;
                connectedDeviceCount = 0;
                TextView textViewDevices = findViewById(R.id.textViewDevices);
                textViewDevices.setText("");
                stopfdmService();


            }

            @Override
            public void onFailure(int i) {
                outputLog("hotspot failed to stop. reason: " + String.valueOf(i) + "\n");
            }
        });
    }

    public void onButtonUpdateTapped(View view){
        outputLog("updating connected device list...\n");
        updateConnectedDeviceList();
    }

    public void updateConnectedDeviceList(){
        if(!isHotspotEnabled){
            return;
        }
        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                TextView textViewDevices = findViewById(R.id.textViewDevices);
                textViewDevices.setText("");
                int i = 0;
                for(WifiP2pDevice client : wifiP2pGroup.getClientList()){
                    textViewDevices.append("  Device" + ++i + ":  " + client.deviceAddress + "\n");
                }
                if(i > connectedDeviceCount){
                    outputLog("device connected\n");
                    connectedDeviceCount = i;
                }else if(i < connectedDeviceCount){
                    outputLog("device disconnected\n");
                    connectedDeviceCount = i;
                }
            }
        });
    }

    private void outputLog(String msg){
        TextView textViewLog = findViewById(R.id.textViewLog);
        textViewLog.append("  " + msg);
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

    @Override
    public void onMachineMetaDataChanged(String machineSerial) {
        try {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //uiPendingReportsValue.setText(String.valueOf(FileStorage.instance().getPendingReportCount()));
                }
            });
        } catch (Exception ignored) {
        } // silently ignore, this may happen occasionally if Activity is closed at the same time we execute this.

        setCloudTransferActive(isCloudTransferActive);
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
                            cloudTransferStateIcon.setImageResource(R.drawable.pending_black_100x100);
                            cloudTransferStateIcon.setVisibility(View.VISIBLE);
                        } else {
                            cloudTransferStateIcon.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            });
        } catch (Exception ignored) {
        } // silently ignore, this may happen occasionally if Activity is closed at the same time we execute this.
    }
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
        *//*int l = filename.lastIndexOf('.');
        String ext = null;
        if (l > 0) {
            ext = filename.substring(l+1);

        }

        if (ext.equals("gz")) {*//*
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
        *//*}else if (ext.equals("raw")) {
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
        }*//*
    }
    public static void ftpFileReceiveCompleted(String ip, String filename, boolean succeeded) {
       *//* int l = filename.lastIndexOf('.');
        String ext = null;
        if (l > 0) {
            ext = filename.substring(l+1);

        }

        if (ext.equals("gz")) {

        *//*
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
       *//* }else if(ext.equals("raw")){
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
        }*//*
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

    private void updateRunningState() {
        Resources res = getResources();
        if (FsService.isRunning()) {
            // Fill in the FTP server address
            InetAddress address = NetUtils.getLocalInetAddress();
            if (address == null) {
                Logger.v(TAG, "Unable to retrieve wifi ip address");
                return;
            }
            String ipText = "ftp://" + address.getHostAddress() + ":"
                    + AppSettings.getPortNumber() + "/";
            String summary = res.getString(R.string.running_summary_started, ipText);
        }
    }*/


}