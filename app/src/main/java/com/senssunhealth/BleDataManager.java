package com.senssunhealth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.senssunhealth.userentities.UserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by wucaiyan on 2017/11/14.
 */

public class BleDataManager implements BluetoothLeService.IDataAvailableListener, BluetoothLeService.IServicesDiscoveredListener, BluetoothLeService.IConnectedListener {
    private final static String TAG ="BleDataManager";
    public static BluetoothGattCharacteristic gattCharacteristic_write;
    public static BluetoothGattCharacteristic gattCharacteristic_notify;
    private BluetoothLeService mBluetoothLeService;

    //体重秤与体脂秤数据传输协议  03xx
    public final static int FUCTION_NUM = 0x03;

    //智能终端至硬件设备的数据请求／应答  data 00（正常接收） /0a(中断)
    public final static int REQUEST_APP_TO_HARDWARE = 0x00;

    //硬件设备至智能终端的应答／请求
    public final static int REPONSE_HARDWARE_TO_APP = 0x80;

    protected static final UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static String UUID_UNLOCK_DATA_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static String UUID_UNLOCK_DATA_NOTIFY = "0000fff1-0000-1000-8000-00805f9b34fb";
    public static String UUID_UNLOCK_DATA_WRITE = "0000fff2-0000-1000-8000-00805f9b34fb";
    public static String ACTION_BLE_GET_DATA = "com.senssunhealth.ACTION_BLE_GET_DATA";
    public static String ACTION_BLE_MANUFACTIRE_DATA = "com.senssunhealth.ACTION_BLE_MANUFACTIRE_DATA";
    public static String ACTION_BLE_CONNECT_STATE = "com.senssunhealth.ACTION_BLE_CONNECT_STATE";

    public static String TYPE_DATA_ALL_USERINFO = "com.senssunhealth.TYPE_DATA_USERINFOS";
    public static String TYPE_DATA_FAT_RATIO = "com.senssunhealth.TYPE_DATA_USERINFOS";
    public static String TYPE_DATA_TEMP_WEIGHT = "com.senssunhealth.TYPE_DATA_TEMP_WEIGHT";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public  final static byte OPTION_SYNC_DATA_DEFAULED = 0x00;
    public  final static byte OPTION_DELETE_USER_DATA = (byte) 0xaa;





    private Context mContext;

    /**
     * 保存0307 查询的用户信息
     * */
    private List<UserInfo> userInfos = new ArrayList<>();
    private List<String> userHexString = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter;
    private boolean isSupportLe;
    private boolean isBluetoothEnabled;
    private List<BluetoothDevice> devices = new ArrayList<>();
    private String mDeviceAddress;
    private Handler mHandler;
    private byte[] userInfo;
    private String baseWeight;
    private boolean isRespond;
    private boolean isStartSetUser;
    private static int count =  0;
    // 标志是否开始扫描，最多1分钟
    public boolean isStartScan;


    public BleDataManager(Context context,BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothLeService = new BluetoothLeService();
        mBluetoothLeService.initialize();
        mHandler = new Handler();
        mBluetoothLeService.setDataAvailableListener(this);
        mBluetoothLeService.setServicesDiscoveredListener(this);
        mBluetoothLeService.setStateConnected(this);

    }


    @Override
    public void onServicesDiscovered(BluetoothGatt bluetoothGatt) {
        Log.d(TAG,"onServicesDiscovered");
        displayGattServices(mBluetoothLeService.getSupportedGattServices());
    }
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG,"onCharacteristicRead");
        if (characteristic.getValue() != null) {
            Log.d(TAG,"onCharacteristicRead UUID ="+characteristic.getUuid()+
                    "\n" +",value ="+Utils.bytesToHexString(characteristic.getValue()));
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG,"onCharacteristicWrite");
        if (characteristic.getValue() != null) {
            Log.d(TAG,"onCharacteristicWrite UUID ="+characteristic.getUuid()+
                    "\n" +",value ="+Utils.bytesToHexString(characteristic.getValue()));
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG,"onCharacteristicChanged");
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

    @Override
    public void onConnect(BluetoothGatt bluetoothGatt,int status,int newStatus) {
        Log.d(TAG,"onConnect");
        Intent intent =new Intent(ACTION_BLE_CONNECT_STATE);
        if (newStatus == BluetoothProfile.STATE_CONNECTED) {
            intent.putExtra(ACTION_BLE_CONNECT_STATE,newStatus);
        } else if (newStatus == BluetoothProfile.STATE_DISCONNECTED){
            intent.putExtra(ACTION_BLE_CONNECT_STATE,newStatus);
        }
        sendBroadcastForResult(intent);
    }

    /**
     * 发送结果广播
     * */

    public void  sendBroadcastForResult(Intent intent){;
        mContext.sendBroadcast(intent);
    }
    /**
     * 获取当前所有电子秤中所有的用户信息
     * */
    public List<UserInfo> getInfoFromEleScales (){
        if (userInfos == null) {
            userInfos = new ArrayList<>();
        }


        return userInfos;
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


    /**
     * 同步用户信息
     * option  0x00 /0x01 /0x02  新增／修改／删除///定义19个字节字符的数组
     * */
    public void writeUserInfoToScale(byte option,byte[] userInfoByte){
        byte[] temp ;
        UserInfo userInfoO = null;
        if (userInfos.size() != 0) {
            userInfoO = userInfos.get(1);
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
        /*byte[] temp ;
        //连接以后设置用户的信息
        if (baseWeight == null) {
            baseWeight = "0000";
        } else {
            Log.d(TAG,"baseWeight =="+baseWeight.toString());
        }
        userInfo = new byte[]{0x10, 0x00, 0x00, (byte) 0xc5, 0x13, 0x03, 0x01, 0x00, 0x06, (byte) 0x90, (byte) 0x81, 0x01, (byte) 0x9e, 0x1a, 0x01, 0x00,0x00,0x00,0x00};
        temp = Utils.getRandomNum(4);
        if (temp.length>1) {
            userInfo[9] = temp[0];
            userInfo[10] = temp[1];
        }
        temp = Utils.hexString2Bytes(baseWeight);
        if (temp.length>1) {
            userInfo[16] = temp[0];
            userInfo[17] = temp[1];
        }*/
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
     * 接收临时数据
     * */
    public void doneCharacteristicChangedData (BluetoothGatt gatt, BluetoothGattCharacteristic character) {
        String optionCMMD = null;
        String dataOutHead =null;
        Intent intent;
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
                        if (!isRespond && isStartSetUser && count <2 && baseWeight != null) {
                            byte[] jjj= new byte[]{0x00,0x00};
                            writeUserInfoToScale((byte) 0x00,jjj);
                        }
                        intent = new Intent(ACTION_BLE_GET_DATA);
                        intent.putExtra(TYPE_DATA_TEMP_WEIGHT,dataOutHead);
                        sendBroadcastForResult(intent);
                        break;
                    case Utils.CMMD_SET_USER_RESPOND:
                        count = count ++;
                        Log.d(TAG,"已经得到回复第"+count+"次");
                        dataOutHead = dataHex.substring(14,20);//设置用户返回标志和pin
                        break;
                    case Utils.CMMD_USERINFOS_RESPOND://返回所有用户信息，一次一条
                        isStartSetUser = false;
                        dataOutHead = dataHex.substring(14,34);
                        userHexString.add(dataOutHead.substring(16));
                        UserInfo userInfo = new UserInfo(dataHex.substring(16,18),dataHex.substring(18,22),dataHex.substring(22,24)
                                ,dataHex.substring(24,26),dataHex.substring(26,28),dataHex.substring(28,30),dataHex.substring(30,34));

                        if (!userInfos.contains(userInfo)) {
                            userInfos.add(userInfo);
                        }
                        if (character.getValue()[7] == 8) {
                            isStartSetUser = true;
                            intent = new Intent(TYPE_DATA_ALL_USERINFO);
                            sendBroadcastForResult(intent);
                        }
                        Log.d(TAG,"userinfos=="+userInfo.toString()+"all num ="+character.getValue()[7]+",size ="+userInfos.size());

                        break;
                    case Utils.CMMD_GET_RESULT_DATA:
                        //返回所测试的所有结果，可能在1416中的字段可看出是实时记录还是历史记录，如果是FF便是发送完毕和无记录
                        dataOutHead = dataHex.substring(14,56);
                        isRespond = true;
                        count = 0;
                        intent = new Intent(ACTION_BLE_GET_DATA);
                        intent.putExtra(TYPE_DATA_FAT_RATIO,dataOutHead);
                        sendBroadcastForResult(intent);
                        break;
                    default:
                        Log.d(TAG,"not same data");
                }
            }
        }
    }
    /**
     * 判断接收到的数据是否有效
     * */
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

    /**
     * 开始扫描
     * */
    public void startScan (BluetoothAdapter bluetoothAdapter,boolean enable,ICallBackResult iCallBackResult){
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

    public void connect(final String address) {
        if (address == null) {
            Log.d(TAG,"connect address null,please scan");
        }
        mHandler.postDelayed(mConnTimeOutRunnable,90*1000);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //真正开始连接设备
                mBluetoothLeService.connect(address);
                Intent intent = new Intent(ACTION_BLE_CONNECT_STATE);
                intent.putExtra(ACTION_BLE_CONNECT_STATE,BluetoothProfile.STATE_CONNECTING);
                sendBroadcastForResult(intent);
            }
        }, 100);
    }

    /**
     * 连接超时，回调
     */
    private Runnable mConnTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            //资源释放
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        }
    };



    public void getUserListFromHar(ICallBackResult iCallBack) {
        if (userInfos != null) {
            iCallBack.onSuccessed(userInfos);
        } else {
            iCallBack.onFailed();
        }
    }


    public interface ICallBackResult{
        void onSuccessed(Object object);
        void onFailed();
    }


    private BluetoothAdapter.LeScanCallback mlLeScanCallback = new BluetoothAdapter.LeScanCallback(){
        @SuppressLint("LongLogTag")
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device != null && !devices.contains(device)) {
                if ((device.getName() != null) && device.getName().equals("SENSSUN CLOUD")){
                    if (scanRecord !=null) {
                        String descripter = Utils.bytesToHex(scanRecord,scanRecord.length);
                        Log.d(TAG,"descripter 开始连接 =="+descripter);
                        Intent intent = new Intent(ACTION_BLE_MANUFACTIRE_DATA);
                        intent.putExtra(ACTION_BLE_MANUFACTIRE_DATA,descripter);
                        sendBroadcastForResult(intent);
                    }
                    mDeviceAddress = device.getAddress();
                    connect(mDeviceAddress);
                    mBluetoothAdapter.stopLeScan(this);
                    devices.add(device);
                    Log.d(TAG,"device name =" +device.toString()+","+device.getUuids()+","+device.getName()+","+device.getBondState()+","+device.getClass());
                }

            }

        }
    };





}
