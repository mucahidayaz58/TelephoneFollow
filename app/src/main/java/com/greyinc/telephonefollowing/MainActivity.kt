package com.greyinc.telephonefollowing

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.greyinc.telephonefollowing.databinding.ActivityMainBinding
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE_PERMISSIONS = 1
    private lateinit var lockScreenReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerLockScreenReceiver()
        // İzin kontrolü ve isteme
        checkPermissions()

        // SMS detay kartı
        binding.smsCardview.setOnClickListener {
            val intent = Intent(this@MainActivity, SMSActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    // Gerekli izinleri kontrol et ve kullanıcıya sor
    private fun checkPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.READ_CALL_LOG)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.RECEIVE_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.READ_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.SEND_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.FOREGROUND_SERVICE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && ContextCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissionsNeeded.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
        } else {
            // İzin zaten verilmiş
            Toast.makeText(this, "İzin verildi", Toast.LENGTH_SHORT).show()
        }

        if (permissionsNeeded.isNotEmpty()) {
            // EasyPermissions kullanarak izin iste
            EasyPermissions.requestPermissions(
                this,
                "Aktivite tanıma izni gereklidir.",
                REQUEST_CODE_PERMISSIONS,
                *permissionsNeeded.toTypedArray()
            )
        } else {
            checkAccessibilityPermission()
        }

        println(permissionsNeeded)
    }

    // Erişilebilirlik iznini kontrol et
    private fun checkAccessibilityPermission() {
        if (!isAccessibilityServiceEnabled(MyAccessibilityService::class.java)) {
            requestAccessibilityPermission()
        } else {
            // İzinler varsa servislere başla
            startServices()
        }
    }

    // Erişilebilirlik iznini kontrol eden metot
    private fun isAccessibilityServiceEnabled(service: Class<out AccessibilityService>): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        // enabledServices null veya boşsa kontrol et
        if (enabledServices.isNullOrEmpty()) {
            return false
        }

        // Kullanıcı erişilebilirlik servislerini kontrol et
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        // Belirli bir ayırıcı kullanarak döngü
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(service.name, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    // Erişilebilirlik iznini isteme
    private fun requestAccessibilityPermission() {
        Toast.makeText(this, "Erişilebilirlik izni gerekli.", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    // İzinler tamamlandıysa çağrıları al ve diğer servisleri başlat
    private fun startServices() {
        fetchCallCounts()
        startUnlockTrackingService()
    }

    // Arama loglarını al ve kaç gelen/giden arama olduğunu hesapla
    private fun fetchCallCounts() {
        GlobalScope.launch(Dispatchers.Main) {
            val (incomingCalls, outgoingCalls) = getCallCounts(contentResolver)
            Log.d("MainActivity", "Gelen Arama Sayısı: $incomingCalls")
            Log.d("MainActivity", "Giden Arama Sayısı: $outgoingCalls")
        }
    }

    // Çağrı loglarından gelen ve giden arama sayısını al
    private suspend fun getCallCounts(contentResolver: ContentResolver): Pair<Int, Int> {
        var incomingCalls = 0
        var outgoingCalls = 0

        return withContext(Dispatchers.IO) {
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )

            cursor?.use {
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                while (it.moveToNext()) {
                    val callType = it.getInt(typeIndex)
                    when (callType) {
                        CallLog.Calls.INCOMING_TYPE -> incomingCalls++
                        CallLog.Calls.OUTGOING_TYPE -> outgoingCalls++
                    }
                }
            } ?: run {
                Log.e("MainActivity", "Cursor null döndü.")
            }

            Pair(incomingCalls, outgoingCalls)
        }
    }

    // Ekran kilidi izleme servisini başlat
    private fun startUnlockTrackingService() {
        val serviceIntent = Intent(this, UnlockTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    // Ekran kilidi açıldığında ve ekran kapandığında BroadcastReceiver'ı kaydet
    private fun registerLockScreenReceiver() {
        lockScreenReceiver = LockScreenReceiver()

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)  // Ekran kilidi açıldığında
            addAction(Intent.ACTION_SCREEN_OFF)    // Ekran kapandığında
        }

        registerReceiver(lockScreenReceiver, intentFilter)
        Log.d("MainActivity", "LockScreenReceiver register edildi")
    }

    // İzin sonuçlarını işle
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // İzinler verilmişse erişilebilirlik iznini kontrol et
                checkAccessibilityPermission()
            } else {
                Toast.makeText(this, "Gerekli izinler verilmedi.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
