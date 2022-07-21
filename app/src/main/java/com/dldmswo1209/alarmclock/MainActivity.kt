package com.dldmswo1209.alarmclock

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dldmswo1209.alarmclock.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    var mBinding : ActivityMainBinding? = null
    val binding get() = mBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // step0 뷰를 초기화해주기
        initOnOffButton()
        initChangeAlarmTimeButton()
        // step1 데이터 가져오기
        val model = fetchDataFromSharedPreferences()
        // step2 뷰에 데이터를 그려주기
        renderView(model)
    }
    private fun initOnOffButton(){ // 알람 켜기/끄기 버튼 클릭 이벤트
        binding.onOffButton.setOnClickListener {
            // 데이터를 확인을 한다.
            val model = it.tag as? AlarmDisplayModel ?: return@setOnClickListener // object -> AlarmDisplayModel 형변환
            val newModel = saveAlarmModel(model.hour, model.minute, !model.onOff)
            renderView(newModel)
            // on/off 버튼의 상태에 따라 작업을 처리한다.
            if(newModel.onOff){
                // 켜진 경우 -> 알람을 등록
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour) // 시간 설정
                    set(Calendar.MINUTE, newModel.minute) // 분 설정

                    // 설정한 시간이 이미 지난 시간일 경우
                    if(before(Calendar.getInstance())){
                        add(Calendar.DATE, 1) // 다음 날로 설정
                    }
                }
                // 알람 작동 시 알림 기능을 추가하기 위한 작업
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this, AlarmReceiver::class.java)
                // API 31 부터 PendingIntent 사용시 FLAG 변수로 FLAG_IMMUTABLE 또는 FLAG_MUTABLE 을 사용하여
                // PendingIntent 사용시 변경 가능성을 명시적으로 지정해줘야 한다.
                // 여러 버전들에 대해서 정상적으로 작동하기 위해서는 아래와 같이 조건문으로 작성해줘야 한다.
                val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE)
                }else {
                    PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                }

                // setInexactRepeating 설정할 경우 설정한 시간보다 1분 늦게(부정확한 시간에) 알람이 울린다.
                // 또한, 핸드폰이 잠자기 모드로 들어갔을 때 알람이 안울리는 문제가 발생 할 수 있다.
                // 잠자기 모드에서도 알람이 울리게 하려면 setAndAllowWhileIdle() 를 사용하면 된다.
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }else{
                // 꺼진 경우 -> 알람을 제거
                cancelAlarm()
            }
        }
    }
    private fun initChangeAlarmTimeButton(){
        // 시간 재설정 버튼 클릭 이벤ㅌ
        binding.changeAlarmTimeButton.setOnClickListener {
            // 현재 시간을 가져온다.
            val calendar = Calendar.getInstance()
            // TimePickDialog 띄워줘서 시간을 설정, 설정된 시간을 가져와서
            TimePickerDialog(this, { picker, hour, minute ->
                // 데이터를 저장
                val model = saveAlarmModel(hour, minute, false)
                // 뷰를 업데이트
                renderView(model)
                // 기존에 있던 알람을 삭제한다.
                cancelAlarm()
            },calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),false)
                .show()
        }
    }
    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ) : AlarmDisplayModel{
        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )
        // sharedPreference 에 알람 정보를 저장
        val sharedPreferences = getSharedPreferences("time", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()){
            putString("alarm", model.makeDataForDB())
            putBoolean("onOff", model.onOff)
            commit()
        }

        return model
    }
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        // sharedPreference 에서 가져오기
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
        val timeDBValue = sharedPreferences.getString(ALARM_KEY, "9:30") ?: "9:30" // getString()함수는 nullable 이므로 ?: 연산자를 통해 not null type으로 만들어준다
        val onOffDBValue = sharedPreferences.getBoolean(ON_OFF_KEY, false)
        val alarmData = timeDBValue.split(":") // 시, 분을 나누기 위해서
        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            minute = alarmData[1].toInt(),
            onOffDBValue
        )
//      보정 예외처리
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)
        }else {
            PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE)
        }
        if ((pendingIntent == null) and alarmModel.onOff){
            // 알람은 꺼져있는데, 데이터는 켜져있는 경우
            alarmModel.onOff = false
        }else if((pendingIntent != null) and alarmModel.onOff.not()){
            // 알람은 켜져있는데, 데이터는 꺼져있는 경우
            pendingIntent.cancel()// 알람을 취소
        }
        return alarmModel
    }
    private fun renderView(model: AlarmDisplayModel){
        // view 에 알람정보를 렌더링
        binding.ampmTextView.apply {
            text = model.ampmText
        }
        binding.timeTextView.apply{
            text = model.timeText
        }
        binding.onOffButton.apply{
            text = model.onOffText
            tag = model
        }
    }
    private fun cancelAlarm(){
        // 알람 제거
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)
        }else {
            PendingIntent.getBroadcast(this, ALARM_REQUEST_CODE, Intent(this, AlarmReceiver::class.java), PendingIntent.FLAG_NO_CREATE)
        }
        pendingIntent?.cancel()
    }

    companion object{
        private const val SHARED_PREFERENCE_NAME = "time"
        private const val ALARM_KEY = "alarm"
        private const val ON_OFF_KEY = "onOff"
        private const val ALARM_REQUEST_CODE = 1000
    }
}