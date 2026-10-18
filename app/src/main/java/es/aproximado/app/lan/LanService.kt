package es.aproximado.app.lan

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import es.aproximado.app.MainActivity
import es.aproximado.app.R

/** Active sessions survive leaving the Activity; no background scan or persistent service when idle. */
class LanService: Service() {
    private var wake: PowerManager.WakeLock?=null
    private var session=-1
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("lan",getString(R.string.lan_channel),NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("lan",true),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop=PendingIntent.getService(this,1,Intent(this,LanService::class.java).setAction("close"),PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(this,"lan").setSmallIcon(R.drawable.ic_lan_notification).setContentTitle(getString(R.string.lan_notification_title)).setContentText(getString(R.string.lan_notification_text)).setContentIntent(open).setOngoing(true).setSilent(true).addAction(0,getString(R.string.lan_leave),stop).build()
        if(Build.VERSION.SDK_INT>=29) startForeground(105,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE) else startForeground(105,notification)
        wake=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Aproximado:LAN").apply { acquire(2*60*60*1000L) }
    }
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        session=intent?.getIntExtra("generation",session) ?: session
        if(intent?.action=="close") { LanRepository.get(this).close();stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        wake?.let { if(it.isHeld) it.release() };wake=null
        LanRepository.get(this).serviceStopped(session);super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder?=null
}
