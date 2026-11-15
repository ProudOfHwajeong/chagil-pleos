package com.chagil.app.vehicle

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Platform driving restriction (CarUxRestrictions). `true` = the platform says the driver is driving.
 * Stays `null` on devices without the Car service. Every call is guarded: a missing `android.car`
 * library or service must never crash the app.
 */
class UxRestrictionWatcher(
    context: Context,
    private val diagnostics: Diagnostics,
) {
    private val appContext = context.applicationContext

    private val _restricted = MutableStateFlow<Boolean?>(null)
    val restricted: StateFlow<Boolean?> = _restricted.asStateFlow()

    // Typed as Any so this class loads even when android.car classes are absent.
    private var connection: Any? = null

    fun start() {
        if (connection != null) return
        try {
            if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
                diagnostics.add("자동차 기기가 아님 · 주행 제한 정보 없음")
                return
            }
            connection = CarApi.connect(appContext) { _restricted.value = it }
            if (connection == null) diagnostics.add("주행 제한 서비스를 찾지 못함")
        } catch (t: Throwable) {
            _restricted.value = null
            diagnostics.add("주행 제한 정보를 읽을 수 없음 · ${t.shortMessage()}")
        }
    }

    fun stop() {
        val c = connection ?: return
        connection = null
        try {
            CarApi.disconnect(c)
        } catch (_: Throwable) {
        }
        _restricted.value = null
    }

    /** Isolates every android.car reference so it is only resolved inside the try blocks above. */
    private object CarApi {
        class Connection(val car: Car, val manager: CarUxRestrictionsManager)

        fun connect(context: Context, onChange: (Boolean?) -> Unit): Any? {
            val car = Car.createCar(context) ?: return null
            val manager = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
            if (manager == null) {
                runCatching { car.disconnect() }
                return null
            }
            onChange(manager.currentCarUxRestrictions?.isRequiresDistractionOptimization)
            manager.registerListener { restrictions -> onChange(restrictions?.isRequiresDistractionOptimization) }
            return Connection(car, manager)
        }

        fun disconnect(connection: Any) {
            val c = connection as? Connection ?: return
            runCatching { c.manager.unregisterListener() }
            runCatching { c.car.disconnect() }
        }
    }
}
