package org.hugoandrade.rtpplaydownloader.network.archive

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.*
import android.view.MenuItem
import android.view.View
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.common.ActivityBase
import org.hugoandrade.rtpplaydownloader.databinding.ActivityArchiveBinding
import org.hugoandrade.rtpplaydownloader.network.*
import org.hugoandrade.rtpplaydownloader.network.persistence.DatabaseModel
import org.hugoandrade.rtpplaydownloader.network.persistence.PersistencePresenterOps
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils

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