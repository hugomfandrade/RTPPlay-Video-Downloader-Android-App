package org.hugoandrade.rtpplaydownloader

import android.Manifest
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import org.hugoandrade.rtpplaydownloader.databinding.ActivityMainBinding
import org.hugoandrade.rtpplaydownloader.network.DownloadManager
import org.hugoandrade.rtpplaydownloader.utils.PermissionDialog
import org.hugoandrade.rtpplaydownloader.utils.PermissionUtils
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils


class MainActivity : ActivityBase(), DownloadManager.DownloadManagerViewOps {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mDownloadManager: DownloadManager;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeUI()

        if (retainedFragmentManager.get<DownloadManager>(DownloadManager::class.java.simpleName) == null) {
            mDownloadManager = DownloadManager()
            mDownloadManager.onCreate(this)
            retainedFragmentManager.put(mDownloadManager.javaClass.simpleName, mDownloadManager)
        }
        else{
            mDownloadManager = retainedFragmentManager.get(DownloadManager::class.java.simpleName)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!isChangingConfigurations()) {
            mDownloadManager.onDestroy();
        }
    }

    override fun onDownloading(float: Float) {


        runOnUiThread(Runnable {
            binding.progressIndicator.text = float.toString()
        })
    }

    private fun initializeUI() {

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.inputUriEditText.setSelection(binding.inputUriEditText.text.length);
    }

    fun download(view: View) {

        ViewUtils.hideSoftKeyboardAndClearFocus(binding.root)

        if (!PermissionUtils.hasGrantedPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionDialog.Builder.instance(this)
                    .setOnPermissionDialog(object : PermissionDialog.OnPermissionListener {
                        override fun onAllowed(wasAllowed: Boolean) {
                            if (wasAllowed) {
                                val activity = this@MainActivity
                                PermissionUtils.requestPermission(
                                        activity,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {
                                // onBackPressed()
                            }
                        }
                    })
                    .create()
                    .show()
        }
        else {
            doDownload(binding.inputUriEditText.text.toString())
        }
    }

    private fun doDownload(url: String) {
        mDownloadManager.start(url)
    }

    override fun onParsingEnded(url: String, isOk: Boolean, message : String) {
        runOnUiThread {
            var i = ""
            if (isOk) {
                i = "Able to"
            }
            else {
                i = "Unable to"
            }

            Log.e(TAG, i + " parse (" + message + ") " + binding.inputUriEditText.text.toString());
            binding.root.let { Snackbar.make(it, i + " parse (" + message + ") " + binding.inputUriEditText.text.toString(), Snackbar.LENGTH_LONG).show() }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        PermissionUtils.onRequestPermissionsResult(this,
                requestCode,
                permissions,
                grantResults,
                object : PermissionUtils.OnRequestPermissionsResultCallback {
                    override fun onRequestPermissionsResult(permissionType: String, wasPermissionGranted: Boolean) {
                        when (permissionType) {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE -> if (wasPermissionGranted) {
                                doDownload(binding.inputUriEditText.text.toString())
                                // mBluetoothState = getInitialBluetoothState()

                                // setupBluetoothUI()
                            } else {
                                // onBackPressed()
                            }
                        }
                    }
                })
    }
}