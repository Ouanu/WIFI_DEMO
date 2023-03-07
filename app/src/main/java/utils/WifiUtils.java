package utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 无线网络管理器工具
 *
 * @author ouanu
 * @date 2023/03/06
 */
@SuppressWarnings({"deprecation", "unused"})
public class WifiUtils {

    /**
     * 扫描wifi
     *
     * @param manager WifiManager
     */
    public static void searchWifi(WifiManager manager) {
        manager.startScan();
    }


    /**
     * 获取扫描结果
     *
     * @param manager    WifiManager
     * @param needMerged 是否需要合并重名
     * @return {@link List}<{@link ScanResult}>
     */
    @SuppressLint("MissingPermission")
    public static List<ScanResult> scanResults(WifiManager manager, boolean needMerged) {
        List<ScanResult> scanResults = new ArrayList<>();
        if (!needMerged) {
            return manager.getScanResults();
        }
        // 合并重名
        HashSet<String> hs = new HashSet<>();

        for (ScanResult scanResult : manager.getScanResults()) {
            if (hs.add(scanResult.SSID)) {
                scanResults.add(scanResult);
            }
        }
        return scanResults;
    }

    /**
     * 得到配置好的网络
     *
     * @param context 上下文
     * @param manager WifiManager
     * @return 配置好的列表
     */
    public static List<WifiConfiguration> getConfiguredNetworks(Context context, WifiManager manager) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return new ArrayList<>();
        }
        return manager.getConfiguredNetworks();
    }


    /**
     * 将已经配置好的网络从附近的网络列表中去掉
     *
     * @param scanResults    扫描结果
     * @param configurations 已配置列表
     * @return 扫描列表
     */
    public static List<ScanResult> getSaveWifiList(List<ScanResult> scanResults, List<WifiConfiguration> configurations) {
        Set<String> sets = new HashSet<>();
        for (WifiConfiguration configuration : configurations) {
            sets.add(configuration.SSID);
        }
        List<ScanResult> saveResults = new ArrayList<>();
        List<ScanResult> nearbyResults = new ArrayList<>();
        for (ScanResult scanResult : scanResults) {
            if (sets.add(scanResult.SSID)) {
                nearbyResults.add(scanResult);
            } else {
                saveResults.add(scanResult);
            }
        }
        scanResults.clear();
        scanResults.addAll(nearbyResults);
        return saveResults;
    }

    /**
     * 连接wifi
     *
     * @param manager       WifiManager
     * @param configuration Wifi配置
     * @return 是否连接成功
     */
    public static boolean connectWifi(WifiManager manager, WifiConfiguration configuration) {
        int id = manager.addNetwork(configuration);
        WifiInfo connectionInfo = manager.getConnectionInfo();
        manager.disableNetwork(connectionInfo.getNetworkId());
        boolean b = manager.enableNetwork(id, true);
        Log.d("WifiManagerUtils", "connectWifi: 连接状态=" + b);
        if (b) {
            manager.saveConfiguration();
        } else {
            Log.d("WifiManagerUtils", configuration.toString());
        }
        return b;
    }

    /**
     * 创建Wifi配置
     *
     * @param SSID         wifi名称
     * @param password     wifi密码
     * @param hidden       网络是否隐藏（该方法与添加隐藏网络通用）
     * @param capabilities 网络安全协议
     * @return 配置好的wifi
     */
    public static WifiConfiguration createWifiInfo(String SSID, String password, boolean hidden, String capabilities) {
        WifiConfiguration configuration = new WifiConfiguration();
        configuration.SSID = "\"" + SSID + "\"";
        if (hidden) {
            configuration.hiddenSSID = true;
        }
        Log.d("WifiManagerUtils", "createWifiInfo: " + capabilities);
        if (capabilities.contains("SAE") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setWPA3(configuration, password);
        } else if (capabilities.contains("WPA-PSK") || capabilities.contains("WPA2-PSK")) {
            setWPA(configuration, password);
        } else if (capabilities.contains("WEP")) {
            setWEP(configuration, password);
        } else {
            setESS(configuration);
        }
        return configuration;
    }

    /**
     * 设置wpa3协议
     *
     * @param configuration 配置
     * @param password      密码
     */
    public static void setWPA3(WifiConfiguration configuration, String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.SAE);
        }
        configuration.preSharedKey = "\"" + password + "\"";
    }

    /**
     * WPA协议
     *
     * @param configuration 配置
     * @param password      密码
     */
    public static void setWPA(WifiConfiguration configuration, String password) {
        configuration.preSharedKey = "\"" + password + "\"";
        //公认的IEEE 802.11验证算法。
        configuration.allowedAuthAlgorithms.clear();
        configuration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        //公认的的公共组密码。
        configuration.allowedGroupCiphers.clear();
        configuration.allowedGroupCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        //公认的密钥管理方案。
        configuration.allowedKeyManagement.clear();
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        //密码为WPA。
        configuration.allowedPairwiseCiphers.clear();
        configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        configuration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        //公认的安全协议。
        configuration.allowedProtocols.clear();
        configuration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
    }

    /**
     * WEP协议
     *
     * @param configuration 配置
     * @param password      密码
     */
    public static void setWEP(WifiConfiguration configuration, String password) {
        configuration.wepKeys[0] = "\"" + password + "\"";
        configuration.wepTxKeyIndex = 0;
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        configuration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
    }


    /**
     * 无密码
     *
     * @param configuration 配置
     */
    public static void setESS(WifiConfiguration configuration) {
        configuration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    }

}
