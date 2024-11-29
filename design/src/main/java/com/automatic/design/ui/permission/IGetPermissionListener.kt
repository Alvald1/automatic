package com.automatic.design.ui.permission

/**
 *  This interface is used to define User action on permissions.
 */
interface IGetPermissionListener {
    fun onPermissionGranted()
    fun onPermissionDenied()
    fun onPermissionRationale()
}