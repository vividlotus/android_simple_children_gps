package com.example.childrengps

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.childrengps.ui.theme.ChildrenGpsTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChildrenGpsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
                        Greeting(name = Util.getDeviceId(this@MainActivity))
                        if (BuildConfig.DEBUG) {
                            Spacer(modifier = Modifier.height(16.dp))
                            DebugButton {
                                sendDebugLocationToSlack()
                            }
                        }
                    }
                }
            }
        }

        // 位置情報が許可されていない場合は要求
        Util.permitLocation(this, this)

        // バックグラウンドで定期的に取得する
        val locationWorkRequest = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueue(locationWorkRequest)
    }

    @SuppressLint("MissingPermission")
    private fun sendDebugLocationToSlack() {
        Util.getLocationAndSendToSlack(this)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Device ID: $name",
        modifier = modifier
    )
}

@Composable
fun DebugButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text = "Send Debug Location to Slack")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChildrenGpsTheme {
        Greeting("Android")
    }
}

@Preview(showBackground = true)
@Composable
fun DebugButtonPreview() {
    ChildrenGpsTheme {
        DebugButton(onClick = {})
    }
}
