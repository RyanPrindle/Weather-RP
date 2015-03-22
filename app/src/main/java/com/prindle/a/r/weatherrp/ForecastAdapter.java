package com.prindle.a.r.weatherrp;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by RP on 1/27/2015.
 */
public class ForecastAdapter extends BaseAdapter {
    private static final String TAG = ForecastAdapter.class.getSimpleName();
    protected Context mContext;
    protected List<FutureWeather> mForecast;

    public ForecastAdapter(Context context, List<FutureWeather> forecast) {
        mContext = context;
        mForecast = forecast;
    }


    @Override
    public int getCount() {
        return mForecast.size();
    }

    @Override
    public Object getItem(int position) {
        return mForecast.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        convertView = LayoutInflater.from(mContext).inflate(R.layout.forecast_day_item, null);

        holder = new ViewHolder();
        holder.iconImageView = (ImageView) convertView.findViewById(R.id.iconImageView);
        holder.dayLabel = (TextView) convertView.findViewById(R.id.dayTextView);
        holder.tempLabel = (TextView) convertView.findViewById(R.id.tempTextView);
        holder.tempLowLabel = (TextView) convertView.findViewById(R.id.tempLowTextView);
        holder.summaryLabel = (TextView) convertView.findViewById(R.id.summaryTextView);
        convertView.setTag(holder);
        holder.dayLabel.setText(mForecast.get(position).getDay());
        holder.iconImageView.setImageResource(mForecast.get(position).getIconId());
        holder.tempLabel.setText(mForecast.get(position).getHighTemperature() + "");
        holder.tempLowLabel.setText(mForecast.get(position).getLowTemperature() + "");
        holder.summaryLabel.setText(mForecast.get(position).getSummary());


        return convertView;
    }

    private static class ViewHolder {
        ImageView iconImageView;
        TextView dayLabel;
        TextView tempLabel;
        TextView tempLowLabel;
        TextView summaryLabel;
    }
}






