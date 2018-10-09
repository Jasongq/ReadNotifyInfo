package com.jiuye.readnotifyinfo.receiver;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.jiuye.readnotifyinfo.NotificationMonitorService;

import java.util.List;
import java.util.Set;

/**
 * <pre>
 *     author : GuoQiang
 *     e-mail : 849199845@qq.com
 *     time   : 2018/09/29
 *     desc   :
 *     version: 1.0
 * </pre>
 */
public class WakeReceiver extends BroadcastReceiver {
    private final static String TAG = WakeReceiver.class.getSimpleName();
    public final static String GRAY_WAKE_ACTION = "com.jiuye.wake";
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        String action = intent.getAction();
        Log.i("notify通知WakeReceiver:", "wake !! wake !! "+intent.getAction());
        switch (action){
            case GRAY_WAKE_ACTION:
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_USER_PRESENT:
//            case Intent.ACTION_BATTERY_CHANGED:
            case ConnectivityManager.CONNECTIVITY_ACTION:
                Intent wakeIntent = new Intent(context, NotificationMonitorService.class);
                context.startService(wakeIntent);
                break;
                default:break;
        }
    }

}
