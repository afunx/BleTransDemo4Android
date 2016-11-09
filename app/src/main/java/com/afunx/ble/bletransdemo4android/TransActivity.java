package com.afunx.ble.bletransdemo4android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.afunx.ble.constants.BleKeys;

public class TransActivity extends AppCompatActivity {

    private TextView mTvReceive;
    private EditText mEdtSend;

    private Button mBtnClearReceive;
    private Button mBtnClearSend;
    private Button mBtnSend;


    private String mBleAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
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
    }

    private void clearSendEditText() {
        mEdtSend.setText("");
    }

    private void clearReceiveTextView() {
        mTvReceive.setText("");
    }

    // send ble data according to mEdtSend
    private void sendBleData() {
    }

}
