package org.YourEyes.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class BusAdapter extends BaseAdapter {
    Context mContext = null;
    LayoutInflater mLayoutInfalter = null;
    ArrayList<RealTimeStationInfo.busArr> list;

    private TextView busNo;
    private TextView time;
    private TextView preStation;

    public BusAdapter(Context context, ArrayList<RealTimeStationInfo.busArr> data) {
        mContext = context;
        list = data;
        mLayoutInfalter = LayoutInflater.from(mContext);
    }

    @Override
    public int getCount() {
        return list.size();
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public RealTimeStationInfo.busArr getItem(int position) {
        return list.get(position);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.bus_item, null);

            busNo = (TextView)convertView.findViewById(R.id.busNo);
            time = (TextView)convertView.findViewById(R.id.time);
            preStation = (TextView)convertView.findViewById(R.id.preStation);
        }

        busNo.setText(list.get(position).getRouteno()+"번 ");
        time.setText(list.get(position).getArrtime()+"분 후 도착예정 ");
        preStation.setText(list.get(position).getPrestationcnt()+"정거장 전");
        return convertView;
    }
}
