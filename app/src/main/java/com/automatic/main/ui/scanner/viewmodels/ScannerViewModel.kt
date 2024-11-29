package com.automatic.main.ui.scanner.viewmodels

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.automatic.scanner.ScannerManager
import com.automatic.scanner.ScannerViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 *  This class contain the business logic for scanner.
 */
@HiltViewModel
class ScannerViewModel @Inject constructor() : ViewModel() {
    private lateinit var qrCodeManager: ScannerManager
    private lateinit var __LifecycleOwner: LifecycleOwner

    /**
     * Initialize Camera Manager class.
     */
    internal fun startCamera(
        viewLifecycleOwner: LifecycleOwner,
        context: Context,
        previewView: PreviewView,
        onResult: (state: ScannerViewState, result: String) -> Unit,
    ) {
        __LifecycleOwner = viewLifecycleOwner
        qrCodeManager = ScannerManager(
            owner = viewLifecycleOwner, context = context,
            viewPreview = previewView,
            onResult = onResult,
            lensFacing = CameraSelector.LENS_FACING_BACK
        )
    }

    fun stopCamera() {
        qrCodeManager.stopCamera()
    }
}