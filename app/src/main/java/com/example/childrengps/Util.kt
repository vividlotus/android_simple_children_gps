package com.example.childrengps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

class Util {
    companion object {

        // デバイスIDの取得
        @SuppressLint("HardwareIds")
        fun getDeviceId(context: Context): String {
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }

        // バッテリーレベルの取得
        fun getBatteryLevel(context: Context): Int {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }

        // 位置情報の要求
        fun permitLocation(context: Context, activity: ComponentActivity) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }

        // 位置情報取得とSlackへの通知
        @SuppressLint("MissingPermission")
        fun getLocationAndSendToSlack(context: Context) {
            // Permissionが取得できていない場合は何もしない
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return

            val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses: MutableList<Address>? =
                            geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        val address = addresses?.get(0)?.getAddressLine(0)

                        val deviceId = getDeviceId(context)
                        val batteryLevel = getBatteryLevel(context)

                        address?.let { it1 ->
                            // 非同期でSlackに送信
                            CoroutineScope(Dispatchers.IO).launch {
                                sendToSlack(deviceId, it.latitude, it.longitude, it1, batteryLevel)
                            }
                        }

                    }

                }
        }

        // Slackへ送信
        fun sendToSlack(deviceId: String, latitude: Double, longitude: Double, address: String, batteryLevel: Int) {
            val client = OkHttpClient()
            val json = JSONObject()
            val text = buildString {
                append("デバイス: $deviceId\n")
                append("充電: $batteryLevel%\n")
                append("位置: $address\n")
                append("マップ: https://www.google.com/maps/search/?api=1&query=$latitude,$longitude\n")
            }
            json.put("text", text)
            val body =
                json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(BuildConfig.SLACK_HOOK_URL)
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("Util", "Failed to send to Slack: ${response.message}")
                } else {
                    Log.d("Util", "Successfully sent to Slack")
                }
            }
        }
    }
}