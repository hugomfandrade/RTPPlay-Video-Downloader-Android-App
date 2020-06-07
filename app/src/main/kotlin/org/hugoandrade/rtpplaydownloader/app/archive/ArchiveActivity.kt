package org.hugoandrade.rtpplaydownloader.app.archive

import android.content.Context
import android.content.Intent
import androidx.databinding.DataBindingUtil
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.app.ActivityBase
import org.hugoandrade.rtpplaydownloader.app.main.DownloadableItemDetailsDialog
import org.hugoandrade.rtpplaydownloader.databinding.ActivityArchiveBinding
import org.hugoandrade.rtpplaydownloader.network.*
import org.hugoandrade.rtpplaydownloader.network.persistence.DownloadableItemRepository
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.ListenableFuture
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ArchiveActivity : ActivityBase() {

    companion object {

        fun makeIntent(context: Context) : Intent {
            return Intent(context, ArchiveActivity::class.java)
        }
    }

    private lateinit var binding: ActivityArchiveBinding

    private lateinit var mDatabaseModel: DownloadableItemRepository
    private lateinit var mArchivedItemsRecyclerView: RecyclerView
    private lateinit var mArchivedItemsAdapter: ArchiveItemsAdapter

    private val downloadableItems: ConcurrentHashMap<Int, DownloadableItem> = ConcurrentHashMap()

    private var detailsDialog : DownloadableItemDetailsDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDatabaseModel = DownloadableItemRepository(application)

        initializeUI()

        val future = mDatabaseModel.retrieveArchivedDownloadableItems()
        future.addCallback(object : ListenableFuture.Callback<List<DownloadableItem>> {
            override fun onFailed(errorMessage: String) {
                Log.e(TAG, errorMessage)
                Toast.makeText(this@ArchiveActivity, "Failed to get Archived Items", Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess(result: List<DownloadableItem>) {

                synchronized(this@ArchiveActivity.downloadableItems) {

                    for (item in result) {
                        downloadableItems.putIfAbsent(item.id ?: -1, item)
                    }
                }

                val listItems = downloadableItems.values.toList()
                listItems.sortedWith(compareBy { it.id })

                displayDownloadableItems(listItems)
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initializeUI() {

        binding = DataBindingUtil.setContentView(this, R.layout.activity_archive)

        setSupportActionBar(findViewById(R.id.toolbar))

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = getString(R.string.archive)
            actionBar.setDisplayHomeAsUpEnabled(true)
            // actionBar.setHomeButtonEnabled(false)
        }

        val simpleItemAnimator : androidx.recyclerview.widget.SimpleItemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        simpleItemAnimator.supportsChangeAnimations = false

        mArchivedItemsRecyclerView = binding.archiveItemsRecyclerView
        mArchivedItemsRecyclerView.itemAnimator = simpleItemAnimator
        mArchivedItemsRecyclerView.layoutManager =
                if (!ViewUtils.isTablet(this) && ViewUtils.isPortrait(this)) androidx.recyclerview.widget.LinearLayoutManager(this)
                else androidx.recyclerview.widget.GridLayoutManager(this, if (!ViewUtils.isTablet(this) && !ViewUtils.isPortrait(this)) 2 else 3)
        mArchivedItemsAdapter = ArchiveItemsAdapter()
        mArchivedItemsAdapter.setListener(object : ArchiveItemsAdapter.Listener {

            override fun onItemClicked(item: DownloadableItem) {
                val dialog = detailsDialog

                if (dialog != null) {
                    dialog.show(item)
                } else {

                    detailsDialog = DownloadableItemDetailsDialog.Builder.instance(this@ArchiveActivity)
                            .setOnItemDetailsDialogListener(object : DownloadableItemDetailsDialog.OnItemDetailsListener {
                                override fun onCancelled() {
                                    detailsDialog = null
                                }

                                override fun onArchive(item: DownloadableItem) {

                                    detailsDialog?.dismissDialog()

                                    item.isArchived = false
                                    mDatabaseModel.updateDownloadableEntry(item)

                                    mArchivedItemsAdapter.remove(item)
                                    binding.emptyListViewGroup.visibility = if (mArchivedItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
                                }

                                override fun onRedirect(item: DownloadableItem) {

                                    detailsDialog?.dismissDialog()

                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                                        startActivity(browserIntent)
                                    } catch (e: Exception) {
                                    }
                                }

                                override fun onShowInFolder(item: DownloadableItem) {

                                    detailsDialog?.dismissDialog()

                                    try {
                                        val dir = Uri.parse(File(item.filepath).parentFile.absolutePath + File.separator)

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
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
                                        val filepath = item.filepath
                                        if (MediaUtils.doesMediaFileExist(item)) {
                                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
                                                    .setDataAndType(Uri.parse(filepath), "video/mp4"))
                                        } else {
                                            ViewUtils.showToast(this@ArchiveActivity, getString(R.string.file_not_found))
                                        }
                                    } catch (ignored: Exception) {
                                    }
                                }

                            })
                            .create(item)
                    detailsDialog?.show()
                }
            }
        })
        mArchivedItemsRecyclerView.adapter = mArchivedItemsAdapter

        binding.emptyListViewGroup.visibility = if (mArchivedItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
    }

    private fun displayDownloadableItems(items: List<DownloadableItem>) {

        runOnUiThread {
            mArchivedItemsAdapter.setItems(items)
            mArchivedItemsAdapter.notifyDataSetChanged()
            mArchivedItemsRecyclerView.scrollToPosition(0)
            binding.emptyListViewGroup.visibility = if (mArchivedItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
        }
    }
}