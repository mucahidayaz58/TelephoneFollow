package com.greyinc.telephonefollowing

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyAccessibilityService : AccessibilityService() {
    private var lastScrollTime: Long = 0 // Son kaydırma zamanı
    private val scrollDelay = 100L // Kaydırma olayları arasındaki minimum süre (ms)

    // Firebase referansları
    private val database: DatabaseReference = FirebaseDatabase.getInstance("https://telephonefollow-default-rtdb.europe-west1.firebasedatabase.app").reference
    private var touchCount: Int = 0 // Dokunma sayısını önbellekle
    private val scrollCountMap = mutableMapOf<String, Int>() // Kaydırma sayısı önbelleği
    private var pendingScrollCount = 0 // Bekleyen kaydırma sayısı
    private val scrollBatchSize = 5 // Her kaydırma sayısını kaydedeceğimiz grup boyutu

    private var currentActivity: String = "UNKNOWN" // Kullanıcının mevcut aktivitesi

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("MyAccessibilityService", "Servis Başlatıldı")
        loadInitialCounts() // Başlangıçta mevcut sayıları yükle
    }

    private fun loadInitialCounts() {
        // Firebase'den başlangıç sayıları yükle
        database.child("touch_count").get().addOnSuccessListener { snapshot ->
            touchCount = snapshot.getValue(Int::class.java) ?: 0
        }

        // Kaydırma sayısını başlat
        database.child("scroll_count").get().addOnSuccessListener { snapshot ->
            snapshot.children.forEach { child ->
                val packageName = child.key?.replace("_", ".") ?: ""
                scrollCountMap[packageName] = child.getValue(Int::class.java) ?: 0
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
                touchCount++ // Dokunma sayısını artır
                if (touchCount % 10 == 0) {
                    saveTouchCountToFirebase() // Her 10 dokunmada bir kaydet
                }
            }

            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val currentTime = System.currentTimeMillis()
                // Kaydırma olayının yeterince süre geçtiğini kontrol et
                if (currentTime - lastScrollTime >= scrollDelay) {
                    val packageName = event.packageName?.toString()
                    if (packageName != null) {
                        pendingScrollCount++ // Bekleyen kaydırma sayısını artır
                        if (pendingScrollCount >= scrollBatchSize) {
                            incrementScrollCountInFirebase(packageName) // Toplu kaydırma sayısını güncelle
                            pendingScrollCount = 0 // Sıfırlama
                        }
                    }
                    // Son kaydırma zamanını güncelle
                    lastScrollTime = currentTime
                }
            }
        }
    }

    private fun saveTouchCountToFirebase() {
        CoroutineScope(Dispatchers.IO).launch {
            database.child("touch_count").setValue(touchCount)
            Log.d("MyAccessibilityService", "Firebase'e Kaydedildi: Dokunma Sayısı: $touchCount")
        }
    }

    private fun incrementScrollCountInFirebase(packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val safePackageName = packageName.replace(".", "_")
            val currentScrollCount = scrollCountMap.getOrDefault(safePackageName, 0) + 1
            scrollCountMap[safePackageName] = currentScrollCount

            database.child("scroll_count").child(safePackageName).setValue(currentScrollCount)

            // Her 5 kaydırmada bir loglama yap
            if (currentScrollCount % 5 == 0) {
                Log.d("MyAccessibilityService", "Firebase'e Kaydedildi: Uygulama: $safePackageName, Kaydırma Sayısı: $currentScrollCount")
            }
        }
    }

    fun updateCurrentActivity(activity: String) {
        currentActivity = activity
        Log.d("MyAccessibilityService", "Mevcut Aktivite Güncellendi: $currentActivity")

        // Aktivite bilgisi ile birlikte verileri kaydet
        val activityKey = if (currentActivity == "In Vehicle") {
            "araba_surma_modunda"
        } else {
            "diğer_aktivite"
        }

        CoroutineScope(Dispatchers.IO).launch {
            val activityData = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "touch_count" to touchCount,
                "scroll_count" to scrollCountMap
            )

            // Firebase'e kaydet
            database.child("activity_data").child(activityKey).push().setValue(activityData)
                .addOnSuccessListener {
                    Log.d("MyAccessibilityService", "Firebase'e kaydedildi: $activityKey - Dokunma: $touchCount, Kaydırma: $scrollCountMap")
                }
                .addOnFailureListener { exception ->
                    Log.e("MyAccessibilityService", "Firebase'e kaydedilemedi: ${exception.message}")
                }
        }
    }

    override fun onInterrupt() {
        Log.d("MyAccessibilityService", "Servis Kesildi")
    }
}
