package com.langiappeworkshop.evidentweather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.langiappeworkshop.evidentweather.data.ApiUtils;
import com.langiappeworkshop.evidentweather.data.ForecastResponse;
import com.langiappeworkshop.evidentweather.data.Forecastday_;
import com.langiappeworkshop.evidentweather.data.RetroFitInterface;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 *  This class {@link DayListFragment} is our main fragment and does most of the work of
 *  our app, including loading and displaying the data.
 */
public class DayListFragment extends Fragment {

    // Our daily weather is loaded into a RecyclerView
    private RecyclerView mDayRecyclerView;

    // local copy of our daily weather data
    private List<Forecastday_> mForecastDayList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // retain instance in case we rotate in the middle of a download.
        setRetainInstance(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_day_list, container, false);

        // intialize our RecyclerView
        mDayRecyclerView = (RecyclerView) rootView.findViewById(R.id.day_list);
        assert mDayRecyclerView != null;
        mDayRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (!mForecastDayList.isEmpty()) {
            setupRecyclerView();
        } else {
            // start our data download task, if our list is empty
            loadForecastDays();
        }

        return rootView;
    }

    private void setupRecyclerView() {
        // check if fragment has been added to Activity before we setup the RecyclerView
        if (isAdded()) {
            RVAdapter rvAdapter = new RVAdapter(getActivity(), mForecastDayList);
            mDayRecyclerView.setAdapter(rvAdapter);
        }
    }

    /**
     *  Our Recycler Adapter class
     */
    private class RVAdapter extends RecyclerView.Adapter<RVAdapter.DayViewHolder> {
        private Context mContext;

        // reference to our weather data
        List<Forecastday_> dayList;

        private RVAdapter(Context mContext, List<Forecastday_> dayList) {
            this.mContext = mContext;
            this.dayList = dayList;
        }

        @SuppressLint("InflateParams")
        @Override
        public DayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.day_list_content, null);
            return new DayViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DayViewHolder holder, int i) {

            final Forecastday_ day = dayList.get(i);
            if (!TextUtils.isEmpty(day.getIconUrl()/*.imageURL*/)) {

                // we are using Picasso library to load individual images because
                // that's what it's made for!
                Picasso.with(getActivity())
                        .load(day.getIconUrl())
                        .placeholder(R.mipmap.ic_launcher)
                        .into(holder.ivIcon);
            } else {
                // if we get an empty image URL, just default to Andy
                holder.ivIcon.setImageResource(R.mipmap.ic_launcher);
            }

            holder.tvSummary.setText(day.getConditions()==null?"":day.getConditions());

            // Format Date
            String strDate = "";
            String month = day.getDate().getMonth()==null ? "" : ""+ day.getDate().getMonth();
            String strDay = day.getDate().getDay()==null ? "" : ""+ day.getDate().getDay();
            String year = day.getDate().getYear()==null ? "" : ""+ day.getDate().getYear();
            String weekday = day.getDate().getWeekday()==null ? "" : day.getDate().getWeekday();
            if (!TextUtils.isEmpty(month) && !TextUtils.isEmpty(strDay) && !TextUtils.isEmpty(year)) {
                strDate = weekday +"  "+ month + "/" + strDay + "/" + year;
            }
            holder.tvDate.setText(strDate);

            // Format Precip
            String precip = day.getPop()==null ? "Precip:" : "Precip: "+ day.getPop() +"%";
            holder.tvPrecip.setText(precip);

            // Format Lo
            String lo = "Lo:";
            if (day.getLow() != null) {
                lo = day.getLow().getFahrenheit()==null ? "Lo:" : "Lo: " + day.getLow().getFahrenheit() + "\u00b0";
            }
            holder.tvLo.setText(lo);

            // Format Hi
            String hi = "Hi:";
            if (day.getHigh() != null) {
                hi = day.getHigh().getFahrenheit()==null ? "Hi:" : "Hi: " + day.getHigh().getFahrenheit() + "\u00b0";
            }
            holder.tvHi.setText(hi);

            // Format Humid
            String humid = day.getAvehumidity()==null ? "Humid:" : "Humid: "+day.getAvehumidity()+"%";
            holder.tvHumid.setText(humid);

            // Format Wind
            String wind = "Wind:";
            if (day.getAvewind() != null) {
                if (day.getAvewind().getMph()!=null  && day.getAvewind().getDir()!=null) {
                    wind = "Wind: " + day.getAvewind().getMph() + " " + day.getAvewind().getDir();
                }
            }

            holder.tvWind.setText(wind);
        }

        @Override
        public int getItemCount() {
            return dayList.size();
        }

        /**
         *  Our ViewHolder for view items associated with our 10 days of weather.
         */
        class DayViewHolder extends RecyclerView.ViewHolder {
            private ImageView ivIcon;
            private TextView tvDate;
            private TextView tvSummary;
            private TextView tvPrecip;
            private TextView tvLo;
            private TextView tvHi;
            private TextView tvHumid;
            private TextView tvWind;

            DayViewHolder(View dayView) {
                super(dayView);
                ivIcon = (ImageView) dayView.findViewById(R.id.ivIcon);
                tvDate = (TextView) dayView.findViewById(R.id.tvDate);
                tvSummary = (TextView) dayView.findViewById(R.id.tvSummary);
                tvPrecip = (TextView) dayView.findViewById(R.id.tvPrecip);
                tvLo = (TextView) dayView.findViewById(R.id.tvLo);
                tvHi = (TextView) dayView.findViewById(R.id.tvHi);
                tvHumid = (TextView) dayView.findViewById(R.id.tvHumid);
                tvWind = (TextView) dayView.findViewById(R.id.tvWind);
            }
        }
    }

    public void loadForecastDays() {
        RetroFitInterface service = ApiUtils.getRetroFitInterface();
        service.getForecastResponse().enqueue(new Callback<ForecastResponse>() {
            @Override
            public void onResponse(@NonNull Call<ForecastResponse> call, @NonNull Response<ForecastResponse> response) {
                if(response.isSuccessful()) {
                    try {
                        ForecastResponse forecastResponse = response.body();
                        if (forecastResponse!=null) {
                            mForecastDayList = forecastResponse.getForecast().getSimpleforecast().getForecastday();
                            Log.i("DayListFragment", "Parsing data successful");
                            setupRecyclerView();
                        }

                    } catch (Exception e) {
                        Log.d("DayListFragment", e.getMessage());
                    }

                } else {
                    int statusCode  = response.code();
                    Log.d("DayListFragment", ""+statusCode);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ForecastResponse> call, @NonNull Throwable t) {
                Log.d("DayListFragment", "error loading from API");
            }

        });
    }

}
