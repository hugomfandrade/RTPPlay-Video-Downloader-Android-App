package org.hugoandrade.rtpplaydownloader.app.archive

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.support.v7.widget.*
import android.view.MenuItem
import android.view.View
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.common.ActivityBase
import org.hugoandrade.rtpplaydownloader.databinding.ActivityArchiveBinding
import org.hugoandrade.rtpplaydownloader.network.*
import org.hugoandrade.rtpplaydownloader.network.persistence.DatabaseModel
import org.hugoandrade.rtpplaydownloader.network.persistence.PersistencePresenterOps
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils
import java.io.File

class ArchiveActivity : ActivityBase() {

    companion object {
        private val TAG = ArchiveActivity::class.java.simpleName

        fun makeIntent(context: Context) : Intent {
            return Intent(context, ArchiveActivity::class.java)
        }
    }

    private lateinit var binding: ActivityArchiveBinding

    private lateinit var mDatabaseModel: DatabaseModel
    private lateinit var mDownloadItemsRecyclerView: RecyclerView
    private lateinit var mArchiveItemsAdapter: ArchiveItemsAdapter

    private val downloadableItems: ArrayList<DownloadableItem> = ArrayList()

    private var detailsDialog : DownloadableItemDetailsDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDatabaseModel = object : DatabaseModel(){}
        mDatabaseModel.onCreate(mPersistencePresenterOps)

        initializeUI()

        mDatabaseModel.retrieveArchivedDownloadableItems()
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

        val simpleItemAnimator : SimpleItemAnimator = DefaultItemAnimator()
        simpleItemAnimator.supportsChangeAnimations = false

        mDownloadItemsRecyclerView = binding.archiveItemsRecyclerView
        mDownloadItemsRecyclerView.itemAnimator = simpleItemAnimator
        mDownloadItemsRecyclerView.layoutManager =
                if (!ViewUtils.isTablet(this) && ViewUtils.isPortrait(this)) LinearLayoutManager(this)
                else GridLayoutManager(this, if (!ViewUtils.isTablet(this) && !ViewUtils.isPortrait(this)) 2 else 3)
        mArchiveItemsAdapter = ArchiveItemsAdapter()
        mArchiveItemsAdapter.setListener(object : ArchiveItemsAdapter.Listener {

            override fun onItemClicked(item: DownloadableItem) {
                val dialog = detailsDialog

                if (dialog != null) {
                    dialog.show(item)
                } else {

                    detailsDialog = DownloadableItemDetailsDialog.Builder.instance(getActivityContext())
                            .setOnItemDetailsDialogListener(object : DownloadableItemDetailsDialog.OnItemDetailsListener {
                                override fun onCancelled() {
                                    detailsDialog = null
                                }

                                override fun onArchive(item: DownloadableItem) {

                                    detailsDialog?.dismissDialog()

                                    item.isArchived = false
                                    mDatabaseModel.updateDownloadableEntry(item)

                                    mArchiveItemsAdapter.remove(item)
                                    binding.emptyListViewGroup.visibility = if (mArchiveItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
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
                                            ViewUtils.showToast(getActivityContext(), getString(R.string.file_not_found))
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
        mDownloadItemsRecyclerView.adapter = mArchiveItemsAdapter

        binding.emptyListViewGroup.visibility = if (mArchiveItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
    }

    private fun displayDownloadableItems(items: List<DownloadableItem>) {

        runOnUiThread {
            mArchiveItemsAdapter.clear()
            mArchiveItemsAdapter.addAll(items)
            mArchiveItemsAdapter.notifyDataSetChanged()
            mDownloadItemsRecyclerView.scrollToPosition(0)
            binding.emptyListViewGroup.visibility = if (mArchiveItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun displayDownloadableItem(item: DownloadableItem) {

        runOnUiThread {
            mArchiveItemsAdapter.add(item)
            binding.emptyListViewGroup.visibility = if (mArchiveItemsAdapter.itemCount == 0) View.VISIBLE else View.INVISIBLE
        }
    }

    private val mPersistencePresenterOps = object : PersistencePresenterOps {

        override fun onArchivedDownloadableItemsRetrieved(downloadableItems: List<DownloadableItem>) {

            for (item in downloadableItems) {

                synchronized(this@ArchiveActivity.downloadableItems) {
                    val listItems = this@ArchiveActivity.downloadableItems


                    var contains = false
                    // add if not already in list
                    for (i in listItems.size - 1 downTo 0) {
                        if (listItems[i].id == item.id) {
                            contains = true
                            break
                        }
                    }

                    if (!contains) {
                        listItems.add(item)
                        listItems.sortedWith(compareBy { it.id })
                    }
                }
            }

            displayDownloadableItems(this@ArchiveActivity.downloadableItems)
        }

        override fun getActivityContext(): Context? {
            return this@ArchiveActivity
        }

        override fun getApplicationContext(): Context? {
            return this@ArchiveActivity.applicationContext
        }
    }
}