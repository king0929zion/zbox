package com.example.messageguardian

import android.app.Application

class MessageGuardianApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MessageGuardianApp
            private set
    }
}
