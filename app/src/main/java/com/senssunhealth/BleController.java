package com.senssunhealth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.senssunhealth.userentities.UserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.senssunhealth.BleDataManager.ACTION_BLE_MANUFACTIRE_DATA;

public class BleController extends AppCompatActivity implements View.OnClickListener, BluetoothLeService.IServicesDiscoveredListener,  BluetoothLeService.IDataAvailableListener, AdapterView.OnItemClickListener {
    private final static String TAG = "BluetoothMainActivity";
    private final static int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private Context context;

    private Button openBt;
    private TextView dataView;
    private Button offBt;
    private Button scanBt;
    private ListView deviceList;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean isSupportLe;
    private boolean isBluetoothEnabled;
    private List<BluetoothDevice> devices = new ArrayList<>();
    private BluetoothLeService mBluetoothLeService;
    private ListViewAdapter mAdapter;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private Button readBt;
    private String mDeviceAddress;

    /**
     * 保存0307 查询的用户信息
     * */
    private List<UserInfo> userInfos = new ArrayList<>();
    private List<String> userHexString = new ArrayList<>();


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private Handler mHandler;
    private Button write;
    private byte[] userInfo;
    private String baseWeight;
    private boolean isRespond;
    private boolean isStartSetUser;
    private static int count =  0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        mHandler = new Handler();
        context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initBluetoothService(mBluetoothAdapter);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothLeService!=null) {
            mBluetoothLeService.setServicesDiscoveredListener(this);
            mBluetoothLeService.setDataAvailableListener(this);
        }
        if (isBluetoothEnabled) {
            startScan(mBluetoothAdapter,true);
        }

    }

    private void initBluetoothService(BluetoothAdapter mBluetoothAdapter) {
        if (mBluetoothAdapter == null) {
            return;
        }
        isSupportLe = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        Toast.makeText(this,R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        isBluetoothEnabled = mBluetoothAdapter.isEnabled();
    }

    private void initView() {
        openBt = (Button)findViewById(R.id.open_button);
        offBt = (Button)findViewById(R.id.off_button);
        scanBt = (Button)findViewById(R.id.scan_button);
        dataView = (TextView) findViewById(R.id.textView);
        deviceList = (ListView)findViewById(R.id.listView);
        readBt = (Button)findViewById(R.id.read);
        write=(Button)findViewById(R.id.write);
        openBt.setOnClickListener(this);
        offBt.setOnClickListener(this);
        scanBt.setOnClickListener(this);
        readBt.setOnClickListener(this);
        write.setOnClickListener(this);
        initListView(devices);


    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.open_button:
                if(!isBluetoothEnabled) {
                    mBluetoothAdapter.enable();
                }
                break;
            case R.id.scan_button:
                startScan(mBluetoothAdapter,false);
                break;

            case R.id.read:

                break;

            case R.id.write:

                break;


        }

    }

    private void initListView(List<BluetoothDevice> list) {
        Log.d("wcy","init");
        if (list.size() == 0) {
            return;
        }
        if (mAdapter == null) {
            mAdapter = new ListViewAdapter(this,list);
            deviceList.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }

       deviceList.setOnItemClickListener(this);
    }


    private boolean isStartScan = false;
    private BluetoothAdapter.LeScanCallback mlLeScanCallback = new BluetoothAdapter.LeScanCallback(){

        @SuppressLint("LongLogTag")
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            //
             //Log.d(TAG,"device name =" +"evice.toString()");
            String descripter= null;
            if (scanRecord != null) {
                descripter=Utils.getHexString(scanRecord);
                Log.d(TAG, "descripter 开始连接 ==" + descripter);

                if (scanRecord[0] == 0xFA && scanRecord[1] == 0xFB) {
                    //Log.d(TAG, "descripter 开始连接 ==" + descripter);
                    //send MANUFACTIRE_DATA
                    Intent intent = new Intent(ACTION_BLE_MANUFACTIRE_DATA);
                    intent.putExtra(ACTION_BLE_MANUFACTIRE_DATA, descripter);

                    if (descripter!=null && device != null && !devices.contains(device)) {
                        if (device.getName() != null) {
                            mDeviceAddress = device.getAddress();
                            mBluetoothLeService.connect(mDeviceAddress);
                            mBluetoothAdapter.stopLeScan(this);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    devices.add(device);
                                    initListView(devices);
                                }
                            });
                            //devices.add(device);
                            Log.d(TAG, "device name =" + device.toString() + "," + device.getUuids() + "," + device.getName() + "," + device.getBondState() + "," + device.getClass());
                        }
                    }
                }
            }
        }
           /* if (device != null && !devices.contains(device)) {
                if ((device.getName() != null) && device.getName().equals("SENSSUN CLOUD")){

                    if (scanRecord !=null) {
                        String descripter = Utils.bytesToHex(scanRecord,scanRecord.length);
                        Log.d(TAG,"descripter 开始连接 =="+descripter);
                    }
                    mDeviceAddress = device.getAddress();
                    mBluetoothLeService.connect(mDeviceAddress);
                    mBluetoothAdapter.stopLeScan(this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            devices.add(device);
                            initListView(devices);
                        }
                    });
                    Log.d(TAG,"device name =" +device.toString()+","+device.getUuids()+","+device.getName()+","+device.getBondState()+","+device.getClass());
                }

            }

        }*/
    };

    public void startScan (BluetoothAdapter bluetoothAdapter,boolean enable){
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isStartScan = false;
                    mBluetoothAdapter.stopLeScan(mlLeScanCallback);
                    Log.d(TAG,"停止扫描");
                }
            }, 60*1000);

            isStartScan = true;
            Log.d(TAG,"开始扫描");
            mBluetoothAdapter.startLeScan(mlLeScanCallback);
        } else {
            isStartScan = false;
            Log.d(TAG,"停止扫描");
            mBluetoothAdapter.stopLeScan(mlLeScanCallback);
        }

    }

    public void stopScan (BluetoothAdapter bluetoothAdapter) {
        bluetoothAdapter.startLeScan(mlLeScanCallback);
    }


    @Override
    public void onServicesDiscovered(BluetoothGatt bluetoothGatt) {
        userInfos.clear();
        displayGattServices(mBluetoothLeService.getSupportedGattServices());
    }




    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
        StringBuilder stringBuilder = null;
        byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
        }
        if (gatt.getService(characteristic.getUuid()) != null) {
            UUID service = gatt.getService(characteristic.getUuid()).getUuid();
            List<BluetoothGattCharacteristic> chars = gatt.getService(characteristic.getUuid()).getCharacteristics();

            for (BluetoothGattCharacteristic chara : chars) {
                Log.d(TAG, "service==" + service + ",chara==UUID==" + chara.getUuid() + ",de= " + chara.getPermissions() + ",dede==" + chara.getProperties());
            }
        }

        Log.d(TAG, "get read ==" + characteristic.getUuid() + ",," + new String(data) + "\n" + stringBuilder.toString());

        Log.d(TAG,"readvalue ==="+new String(characteristic.getValue()));

    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        StringBuilder stringBuilder = null;
        byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
        }
        if (stringBuilder != null) {
            Log.d(TAG, "get write ==" + characteristic.getUuid() + ",,"  + stringBuilder.toString());
        }





    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().toString().equals(BleDataManager.UUID_UNLOCK_DATA_NOTIFY)) {
            if (characteristic.getValue() != null) {
                String tempData = Utils.bytesToHex(characteristic.getValue(),characteristic.getValue().length);
                //Log.d(TAG,"临时数据一直在变=="+Integer.parseInt(tempData.substring(15,17),16)*10+"."+Integer.parseInt(tempData.substring(17,19),16));
                if (tempData.substring(24,26).equals("AA")){
                   Log.d(TAG,"result 不变了 datd=="+Utils.bytesToHexString(characteristic.getValue()));
                    baseWeight = tempData.substring(14,18);
                }
                doneCharacteristicChangedData(gatt,characteristic);
            }
        }
    }

    public void doneCharacteristicChangedData (BluetoothGatt gatt, BluetoothGattCharacteristic character) {
        String optionCMMD = null;
        String dataOutHead =null;
        if (character.getUuid().toString().equals(BleDataManager.UUID_UNLOCK_DATA_NOTIFY)) {
            if (character.getValue() == null) {
                Log.d(TAG,"character.getUuid().toString() not respond data");
            }  else {
                String dataHex = Utils.bytesToHex(character.getValue(),character.getValue().length);
                Log.d(TAG,"doneCharacteristicChangedData="+dataHex);

                optionCMMD = dataHex.substring(10,14);
                if (isInvalidForResult(optionCMMD,dataHex)) {
                    return;
                }
                isStartSetUser = true;
                switch (optionCMMD) {
                    case Utils.CMMD_GET_TEMP_DATA:
                        dataOutHead = dataHex.substring(14,24); //weight
                        if (!isRespond && isStartSetUser && count <3) {
                            byte[] jjj= new byte[]{0x00,0x00};
                            writeUserInfoToScale((byte) 0x00,jjj);
                        }
                        break;
                    case Utils.CMMD_SET_USER_RESPOND:
                        count = count +1;
                        Log.d(TAG,"已经得到回复第"+count+"次");
                        dataOutHead = dataHex.substring(14,20);//设置用户返回标志和pin
                        break;
                    case Utils.CMMD_USERINFOS_RESPOND://返回所有用户信息，一次一条
                        isStartSetUser = false;
                        UserInfo userInfo;
                        dataOutHead = dataHex.substring(14,34);
                        userHexString.add(dataOutHead.substring(16));
                        userInfo = new UserInfo(dataHex.substring(16,18),dataHex.substring(18,22),dataHex.substring(22,24)
                                ,dataHex.substring(24,26),dataHex.substring(26,28),dataHex.substring(28,30),dataHex.substring(30,34));

                        if (!userInfos.contains(userInfo)) {
                            userInfos.add(userInfo);
                        }

                        if (character.getValue()[7] == 8) {

                        }
                        Log.d(TAG,"userinfos=="+userInfo.toString()+"all num ="+character.getValue()[7]+",size="+userInfos.size());

                        break;
                    case Utils.CMMD_GET_RESULT_DATA:
                        //返回所测试的所有结果，可能在1416中的字段可看出是实时记录还是历史记录，如果是FF便是发送完毕和无记录
                        dataOutHead = dataHex.substring(14,56);
                        isRespond = true;
                        count = 0;
                        break;
                    default:
                        Log.d(TAG,"not same data");
                }
            }
        }
    }

    private boolean isInvalidForResult(String optionCMMD,String dataOutHead) {
        int lenght = dataOutHead.length();
        Log.d(TAG,"isInvalidForResult option="+optionCMMD+" ,dataOutHead="+dataOutHead+",len ="+lenght);
        if (optionCMMD.equals(Utils.CMMD_GET_TEMP_DATA) && lenght < 28){
            return true;
        } else if (optionCMMD.equals(Utils.CMMD_USERINFOS_RESPOND) && lenght < 36) {
            return true;
        } else if (optionCMMD.equals(Utils.CMMD_SET_USER_RESPOND) && lenght < 22) {
            return true;
        } else if (optionCMMD.equals(Utils.CMMD_GET_RESULT_DATA) && lenght < 58){
            return true;
        } else {
            return false;
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
        if (mBluetoothLeService!= null)
        mBluetoothLeService.connect(device.getAddress());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO request success
                    Log.d(TAG,"wcy request success");
                }
                break;
        }

    }

    /**
     * get the supported characteristics , maybe need to change
     *
     * @param gattServices gattServices
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return;
        }
        for (BluetoothGattService gattService : gattServices) {
            Log.d(TAG,"getService ===="+gattService.getUuid()+",,"+gattService.getType());
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Log.d(TAG,"gattCharacteristic uuid =="+gattCharacteristic.getUuid()+",,"+gattCharacteristic.getValue()+",,"+gattCharacteristic.getProperties());
                if (gattCharacteristic.getUuid().toString().equals(BleDataManager.UUID_UNLOCK_DATA_NOTIFY)) {
                    BleDataManager.gattCharacteristic_notify = gattCharacteristic;
                    mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
                }
                if (gattCharacteristic.getUuid().toString().equals(BleDataManager.UUID_UNLOCK_DATA_WRITE)) {
                    BleDataManager.gattCharacteristic_write = gattCharacteristic;
                }
            }


        }
    }

    public void setCharacteristicType(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        if (bluetoothGattCharacteristic != null) {
            final int charaProp = bluetoothGattCharacteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
                //mBluetoothLeService.readCharacteristic(bluetoothGattCharacteristic);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic = bluetoothGattCharacteristic;
                mBluetoothLeService.setCharacteristicNotification(
                        bluetoothGattCharacteristic, true);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    /**
     * option  0x00 /0x01 /0x02  新增／修改／删除///定义19个字节字符的数组
     * */
    public void writeUserInfoToScale(byte option,byte[] userInfoByte){
        byte[] temp ;
        UserInfo userInfoO = null;
        if (userInfos.size() != 0) {
            userInfoO = userInfos.get(1);
        }

        //连接以后设置用户的信息
        if (baseWeight == null) {
            baseWeight = "0000";
        } else {
            Log.d(TAG,"baseWeight =="+baseWeight.toString());
        }
        userInfo = new byte[]{0x10, 0x00, 0x00, (byte) 0xc5, 0x13, 0x03, 0x01, 0x00, 0x06, (byte) 0x90, (byte) 0x81, 0x01, (byte) 0x9e, 0x1a, 0x01, 0x00,0x00,0x3D,0x00};
        if (userInfoO != null) {
            userInfo[8] = Byte.parseByte(userInfoO.getSerialNum());
            temp = Utils.hexString2Bytes(userInfoO.getPinId());
            if (temp.length>1) {
                userInfo[9] = temp[0];
                userInfo[10] = temp[1];
            }
            temp = Utils.hexString2Bytes(userInfoO.getSexId());
            if (temp.length>0) {
                userInfo[11] = temp[0];
            }
            temp = Utils.hexString2Bytes(userInfoO.getHeight());
            if (temp.length>0) {
                userInfo[12] = temp[0];
            }
            temp = Utils.hexString2Bytes(userInfoO.getAge());
            if (temp.length>0) {
                userInfo[13] = temp[0];
            }
            temp = Utils.hexString2Bytes(userInfoO.getPhys());
            if (temp.length>0) {
                userInfo[14] = temp[0];
            }
            temp = Utils.hexString2Bytes(userInfoO.getWeight());
            if (temp.length>1) {
                userInfo[16] = temp[0];
                userInfo[17] = temp[1];
            }
        }
        temp = Utils.constructionCheckCode(Utils.getHexString(userInfo));
        if (temp.length>0) {
            userInfo[18] = temp[0];
        }

        if (userInfo != null) {
            BleDataManager.gattCharacteristic_write.setValue(userInfo);
        }
        mBluetoothLeService.writeCharacteristic(BleDataManager.gattCharacteristic_write);
    }

     /**
      * 手机请求同步
      * pinHex userPin 2byte 7,8
      * optionTag : 00-update new data
      *             1-50 recent 1-50 datas
      *             0xaa  delete all data
      * **/
    public void delectedUserInfo(byte[] pinHex,byte optionTag){
        byte[] bytes = {0x10, 0x00,0x00,(byte) 0xc5, 0x0b, 0x03, 0x02, 0x00, 0x00, 0x00, 0x00};
        if (pinHex.length < 2) {
            Log.d(TAG,"pin error defauled 0000");
        } else {
            bytes[7] = pinHex[0];
            bytes[8] = pinHex[1];
        }
        if (Integer.valueOf(optionTag) != 0) {
            bytes[9] = optionTag;
        } else {
            bytes[9] = BleDataManager.OPTION_SYNC_DATA_DEFAULED;
        }
    }


}
