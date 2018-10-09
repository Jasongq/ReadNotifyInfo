package com.jiuye.readnotifyinfo;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.jiuye.readnotifyinfo.receiver.WakeReceiver;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 *     author : GuoQiang
 *     e-mail : 849199845@qq.com
 *     time   : 2018/09/29
 *     desc   :  Android 4.3 及以上使用
 *     version: 1.0
 * </pre>
 */
public class NotificationMonitorService extends NotificationListenerService {
    private final static String TAG = NotificationMonitorService.class.getSimpleName();
    private final static int GRAY_SERVICE_ID = -10086;

    private static final String WECHAT = "com.tencent.mm";
    private static final String ALIPAT = "com.eg.android.AlipayGphone";
    private String notificationPkg, notificationTitle, notificationText;
    /**
     * 定时唤醒的时间间隔，5分钟
     */
    private final static int ALARM_INTERVAL = 5 * 60 * 1000;
    private final static int WAKE_REQUEST_CODE = 6666;
    @Override
    public void onCreate() {
        super.onCreate();
        ensureCollectorRunning();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //API < 18 ，此方法能有效隐藏Notification上的图标
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
            Intent innerIntent = new Intent(this, GrayInnerService.class);
            startService(innerIntent);
            startForeground(GRAY_SERVICE_ID, new Notification());
        }
        //发送唤醒广播来促使挂掉的UI进程重新启动起来
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent();
        alarmIntent.setAction(WakeReceiver.GRAY_WAKE_ACTION);
        PendingIntent operation = PendingIntent.getBroadcast(this, WAKE_REQUEST_CODE, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), ALARM_INTERVAL, operation);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onDestroy() {
        sendBroadcast(new Intent("com.jiuye.wake"));
        super.onDestroy();
    }
    /**
     * 给 API >= 18 的平台上用的灰色保活手段
     */
    public static class GrayInnerService extends Service {

        @Override
        public void onCreate() {
            Log.i(TAG, "InnerService -> onCreate");
            super.onCreate();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.i(TAG, "InnerService -> onStartCommand");
            startForeground(GRAY_SERVICE_ID, new Notification());
            //stopForeground(true);
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onDestroy() {
            Log.i(TAG, "InnerService -> onDestroy");
            super.onDestroy();
        }
    }


    // 在收到消息时触发
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
//        super.onNotificationPosted(sbn);
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        if (null == notification) return;
        // 当 API > 18 时，使用 extras 获取通知的详细信息
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            notificationPkg = TextUtils.isEmpty(sbn.getPackageName()) ? "" : sbn.getPackageName();
            notificationTitle = extras.getString(Notification.EXTRA_TITLE, "");
            notificationText = extras.getString(Notification.EXTRA_TEXT, "");
        } else {
            // 当 API = 18 时，利用反射获取内容字段
            List<String> textList = getText(notification);
            if (textList != null && textList.size() > 0) {
                for (String text : textList) {
                    if (!TextUtils.isEmpty(text) && text.contains("微信支付收款")) {
                        notificationText = text;
                        break;
                    }
                }
            }
        }
        // com.tencent.mm & 微信支付 & 微信支付收款0.01元
        //com.eg.android.AlipayGphone & 支付宝通知 & 小强通过扫码向你付款0.01元
        Log.v("notify通知接收:", notificationPkg + " & " + notificationTitle + " & " + notificationText);
        if (!TextUtils.isEmpty(notificationPkg)) {
            switch (notificationPkg) {
                case WECHAT:
                    if ("微信支付".equals(notificationTitle) && !TextUtils.isEmpty(notificationText) && notificationText.contains("微信支付收款")) {
//                    if (!TextUtils.isEmpty(notificationText) && notificationText.contains("微信支付收款")) {
                        Intent intent = new Intent(MainActivity.ACTION_BOARDCAST);
                        intent.putExtra("notifyType", 1);
                        intent.putExtra("title", notificationTitle);
                        intent.putExtra("content", notificationText);
                        sendBroadcast(intent);
                    }
                    break;
                case ALIPAT:
                    if ("支付宝通知".equals(notificationTitle) && !TextUtils.isEmpty(notificationText) && notificationText.contains("通过扫码向你付款")) {
//                    if (!TextUtils.isEmpty(notificationText) && notificationText.contains("通过扫码向你付款")) {
                        Intent intent = new Intent(MainActivity.ACTION_BOARDCAST);
                        intent.putExtra("notifyType", 2);
                        intent.putExtra("title", notificationTitle);
                        intent.putExtra("content", notificationText);
                        sendBroadcast(intent);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    // 在删除消息时触发
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        // TODO Auto-generated method stub
        Notification notification = sbn.getNotification();
        if (null == notification) return;
        Bundle extras = notification.extras;
        String notificationPkg = sbn.getPackageName();
        String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
        String notificationText = extras.getString(Notification.EXTRA_TEXT);
        Log.v("notify通知移除:", notificationPkg + " & " + notificationTitle + " & " + notificationText);
    }

    /**
     * 取消通知
     *
     * @param sbn
     */
    public void cancelNotification(StatusBarNotification sbn) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cancelNotification(sbn.getKey());
        } else {
            cancelNotification(sbn.getPackageName(), sbn.getTag(), sbn.getId());
        }
    }

    /**
     * onNotificationPosted
     * 当 API = 18 时，利用反射获取 Notification 中的内容
     * @param notification
     * @return
     */
    public List<String> getText(Notification notification) {
        if (null == notification) {
            return null;
        }
        RemoteViews views = notification.bigContentView;
        if (views == null) {
            views = notification.contentView;
        }
        if (views == null) {
            return null;
        }
        // Use reflection to examine the m_actions member of the given RemoteViews object.
        // It's not pretty, but it works.
        List<String> text = new ArrayList<>();
        try {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);
            // Find the setText() and setTime() reflection actions
            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);
                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 2) continue;
                // View ID
                parcel.readInt();
                String methodName = parcel.readString();
                if (null == methodName) {
                    continue;
                } else if (methodName.equals("setText")) {
                    // Parameter type (10 = Character Sequence)
                    parcel.readInt();
                    // Store the actual string
                    String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                    text.add(t);
                }
                parcel.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }


    //确认NotificationMonitor是否开启
    private void ensureCollectorRunning() {
//        ComponentName collectorComponent = new ComponentName(this, NotificationMonitorService.class);
//        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        boolean collectorRunning = false;
//        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
//        if (runningServices == null ) {
//            return;
//        }
//        for (ActivityManager.RunningServiceInfo service : runningServices) {
//            if (service.service.equals(collectorComponent)) {
//                if (service.pid == android.os.Process.myPid() ) {
//                    collectorRunning = true;
//                }
//            }
//        }
//        if (collectorRunning) {
//            return;
//        }
        toggleNotificationListenerService();
    }

    //重新开启NotificationMonitor
    private void toggleNotificationListenerService() {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, NotificationMonitorService.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(new ComponentName(this, NotificationMonitorService.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }


}
