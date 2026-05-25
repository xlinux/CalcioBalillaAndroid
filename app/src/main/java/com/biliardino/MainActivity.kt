package com.biliardino

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.biliardino.ui.ContentScreen
import com.biliardino.ui.theme.BiliardinoTheme
import com.biliardino.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BiliardinoTheme {
                Surface {
                    val vm: AppViewModel = viewModel()
                    ContentScreen(vm)
                }
            }
        }
    }
}
