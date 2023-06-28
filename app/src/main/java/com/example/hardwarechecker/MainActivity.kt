package com.example.hardwarechecker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hardwarechecker.databinding.ActivityMainBinding

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var  viewBinding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val memoryButton: Button = viewBinding.memoryButton
        val batteryButton: Button = viewBinding.batteryButton
        val soundButton: Button = viewBinding.soundButton
        val networkButton: Button = viewBinding.networkButton
        val cameraButton: Button = viewBinding.cameraButton
        val sensorButton: Button = viewBinding.sensorsButton
        viewBinding.resultTextView
        batteryButton.setOnClickListener {
            clearTextView()
            checkBattery()
        }
        memoryButton.setOnClickListener {
            clearTextView()
            checkMemoryAndStorage()
        }
        cameraButton.setOnClickListener {
            clearTextView()
            checkCamera()
        }
        sensorButton.setOnClickListener {
            clearTextView()
            checkSensors()
        }
        networkButton.setOnClickListener {
            clearTextView()
            checkBluetooth()
            checkNetworkStatus()

        }
        soundButton.setOnClickListener {

            clearTextView()
            checkSound()
            checkMicrophone()
        }
        checkPermissions()
    }



    private fun checkPermissions(){
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,

            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.RECORD_AUDIO)
        val requestPermission = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, permissions.toString())!=PackageManager.PERMISSION_GRANTED){
            requestPermission.add(permissions.toString())
            //ActivityCompat.requestPermissions(this,requestPermission.toTypedArray(),requestCode)
        }

        if (requestPermission.isNotEmpty()){
            ActivityCompat.requestPermissions(this,requestPermission.toTypedArray(),requestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == requestCode){
            for (i in grantResults.indices){
                if(grantResults[i]!= PackageManager.PERMISSION_GRANTED){
                    Toast.makeText( this,"PD:${permissions[i]}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun clearTextView() {
        viewBinding.resultTextView.text = ""
    }

    private fun checkMemoryAndStorage() {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val freeMemory = runtime.freeMemory()
        val totalMemory = runtime.totalMemory()

        val externalStorageDirectory = Environment.getExternalStorageDirectory()
        val stat = StatFs(externalStorageDirectory.path)
        val availableBytes: Long = stat.availableBlocksLong * stat.blockSizeLong

        val memMessage = """
            |Max Memory: ${maxMemory / 1024} KB
            |Free Memory: ${freeMemory / 1024} KB
            |Total Memory: ${totalMemory / 1024} KB
        """.trimMargin()

        val formattedSize = formatSize(availableBytes)

        val storageMessage = "Available Storage: $formattedSize"

        val combinedMessage = "$memMessage\n\n$storageMessage"

        viewBinding.resultTextView.text = combinedMessage
    }

    private fun formatSize(bytes: Long): String {
        val kilobytes = bytes / 1024
        val megabytes = kilobytes / 1024
        val gigabytes = megabytes / 1024

        return when {
            gigabytes > 0 -> "$gigabytes GB"
            megabytes > 0 -> "$megabytes MB"
            kilobytes > 0 -> "$kilobytes KB"
            else -> "$bytes bytes"
        }
    }

    private fun checkSound(){
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val isSoundOn = audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT

        val soundStatus = if (isSoundOn) "Phone is Ringing " else "Sound is OFF"

        viewBinding.resultTextView.append("\n$soundStatus")
    }
    private fun checkMicrophone() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        val audioRecord: AudioRecord? = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                requestCode
            )
            null
        } else {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        }

        val isMicrophoneWorking = audioRecord?.state == AudioRecord.STATE_INITIALIZED

        val microphoneStatus =
            if (isMicrophoneWorking) "Microphone is working" else "Microphone is not working"

        viewBinding.resultTextView.append("\n$microphoneStatus")
    }

    private fun checkCamera(){
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            val cameraId = cameraManager.cameraIdList[0]
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)

            val isCameraWorking = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) != null

            if (isCameraWorking) {
                // Camera is working
                viewBinding.resultTextView.text = "Camera is working"
            } else {
                // Camera is not working
                viewBinding.resultTextView.text = "Camera is not working"
            }
        } catch (e: CameraAccessException) {
            // Failed to access camera
            viewBinding.resultTextView.text = "Failed to access camera"
            e.printStackTrace()
        }
    }

    private fun checkBluetooth(){
        val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
//            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            viewBinding.resultTextView.append("Bluetooth not supported")

        } else {
            if (bluetoothAdapter.isEnabled) {
                //Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show()
                viewBinding.resultTextView.append("Bluetooth Enabled")
            } else {
                //Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_SHORT).show()
                viewBinding.resultTextView.append("Bluetooth Disabled")
            }
        }
    }

    @SuppressLint("ServiceCast")
    private fun checkNetworkStatus(){
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        //val isWifiEnabled = wifiManager.isWifiEnabled
        if(wifiManager.isWifiEnabled){
            //Toast.makeText(this,"Enabled",Toast.LENGTH_SHORT).show()
            viewBinding.resultTextView.append("\nWiFi Enabled")
        }else{
            //Toast.makeText(this,"Disabled",Toast.LENGTH_SHORT).show()
            viewBinding.resultTextView.append("\nWiFi Disabled")
        }

        val isSimNetworkEnabled = isSimNetworkEnabled(connectivityManager, telephonyManager)

        if (isSimNetworkEnabled) {
            //Toast.makeText(this, "SIM network is enabled", Toast.LENGTH_SHORT).show()
            viewBinding.resultTextView.append("\nSIM Enabled")
        } else {
            //Toast.makeText(this, "SIM network is disabled", Toast.LENGTH_SHORT).show()
            viewBinding.resultTextView.append("\nSIM Disabled")
        }
    }

    private fun isSimNetworkEnabled(
        connectivityManager: ConnectivityManager,
        telephonyManager: TelephonyManager
    ): Boolean {
        // Check for SIM network availability using ConnectivityManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                ?: false
        } else {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            return networkInfo?.type == ConnectivityManager.TYPE_MOBILE
        }

    }

    private fun checkBattery(){
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { intentFilter ->
            applicationContext.registerReceiver(null, intentFilter)
        }

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercentage: Int = (level.toFloat() / scale.toFloat() * 100).toInt()

        val batteryType: String? = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)?.let {
            when (it) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Unknown"
            }
        }

        val batteryHealth: String? = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)?.let {
            when (it) {
                BatteryManager.BATTERY_HEALTH_UNKNOWN -> "Unknown"
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }
        }


        viewBinding.resultTextView.append("Battery Level: $batteryPercentage%\n")
        viewBinding.resultTextView.append("Battery Type: $batteryType\n")
        viewBinding.resultTextView.append("Battery Health: $batteryHealth\n")
    }

    private fun checkSensors() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val sensorTypes = arrayOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_MOTION_DETECT,
            Sensor.TYPE_STEP_COUNTER
        )

        val sensorInfo = StringBuilder()
        sensorInfo.append("Available Sensors:\n")
        for (sensorType in sensorTypes) {
            val sensor = sensorManager.getDefaultSensor(sensorType)
            val sensorStatus = if (sensor != null) "Available" else "Not Available"
            sensorInfo.append("${sensorTypeToString(sensorType)}: $sensorStatus\n")
        }

        viewBinding.resultTextView.text = sensorInfo.toString()
    }

    private fun sensorTypeToString(sensorType: Int): String {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_LIGHT -> "Light Sensor"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            Sensor.TYPE_MOTION_DETECT -> "Motion Sensor"
            Sensor.TYPE_STEP_COUNTER -> "Step Sensor"
            else -> "Unknown Sensor"
        }
    }
    companion object {
        private const val requestCode =1001
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
}