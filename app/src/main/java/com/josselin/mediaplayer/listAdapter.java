package com.josselin.mediaplayer;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class listAdapter extends ArrayAdapter<String> {
    customButtonListener customListner;

    public interface customButtonListener {
        public void onButtonClickListner(int position,String value, String button, String url);
    }

    private Context context;
    private ArrayList<String> data = new ArrayList<String>();
    private ArrayList<String> url = new ArrayList<String>();

    public listAdapter(Context context, ArrayList<String> dataItem, ArrayList<String> urlItem) {
        super(context, R.layout.activity_listview, dataItem);
        this.data = dataItem;
        this.url = urlItem;
        this.context = context;
    }

    public void setCustomButtonListener(customButtonListener listener) {
        this.customListner = listener;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.activity_listview, null);
            viewHolder = new ViewHolder();
            viewHolder.text = (TextView) convertView.findViewById(R.id.text);
            viewHolder.play = (Button) convertView.findViewById(R.id.play);
            viewHolder.download = (Button) convertView.findViewById(R.id.download);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final String temp = getItem(position);
        viewHolder.text.setText(temp);
        viewHolder.play.setOnClickListener(new OnClickListener() {
            String button = "play";
            @Override
            public void onClick(View v) {
                if (customListner != null) {
                    String s_url = url.get(position);
                    customListner.onButtonClickListner(position, temp, button, s_url);
                }

            }
        });

        viewHolder.download.setOnClickListener(new OnClickListener() {
            String button = "download";
            @Override
            public void onClick(View v) {
                if (customListner != null) {
                    String s_url = url.get(position);
                    customListner.onButtonClickListner(position, temp, button, s_url);
                }

            }
        });

        return convertView;
    }

    public class ViewHolder {
        TextView text;
        Button play;
        Button download;
    }
}