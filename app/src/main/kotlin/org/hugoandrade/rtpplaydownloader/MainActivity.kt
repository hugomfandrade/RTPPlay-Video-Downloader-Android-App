package org.hugoandrade.rtpplaydownloader

import android.Manifest
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
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
import android.content.Intent
import android.support.v7.widget.*
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase
import org.hugoandrade.rtpplaydownloader.network.utils.FilenameLockerAdapter
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils

class MainActivity : ActivityBase(), DownloadManagerViewOps {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mDownloadItemsRecyclerView: RecyclerView
    private lateinit var mDownloadItemsAdapter: DownloadItemsAdapter

    private lateinit var mDownloadManager: DownloadManager


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        extractActionSendIntentAndUpdateUI(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val oldDownloadManager = retainedFragmentManager.get<DownloadManager>(DownloadManager::class.java.simpleName)

        if (oldDownloadManager == null) {
            mDownloadManager = DownloadManager()
            mDownloadManager.onCreate(this)
            retainedFragmentManager.put(DownloadManager::class.java.simpleName, mDownloadManager)
        }
        else{
            mDownloadManager = oldDownloadManager
            mDownloadManager.onConfigurationChanged(this)
        }

        initializeUI()

        val url : String? = DevConstants.url
        url.let {
            binding.inputUriEditText.setText(it)
            binding.inputUriEditText.setSelection(binding.inputUriEditText.text.length)
            ViewUtils.showSoftKeyboardAndRequestFocus(binding.inputUriEditText)
        }

        extractActionSendIntentAndUpdateUI(intent)
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
        mDownloadItemsRecyclerView.layoutManager =
                if (!ViewUtils.isTablet(this) && ViewUtils.isPortrait(this)) LinearLayoutManager(this)
                else GridLayoutManager(this, if (!ViewUtils.isTablet(this) && !ViewUtils.isPortrait(this)) 2 else 3)
        mDownloadItemsAdapter = DownloadItemsAdapter()
        mDownloadItemsRecyclerView.adapter = mDownloadItemsAdapter

        binding.emptyListViewGroup.visibility = if (mDownloadItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
    }

    private fun extractActionSendIntentAndUpdateUI(intent: Intent?) {

        if (intent != null && checkNotNull(intent.action?.equals(Intent.ACTION_SEND))) {
            val url: String = intent.getStringExtra(Intent.EXTRA_TEXT)
            binding.inputUriEditText.setText(url)
            binding.inputUriEditText.setSelection(binding.inputUriEditText.text.length)
        }
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

        val primaryClipDescription = clipboard.primaryClipDescription

        // If it does contain data, decide if you can handle the data.
        if (clipboard.hasPrimaryClip()
                && primaryClipDescription != null
                && primaryClipDescription.hasMimeType(MIMETYPE_TEXT_PLAIN)) {

            // since the clipboard has data but it is not plain text

            //since the clipboard contains plain text.
            val item = clipboard.primaryClip?.getItemAt(0)

            // Gets the clipboard as text.
            val pasteData = item?.text.toString()

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

        ViewUtils.hideSoftKeyboardAndClearFocus(binding.inputUriEditText)
        binding.inputUriEditText.clearFocus()

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
        future.addCallback(object : FutureCallback<ParsingData> {

            override fun onSuccess(result: ParsingData) {

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
                .setOnParsingDialogListener(object : ParsingDialog.OnParsingListener {

                    var paginationFuture : PaginationParseFuture? = null
                    var paginationMoreFuture : PaginationParseFuture? = null

                    override fun onCancelled() {
                        future.failed("parsing was cancelled")

                        paginationFuture?.failed("parsing was cancelled")
                        paginationMoreFuture?.failed("parsing was cancelled")
                        FilenameLockerAdapter.instance.clear()
                    }

                    override fun onDownload(tasks : ArrayList<DownloaderTaskBase>) {
                        tasks.forEach(action = { task ->
                            val filename: String? = task.videoFileName
                            if (filename != null) {
                                FilenameLockerAdapter.instance.putUnremovable(filename)
                            }
                            startDownload(task)
                        })

                        parsingDialog?.dismissDialog()
                        parsingDialog = null
                    }

                    override fun onParseEntireSeries(paginationTask: PaginationParserTaskBase) {
                        parsingDialog?.loading()
                        paginationFuture = mDownloadManager.parsePagination(url, paginationTask)
                        paginationFuture?.addCallback(object : FutureCallback<ArrayList<DownloaderTaskBase>> {

                            override fun onSuccess(result: ArrayList<DownloaderTaskBase>) {

                                runOnUiThread {
                                    parsingDialog?.showPaginationResult(paginationTask, result)
                                }
                            }

                            override fun onFailed(errorMessage: String) {

                                runOnUiThread {
                                    val message = "Unable to parse pagination: $errorMessage"

                                    binding.root.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }

                                    parsingDialog?.dismissDialog()
                                    parsingDialog = null
                                }
                            }
                        })
                    }

                    override fun onParseMore(paginationTask: PaginationParserTaskBase) {
                        parsingDialog?.loadingMore()
                        paginationMoreFuture = mDownloadManager.parseMore(url, paginationTask)
                        paginationMoreFuture?.addCallback(object : FutureCallback<ArrayList<DownloaderTaskBase>> {

                            override fun onSuccess(result: ArrayList<DownloaderTaskBase>) {

                                runOnUiThread {
                                    parsingDialog?.showPaginationMoreResult(paginationTask, result)
                                }
                            }

                            override fun onFailed(errorMessage: String) {

                                runOnUiThread {
                                    val message = "Unable to parse more pagination: $errorMessage"

                                    binding.root.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }

                                    parsingDialog?.dismissDialog()
                                    parsingDialog = null
                                }
                            }
                        })

                    }

                })
                .create()
        parsingDialog?.show()
    }

    private fun startDownload(task: DownloaderTaskBase) {
        val item : DownloadableItem = mDownloadManager.download(task)
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

                    // upload history
                    VersionUtils.uploadHistory(getActivityContext(), downloadableItem)
                }
            }

        })
        runOnUiThread {
            mDownloadItemsAdapter.add(item)
            binding.emptyListViewGroup.visibility = if (mDownloadItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
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