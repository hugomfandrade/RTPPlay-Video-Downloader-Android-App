package org.hugoandrade.rtpplaydownloader

import android.Manifest
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.util.Log
import android.view.View
import org.hugoandrade.rtpplaydownloader.databinding.ActivityMainBinding
import org.hugoandrade.rtpplaydownloader.network.*
import org.hugoandrade.rtpplaydownloader.utils.PermissionDialog
import org.hugoandrade.rtpplaydownloader.utils.PermissionUtils
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils
import java.net.URL

class MainActivity : ActivityBase(), DownloadManagerViewOps {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mDownloadItemsRecyclerView: RecyclerView
    private lateinit var mDownloadItemsAdapter: DownloadItemsAdapter

    private lateinit var mDownloadManager: DownloadManager

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

        if (!isChangingConfigurations) {
            mDownloadManager.onDestroy()
        }
    }

    private fun initializeUI() {

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.inputUriEditText.setSelection(binding.inputUriEditText.text.length)

        val simpleItemAnimator : SimpleItemAnimator = DefaultItemAnimator()
        simpleItemAnimator.supportsChangeAnimations = false

        mDownloadItemsRecyclerView = binding.downloadItemsRecyclerView
        mDownloadItemsRecyclerView.itemAnimator = simpleItemAnimator
        mDownloadItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        mDownloadItemsAdapter = DownloadItemsAdapter(object : DownloadItemsAdapter.DownloadItemsAdapterListener {
            override fun onOn() {

            }
        })
        mDownloadItemsRecyclerView.adapter = mDownloadItemsAdapter
    }

    fun pasteFromClipboard(view: View) {

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // If it does contain data, decide if you can handle the data.
        if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription!!.hasMimeType(MIMETYPE_TEXT_PLAIN)) {

            // since the clipboard has data but it is not plain text

            //since the clipboard contains plain text.
            val item = clipboard.primaryClip!!.getItemAt(0)

            // Gets the clipboard as text.
            val pasteData = item.text.toString()

            if (isValidURL(pasteData)) {
                binding.inputUriEditText.setText(pasteData)
                binding.inputUriEditText.setSelection(binding.inputUriEditText.text.length)
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
        }
        else {
            doDownload(binding.inputUriEditText.text.toString())
        }
    }

    private fun doDownload(url: String) {
        mDownloadManager.start(url)
    }

    override fun onParsingError(url: String, message: String) {
        runOnUiThread {
            val errorMessage = "Unable to parse $message"

            Log.e(TAG, errorMessage)
            binding.root.let { Snackbar.make(it, errorMessage, Snackbar.LENGTH_LONG).show() }
        }
    }

    override fun onParsingSuccessful(item: DownloadableItem) {
        item.addDownloadStateChangeListener(object :DownloadableItemStateChangeListener {
            override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
                // listen for end of download and show message
                if (downloadableItem.state == DownloadableItem.State.End) {
                    runOnUiThread {
                        val message = "Finished downloading " + downloadableItem.filename
                        Log.e(TAG, message)
                        binding.root.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }
                    }
                    downloadableItem.removeDownloadStateChangeListener(this)
                }
            }

        })
        runOnUiThread {
            mDownloadItemsAdapter.add(item)
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
                            } else {
                                // onBackPressed()
                            }
                        }
                    }
                })
    }

    private fun isValidURL(urlText: String): Boolean {
        return try {
            val url = URL(urlText)
            "http" == url.protocol || "https" == url.protocol
        } catch (e: Exception) {
            false
        }
    }
}