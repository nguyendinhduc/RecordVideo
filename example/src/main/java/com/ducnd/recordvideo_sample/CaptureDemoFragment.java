/**
 * Copyright 2014 Jeroen Mols
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ducnd.recordvideo_sample;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Video.Thumbnails;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.ducnd.VideoCaptureActivity;
import com.ducnd.recordvideo.configuration.CaptureConfiguration;
import com.ducnd.recordvideo.configuration.PredefinedCaptureConfigurations.CaptureQuality;
import com.ducnd.recordvideo.configuration.PredefinedCaptureConfigurations.CaptureResolution;

import java.util.List;

public class CaptureDemoFragment extends Fragment implements OnClickListener {

    private final String KEY_STATUSMESSAGE    = "com.ducnd.statusmessage";
    private final String KEY_ADVANCEDSETTINGS = "com.ducnd.advancedsettings";
    private final String KEY_FILENAME         = "com.ducnd.outputfilename";

    private final String[] RESOLUTION_NAMES = new String[]{"1080p", "720p", "480p"};
    private final String[] QUALITY_NAMES    = new String[]{"high", "medium", "low"};

    private String statusMessage = null;
    private String filename      = null;

    private ImageView thumbnailIv;
    private TextView  statusTv;
    private Spinner   resolutionSp;
    private Spinner   qualitySp;

    private RelativeLayout advancedRl;
    private EditText       filenameEt;
    private EditText       maxDurationEt;
    private EditText       maxFilesizeEt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final Button captureBtn = (Button) rootView.findViewById(R.id.btn_capturevideo);
        captureBtn.setOnClickListener(this);

        thumbnailIv = (ImageView) rootView.findViewById(R.id.iv_thumbnail);
        thumbnailIv.setOnClickListener(this);
        statusTv = (TextView) rootView.findViewById(R.id.tv_status);
        advancedRl = (RelativeLayout) rootView.findViewById(R.id.rl_advanced);
        filenameEt = (EditText) rootView.findViewById(R.id.et_filename);
        maxDurationEt = (EditText) rootView.findViewById(R.id.et_duration);
        maxFilesizeEt = (EditText) rootView.findViewById(R.id.et_filesize);

        if (savedInstanceState != null) {
            statusMessage = savedInstanceState.getString(KEY_STATUSMESSAGE);
            filename = savedInstanceState.getString(KEY_FILENAME);
            advancedRl.setVisibility(savedInstanceState.getInt(KEY_ADVANCEDSETTINGS));
        }

        updateStatusAndThumbnail();
        initializeSpinners(rootView);
        return rootView;
    }

    private void initializeSpinners(final View rootView) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, RESOLUTION_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resolutionSp = (Spinner) rootView.findViewById(R.id.sp_resolution);
        resolutionSp.setAdapter(adapter);

        final ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, QUALITY_NAMES);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        qualitySp = (Spinner) rootView.findViewById(R.id.sp_quality);
        qualitySp.setAdapter(adapter2);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_STATUSMESSAGE, statusMessage);
        outState.putString(KEY_FILENAME, filename);
        outState.putInt(KEY_ADVANCEDSETTINGS, advancedRl.getVisibility());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_capturevideo) {
            startVideoCaptureActivity();
        } else if (v.getId() == R.id.iv_thumbnail) {
            playVideo();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.capture_demo, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_advanced:
                toggleAdvancedSettings();
                break;
            case R.id.menu_github:
                openGitHub();
                break;
        }
        return true;
    }

    private void toggleAdvancedSettings() {
        advancedRl.setVisibility(advancedRl.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }

    private void openGitHub() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url)));
        if (canHandleIntent(browserIntent)) {
            startActivity(browserIntent);
        }
    }

    private boolean canHandleIntent(Intent intent) {
        final PackageManager mgr = getActivity().getPackageManager();
        List<ResolveInfo> list = mgr.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    private void startVideoCaptureActivity() {
        final CaptureConfiguration config = createCaptureConfiguration();
        final String filename = filenameEt.getEditableText().toString();

        final Intent intent = new Intent(getActivity(), VideoCaptureActivity.class);
        intent.putExtra(VideoCaptureActivity.EXTRA_CAPTURE_CONFIGURATION, config);
        intent.putExtra(VideoCaptureActivity.EXTRA_OUTPUT_FILENAME, filename);
        startActivityForResult(intent, 101);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            filename = data.getStringExtra(VideoCaptureActivity.EXTRA_OUTPUT_FILENAME);
            statusMessage = String.format(getString(R.string.status_capturesuccess), filename);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            filename = null;
            statusMessage = getString(R.string.status_capturecancelled);
        } else if (resultCode == VideoCaptureActivity.RESULT_ERROR) {
            filename = null;
            statusMessage = getString(R.string.status_capturefailed);
        }
        updateStatusAndThumbnail();

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateStatusAndThumbnail() {
        if (statusMessage == null) {
            statusMessage = getString(R.string.status_nocapture);
        }
        statusTv.setText(statusMessage);

        final Bitmap thumbnail = getThumbnail();

        if (thumbnail != null) {
            thumbnailIv.setImageBitmap(thumbnail);
        } else {
            thumbnailIv.setImageResource(R.drawable.thumbnail_placeholder);
        }
    }

    private Bitmap getThumbnail() {
        if (filename == null) return null;
        return ThumbnailUtils.createVideoThumbnail(filename, Thumbnails.FULL_SCREEN_KIND);
    }

    private CaptureConfiguration createCaptureConfiguration() {
        final CaptureResolution resolution = getResolution(resolutionSp.getSelectedItemPosition());
        final CaptureQuality quality = getQuality(qualitySp.getSelectedItemPosition());
        int fileDuration = CaptureConfiguration.NO_DURATION_LIMIT;
        try {
            fileDuration = Integer.valueOf(maxDurationEt.getEditableText().toString());
        } catch (final Exception e) {
            //NOP
        }
        int filesize = CaptureConfiguration.NO_FILESIZE_LIMIT;
        try {
            filesize = Integer.valueOf(maxFilesizeEt.getEditableText().toString());
        } catch (final Exception e2) {
            //NOP
        }
        final CaptureConfiguration config = new CaptureConfiguration(resolution, quality, fileDuration, filesize);
        return config;
    }

    private CaptureQuality getQuality(int position) {
        final CaptureQuality[] quality = new CaptureQuality[]{CaptureQuality.HIGH, CaptureQuality.MEDIUM,
                                                              CaptureQuality.LOW};
        return quality[position];
    }

    private CaptureResolution getResolution(int position) {
        final CaptureResolution[] resolution = new CaptureResolution[]{CaptureResolution.RES_1080P,
                                                                       CaptureResolution.RES_720P, CaptureResolution.RES_480P};
        return resolution[position];
    }

    public void playVideo() {
        if (filename == null) return;

        final Intent videoIntent = new Intent(Intent.ACTION_VIEW);
        videoIntent.setDataAndType(Uri.parse(filename), "video/*");
        try {
            startActivity(videoIntent);
        } catch (ActivityNotFoundException e) {
            // NOP
        }
    }

}