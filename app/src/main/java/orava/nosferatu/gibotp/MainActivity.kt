package orava.nosferatu.gibotp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.ListenableWorker.Result
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity() {

    companion object {
        const val SMS_PERMISSION_CODE: Int = 101
    }

    private lateinit var otpTextView: TextView
    private lateinit var settingsButton: Button

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        otpTextView = findViewById(R.id.otp_textview)
        settingsButton = findViewById(R.id.settings_btn)

        sharedPreferences = this.getSharedPreferences(Tokens.sp, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (!sharedPreferences.getBoolean(Tokens.spDeviceIdSent, Tokens.defaultDeviceIdSent)) {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task: Task<String> ->
                    if (!task.isSuccessful) {
                        Log.w(this::class.simpleName, "Fetching FCM registration token failed", task.exception)
                        return@addOnCompleteListener
                    }
                    val token = task.result
                    Log.d(this::class.simpleName, "FCM Device Token: $token")
                    sendRegistrationToServer(token)
                }
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(otpReceiver, IntentFilter(Tokens.intentOtpReceived))
        requestSmsPermission()
    }

    private fun sendRegistrationToServer(token: String) {
        val ip = sharedPreferences.getString(Tokens.spIp, Tokens.defaultIp)
        val port = sharedPreferences.getString(Tokens.spPort, Tokens.defaultPort)
        val registerDeviceEndpoint = sharedPreferences.getString(Tokens.spRegisterDeviceEndpoint, Tokens.defaultRegisterDeviceEndpoint)
        val url = "http://$ip:$port/$registerDeviceEndpoint"

        val uuid = DeviceUtils.getDeviceId(this)
        val json = JSONObject().apply {
            put("uuid", uuid)
            put("fcm_token", token)
        }

        val requestBody = json.toString().toRequestBody(Tokens.MEDIA_TYPE_JSON)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to register Device: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Device Registration successful", Toast.LENGTH_SHORT).show()
                        editor.apply {
                            putBoolean(Tokens.spDeviceIdSent, true)
                            apply()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to register Device: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private val otpReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra("otp")?.let { otp ->
                displayOtp(otp)
            } ?: run {
                println("No OTP received")
            }
        }
    }

    private fun displayOtp(otp: String) {
        otpTextView.text = otp
    }

    private fun requestSmsPermission() {
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                SMS_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Your trust is highly appreciated!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Your trust is essential for this to function properly. " +
                        "Please restart the app to bring the dialog back.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(otpReceiver)
    }
}
