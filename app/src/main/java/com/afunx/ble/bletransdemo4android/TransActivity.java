package com.afunx.ble.bletransdemo4android;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afunx.ble.connector.BleConnector;
import com.afunx.ble.constants.BleKeys;

import java.util.UUID;

public class TransActivity extends AppCompatActivity implements BleConnector.Reader {

    private static final String TAG = "TransActivity";

    private static final long GATT_CONNECT_TIMEOUT = 3000;
    private static final long GATT_DISCOVER_SERVICE_TIMEOUT = 500;
    private static final long GATT_ENABLE_NOTIFICATION_TIMEOUT = 500;

    private static final long GATT_CHAR_WRITE_TIMEOUT = 100;

    public static final UUID UUID_TRANS_SERVICE = UUID
            .fromString("0000ffff-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_TRANS_CHARACTERISTIC = UUID
            .fromString("0000ff01-0000-1000-8000-00805f9b34fb");

    private TextView mTvReceive;
    private EditText mEdtSend;

    private Handler mHandler;

    private Button mBtnClearReceive;
    private Button mBtnClearSend;
    private Button mBtnSend;

    private String mBleAddress;

    private BleConnector mConnector;
    private BluetoothGattService mGattService;
    private BluetoothGattCharacteristic mGattCharacteristic;

    @Override
    public void onRead(byte[] bytesArrived) {
        final String newBytesPretty = new String(bytesArrived);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String oldString = mTvReceive.getText().toString();
                String newString = oldString + newBytesPretty;
                mTvReceive.setText(newString);
            }
        });
    }

    class InitTask extends AsyncTask<Void, Void, Integer> {

        private final int RESULT_CODE_SUC = 0x00;
        private final int RESULT_CODE_CONNECT_FAIL = 0x01;
        private final int RESULT_CODE_DISCOVER_SERVICE_FAIL = 0x02;
        private final int RESULT_CODE_DISCOVER_CHARACTERISTIC_FAIL = 0x03;
        private final int RESULT_CODE_ENABLE_NOTIFICATION_FAIL = 0x04;
        private ProgressDialog mProgressDialog;
        private Handler mHandler;

        @Override
        protected void onPreExecute() {
            mHandler = new Handler();
            Context context = TransActivity.this;
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setTitle("BLE INIT TASK");
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        private void updateMessage(final String message){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.setMessage(message);
                }
            });
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            updateMessage("connecting...");
            Context appContext = TransActivity.this.getApplicationContext();
            String bleAddress = mBleAddress;
            mConnector = BleConnector.Builder.build(bleAddress, appContext);
            // connect
            if (!mConnector.connect(GATT_CONNECT_TIMEOUT)) {
                Log.w(TAG,"InitTask connect fail");
                return RESULT_CODE_CONNECT_FAIL;
            }
            updateMessage("discovering service...");
            // discover service
            BluetoothGattService gattService = mConnector.discoverGattService(UUID_TRANS_SERVICE, GATT_DISCOVER_SERVICE_TIMEOUT);
            if (gattService == null) {
                Log.w(TAG,"InitTask discover service fail");
                return RESULT_CODE_DISCOVER_SERVICE_FAIL;
            }
            mGattService = gattService;
            updateMessage("discovering characteristic...");
            // discover characteristic
            BluetoothGattCharacteristic gattCharacteristic = mConnector.discoverCharacteristic(gattService, UUID_TRANS_CHARACTERISTIC);
            if (gattCharacteristic == null) {
                Log.w(TAG,"InitTask discover characteristic fail");
                return RESULT_CODE_DISCOVER_CHARACTERISTIC_FAIL;
            }
            mGattCharacteristic = gattCharacteristic;
            updateMessage("enable notification");
            // enable notification
            if (!mConnector.enableCharacteristicNotification(gattCharacteristic, GATT_ENABLE_NOTIFICATION_TIMEOUT)) {
                Log.w(TAG,"InitTask enable notification fail");
                return RESULT_CODE_ENABLE_NOTIFICATION_FAIL;
            }
            return RESULT_CODE_SUC;
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            mProgressDialog.dismiss();
            Context context = TransActivity.this;
            switch (resultCode) {
                case RESULT_CODE_SUC:
                    Toast.makeText(context, "BLE DEVICE INIT SUC", Toast.LENGTH_LONG).show();
                    mConnector.setReader(TransActivity.this);
                    break;
                case RESULT_CODE_CONNECT_FAIL:
                    Toast.makeText(context, "BLE DEVICE INIT FAIL 0x01 CONNECT FAIL", Toast.LENGTH_LONG).show();
                    finish();
                    break;
                case RESULT_CODE_DISCOVER_SERVICE_FAIL:
                    Toast.makeText(context, "BLE DEVICE INIT FAIL 0x02 DISCOVER SERVICE FAIL", Toast.LENGTH_LONG).show();
                    finish();
                    break;
                case RESULT_CODE_DISCOVER_CHARACTERISTIC_FAIL:
                    Toast.makeText(context, "BLE DEVICE INIT FAIL 0x03 DISCOVER CHARACTERISTIC FAIL", Toast.LENGTH_LONG).show();
                    finish();
                    break;
                case RESULT_CODE_ENABLE_NOTIFICATION_FAIL:
                    Toast.makeText(context, "BLE DEVICE INIT FAIL 0x04 ENABLE NOTIFICATION FAIL", Toast.LENGTH_LONG).show();
                    finish();
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        new InitTask().execute();
    }

    @Override
    protected void onDestroy() {
        if (mConnector != null) {
            mConnector.close();
            mConnector = null;
        }
        super.onDestroy();
    }

    private void init() {
        setContentView(R.layout.activity_trans);

        mTvReceive = (TextView) findViewById(R.id.tv_receive);
        mEdtSend = (EditText) findViewById(R.id.edt_send);

        mBtnClearReceive = (Button) findViewById(R.id.btn_clear_receive);
        mBtnClearReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearReceiveTextView();
            }
        });

        mBtnClearSend = (Button) findViewById(R.id.btn_clear_send);
        mBtnClearSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearSendEditText();
            }
        });

        mBtnSend = (Button) findViewById(R.id.btn_send);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBleData();
            }

        });

        mBleAddress = getIntent().getExtras().getString(BleKeys.BLE_ADDRESS);

        mHandler = new Handler();
    }

    private void clearSendEditText() {
        mEdtSend.setText("");
    }

    private void clearReceiveTextView() {
        mTvReceive.setText("");
    }

    // send ble data according to mEdtSend
    private void sendBleData() {
        final Handler handler = mHandler;
        final BluetoothGattCharacteristic gattCharacteristic = mGattCharacteristic;
        final byte[] bytes = mEdtSend.getText().toString().getBytes();
        final Context context = this;

        new Thread() {
            public void run() {
                boolean isSuc = mConnector.write(gattCharacteristic, bytes, GATT_CHAR_WRITE_TIMEOUT);
                if (isSuc) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "send ble data suc", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "send ble data fail", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }.start();
    }

}