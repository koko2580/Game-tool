package com.example

import android.app.Application

class NexoraApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
