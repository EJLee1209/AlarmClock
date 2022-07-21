package com.dldmswo1209.alarmclock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)
        notifyNotification(context)
    }
    private fun createNotificationChannel(context: Context){
        // 채널 생성 메소드
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            // 채널이 필요
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, // 알림을 받을 채널 id 설정
                "기상 알람", // 채널 이름 설정
                NotificationManager.IMPORTANCE_HIGH // 알림의 중요도 설정
            )
            // 만든 채널 정보를 시스템에 등록
            NotificationManagerCompat.from(context).createNotificationChannel(notificationChannel)


        }
    }
    private fun notifyNotification(context: Context){
        // 알림 표시 메소드
        with(NotificationManagerCompat.from(context)){
            val build = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("알람") // 알림 제목
                .setContentText("일어날 시간입니다.") // 알림 내용
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 알림 아이콘
                .setPriority(NotificationCompat.PRIORITY_HIGH)
            notify(NOTIFICATION_ID, build.build()) // 알림 표시
        }
    }
    companion object{
        const val NOTIFICATION_CHANNEL_ID = "1000"
        const val NOTIFICATION_ID = 100
    }
}