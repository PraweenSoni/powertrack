package com.example.powertrack

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    private lateinit var batteryInfoText: TextView
    private lateinit var btnEmail: Button
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        batteryInfoText = findViewById(R.id.batteryInfoText)

        // Register BroadcastReceiver for battery status updates
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Start periodic updates for current (mA)
        startUpdatingCurrent()

        btnEmail = findViewById(R.id.btnEmail)

        btnEmail.setOnClickListener {
            val email = "ask.psoni@gmail.com"
            val subject = "Feedback! For improvement.. From Timer Torch App."
            val uriText = "mailto:$email?subject=${Uri.encode(subject)}"
            val emailIntent = Intent(Intent.ACTION_SENDTO, uriText.toUri())
            startActivity(Intent.createChooser(emailIntent, "Feedback! For improvement.."))
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = level * 100 / scale
                val voltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000.0
                val temperature = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0
                val health = it.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                val chargePlug = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val isUSB = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
                val isAC = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
                val isWireless =
                    chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

                val chargeType = when {
                    isAC -> "Fast (AC)"
                    isUSB -> "Slow (USB)"
                    isWireless -> "Wireless "
                    else -> "Not Charging"
                }

                val batteryHealth = when (health) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                    else -> "Unknown"
                }

                val manufacturer = Build.MANUFACTURER
                val model = Build.MODEL
                val androidVersion = Build.VERSION.RELEASE

                // Update UI
                batteryInfoText.text = """
                    🔋 Charging: $isCharging ($chargeType)
                    ⚡ Battery: $batteryPct%
                    💖 Health: $batteryHealth
                    🔌 Voltage: $voltage V
                    🌡 Temperature: $temperature°C
                    🏭 Manufacturer: $manufacturer
                    📱 Model: $model
                    📌 Android Version: $androidVersion
                    ⚡ Current: Fetching...
                """.trimIndent()
            }
        }
    }

    private fun startUpdatingCurrent() {
        handler.post(object : Runnable {
            override fun run() {
                val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
                val chargeCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000.0

                // Round the value to an integer (No decimal places)
                var roundedCurrent = chargeCurrent.toInt()

                // Get charging status
                val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                // Correct the sign: Ensure positive value while charging, negative while discharging
                roundedCurrent = if (isCharging) kotlin.math.abs(roundedCurrent) else -kotlin.math.abs(roundedCurrent)

                // Update UI dynamically
                runOnUiThread {
                    val currentValue = "$roundedCurrent mA"
                    batteryInfoText.text = batteryInfoText.text.toString().replace(
                        Regex("⚡ Current: .*"),
                        "⚡ Current: $currentValue"
                    )
                }

                // Repeat every second
                handler.postDelayed(this, 1000)
            }
        })
    }



    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        handler.removeCallbacksAndMessages(null) // Stop updates
    }
}
