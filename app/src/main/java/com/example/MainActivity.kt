package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ExamStudioApp
import com.example.ui.theme.NadiyarExamTheme
import com.example.ui.viewmodel.ExamViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NadiyarExamTheme {
                val examViewModel: ExamViewModel = viewModel()
                ExamStudioApp(viewModel = examViewModel)
            }
        }
    }
}
