package com.senssunhealth;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class Main2Activity extends AppCompatActivity {

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mContext = getApplicationContext();
        BleDataManager bleDataManager = new Bl;


    }

    public class MResiurce {
        public int getIdByName(Context context, String classname,String resName) {
            String packageName = context.getPackageName();
            int id = 0;
            Class r = null;
            try {
                r = Class.forName(classname+".R");

            Class[] classes = r.getClasses();
            Class desireClass = null;
            for (Class cls:classes) {
                if (cls.getName().split("\\$")[1].equals(classname)) {
                    desireClass = cls;
                    break;
                }
            }
            if (desireClass != null) {
                id = desireClass.getField(resName).getInt(desireClass);
            }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            return id;
        }
    }
}
