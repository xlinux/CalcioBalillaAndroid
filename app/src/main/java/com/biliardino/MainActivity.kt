package com.biliardino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biliardino.ui.ContentScreen
import com.biliardino.ui.theme.CampionatoCoppeTheme
import com.biliardino.viewmodel.AppViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            val state by vm.state.collectAsState()
            
            val isDarkTheme = when(state.theme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            CampionatoCoppeTheme(darkTheme = isDarkTheme) {
                Surface {
                    ContentScreen(vm)
                }
            }
        }
    }
}
