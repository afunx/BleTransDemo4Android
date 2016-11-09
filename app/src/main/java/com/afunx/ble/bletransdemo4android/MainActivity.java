package com.afunx.ble.bletransdemo4android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        // it is brutally sometimes
        BleUtils.openBleBrutally();
        init();
        doRefresh();
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
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.i(TAG, "item " + position + " is selected");
//                Intent intent = new Intent(MainActivity.this, ConnectWifiActivity.class);
//                BleDevice bleDevice = (BleDevice) mBleDeviceAdapter.getItem(position);
//                intent.putExtra(BleKeys.BLE_ADDRESS, bleDevice.getBluetoothDevice().getAddress());
//                startActivity(intent);
            }

        });

        // LeScanCallback
        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if(BleDeviceUtils.isEspBluetoothDevice(device)) {
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
