package orava.nosferatu.gibotp

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.regex.Pattern
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SmsBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            val bundle = intent?.extras
            if (bundle != null) {
                val protocolDataUnits = bundle.get("pdus") as Array<*>
                for (protocolDataUnit in protocolDataUnits) {
                    val format = bundle.getString("format")
                    val smsMessage = SmsMessage.createFromPdu(protocolDataUnit as ByteArray, format)
                    val messageBody = smsMessage.messageBody
                    val otp = extractOtp(messageBody)
                    if (otp.isNotEmpty()) {
                        Log.d(this::class.simpleName, "OTP extracted: $otp")

                        val sharedPreferences = context?.getSharedPreferences(Tokens.sp, Context.MODE_PRIVATE)
                        val isSendOtpEnabled = sharedPreferences?.getBoolean(Tokens.spSendOtp, Tokens.defaultSendOtp)?: false
                        val isBackgroundServiceEnabled = sharedPreferences?.getBoolean(Tokens.spBackgroundService, Tokens.defaultBackgroundService)?: false

                        // If App is running in background, check if switch is enabled
                        if (!isAppInForeground(context)) {
                            if (isSendOtpEnabled && isBackgroundServiceEnabled) {
                                sendOtpToBackground(context, otp)
                            }
                        } else {
                            // App is running in the foreground
                            val localIntent = Intent(Tokens.intentOtpReceived)
                            localIntent.putExtra("otp", otp)
                            context?.let {
                                LocalBroadcastManager.getInstance(it).sendBroadcast(localIntent)
                            }
                            if (isSendOtpEnabled) {
                                sendOtpToBackground(context, otp)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractOtp(message: String): String {
        // NOTE: Here it is assumed that OTP is any number ranging from 4-6 digits
        // TODO: This is not foolproof, as we are not aware about the format of OTP messages.
        //  For example we cannot parse OTP if it is belonging to any of the following formats:
        //  1. 111-111 or 111 111 (or any other delimiter)
        //  2. one one one one one one (otp in words)
        //  3. ABC11A (alphanumeric)
        val pattern = Pattern.compile("(\\d{4,6})")
        val matcher = pattern.matcher(message)
        return if (matcher.find()) matcher.group(0) ?: "" else ""
    }

    private fun sendOtpToBackground(context: Context?, otp: String) {
        val data = Data.Builder().putString("otp", otp).build()
        val sendOtpWork = OneTimeWorkRequestBuilder<SendOtpWorker>()
            .setInputData(data).build()
        context?.let {
            WorkManager.getInstance(it).enqueue(sendOtpWork)
        }
    }

    private fun isAppInForeground(context: Context?): Boolean {
        val activityManager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses ?: return false

        for (appProcess in runningAppProcesses) {
            if (appProcess.processName == context.packageName) {
                return appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            }
        }
        return false
    }
}