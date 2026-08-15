package com.resurrect.xperi_r.feature

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.hardware.SensorManager.SENSOR_DELAY_UI
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.resurrect.xperi_r.XperiRApplication
import com.resurrect.xperi_r.util.DeviceModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.logcat
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class FlipToShush(
    private val lifecycleOwner: LifecycleOwner,
    private val service: AccessibilityService,
) : DefaultLifecycleObserver {
    private val sensorManager: SensorManager = service.getSystemService()!!
    private val notificationManager: NotificationManager = service.getSystemService()!!
    private val vibrator: Vibrator = service.getSystemService()!!
    private val sensorWakeLock =
        service
            .getSystemService<PowerManager>()!!
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "XperiR::FlipToShushSensor")

    private val isFullTimeListening = supportFullTimeListening(service)

    private val shushOnVibrationEffect =
        VibrationEffect
            .createWaveform(longArrayOf(16L, 150L, 14L, 250L, 12L), intArrayOf(200, 0, 150, 0, 100), -1)
    private val shushOffVibrationEffect = VibrationEffect.createOneShot(20, 255)

    private var deviceInclination = 0.0
    private var isProximityNear = false
    private var isDndOnByService = false

    private val isDoNotDisturbOff: Boolean
        get() = notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL

    private val isDeviceFlatFaceDown: Boolean
        get() = deviceInclination >= 170

    private var accelerometerListenerRegistered = false
    private val accelerometerEventListener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                when (event?.sensor?.type) {
                    SOMC_WAKEUP_ACCELEROMETER, Sensor.TYPE_ACCELEROMETER -> {
                        val (x, y, z) = event.values
                        val nG = sqrt(x.pow(2) + y.pow(2) + z.pow(2)).toDouble()
                        deviceInclination = Math.toDegrees(acos(z / nG))
                    }
                }
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int,
            ) {
            }
        }

    private var proximityListenerRegistered = false
    private val proximityEventListener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                when (event?.sensor?.type) {
                    Sensor.TYPE_PROXIMITY -> {
                        isProximityNear = event.values[0] == 0F

                        shushCheckerJob?.cancel()
                        unshushCheckerJob?.cancel()

                        if (!isDndOnByService && isDoNotDisturbOff && isProximityNear) {
                            shushCheckerJob = startCheckForShush()
                        }

                        if (isDndOnByService && !isProximityNear) {
                            unshushCheckerJob = startCheckForUnshush()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int,
            ) {
            }
        }

    private var screenEventReceiverRegistered = false
    private val screenEventReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> updateFlipToShush(true)
                    Intent.ACTION_SCREEN_OFF -> updateFlipToShush(false)
                }
            }
        }

    private var shushCheckerJob: Job? = null
    private var unshushCheckerJob: Job? = null

    override fun onDestroy(owner: LifecycleOwner) {
        updateFlipToShush(false)
        updateScreenReceiverState(false)
        switchDndState(false)
    }

    private fun updateScreenReceiverState(state: Boolean) {
        if (state) {
            if (!screenEventReceiverRegistered) {
                logcat { "Registering screen listener" }
                val filter =
                    IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                    }
                service.registerReceiver(screenEventReceiver, filter)
                screenEventReceiverRegistered = true
            }
        } else if (screenEventReceiverRegistered) {
            logcat { "Unregistering screen listener" }
            service.unregisterReceiver(screenEventReceiver)
            screenEventReceiverRegistered = false
        }
    }

    private fun updateFlipToShush(state: Boolean) {
        if (state) {
            registerSensors(proximity = true, accelerometer = false)
        } else {
            registerSensors(proximity = false, accelerometer = false)
        }
    }

    private fun registerSensors(
        proximity: Boolean,
        accelerometer: Boolean,
    ) {
        if (proximity) {
            if (!proximityListenerRegistered) {
                val sensor = sensorManager.getProximity()
                logcat { "Registering \"${sensor?.name}\" to $proximityEventListener" }
                sensorManager.registerListener(proximityEventListener, sensor, SENSOR_DELAY_NORMAL)
                proximityListenerRegistered = true
            }
        } else if (proximityListenerRegistered) {
            logcat { "Unregistering proximity $proximityEventListener" }
            sensorManager.unregisterListener(proximityEventListener)
            proximityListenerRegistered = false
        }
        if (accelerometer) {
            if (!accelerometerListenerRegistered) {
                val sensor = sensorManager.getAccelerometer()
                logcat { "Registering sensor \"${sensor?.name}\" to $accelerometerEventListener" }
                if (isFullTimeListening && !sensor!!.isWakeUpSensor) {
                    logcat { "Acquiring wakelock because \"${sensor.name}\" is a non-wakeup sensor" }
                    sensorWakeLock.acquire(SENSOR_WAKELOCK_TIMEOUT)
                }
                sensorManager.registerListener(accelerometerEventListener, sensor, SENSOR_DELAY_UI)
                accelerometerListenerRegistered = true
            }
        } else if (accelerometerListenerRegistered) {
            logcat { "Unregistering accelerometer $accelerometerEventListener" }
            sensorManager.unregisterListener(accelerometerEventListener)
            if (sensorWakeLock.isHeld) {
                sensorWakeLock.release()
            }
            accelerometerListenerRegistered = false
        }
    }

    private fun switchDndState(state: Boolean) {
        isDndOnByService =
            if (state) {
                if (!isDndOnByService) {
                    logcat { "Shush state on" }
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    vibrator.vibrate(shushOnVibrationEffect)
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                    true
                } else {
                    logcat { "User DND is active, shush state unchanged" }
                    false
                }
            } else {
                if (isDndOnByService) {
                    logcat { "Shush state off" }
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    vibrator.vibrate(shushOffVibrationEffect)
                }
                false
            }
    }

    private fun startCheckForShush() = lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
        try {
            logcat { "Waiting period before rechecking conditions" }
            registerSensors(proximity = true, accelerometer = true)

            val inclinations = mutableListOf<Double>()
            val startWait = SystemClock.elapsedRealtime()
            var currentWaitTime = SystemClock.elapsedRealtime() - startWait
            while (currentWaitTime < SHUSH_WAITING_PERIOD) {
                if (!isActive) {
                    throw CancellationException()
                }
                if (currentWaitTime >= SHUSH_WAITING_PERIOD / 2) {
                    inclinations += deviceInclination
                }
                currentWaitTime = SystemClock.elapsedRealtime() - startWait
            }

            if (!isDndOnByService && isProximityNear && isDoNotDisturbOff && isDeviceFlatFaceDown) {
                val inclinationsAvg = inclinations.average().toBigDecimal().setScale(1, RoundingMode.HALF_EVEN)
                val currentInclination = deviceInclination.toBigDecimal().setScale(1, RoundingMode.HALF_EVEN)
                if ((inclinationsAvg - currentInclination).abs() <= BigDecimal.ONE) {
                    logcat { "Shush conditions met and stopping check" }
                    switchDndState(true)
                } else {
                    logcat { "No shush, device wasn't in stationary position" }
                }
            } else {
                logcat { "Shush conditions unmet" }
            }
        } catch (_: CancellationException) {
            logcat { "Job cancelled" }
        } finally {
            registerSensors(proximity = true, accelerometer = false)
        }
    }

    private fun startCheckForUnshush() = lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
        try {
            logcat { "Waiting period before rechecking conditions" }
            registerSensors(proximity = true, accelerometer = true)

            val startWait = SystemClock.elapsedRealtime()
            while (SystemClock.elapsedRealtime() - startWait < UNSHUSH_WAITING_PERIOD) {
                if (!isActive) {
                    throw CancellationException()
                }
            }

            if (isDndOnByService && !isProximityNear) {
                logcat { "Unshush condition met" }
                switchDndState(false)
            } else {
                logcat { "Unshush condition unmet, shush state unchanged" }
            }
        } catch (_: CancellationException) {
            logcat { "Job cancelled" }
        } finally {
            registerSensors(proximity = true, accelerometer = false)
        }
    }

    init {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                XperiRApplication.prefs.flipToShushEnabledFlow.collect {
                    val shouldEnable = it && notificationManager.isNotificationPolicyAccessGranted
                    updateFlipToShush(shouldEnable)
                    updateScreenReceiverState(shouldEnable && !isFullTimeListening)
                    if (!shouldEnable) {
                        switchDndState(false)
                    }
                    logcat { "Flip2Shush enabled=$it" }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(this)
    }

    companion object {
        private const val SHUSH_WAITING_PERIOD = 2000L
        private const val UNSHUSH_WAITING_PERIOD = 350L
        private const val SENSOR_WAKELOCK_TIMEOUT = 2000L
        private const val SOMC_WAKEUP_ACCELEROMETER = 65661
        private fun SensorManager.getAccelerometer(): Sensor? {
            var sensor: Sensor? = null
            if (DeviceModel.isPDX206 || DeviceModel.isPDX203) {
                sensor = getDefaultSensor(SOMC_WAKEUP_ACCELEROMETER, true)
            }
            if (sensor == null) {
                sensor = getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
            }
            if (sensor == null) {
                sensor = getDefaultSensor(Sensor.TYPE_ACCELEROMETER, false)
            }
            return sensor
        }
        private fun SensorManager.getProximity(): Sensor? {
            // Try to get wake up variant first
            var sensor = getDefaultSensor(Sensor.TYPE_PROXIMITY, true)
            if (sensor == null) {
                sensor = getDefaultSensor(Sensor.TYPE_PROXIMITY, false)
            }
            return sensor
        }

        fun supportFullTimeListening(context: Context): Boolean = context.getSystemService<SensorManager>()?.getProximity()?.isWakeUpSensor == true
    }
}
