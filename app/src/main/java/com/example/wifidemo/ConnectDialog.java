package com.example.wifidemo;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.wifidemo.databinding.DialogConnectBinding;

import java.util.Objects;

public class ConnectDialog extends DialogFragment {
    DialogConnectBinding connectBinding;
    String ssid;
    String passwd;
    String capacities;
    ConnectCallback callback;
    public interface ConnectCallback {
        void connect(String ssid, String passwd, String capacities);
    }

    public void setCallback(ConnectCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Material_Dialog_NoActionBar_MinWidth);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        connectBinding = DialogConnectBinding.inflate(inflater, container, false);
        Objects.requireNonNull(Objects.requireNonNull(getDialog()).getWindow()).setBackgroundDrawableResource(R.color.translate);
        if (getArguments() == null || (ssid = getArguments().getString("SSID", null)) == null
                || (capacities = getArguments().getString("CAPACITIES", null)) == null) {
            dismiss();
        }
        connectBinding.ssid.setText(ssid);
        connectBinding.passwd.setText("");
        connectBinding.connect.setOnClickListener(v -> {
            passwd = Objects.requireNonNull(connectBinding.passwd.getText()).toString();
            callback.connect(ssid, passwd, capacities);
            dismiss();
        });
        return connectBinding.getRoot();
    }
}
