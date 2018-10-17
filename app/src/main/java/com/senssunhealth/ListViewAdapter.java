package com.senssunhealth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by wucaiyan on 17-11-7.
 */

public class ListViewAdapter extends BaseAdapter {
    private Context mContext;
    private List<BluetoothDevice> mlist;
    public ListViewAdapter(Context context, List<BluetoothDevice> bluetoothDeviceList){
        this.mContext = context;
        this.mlist = bluetoothDeviceList;
    }
    @Override
    public int getCount() {
        return mlist.size();
    }

    @Override
    public Object getItem(int position) {
        return mlist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder=null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(android.R.layout.simple_expandable_list_item_1,null,false);
            viewHolder = new ViewHolder();
            viewHolder.mTextView = (TextView) convertView.findViewById(android.R.id.text1);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        BluetoothDevice device = mlist.get(position);
        viewHolder.mTextView.setText(device.getName()+"\n"+device.getAddress()+"\n");
        Log.d("wcy myAdapter",device.getName()+"");
        return convertView;
    }

    class ViewHolder {
        private TextView mTextView;
    }
}
