package dev.rtrcompanion.app

import android.app.Application
import timber.log.Timber

/**
 * Application entry point.
 * Initialises Timber logging for debug builds.
 */
class RtrApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
