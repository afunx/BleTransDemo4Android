package com.afunx.ble.bletransdemo4android;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.afunx.ble.adapter.BleDeviceAdapter;
import com.afunx.ble.constants.BleKeys;
import com.afunx.ble.device.BleDevice;
import com.afunx.ble.utils.BleDeviceUtils;
import com.afunx.ble.utils.BleUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ListView mListView;
    private BleDeviceAdapter mBleDeviceAdapter;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private Handler mHandler;

    private int REQUEST_CODE = 1;

    private void requestBleAuthority() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE);
            } else {
                doRefresh();
            }
        } else {
            doRefresh();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode==REQUEST_CODE) {
            boolean isForbidden = false;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != 0) {
                    isForbidden = true;
                    break;
                }
            }
            if (isForbidden) {
                Toast.makeText(this, R.string.fail_get_bluetooth_authority, Toast.LENGTH_LONG).show();
            } else {
                doRefresh();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // it is brutally sometimes
        BleUtils.openBleBrutally();
        init();
        mHandler = new Handler();
        requestBleAuthority();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void init() {
        setContentView(R.layout.activity_main);
        // swipe refresh layout and listview
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mListView = (ListView) findViewById(R.id.lv_devices);
//		mListView.setEmptyView(findViewById(R.id.pb_empty));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doRefresh();
            }
        });

        // ble device adapter
        mBleDeviceAdapter = new BleDeviceAdapter(this);
        mListView.setAdapter(mBleDeviceAdapter);
        // listview OnItemClickListener
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.i(TAG, "item " + position + " is selected");
                Intent intent = new Intent(MainActivity.this, TransActivity.class);
                BleDevice bleDevice = (BleDevice) mBleDeviceAdapter.getItem(position);
                intent.putExtra(BleKeys.BLE_ADDRESS, bleDevice.getBluetoothDevice().getAddress());
                startActivity(intent);
            }

        });

        // LeScanCallback
        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if (BleDeviceUtils.isEspBluetoothDevice(device)) {
                    BleDevice bleDevice = new BleDevice();
                    bleDevice.setBluetoothDevice(device);
                    bleDevice.setRssi(rssi);
                    bleDevice.setScanRecord(scanRecord);
                    mBleDeviceAdapter.addOrUpdateDevice(bleDevice);
                    mBleDeviceAdapter.notifyDataSetChanged();
                }
            }
        };

    }

    private void doRefresh() {
        final long interval = 5000;
        mSwipeRefreshLayout.setRefreshing(true);
        Toast.makeText(this, R.string.scaning, Toast.LENGTH_LONG).show();
        // clear ble devices in UI
        mBleDeviceAdapter.clear();
        new Thread() {
            public void run() {
                BleUtils.startLeScan(mLeScanCallback);
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ignore) {
                }
                BleUtils.stopLeScan(mLeScanCallback);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // stop swipe refresh refreshing
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        }.start();
    }

}
