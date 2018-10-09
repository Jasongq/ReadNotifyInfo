package com.jiuye.readnotifyinfo;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private AppCompatTextView mButton;
    public static final String ACTION_BOARDCAST="com.action.boardcastreceive";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = (AppCompatTextView) findViewById(R.id.notificationMonitorTv);
        mButton.setOnClickListener(this);

        Intent grayIntent = new Intent(getApplicationContext(), NotificationMonitorService.class);
        startService(grayIntent);

        IntentFilter filter=new IntentFilter(ACTION_BOARDCAST);
        registerReceiver(receiver,filter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.notificationMonitorTv:
                if (!isEnabled()) {
                    openNotificationAccessSetting();
                    Toast.makeText(this,"找到[监听],然后开启服务即可",Toast.LENGTH_LONG).show();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), "监控器开关已打开", Toast.LENGTH_SHORT);
                    toast.show();
                }
                break;
            default:
                break;
        }
    }

    // 判断是否打开了通知监听权限
    private boolean isEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 去设置页面
     *
     * @return
     */
    private boolean openNotificationAccessSetting() {
        try {
            //Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS     >=Build.VERSION_CODES.LOLLIPOP_MR1(22)
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {//普通情况下找不到的时候需要再特殊处理找一次
            try {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$NotificationAccessSettingsActivity");
                intent.setComponent(cn);
                intent.putExtra(":settings:show_fragment", "NotificationAccessSettings");
                startActivity(intent);
                return true;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            Toast.makeText(this, "对不起，您的手机暂不支持", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        }
    }


private BroadcastReceiver receiver=new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (null!=intent){
            int notifyType=intent.getIntExtra("notifyType",0);
            String notifyTitle=intent.getStringExtra("title");
            String notifyContents=intent.getStringExtra("content");

            Toast.makeText(MainActivity.this, notifyType==1?"微信来通知啦":"支付宝来通知啦", Toast.LENGTH_SHORT).show();
            int dex = notifyContents.indexOf("款");
            Toast toast = Toast.makeText(MainActivity.this, "收款金额:" + notifyContents.substring(dex + 1, notifyContents.length()), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }
};

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
