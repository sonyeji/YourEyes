package org.YourEyes.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class StatAdapter extends BaseAdapter {
    Context mContext = null;
    LayoutInflater mLayoutInfalter = null;
    ArrayList<BusActivity.Station> list;

    TextView name;
    TextView id;
    public StatAdapter(Context context, ArrayList<BusActivity.Station> data) {
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
    public BusActivity.Station getItem(int position) {
        return list.get(position);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item, null);
            name = (TextView)convertView.findViewById(R.id.stationName);
            id = (TextView)convertView.findViewById(R.id.stationID);
        }

        name.setText(list.get(position).getName());
        id.setText("("+list.get(position).getId()+")");

        return convertView;
    }

}
