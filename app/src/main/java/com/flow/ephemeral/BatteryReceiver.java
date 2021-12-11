package com.flow.ephemeral;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BatteryReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
            // 获取当前电量
            int level = intent.getIntExtra("level", 0);
            // 获取总电量
            int scale = intent.getIntExtra("scale", 0);
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("当前电量为："+level* 100 / scale + "%" + "  ");
            // 如果当前电量小于总电量的15%
            if (level * 1.0 / scale < 0.15) {
                stringBuffer.append("电量过低，请尽快充电！");
            } else {
                stringBuffer.append("电量足够，请放心使用！");
            }
            Toast.makeText(context, stringBuffer.toString(), Toast.LENGTH_LONG).show();
        }
    }
}