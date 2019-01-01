package org.hugoandrade.rtpplaydownloader;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends ActivityBase {

    private ViewDataBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeUI();
    }

    private void initializeUI() {

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_kt);

    }
}
