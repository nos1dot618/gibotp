package orava.nosferatu.gibotp

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SendOtpWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val sharedPreferences = applicationContext.getSharedPreferences(Tokens.sp, Context.MODE_PRIVATE)

        val otp = inputData.getString("otp") ?: return Result.failure()
        val json = JSONObject().apply {
            put("otp", otp)
        }

        val ip = sharedPreferences.getString(Tokens.spIp, Tokens.defaultIp)
        val port = sharedPreferences.getString(Tokens.spPort, Tokens.defaultPort)
        val otpEndpoint = sharedPreferences.getString(Tokens.spOtpEndpoint, Tokens.defaultOtpEndpoint)
        val serverUrl = "http://$ip:$port/$otpEndpoint"

        val requestBody = json.toString().toRequestBody(Tokens.MEDIA_TYPE_JSON)
        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: IOException) {
            Result.retry()
        }
    }
}
