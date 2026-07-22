package com.artier.ide

import android.app.Application
import android.util.Log
import com.artier.ide.proot.SystemInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ArtierApp : Application() {
    
    companion object {
        private const val TAG = "ArtierApp"
    }
    
    @Inject
    lateinit var systemInitializer: SystemInitializer
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Artier IDE starting...")
        
        // Initialize system in background
        applicationScope.launch {
            val success = systemInitializer.initialize()
            if (success) {
                Log.i(TAG, "System initialized successfully")
            } else {
                Log.e(TAG, "System initialization failed")
            }
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        systemInitializer.shutdown()
        Log.i(TAG, "Artier IDE terminated")
    }
}