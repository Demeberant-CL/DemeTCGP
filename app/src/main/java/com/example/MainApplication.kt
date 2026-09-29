package com.example

import android.app.Application
import com.example.data.util.ErrorLogManager

class MainApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    ErrorLogManager.init(this)
  }
}
