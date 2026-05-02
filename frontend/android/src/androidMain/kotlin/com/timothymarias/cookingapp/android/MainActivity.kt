package com.timothymarias.cookingapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.timothymarias.cookingapp.shared.App
import com.timothymarias.cookingapp.shared.data.local.DriverConfig
import com.timothymarias.cookingapp.shared.di.initSeeding
import com.timothymarias.cookingapp.shared.di.sharedModule
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val koin = startKoin {
            modules(sharedModule(DriverConfig(androidContext = applicationContext)))
        }.koin
        initSeeding(koin)
        setContent {
            App()
        }
    }
}
