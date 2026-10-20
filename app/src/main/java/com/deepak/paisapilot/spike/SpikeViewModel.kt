package com.deepak.paisapilot.spike

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SpikeViewModel @Inject constructor(
    private val probeDao: ToolchainProbeDao,
) : ViewModel() {
    val message: String = "PaisaPilot"

    fun probeCount(): Int = probeDao.count()
}
