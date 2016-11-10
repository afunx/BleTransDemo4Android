package com.afunx.ble.connector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

/**
 * Created by afunx on 09/11/2016.
 */

public class BleConnector {

    private static final String TAG = "BleConnector";

    /**
     * BluetoothGattCallback
     */

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange() status: " + status + ", newState: " + newState);
            if (isClosed()) {
                Log.d(TAG, "onConnectionStateChange() just ignore for BleConnector has been closed already");
                return;
            }

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    notifyLockConnect();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    connect(0);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered() status: " + status);
            notifyLockDiscoverService();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead() characteristic: " + characteristic + ", status: " + status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite() characteristic: " + characteristic + ", status: " + status);
            mIsWriteSuc = status == BluetoothGatt.GATT_SUCCESS;
            notifyLockWrite();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged() characteristic: " + characteristic);
            if (mReader != null) {
                mReader.onRead(characteristic.getValue());
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorRead() descriptor: " + descriptor + ", status: " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite() descriptor: " + descriptor + ", status: " + status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onReliableWriteCompleted() status: " + status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "onReadRemoteRssi() rssi: " + rssi + ", status: " + status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d(TAG, "onMtuChanged() mtu: " + mtu + ", status: " + status);
        }
    };

    /**
     * Reader
     */

    public interface Reader {
        public void onRead(byte[] bytesArrived);
    }

    private Reader mReader;

    public void setReader(Reader reader) {
        mReader = reader;
    }

    /**
     * Other Global fields
     */
    private volatile boolean mIsClosed;
    private BluetoothGatt mBluetoothGatt;
    private final String mBleAddress;
    private final Context mAppContext;

    /**
     * Locks
     */

    // lock for connect
    private final Object mLockConnect = new Object();
    private volatile boolean mIsConnectSuc = false;

    private void notifyLockConnect() {
        mIsConnectSuc = true;
        synchronized (mLockConnect) {
            mLockConnect.notify();
        }
    }

    private boolean waitLockConnect(long millis) {
        synchronized (mLockConnect) {
            try {
                mLockConnect.wait(millis);
            } catch (InterruptedException ignore) {
            }
            return mIsConnectSuc;
        }
    }

    // lock for discover service
    private final Object mLockDiscoverService = new Object();
    private volatile boolean mIsDiscoverServiceSuc = false;

    private void notifyLockDiscoverService() {
        mIsDiscoverServiceSuc = true;
        synchronized (mLockDiscoverService) {
            mLockDiscoverService.notify();
        }
    }

    private boolean waitLockDiscoverService(long millis) {
        synchronized (mLockDiscoverService) {
            try {
                mLockDiscoverService.wait(millis);
            } catch (InterruptedException ignore) {
            }
            return mIsDiscoverServiceSuc;
        }
    }

    // lock for write
    private final Object mLockWrite = new Object();
    private volatile boolean mIsWriteSuc = false;

    private void notifyLockWrite() {
        mIsWriteSuc = true;
        synchronized (mLockWrite) {
            mLockWrite.notify();
        }
    }

    private boolean waitLockWrite(long millis) {
        synchronized (mLockWrite) {
            try {
                mLockWrite.wait(millis);
            } catch (InterruptedException ignore) {
            }
            return mIsWriteSuc;
        }
    }


    private BleConnector(String bleAddress, Context appContext) {
        mIsClosed = false;
        mBleAddress = bleAddress;
        // make use application context to avoid memory leak
        mAppContext = appContext.getApplicationContext();
    }

    private static BluetoothAdapter getAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Operations
     */

    /**
     * Connect to ble device(when timeout > 0, it is sync; when timeout <=0, it is async).
     * callbackSuc and callbackFail will be invoked only first time.
     * Before BleConnector is closed, it will automatically reconnect,
     * but callbackSuc and callbackFail won't be invoked anymore.
     *
     * @param timeout connect timeout in milliseconds, 0 or negative means forever and async
     * @return whether connect is suc(when timeout < 0, it always return false)
     */
    public boolean connect(long timeout) {
        mIsClosed = false;
        BluetoothAdapter adapter = getAdapter();
        if (adapter == null) {
            Log.w(TAG, "connect() BluetoothAdapter is null");
            return false;
        }
        if (mBluetoothGatt == null) {
            final BluetoothDevice device = adapter.getRemoteDevice(mBleAddress);
            if (device == null) {
                Log.w(TAG, "connect() device not found");
                return false;
            } else {
                Log.d(TAG, "connect() connectGatt");
                mBluetoothGatt = device.connectGatt(mAppContext, false,
                        mBluetoothGattCallback);
            }
        } else {
            Log.d(TAG, "connect() directly");
            mBluetoothGatt.connect();
        }
        if (timeout > 0) {
            Log.d(TAG, "connect() wait...");
            boolean isConnectSuc = waitLockConnect(timeout);
            Log.i(TAG, "connect() " + (isConnectSuc ? "SUC" : "FAIL"));
            return isConnectSuc;
        } else {
            // let compiler stop complaining
            return false;
        }
    }

    /**
     * discover gatt service by uuid
     *
     * @param uuid    uuid belong to gatt service
     * @param timeout timeout in milliseconds(it should > 0)
     * @return gatt service according to uuid
     */
    public BluetoothGattService discoverGattService(UUID uuid, long timeout) {
        if (mBluetoothGatt.discoverServices()) {
            waitLockDiscoverService(timeout);
            if (mIsDiscoverServiceSuc) {
                return mBluetoothGatt.getService(uuid);
            }
        }
        return null;
    }

    /**
     * discover gatt characteristic by uuid
     *
     * @param gattService gatt service the gatt characteristic belong to
     * @param uuid        uuid belong to gatt characteristic
     * @return gatt characteristic belong to the gatt service by uuid
     */
    public BluetoothGattCharacteristic discoverCharacteristic(BluetoothGattService gattService, UUID uuid) {
        return gattService.getCharacteristic(uuid);
    }

    /**
     * enable gatt characteristic notification
     *
     * @param gattCharacteristic gatt characteristic to be enable notification
     * @param timeout            timeout in milliseconds(it should > 0)
     * @return whether the gatt characteristic is enabled notification
     */
    public boolean enableCharacteristicNotification(BluetoothGattCharacteristic gattCharacteristic, long timeout) {
        if (mBluetoothGatt != null) {
            return mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
        }
        return false;
    }

    /**
     * write bytes to gatt characteristic
     *
     * @param gattCharacteristic the gatt characteristic to be written
     * @param bytes              the bytes to be written
     * @param timeout            timeout in milliseconds(it should > 0)
     * @return whether the gatt characteristic is written suc
     */
    public boolean write(BluetoothGattCharacteristic gattCharacteristic, byte[] bytes, long timeout) {
        gattCharacteristic.setValue(bytes);
        mBluetoothGatt.writeCharacteristic(gattCharacteristic);
        waitLockWrite(timeout);
        return mIsWriteSuc;
    }

    private synchronized boolean isClosed() {
        return mIsClosed;
    }

    /**
     * close BleConnector and release resources
     */
    public synchronized void close() {
        if (!mIsClosed) {
            mIsClosed = true;
            if (mBluetoothGatt != null) {
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
        }
    }

    /**
     * Builder
     */
    public static class Builder {
        public static BleConnector build(String bleAddress, Context appContext) {
            return new BleConnector(bleAddress, appContext);
        }
    }
}
