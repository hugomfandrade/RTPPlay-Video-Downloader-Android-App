package org.hugoandrade.rtpplaydownloader

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import org.hugoandrade.rtpplaydownloader.databinding.ActivityMainKtBinding
import org.hugoandrade.rtpplaydownloader.network.DownloadManager
import org.hugoandrade.rtpplaydownloader.utils.ViewUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL


class MainActivityKt : ActivityBase(), DownloadManager.DownloadManagerViewOps {

    private lateinit var binding: ActivityMainKtBinding

    private var mDownloadManager: DownloadManager = DownloadManager();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDownloadManager.onCreate(this);

        initializeUI()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!isChangingConfigurations()) {
            mDownloadManager.onDestroy();
        }
    }

    private fun initializeUI() {

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_kt)
        binding.inputUriEditText.setSelection(binding.inputUriEditText.text.length);
    }

    public fun download(view: View) {

        ViewUtils.hideSoftKeyboardAndClearFocus(binding.root)

        mDownloadManager.start(binding.inputUriEditText.text.toString())
        // Log.e(TAG, binding?.inputUriEditText?.text.toString());
        // binding?.root?.let { Snackbar.make(it, binding?.inputUriEditText?.text.toString(), Snackbar.LENGTH_LONG).show() }
    }

    override fun onParsingEnded(url: String, isOk: Boolean, message : String) {
        var i = ""
        if (isOk) {
            i = "Able to"
        }
        else {
            i = "Unable to"
        }

        Log.e(TAG, i + " parse (" + message + ") " + binding.inputUriEditText.text.toString());
        binding.root.let { Snackbar.make(it, i + " parse (" + message + ") " + binding.inputUriEditText.text.toString(), Snackbar.LENGTH_LONG).show() }
    }
}