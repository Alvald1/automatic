package com.automatic.main.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.automatic.design.ui.base.BaseActivity
import com.automatic.design.ui.permission.IGetPermissionListener
import com.automatic.design.ui.permission.PermissionUtil
import com.licious.automatic.main.R
import com.licious.automatic.main.databinding.ActivityScannerBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 *  This class
 */
@AndroidEntryPoint
class ScannerActivity : BaseActivity<ActivityScannerBinding>(), IGetPermissionListener {
    private var navController: NavController? = null

    // Launcher to lunch Single Camera request.
    private val requestLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isGranted: Boolean ->
            permissionUtil.handleSinglePermissionResult(this, isGranted)
        }

    // OnActivityResult to handle permission result.
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                checkPermission()
            }
        }

    override fun getLogTag(): String = TAG

    override fun getViewBinding(): ActivityScannerBinding =
        ActivityScannerBinding.inflate(layoutInflater)

    @Inject
    lateinit var permissionUtil: PermissionUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация и проверка разрешений
        initView()
        checkPermission()

        // Обработка результата от ScannerFragment
        supportFragmentManager.setFragmentResultListener("scanResult", this) { _, bundle ->
            val result = bundle.getString("result")
            if (result != null) {
                val intent = Intent().apply {
                    putExtra("SCAN_RESULT", result)
                }
                setResult(Activity.RESULT_OK, intent)  // Передаем результат в MainActivity
                finish()  // Завершаем ScannerActivity
            }
        }
    }


    override fun onPermissionGranted() {
        navController?.setGraph(R.navigation.nav_main)
    }

    override fun onPermissionDenied() {
        checkPermission()
    }

    override fun onPermissionRationale() {
        permissionAlertDialog()
    }

    private fun initView() {
        permissionUtil.setPermissionListener(this)
        navController = binding.navHostFragment.getFragment<NavHostFragment>().navController
        binding.viewToolBar.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     *  Check camera permission.
     */
    private fun checkPermission() {
        permissionUtil.apply {
            if (!hasPermission(
                    this@ScannerActivity as AppCompatActivity,
                    Manifest.permission.CAMERA
                )
            ) {
                requestPermission(Manifest.permission.CAMERA, requestLauncher)
            } else {
                navController?.setGraph(R.navigation.nav_main)
            }
        }
    }

    /**
     *  Ask User to enable camera permissions.
     */
    private fun permissionAlertDialog() {
        AlertDialog.Builder(this).apply {
            setTitle(getString(R.string.permission_required))
            setMessage(getString(R.string.permission_msg))

            setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                permissionUtil.openAppSettingPage(
                    this@ScannerActivity as AppCompatActivity,
                    resultLauncher
                )
                dialog.dismiss()
            }

            setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
                checkPermission()
            }
            show()
        }
    }

    companion object {
        private const val TAG = "ScannerActivity"
    }
}