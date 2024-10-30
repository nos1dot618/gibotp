package orava.nosferatu.gibotp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputLayout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SettingsActivity : AppCompatActivity() {

    private lateinit var saveButton: Button
    private lateinit var ipTextInputLayout: TextInputLayout
    private lateinit var portTextInputLayout: TextInputLayout
    private lateinit var otpEndpointTextInputLayout: TextInputLayout
    private lateinit var registerDeviceEndpointTextInputLayout: TextInputLayout
    private lateinit var backgroundSwitch: SwitchCompat
    private lateinit var sendOtpSwitch: SwitchCompat
    private lateinit var testServerButton: Button

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var client: OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        saveButton = findViewById(R.id.save_button)
        ipTextInputLayout = findViewById(R.id.ip_text_field)
        portTextInputLayout = findViewById(R.id.port_text_field)
        otpEndpointTextInputLayout = findViewById(R.id.otp_endpoint_text_field)
        registerDeviceEndpointTextInputLayout = findViewById(R.id.register_device_endpoint_text_field)
        backgroundSwitch = findViewById(R.id.background_switch)
        sendOtpSwitch = findViewById(R.id.send_otp_switch)
        testServerButton = findViewById(R.id.test_server_btn)

        sharedPreferences = this.getSharedPreferences(Tokens.sp, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        client = OkHttpClient()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ipTextInputLayout.editText?.setText(sharedPreferences.getString(Tokens.spIp, Tokens.defaultIp))
        portTextInputLayout.editText?.setText(sharedPreferences.getString(Tokens.spPort, Tokens.defaultPort))
        otpEndpointTextInputLayout.editText?.setText(sharedPreferences.getString(Tokens.spOtpEndpoint, Tokens.defaultOtpEndpoint))
        registerDeviceEndpointTextInputLayout.editText?.setText(sharedPreferences.getString(Tokens.spRegisterDeviceEndpoint, Tokens.defaultRegisterDeviceEndpoint))
        backgroundSwitch.isChecked = sharedPreferences.getBoolean(Tokens.spBackgroundService, Tokens.defaultBackgroundService)
        sendOtpSwitch.isChecked = sharedPreferences.getBoolean(Tokens.spSendOtp, Tokens.defaultSendOtp)

        toggleSendOtp(sendOtpSwitch.isChecked)

        saveButton.setOnClickListener {
            if (ipTextInputLayout.editText?.text.toString().isEmpty()) {
                Toast.makeText(this, "Remote Server IP cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (portTextInputLayout.editText?.text.toString().isEmpty()) {
                Toast.makeText(this, "Remote Server Port cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (otpEndpointTextInputLayout.editText?.text.toString().isEmpty()) {
                Toast.makeText(this, "Remote Server OTP Endpoint cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (registerDeviceEndpointTextInputLayout.editText?.text.toString().isEmpty()) {
                Toast.makeText(this, "Remote Server Register Device Endpoint cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            editor.apply {
                putString(Tokens.spIp, ipTextInputLayout.editText?.text.toString())
                putString(Tokens.spPort, portTextInputLayout.editText?.text.toString())
                putString(Tokens.spOtpEndpoint, otpEndpointTextInputLayout.editText?.text.toString())
                putString(Tokens.spRegisterDeviceEndpoint, registerDeviceEndpointTextInputLayout.editText?.text.toString())
                putBoolean(Tokens.spBackgroundService, backgroundSwitch.isChecked)
                putBoolean(Tokens.spSendOtp, sendOtpSwitch.isChecked)
                apply()
            }
            Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        }

        sendOtpSwitch.setOnCheckedChangeListener {_, isChecked ->
            toggleSendOtp(isChecked)
        }

        testServerButton.setOnClickListener {
            if (ipTextInputLayout.editText?.text.toString().isEmpty()) {
                Toast.makeText(this, "Remote Server IP cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (portTextInputLayout.editText?.text.toString().isEmpty()) {
                Toast.makeText(this, "Remote Server Port cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (otpEndpointTextInputLayout.editText?.text.toString().isEmpty()) {
                Toast.makeText(this, "Remote Server OTP Endpoint cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val json = JSONObject().apply {
                put("otp", "0000")
            }
            val requestBody = json.toString().toRequestBody(Tokens.MEDIA_TYPE_JSON)

            val ip = ipTextInputLayout.editText?.text.toString()
            val port = portTextInputLayout.editText?.text.toString()
            val otpEndpoint = otpEndpointTextInputLayout.editText?.text.toString()
            val serverUrl = "http://$ip:$port/$otpEndpoint"
            val request = Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Failed to send OTP to server: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    runOnUiThread {
                        if (response.isSuccessful) {
                            Toast.makeText(applicationContext, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(applicationContext, "Failed to send OTP to server: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }
    
    private fun toggleSendOtp(enabled: Boolean) {
        ipTextInputLayout.isEnabled = enabled
        ipTextInputLayout.editText?.isEnabled = enabled
        portTextInputLayout.isEnabled = enabled
        portTextInputLayout.editText?.isEnabled = enabled
        otpEndpointTextInputLayout.isEnabled = enabled
        otpEndpointTextInputLayout.editText?.isEnabled = enabled
        registerDeviceEndpointTextInputLayout.isEnabled = enabled
        registerDeviceEndpointTextInputLayout.editText?.isEnabled = enabled
        testServerButton.isEnabled = enabled
        backgroundSwitch.isEnabled = enabled
    }
}