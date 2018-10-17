package com.senssunhealth;

import android.util.Log;

import java.util.Random;

/**
 * Created by wucaiyan on 17-11-9.
 */

public class Utils {
    private final static String TAG ="PaserUtils";
    private final static boolean DEBUG = false;
    //厂商的ID：占两个字节 协议ID：0x01 产品ModeID：0x0115
    public final static int COUNT_MANUFACTURERS_DATA = 11;

    static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    public final static byte[] CMMD_QUERY_USER = {0x10,0x00,0x00, (byte) 0xC5,0x08,0x03,0x07,0x12};
    public final static String CMMD_SET_USER_REQUEST = "0301";
    public final static String CMMD_USERINFOS_RESPOND = "0387";
    public final static String CMMD_SET_USER_RESPOND = "0381";
    public final static String CMMD_GET_TEMP_DATA = "0380";
    public final static String CMMD_GET_RESULT_DATA = "0382";

    public static String bytesToHex(byte[] bytes,int count) {
        if (bytes.length > count) {
            bytes = subBytes(bytes,0,count);
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
   /**
    * 将字节转换成16进制字符串
    * */
    public static String bytesToHexString(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            if ((src[i]+"").length()>3) {
                String hv = (src[i] + "").substring(2);

                if (hv.length() < 2) {
                    stringBuilder.append(0);
                }
                stringBuilder.append(hv);
            }
        }
        return stringBuilder.toString();
    }

    public static String getHexString(byte[] data) {
        StringBuffer stringBuffer = null;
        if (data != null && data.length > 0) {
            stringBuffer = new StringBuffer(data.length);
            for (byte byteChar : data) {
                stringBuffer.append(String.format("%02X", byteChar));
            }
        }
        return stringBuffer.toString();
    }

    //截取字节数组
    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }

     /**
      * 从十六进制字符串到字节数组转换
      *  hexstr 0x00
      * */
    public static byte[] hexString2Bytes(String hexstr) {
        int length=hexstr.length();
        if(length%2!=0){
            throw new RuntimeException("Hex  bit string length must be even");
        }
        byte[] b = new byte[hexstr.length() / 2];
        int j = 0;
        for (int i = 0; i < b.length; i++) {
            char c0 = hexstr.charAt(j++);
            char c1 = hexstr.charAt(j++);
            Log.d(TAG,"getchar c0=="+c0+",c1=="+c1);
            b[i] = (byte) ((parse(c0) << 4) | parse(c1));
            if (DEBUG) {
                Log.d(TAG,"b[+"+i+"]"+"parsec0="+(parse(c0)<<4)+"cparsec1="+parse(c1)+","+b[i]);
            }
        }
        return b;
    }

    private static int parse(char c) {
        if (c >= 'a')
            return (c - 'a' + 10) & 0x0f;
        if (c >= 'A')
            return (c - 'A' + 10) & 0x0f;
        return (c - '0') & 0x0f;
    }
    /**
     *  构造验证码
     * */
    public static byte[] constructionCheckCode (String data) {
        if (data == null || data.equals("")) {
            return hexString2Bytes(00+"");
        }
        int len = data.length();
        int num = 8;
        if (len <8) {
            throw new RuntimeException("Character length must be greater than 8");
        }
        Log.d(TAG,"constructionCheckCode==len=="+len);
        int total = 0;
        while (num < len) {
            total += Integer.parseInt(data.substring(num,num+2), 16);
            if (true) {
                Log.d(TAG,"data.substring(num,num+2)= "+data.substring(num,num+2)+"Integer="+Integer.parseInt(data.substring(num,num+2),16));
            }
            num = num + 2;
        }
        String hex = Integer.toHexString(total);
        len = hex.length();
        // 如果不够校验位的长度，补0,这里用的是两位校验
        if (len < 1) {
            hex = "00";
        } else if (len <2){
            hex ="0"+hex;
        } else {
            hex = hex.substring(len-2);
        }
        Log.d(TAG,"constructionCheckCode result =="+hexString2Bytes(hex));
        return hexString2Bytes(hex);
    }

    public static byte[] getRandomNum(int count){
        StringBuffer sb = new StringBuffer();
        String str = "0123456789";
        Random r = new Random();
        for(int i=0;i<count;i++){
            int num = r.nextInt(str.length());
            sb.append(str.charAt(num));
            str = str.replace((str.charAt(num)+""), "");
        }
        return hexString2Bytes(sb.toString());
    }
}
