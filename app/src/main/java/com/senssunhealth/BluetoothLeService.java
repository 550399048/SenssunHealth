package com.senssunhealth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Created by wucaiyan on 17-11-6.
 */

public class BluetoothLeService extends Service{
    private final static String TAG ="BluetoothLeService";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";

    private static BluetoothGatt mBluetoothGatt = null;

    private IServicesDiscoveredListener mIServicesDiscoveredListener;
    private IDataAvailableListener mIDataAvailableListener;
    private IConnectedListener mIConnectedListener;
    private IDisconnectedListener mIDisconnectedListener;
    private Context mContext;

    public BluetoothLeService(){

    }




    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG,"newState =="+newState+"已经链接成功！！！！！！！！！！！！！！");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
                if (mIConnectedListener != null){
                    mIConnectedListener.onConnect(mBluetoothGatt,status,newState);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (mIDisconnectedListener != null){
                    mIDisconnectedListener.onDisconnect(mBluetoothGatt);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS && mIServicesDiscoveredListener != null) {
                Log.d(TAG,"onServicesDiscovered status ="+status);
                mIServicesDiscoveredListener.onServicesDiscovered(gatt);
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (characteristic.getValue()!= null) {
                Log.d(TAG,characteristic.getValue()+"UUID="+characteristic.getUuid());
            }
            if (characteristic.getUuid().toString().equals(BleDataManager.UUID_UNLOCK_DATA_NOTIFY)){
                Log.d(TAG,"onCharacteristicChanged value ="+characteristic.getValue().toString());

            }
            if (mIDataAvailableListener != null) {
                mIDataAvailableListener.onCharacteristicChanged(gatt,characteristic);
            }

        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS && mIDataAvailableListener != null) {
               mIDataAvailableListener.onCharacteristicRead(gatt, characteristic, status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (mIDataAvailableListener != null) {
                mIDataAvailableListener.onCharacteristicWrite(gatt, characteristic);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            BleDataManager.gattCharacteristic_write.setValue(Utils.CMMD_QUERY_USER);
            writeCharacteristic(BleDataManager.gattCharacteristic_write);
        }
    };

    public void setServicesDiscoveredListener(IServicesDiscoveredListener iServicesDiscoveredListener){
        mIServicesDiscoveredListener = iServicesDiscoveredListener;

    }

    public void setDataAvailableListener(IDataAvailableListener iDataAvailableListener){
        this.mIDataAvailableListener = iDataAvailableListener;
    }

    public void setStateConnected(IConnectedListener iConnectedListener){
        this.mIConnectedListener = iConnectedListener;
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();


    public interface IServicesDiscoveredListener{
       void onServicesDiscovered(BluetoothGatt bluetoothGatt);
    }



    public interface IDataAvailableListener {
        void onCharacteristicRead(BluetoothGatt gatt,
                                  BluetoothGattCharacteristic characteristic, int status);

        void onCharacteristicWrite(BluetoothGatt gatt,
                                   BluetoothGattCharacteristic characteristic);

        void onCharacteristicChanged(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic);
    }


    public interface IConnectedListener{
        void onConnect(BluetoothGatt bluetoothGatt,int status,int newStatus);
    }

    public interface IDisconnectedListener{
        void onDisconnect(BluetoothGatt bluetoothGatt );
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            return null;
        }
        return mBluetoothGatt.getServices();
    }

    public BluetoothGattService getSupportedGattServiceByUUID (UUID uuid) {
        if (mBluetoothGatt == null) {
            return null;
        }
        return  mBluetoothGatt.getService(uuid);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic gattCharacterisitic, boolean enable) {
        if (mBluetoothGatt == null || gattCharacterisitic == null) {
            Log.d(TAG,"gattCharacterisitic == null");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(gattCharacterisitic, enable);
        BluetoothGattDescriptor descriptor = gattCharacterisitic.getDescriptor(BleDataManager.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            Log.d("wcy",descriptor.getUuid()+",,"+descriptor.getValue()+","+descriptor.getPermissions());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);

            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * connect a device
     *
     * @param address bleDevice.assress
     * @return if connect successful return true ,else return false
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
       /*if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()){
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }*/

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false,bluetoothGattCallback );
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * disconnect
     */
    public void disconnect() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * close the BluetoothGatt
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * read
     *
     * @param characteristic characteristic
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null) {
            return;
        }
        boolean isRead = mBluetoothGatt.readCharacteristic(characteristic);
        Log.d(TAG,"已经读到内容了 isRead =" +isRead);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null) {
            return;
        }
        if (mBluetoothGatt.writeCharacteristic(characteristic)) {
            Log.d(TAG,"writeCharacteristic done !");
        }
    }


}
