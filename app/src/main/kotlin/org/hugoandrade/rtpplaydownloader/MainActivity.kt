package org.hugoandrade.rtpplaydownloader

import android.Manifest
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.*
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import org.hugoandrade.rtpplaydownloader.common.ActivityBase
import org.hugoandrade.rtpplaydownloader.databinding.ActivityMainBinding
import org.hugoandrade.rtpplaydownloader.network.*
import org.hugoandrade.rtpplaydownloader.network.parsing.ParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingDialog
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParseFuture
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTaskBase
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTaskBase
import org.hugoandrade.rtpplaydownloader.network.utils.FilenameLockerAdapter
import org.hugoandrade.rtpplaydownloader.utils.*
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
        } else {
            mDownloadManager = oldDownloadManager
            mDownloadManager.onConfigurationChanged(this)
        }

        initializeUI()

        if (oldDownloadManager == null ) { // is first
            val devUrl: String? = DevConstants.url
            if (devUrl != null) {
                binding.inputUriEditText.setText(devUrl)
                binding.inputUriEditText.setSelection(binding.inputUriEditText.text.length)
            } else {
                ViewUtils.hideSoftKeyboardAndClearFocus(binding.inputUriEditText)
            }

            extractActionSendIntentAndUpdateUI(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_empty_db).isVisible = false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_empty_db -> {
                mDownloadManager.emptyDB()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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
        if (DevConstants.enableSwipe) {
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                    0,
                    ItemTouchHelper.LEFT.or(ItemTouchHelper.RIGHT)) {

                override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                    return super.getSwipeThreshold(viewHolder)
                }

                override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
                    return super.getSwipeEscapeVelocity(defaultValue) * 5
                }

                override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
                    return super.getSwipeVelocityThreshold(defaultValue) * 0.2f
                }

                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder1: RecyclerView.ViewHolder,
                                    viewHolder2: RecyclerView.ViewHolder): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, p: Int) {
                    val position = viewHolder.adapterPosition
                    val downloadableItem = mDownloadItemsAdapter.get(position)
                    if (downloadableItem.isDownloading()) {
                        downloadableItem.cancel()
                    }
                    mDownloadManager.archive(downloadableItem.item)
                    mDownloadItemsAdapter.remove(downloadableItem)
                    binding.emptyListViewGroup.visibility = if (mDownloadItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
                }
            }).attachToRecyclerView(mDownloadItemsRecyclerView)
        }

        binding.emptyListViewGroup.visibility = if (mDownloadItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
    }

    override fun displayDownloadableItems(actions: List<DownloadableItemAction>) {

        runOnUiThread {
            mDownloadItemsAdapter.clear()
            mDownloadItemsAdapter.addAll(actions)
            mDownloadItemsAdapter.notifyDataSetChanged()
            mDownloadItemsRecyclerView.scrollToPosition(0)
            binding.emptyListViewGroup.visibility = if (mDownloadItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
        }
    }

    override fun displayDownloadableItem(action: DownloadableItemAction) {

        uploadHistoryMap[action.item.id] = action

        action.item.addDownloadStateChangeListener(changeListener)

        runOnUiThread {
            mDownloadItemsAdapter.add(action)
            binding.emptyListViewGroup.visibility = if (mDownloadItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun extractActionSendIntentAndUpdateUI(intent: Intent?) {
        if (intent == null) return

        val action: String = intent.action?: return

        if (action != Intent.ACTION_SEND) return
        if (!intent.hasExtra(Intent.EXTRA_TEXT)) return

        val url: String = intent.getStringExtra(Intent.EXTRA_TEXT)?: return

        binding.inputUriEditText.setText(url)
        binding.inputUriEditText.setSelection(binding.inputUriEditText.text.length)

        ViewUtils.hideSoftKeyboardAndClearFocus(binding.inputUriEditText)
        binding.inputUriEditText.clearFocus()
        doDownload(binding.inputUriEditText.text.toString())
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
                ViewUtils.showSnackBar(binding.root, getString(R.string.not_a_valid_url))
            }
        } else {
            ViewUtils.showSnackBar(binding.root, getString(R.string.nothing_to_paste))
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

        doDownload(binding.inputUriEditText.text.toString())
    }

    private var parsingDialog : ParsingDialog? = null

    private fun doDownload(url: String) {

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
            return
        }

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

                    ViewUtils.showSnackBar(binding.root, getString(R.string.unable_to_parse))

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

                    override fun onDownload(tasks : ArrayList<ParsingTaskBase>) {
                        tasks.forEach(action = { task ->
                            val filename: String? = task.filename
                            if (filename != null) {
                                FilenameLockerAdapter.instance.putUnremovable(filename)
                            }
                            startDownload(task)
                        })

                        parsingDialog?.dismissDialog()
                        parsingDialog = null
                    }

                    override fun onParseEntireSeries(paginationTask: PaginationParserTaskBase) {
                        FilenameLockerAdapter.instance.clear()
                        parsingDialog?.loading()
                        paginationFuture = mDownloadManager.parsePagination(url, paginationTask)
                        paginationFuture?.addCallback(object : FutureCallback<ArrayList<ParsingTaskBase>> {

                            override fun onSuccess(result: ArrayList<ParsingTaskBase>) {

                                runOnUiThread {
                                    parsingDialog?.showPaginationResult(paginationTask, result)
                                }
                            }

                            override fun onFailed(errorMessage: String) {

                                runOnUiThread {
                                    val message = "Unable to parse pagination: $errorMessage"

                                    ViewUtils.showSnackBar(binding.root, getString(R.string.unable_to_parse_pagination))

                                    parsingDialog?.dismissDialog()
                                    parsingDialog = null
                                }
                            }
                        })
                    }

                    override fun onParseMore(paginationTask: PaginationParserTaskBase) {
                        parsingDialog?.loadingMore()
                        paginationMoreFuture = mDownloadManager.parseMore(url, paginationTask)
                        paginationMoreFuture?.addCallback(object : FutureCallback<ArrayList<ParsingTaskBase>> {

                            override fun onSuccess(result: ArrayList<ParsingTaskBase>) {

                                runOnUiThread {
                                    parsingDialog?.showPaginationMoreResult(paginationTask, result)
                                }
                            }

                            override fun onFailed(errorMessage: String) {

                                runOnUiThread {
                                    val message = "Unable to parse more pagination: $errorMessage"

                                    ViewUtils.showSnackBar(binding.root, getString(R.string.unable_to_parse_pagination))

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

    private fun startDownload(task: ParsingTaskBase) {
        mDownloadManager.download(task)
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

    private val changeListener = object : DownloadableItemState.ChangeListener {

        override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
            // listen for end of download and show message
            if (downloadableItem.state == DownloadableItemState.End) {
                runOnUiThread {
                    val message = getString(R.string.finished_downloading) + " " + downloadableItem.filename
                    Log.e(TAG, message)
                    ViewUtils.showSnackBar(binding.root, message)
                }
                downloadableItem.removeDownloadStateChangeListener(this)

                // upload history
                val action = uploadHistoryMap[downloadableItem.id]?: return

                VersionUtils.uploadHistory(getActivityContext(), action)
            }
        }
    }

    private val uploadHistoryMap : HashMap<Int, DownloadableItemAction> = HashMap()
}