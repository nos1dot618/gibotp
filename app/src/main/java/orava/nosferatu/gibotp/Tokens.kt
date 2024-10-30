package orava.nosferatu.gibotp

import okhttp3.MediaType.Companion.toMediaType

@Suppress("ConstPropertyName")
class Tokens {
    companion object {
        const val sp: String = "SP@gibotp"

        const val spIp: String = "remote_ip"
        const val spPort: String = "remote_port"
        const val spOtpEndpoint: String = "remote_otp_endpoint"
        const val spRegisterDeviceEndpoint: String = "remote_register_device_endpoint"
        const val spBackgroundService: String = "background_service"
        const val spSendOtp: String = "send_otp"
        const val spDeviceIdSent: String = "device_id_sent"

        const val defaultIp: String = "10.0.2.2"
        const val defaultPort: String = "3000"
        const val defaultOtpEndpoint: String = "receive_otp"
        const val defaultRegisterDeviceEndpoint: String = "register_device"
        const val defaultBackgroundService: Boolean = false
        const val defaultSendOtp: Boolean = true
        const val defaultDeviceIdSent: Boolean = false

        const val intentOtpReceived: String = "otp_received"

        val MEDIA_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
    }
}