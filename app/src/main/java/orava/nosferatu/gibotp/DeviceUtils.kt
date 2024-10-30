package orava.nosferatu.gibotp

import android.content.Context
import android.provider.Settings

class DeviceUtils {
    companion object {
        fun getDeviceId(context: Context): String {
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
    }
}