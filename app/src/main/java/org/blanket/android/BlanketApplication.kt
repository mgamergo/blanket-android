package org.blanket.android

import android.app.Application
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.blanket.android.data.BlanketRepository

class BlanketApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val repository by lazy {
        BlanketRepository.create(File(filesDir, "blanket.json"), applicationScope)
    }
}
