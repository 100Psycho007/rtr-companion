package dev.rtrcompanion.protocol

import org.junit.rules.ExternalResource
import timber.log.Timber

/**
 * JUnit [ExternalResource] rule that plants a no-op Timber tree for unit tests.
 *
 * Timber is used throughout the production code. Without a planted tree,
 * it silently discards all log calls — but this rule makes that explicit
 * and safe, and avoids any accidental NPEs on Android-stub logging internals.
 *
 * Usage in a test class:
 * ```kotlin
 * @get:Rule val timber = TimberTestRule()
 * ```
 */
class TimberTestRule : ExternalResource() {

    private val noOpTree = object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Intentionally empty — suppress all log output during tests
        }
    }

    override fun before() {
        Timber.plant(noOpTree)
    }

    override fun after() {
        Timber.uproot(noOpTree)
    }
}
