package org.hugoandrade.rtpplaydownloader.app.main

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.DocumentsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.hugoandrade.rtpplaydownloader.DevConstants
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.app.archive.ArchiveActivity
import org.hugoandrade.rtpplaydownloader.app.settings.SettingsActivity
import org.hugoandrade.rtpplaydownloader.app.ActivityBase
import org.hugoandrade.rtpplaydownloader.databinding.ActivityMainBinding
import org.hugoandrade.rtpplaydownloader.network.*
import org.hugoandrade.rtpplaydownloader.network.parsing.ParsingData
import org.hugoandrade.rtpplaydownloader.network.parsing.pagination.PaginationParserTask
import org.hugoandrade.rtpplaydownloader.network.parsing.tasks.ParsingTask
import org.hugoandrade.rtpplaydownloader.network.utils.FilenameLockerAdapter
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.*
import java.io.File

class MainActivity : ActivityBase() {

    private lateinit var searchView: SearchView
    private lateinit var binding: ActivityMainBinding

    private lateinit var mDownloadItemsRecyclerView: RecyclerView
    private lateinit var mDownloadItemsAdapter: DownloadItemsAdapter

    private lateinit var mDownloadManager: DownloadManager

    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mDrawerAdapter: NavigationDrawerAdapter? = null
    private var mPendingRunnable: Runnable? = null
    private val mHandler = Handler()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        extractActionSendIntentAndUpdateUI(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDownloadManager = ViewModelProvider(this).get(DownloadManager::class.java)
        mDownloadManager.retrieveItemsFromDB()
        mDownloadManager.getItems().observe(this, Observer { actions ->

            actions.forEach{ action -> action.addActionListener(actionListener)}
            mDownloadItemsAdapter.set(actions)
            mDownloadItemsAdapter.notifyDataSetChanged()
            mDownloadItemsRecyclerView.scrollToPosition(0)
            binding.emptyListViewGroup.visibility = if (mDownloadItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
        })

        initializeUI()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Pass any configuration change to the drawer toggle
        mDrawerToggle?.onConfigurationChanged(newConfig)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // set up SearchView
        searchView = menu.findItem(R.id.app_search_bar).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(p0: String?): Boolean {
                doDownload(searchView.query.toString())
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }
        })
        searchView.setOnCloseListener {
            mDrawerToggle?.onDrawerSlide(binding.drawerLayout, 0f)
            false
        }
        searchView.setOnSearchClickListener {
            mDrawerToggle?.onDrawerSlide(binding.drawerLayout, 1f)
        }
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                iconifySearchView()
            }
        }

        //
        val editText: EditText? = searchView.findViewById(R.id.search_src_text)
        editText?.setTextColor(Color.WHITE)
        editText?.setHintTextColor(Color.parseColor("#90ffffff"))

        //
        val devUrl: String? = DevConstants.url
        if (devUrl != null) {
            searchView.setQuery(devUrl, false)
            editText?.setSelection(editText.text.length)
            searchView.isIconified = false
        } else {
            ViewUtils.hideSoftKeyboardAndClearFocus(searchView)
        }

        extractActionSendIntentAndUpdateUI(intent)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val drawerToggle = mDrawerToggle

        if (item.itemId == android.R.id.home) {
            if (!iconifySearchView()) {
                return true
            }
        }

        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) return true

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        // close if drawer is open
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        // iconify search view if showing
        else if (!iconifySearchView()) {
        }
        // back press
        else {
            super.onBackPressed()
        }
    }

    private fun initializeUI() {

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = getString(R.string.app_name)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(false)
        }

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        val drawerToggle = object : ActionBarDrawerToggle(this,
                binding.drawerLayout,
                R.string.drawer_open,
                R.string.drawer_close) {
            /**
             * Called when a drawer has settled in a completely closed state.
             */
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                if (mPendingRunnable != null) {
                    mHandler.post(mPendingRunnable)
                    mPendingRunnable = null
                }
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
            }
        }
        binding.drawerLayout.addDrawerListener(drawerToggle)

        val drawerAdapter = NavigationDrawerAdapter(this)
        drawerAdapter.addOptionItem(NavigationDrawerAdapter.OptionItem(R.drawable.ic_archive, getString(R.string.archive), ArchiveActivity.makeIntent(this)))
        drawerAdapter.addHeader(getString(R.string.quick_assess))
        drawerAdapter.addItem(NavigationDrawerAdapter.QuickAccessItem(R.mipmap.ic_rtpplay, "RTP Play", "https://www.rtp.pt/play/"))
        drawerAdapter.addItem(NavigationDrawerAdapter.QuickAccessItem(R.mipmap.ic_tvi_player, "TVI Player", "https://tviplayer.iol.pt/"))
        drawerAdapter.addItem(NavigationDrawerAdapter.QuickAccessItem(R.mipmap.ic_sicradical, "SIC Radical", "https://sicradical.pt/"))
        drawerAdapter.addItem(NavigationDrawerAdapter.QuickAccessItem(R.mipmap.ic_sicnoticias, "SIC Notícias", "https://sicnoticias.pt/"))
        drawerAdapter.addItem(NavigationDrawerAdapter.QuickAccessItem(R.mipmap.ic_sic, "SIC", "https://sic.pt/"))
        drawerAdapter.addHeader("")
        drawerAdapter.addOptionItem(NavigationDrawerAdapter.OptionItem(R.drawable.ic_settings, getString(R.string.settings), SettingsActivity.makeIntent(this)))
        drawerAdapter.setOnItemClickListener(object : NavigationDrawerAdapter.OnDrawerClickListener {

            override fun onItemClicked(drawerItem: NavigationDrawerAdapter.Item?) {
                if (drawerItem != null) {
                    if (drawerItem is NavigationDrawerAdapter.QuickAccessItem) {
                        mPendingRunnable = Runnable {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(drawerItem.url))
                                startActivity(browserIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    else if (drawerItem is NavigationDrawerAdapter.OptionItem) {
                        try {
                            mPendingRunnable = Runnable {
                                startActivity(drawerItem.intent)
                            }
                        } catch (e: Exception) {

                        }
                    }
                }
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        })

        binding.drawerLayout.navigationDrawerContent.adapter = drawerAdapter
        binding.drawerLayout.navigationDrawerContent.layoutManager = LinearLayoutManager(this)

        this.mDrawerToggle = drawerToggle
        this.mDrawerAdapter = drawerAdapter

        val simpleItemAnimator = DefaultItemAnimator()
        simpleItemAnimator.supportsChangeAnimations = false

        mDownloadItemsRecyclerView = binding.downloadItemsRecyclerView
        mDownloadItemsRecyclerView.itemAnimator = simpleItemAnimator
        mDownloadItemsRecyclerView.layoutManager =
                if (!ViewUtils.isTablet(this) && ViewUtils.isPortrait(this)) androidx.recyclerview.widget.LinearLayoutManager(this)
                else androidx.recyclerview.widget.GridLayoutManager(this, if (!ViewUtils.isTablet(this) && !ViewUtils.isPortrait(this)) 2 else 3)
        mDownloadItemsAdapter = DownloadItemsAdapter()
        mDownloadItemsRecyclerView.adapter = mDownloadItemsAdapter
        if (DevConstants.enableSwipe) {
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT.or(ItemTouchHelper.RIGHT)) {

                override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
                    return super.getSwipeEscapeVelocity(defaultValue) * 5
                }

                override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
                    return super.getSwipeVelocityThreshold(defaultValue) * 0.2f
                }

                override fun onMove(recyclerView: RecyclerView, viewHolder1: RecyclerView.ViewHolder, viewHolder2: RecyclerView.ViewHolder): Boolean {
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

    /**
     * tries to iconify search view, and syncs with toggle. returns the previous iconified state
     */
    private fun iconifySearchView(): Boolean {
        val wasIconified = searchView.isIconified
        if (!wasIconified) {
            searchView.isIconified = true
            mDrawerToggle?.onDrawerSlide(binding.drawerLayout, 0f)
        }
        return wasIconified
    }

    private fun displayDownloadableItem(action: DownloadableItemAction) {
        action.addActionListener(actionListener)

        uploadHistoryMap[action.item.id] = action

        action.item.addDownloadStateChangeListener(changeListener)

        runOnUiThread {
            mDownloadItemsAdapter.add(action)
            binding.downloadItemsRecyclerView.scrollToPosition(0)
            binding.emptyListViewGroup.visibility = if (mDownloadItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
        }
    }

    private val actionListener: DownloadableItemAction.Listener = object : DownloadableItemAction.Listener {

        override fun onPlay(action: DownloadableItemAction) {

            val dialog = detailsDialog

            if (dialog != null) {
                dialog.show(action.item)
            }
            else {

                detailsDialog = DownloadableItemDetailsDialog.Builder.instance(this@MainActivity)
                        .setOnItemDetailsDialogListener(object : DownloadableItemDetailsDialog.OnItemDetailsListener {
                            override fun onCancelled() {
                                detailsDialog = null
                            }

                            override fun onArchive(item: DownloadableItem) {

                                detailsDialog?.dismissDialog()

                                mDownloadManager.archive(item)
                                mDownloadItemsAdapter.remove(item)
                                binding.emptyListViewGroup.visibility = if (mDownloadItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
                            }

                            override fun onRedirect(item: DownloadableItem) {

                                detailsDialog?.dismissDialog()

                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                    startActivity(browserIntent)
                                } catch (e: Exception) { }
                            }

                            override fun onShowInFolder(item: DownloadableItem) {

                                detailsDialog?.dismissDialog()

                                try {
                                    val dir = Uri.parse(File(item.filepath).parentFile.absolutePath + File.separator)

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                        intent.addCategory(Intent.CATEGORY_DEFAULT)

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, dir)
                                        }

                                        intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
                                        startActivity(Intent.createChooser(intent, getString(R.string.open_folder)))
                                    } else {
                                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                                        intent.setDataAndType(dir, "*/*")
                                        startActivity(Intent.createChooser(intent, getString(R.string.open_folder)))
                                    }
                                } catch (e: Exception) { }
                            }

                            override fun onPlay(item: DownloadableItem) {

                                detailsDialog?.dismissDialog()

                                try {
                                    val filepath = action.item.filepath
                                    if (MediaUtils.doesMediaFileExist(action.item)) {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
                                                .setDataAndType(Uri.parse(filepath), "video/mp4"))
                                    } else {
                                        ViewUtils.showToast(this@MainActivity, getString(R.string.file_not_found))
                                    }
                                } catch (ignored: Exception) { }
                            }

                        })
                        .create(action.item)
                detailsDialog?.show()
            }
        }

        override fun onRefresh(action: DownloadableItemAction) {
            // no-ops
        }
    }

    private fun extractActionSendIntentAndUpdateUI(intent: Intent?) {
        if (intent == null) return

        val action: String = intent.action ?: return

        if (action != Intent.ACTION_SEND || !intent.hasExtra(Intent.EXTRA_TEXT)) return

        val url: String = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return

        intent.removeExtra(Intent.EXTRA_TEXT)

        //
        val editText: EditText? = searchView.findViewById(R.id.search_src_text)

        searchView.setQuery(url, true)
        editText?.setSelection(editText.text.length)
    }

    private var parsingDialog : ParsingDialog? = null
    private var detailsDialog : DownloadableItemDetailsDialog? = null

    @Synchronized
    private fun doDownload(url: String) {
        Log.e(TAG, "doDownload " + Thread.currentThread().getStackTrace()[2].methodName)
        Log.e(TAG, "doDownload " + Thread.currentThread().getStackTrace()[3].methodName)
        Log.e(TAG, "doDownload " + Thread.currentThread().getStackTrace()[4].methodName)

        if (!PermissionUtils.hasGrantedPermissionAndRequestIfNeeded(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) return

        val isParsing : Boolean = parsingDialog?.isShowing() ?: false

        if (isParsing) {
            return
        }

        // dismiss previous instance
        parsingDialog?.dismissDialog()

        val future : ListenableFuture<ParsingData> = mDownloadManager.parseUrl(url)
        future.addCallback(object : ListenableFuture.Callback<ParsingData> {

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

        val parsingDialogListener = object : ParsingDialog.OnParsingListener {

            var paginationFuture : ListenableFuture<ArrayList<ParsingTask>>? = null
            var paginationMoreFuture : ListenableFuture<ArrayList<ParsingTask>>? = null

            override fun onCancelled() {
                future.failed("parsing was cancelled")
                paginationFuture?.failed("parsing was cancelled")
                paginationMoreFuture?.failed("parsing was cancelled")
                FilenameLockerAdapter.instance.clear()
            }

            override fun onDownload(tasks : ArrayList<ParsingTask>) {
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

            override fun onParseEntireSeries(paginationTask: PaginationParserTask) {
                FilenameLockerAdapter.instance.clear()
                parsingDialog?.loading()
                paginationFuture = mDownloadManager.parsePagination(url, paginationTask)
                paginationFuture?.addCallback(object : ListenableFuture.Callback<ArrayList<ParsingTask>> {

                    override fun onSuccess(result: ArrayList<ParsingTask>) {

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

            override fun onParseMore(paginationTask: PaginationParserTask) {
                parsingDialog?.loadingMore()
                paginationMoreFuture = mDownloadManager.parseMore(url, paginationTask)
                paginationMoreFuture?.addCallback(object : ListenableFuture.Callback<ArrayList<ParsingTask>> {

                    override fun onSuccess(result: ArrayList<ParsingTask>) {

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
        }

        parsingDialog = ParsingDialog.Builder.instance(this)
                .setOnParsingDialogListener(parsingDialogListener)
                .create()

        parsingDialog?.show()
    }

    private fun startDownload(task: ParsingTask) {
        val future = mDownloadManager.download(task)
        future.addCallback(object : ListenableFuture.Callback<DownloadableItemAction> {
            override fun onFailed(errorMessage: String) {
                Log.e(TAG, errorMessage)
            }

            override fun onSuccess(result: DownloadableItemAction) {
                displayDownloadableItem(result)
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (permissions.contains(permission) && PermissionUtils.hasGrantedPermission(this, permission)) {

            doDownload(searchView.query.toString())
        }
    }

    private val changeListener = object : DownloadableItem.State.ChangeListener {

        override fun onDownloadStateChange(downloadableItem: DownloadableItem) {
            // listen for end of download and show message
            if (downloadableItem.state == DownloadableItem.State.End) {
                runOnUiThread {
                    val message = getString(R.string.finished_downloading) + " " + downloadableItem.filename
                    Log.e(TAG, message)
                    ViewUtils.showSnackBar(binding.root, message)
                }
                downloadableItem.removeDownloadStateChangeListener(this)

                // upload history
                val action = uploadHistoryMap[downloadableItem.id]?: return

                VersionUtils.uploadHistory(this@MainActivity, action)
            }
        }
    }

    private val uploadHistoryMap : HashMap<Int, DownloadableItemAction> = HashMap()
}