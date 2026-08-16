package my.id.rmalan.cache.sweep

import android.app.Application
import my.id.rmalan.cache.sweep.di.AppContainer

class CacheSweepApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
