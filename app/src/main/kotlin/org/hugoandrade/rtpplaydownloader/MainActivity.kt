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
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import java.net.URL


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
        } else {
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

    fun pasteFromClipboard(view: View) {

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // If it does contain data, decide if you can handle the data.
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription()!!.hasMimeType(MIMETYPE_TEXT_PLAIN)) {

            // since the clipboard has data but it is not plain text

            //since the clipboard contains plain text.
            val item = clipboard.getPrimaryClip()!!.getItemAt(0)

            // Gets the clipboard as text.
            val pasteData = item.getText().toString()

            if (isValidURL(pasteData)) {
                binding.inputUriEditText.setText(pasteData);
                binding.inputUriEditText.setSelection(binding.inputUriEditText.text.length);
            } else {
                binding.root.let { Snackbar.make(it, "Not a valid URL", Snackbar.LENGTH_LONG).show() }
            }
        } else {
            binding.root.let { Snackbar.make(it, "Nothing to paste from clipboard", Snackbar.LENGTH_LONG).show() }
        }
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
        } else {
            doDownload(binding.inputUriEditText.text.toString())
        }
    }

    private fun doDownload(url: String) {
        mDownloadManager.start(url)
    }

    override fun onParsingEnded(url: String, isOk: Boolean, message: String) {
        runOnUiThread {
            var i = ""
            if (isOk) {
                i = "Able to"
            } else {
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

    private fun isValidURL(urlText: String): Boolean {
        try {
            val url = URL(urlText);
            return "http".equals(url.getProtocol()) || "https".equals(url.getProtocol());
        } catch (e: Exception) {
            return false;
        }
    }
}