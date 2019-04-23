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
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import org.hugoandrade.rtpplaydownloader.common.ActivityBase
import org.hugoandrade.rtpplaydownloader.databinding.ActivityMainBinding
import org.hugoandrade.rtpplaydownloader.network.*
import org.hugoandrade.rtpplaydownloader.network.download.DownloaderTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingDialog
import org.hugoandrade.rtpplaydownloader.utils.*

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
        binding.inputUriEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                toggleClearTextButton()
            }
        })

        toggleClearTextButton()

        val simpleItemAnimator : SimpleItemAnimator = DefaultItemAnimator()
        simpleItemAnimator.supportsChangeAnimations = false

        mDownloadItemsRecyclerView = binding.downloadItemsRecyclerView
        mDownloadItemsRecyclerView.itemAnimator = simpleItemAnimator
        mDownloadItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        mDownloadItemsAdapter = DownloadItemsAdapter()
        mDownloadItemsRecyclerView.adapter = mDownloadItemsAdapter
    }

    private fun toggleClearTextButton() {

        binding.clearTextButton.visibility =
                if (binding.inputUriEditText.text.isEmpty()) View.INVISIBLE
                else View.VISIBLE
    }

    /**
     * from activity_main.xml
     */
    fun pasteFromClipboard(view: View) {

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // If it does contain data, decide if you can handle the data.
        if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription!!.hasMimeType(MIMETYPE_TEXT_PLAIN)) {

            // since the clipboard has data but it is not plain text

            //since the clipboard contains plain text.
            val item = clipboard.primaryClip!!.getItemAt(0)

            // Gets the clipboard as text.
            val pasteData = item.text.toString()

            if (NetworkUtils.isValidURL(pasteData)) {
                binding.inputUriEditText.setText(pasteData)
                binding.inputUriEditText.setSelection(binding.inputUriEditText.text.length)
            } else {
                binding.root.let { Snackbar.make(it, "Not a valid URL", Snackbar.LENGTH_LONG).show() }
            }
        } else {
            binding.root.let { Snackbar.make(it, "Nothing to paste from clipboard", Snackbar.LENGTH_LONG).show() }
        }
    }

    /**
     * from activity_main.xml
     */
    fun clearUriEditText(view: View) {

        doClearUriEditText()
    }

    private fun doClearUriEditText() {

        binding.inputUriEditText.setText("")
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

    private var parsingDialog : ParsingDialog? = null

    private fun doDownload(url: String) {

        val isParsing : Boolean = parsingDialog?.isShowing() ?: false

        if (isParsing) {
            return
        }
        else {
            parsingDialog?.dismissDialog()
        }

        val future : ParseFuture = mDownloadManager.parseUrl(url)
        future.addCallback(object : FutureCallback<DownloaderTaskBase> {

            override fun onSuccess(result: DownloaderTaskBase?) {

                runOnUiThread {
                    parsingDialog?.showParsingResult(result)
                }
            }

            override fun onFailed(errorMessage: String) {
                runOnUiThread {
                    val message = "Unable to parse $errorMessage"

                    binding.root.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }

                    parsingDialog?.dismissDialog()
                    parsingDialog = null
                }
            }
        })

        parsingDialog = ParsingDialog.Builder.instance(this)
                .setOnParsingDialog(object : ParsingDialog.OnParsingListener {
                    override fun onCancelled() {
                        future.failed("parsing was cancelled")
                    }
                    override fun onDownload(task : DownloaderTaskBase?) {
                        if (task != null) {
                            mDownloadManager.download(task)
                        }

                        parsingDialog?.dismissDialog()
                        parsingDialog = null
                    }
                })
                .create()
        parsingDialog?.show()
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
                if (downloadableItem.state == DownloadableItemState.End) {
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
}