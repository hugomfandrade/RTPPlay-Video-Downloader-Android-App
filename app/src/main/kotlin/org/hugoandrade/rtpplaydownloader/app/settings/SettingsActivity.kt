package org.hugoandrade.rtpplaydownloader.app.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.MenuItem
import org.hugoandrade.rtpplaydownloader.R
import org.hugoandrade.rtpplaydownloader.network.utils.MediaUtils
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils

class SettingsActivity : AppCompatActivity() {

    companion object {

        private val TAG = SettingsActivity::class.java.simpleName

        private const val REQUEST_EXTERNAL_ACCESS = 100

        fun makeIntent(context: Context) : Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        setSupportActionBar(findViewById(R.id.toolbar))

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = getString(R.string.settings)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.content_frame, SettingsFragment())
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private var filePicker: Preference? = null

        override fun onCreatePreferences(bundle : Bundle?, rootKey : String?) {

            setPreferencesFromResource(R.xml.preferences_main, rootKey)

            val context = activity as Context

            val filePicker = findPreference(getString(R.string.key_directory_name))
            filePicker.setDefaultValue(MediaUtils.getDownloadsDirectory(context))
            filePicker.summary = MediaUtils.getDownloadsDirectory(context).toString().replace("/storage/emulated/0", "")
            filePicker.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val activity = activity as Activity

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    ViewUtils.showSnackBar(activity.findViewById(android.R.id.content), "Cannot be done with this Android Version")
                    return@OnPreferenceClickListener true
                }
                else {

                    val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    i.addCategory(Intent.CATEGORY_DEFAULT)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, MediaUtils.getDownloadsDirectory(context))
                    }

                    i.putExtra("android.content.extra.SHOW_ADVANCED", true)
                    startActivityForResult(Intent.createChooser(i, getString(R.string.directory_storage_summary)), REQUEST_EXTERNAL_ACCESS)
                    true
                }
            }

            this.filePicker = filePicker
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

            if (requestCode == REQUEST_EXTERNAL_ACCESS && resultCode == RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val uri = data?.data
                val activity = activity as Activity

                val docUri: Uri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
                val path: String? = SettingsUtils.getPath(activity, docUri)

                if (path != null) {
                    MediaUtils.putDownloadsDirectory(activity, path)

                    filePicker?.summary = MediaUtils.getDownloadsDirectory(activity).toString().replace("/storage/emulated/0", "")
                }
            }
        }
    }
}