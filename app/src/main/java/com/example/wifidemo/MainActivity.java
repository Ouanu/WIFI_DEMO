package com.example.wifidemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.wifidemo.databinding.ActivityMainBinding;
import com.example.wifidemo.databinding.ItemWifiBinding;

import java.util.ArrayList;
import java.util.List;

import utils.WifiUtils;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding mainBinding;
    private MutableLiveData<List<ScanResult>> results = new MutableLiveData<>(new ArrayList<>());
    private WifiManager manager;
    private Toast toast;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.d("MainActivity", "onReceive: 刷新数据");
            } else if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                // 此处的广播一般用于监听wifi的启停。
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_DISABLED:
                        Log.d("MainActivity", "onReceive: wifi 关闭");
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        Log.d("MainActivity", "onReceive: wifi 打开");
                        break;
                    default:
                        break;
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        results.observe(this, new Observer<List<ScanResult>>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onChanged(List<ScanResult> scanResults) {
                if (mainBinding.wirelessRecyclerview.getAdapter() != null) {
                    mainBinding.wirelessRecyclerview.getAdapter().notifyDataSetChanged();
                    Log.d("MainActivity", "onChanged: " + scanResults.size());
                }
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
        }
        manager = (WifiManager) getSystemService(WIFI_SERVICE);
        mainBinding.wirelessRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        mainBinding.wirelessRecyclerview.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.right = 10;
                outRect.left = 10;
                outRect.top = 10;
                outRect.bottom = 10;
            }
        });
        mainBinding.wirelessRecyclerview.setAdapter(new ItemAdapter());
        mainBinding.refresh.setOnClickListener(v -> {
            List<ScanResult> scanResults = WifiUtils.scanResults(manager, false);
            results.postValue(scanResults);
        });

    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, ConnectDialog.ConnectCallback {
        ItemWifiBinding wifiBinding;
        String ssid;
        String capacities;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            wifiBinding = ItemWifiBinding.bind(itemView);
            itemView.setOnClickListener(this);
        }

        public void bind(String ssid, String capacities) {
            this.ssid = ssid;
            this.capacities = capacities;
            wifiBinding.capacities.setText(capacities);
            wifiBinding.ssid.setText(ssid);
        }

        @Override
        public void onClick(View v) {
            if (!capacities.contains("WPA")) {
                connect(ssid, "", capacities);
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putString("SSID", ssid);
            bundle.putString("CAPACITIES", capacities);
            ConnectDialog dialog = new ConnectDialog();
            dialog.setArguments(bundle);
            dialog.setCallback(this);
            dialog.show(getSupportFragmentManager(), "connect-tag");
        }

        @Override
        public void connect(String ssid, String passwd, String capacities) {
            MainActivity.this.connect(ssid, passwd, capacities);
        }
    }

    public void connect(String ssid, String passwd, String capacities) {
        WifiConfiguration wifiInfo = WifiUtils.createWifiInfo(ssid, passwd, false, capacities);
        boolean b = WifiUtils.connectWifi(manager, wifiInfo);
        if (b) {
            showToast("连接成功");
        } else {
            showToast("连接失败");
        }
    }

    public void showToast(String msg) {
        if (toast != null) {
            toast.cancel();
        }
        toast = new Toast(this);
        toast.setText(msg);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }
    class ItemAdapter extends RecyclerView.Adapter<ItemViewHolder> {

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemWifiBinding wifiBinding = ItemWifiBinding.inflate(getLayoutInflater(), parent, false);
            return new ItemViewHolder(wifiBinding.getRoot());
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            if (results.getValue() == null) {
                return;
            }
            holder.bind(results.getValue().get(position).SSID, results.getValue().get(position).capabilities);
        }

        @Override
        public int getItemCount() {
            if (results.getValue() == null) {
                return 0;
            }
            return results.getValue().size();
        }
    }
}