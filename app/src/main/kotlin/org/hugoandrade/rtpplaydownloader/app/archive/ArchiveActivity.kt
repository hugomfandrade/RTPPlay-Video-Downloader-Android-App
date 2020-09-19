package org.hugoandrade.rtpplaydownloader.app.archive

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.app.ActivityBase
import org.hugoandrade.rtpplaydownloader.app.main.DownloadableItemDetailsDialog
import org.hugoandrade.rtpplaydownloader.databinding.ActivityArchiveBinding
import org.hugoandrade.rtpplaydownloader.network.DownloadableItem
import org.hugoandrade.rtpplaydownloader.network.persistence.DownloadableItemRepository
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.ListenableFuture
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils
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
                        downloadableItems.putIfAbsent(item.id, item)
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
                if (!ViewUtils.isTablet(this) && ViewUtils.isPortrait(this)) LinearLayoutManager(this)
                else GridLayoutManager(this, if (ViewUtils.isTablet(this) && !ViewUtils.isPortrait(this)) 3 else 2)
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

                                    detailsDialog?.dismiss()

                                    item.isArchived = false
                                    mDatabaseModel.updateDownloadableEntry(item)

                                    mArchivedItemsAdapter.remove(item)
                                    binding.emptyListViewGroup.visibility = if (mArchivedItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
                                }

                                override fun onRedirect(item: DownloadableItem) {

                                    detailsDialog?.dismiss()

                                    MediaUtils.openUrl(this@ArchiveActivity, item)
                                }

                                override fun onShowInFolder(item: DownloadableItem) {

                                    detailsDialog?.dismiss()

                                    MediaUtils.showInFolderIntent(this@ArchiveActivity, item)
                                }

                                override fun onPlay(item: DownloadableItem) {

                                    detailsDialog?.dismiss()

                                    MediaUtils.play(this@ArchiveActivity, item)
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