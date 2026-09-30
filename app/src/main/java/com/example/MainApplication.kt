package com.example

import android.app.Application
import com.example.data.util.ErrorLogManager
import com.example.data.util.NetworkUtils

class MainApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    // Inicializar manejador global de errores (Crash Catcher)
    ErrorLogManager.init(this)
    // Inicializar monitor de red reactivo (NetworkCallback pasivo)
    NetworkUtils.init(this)
  }
}
