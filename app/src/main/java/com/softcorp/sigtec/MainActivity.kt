package com.softcorp.sigtec

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.softcorp.sigtec.core.navigation.SigtecRaiz
import com.softcorp.sigtec.core.theme.SigtecTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SigtecTheme {
                SigtecRaiz()
            }
        }
    }
}