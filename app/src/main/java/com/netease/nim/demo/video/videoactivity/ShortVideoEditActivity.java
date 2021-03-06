package com.netease.nim.demo.video.videoactivity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.netease.nim.demo.R;
import com.netease.nim.demo.activity.VideoShootActivity;
import com.netease.nim.demo.controller.MediaCaptureOptions;
import com.netease.nim.demo.controller.VideoProcessController;
import com.netease.nim.demo.modle.VideoItem;
import com.netease.nim.demo.video.dialog.EasyEditDialog;
import com.netease.nim.demo.video.others.VideoProcessOptions;
import com.netease.nim.demo.video.utils.FileUtil;
import com.netease.nim.demo.video.view.AutoResizeEditText;
import com.netease.nim.demo.video.view.CircleProgressView;
import com.netease.nim.demo.video.view.MoveImageView;
import com.netease.nim.demo.video.view.TwoWaysVerticalSeekBar;
import com.netease.nim.uikit.common.ui.dialog.DialogMaker;
import com.netease.nim.uikit.common.util.log.LogUtil;
import com.netease.nim.uikit.common.util.storage.StorageType;
import com.netease.nim.uikit.common.util.storage.StorageUtil;
import com.netease.nim.uikit.common.util.sys.ScreenUtil;
import com.netease.nim.uikit.common.util.sys.TimeUtil;
import com.netease.transcoding.MediaMetadata;
import com.netease.transcoding.TranscodingAPI;
import com.netease.transcoding.player.MediaPlayerAPI;
import com.netease.vcloud.video.effect.VideoEffect;
import com.netease.vcloud.video.render.NeteaseView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * 视频编辑界面
 * Created by winnie on 2017/6/20.
 */

public class ShortVideoEditActivity extends BaseActivity implements View.OnClickListener, VideoProcessController.VideoProcessCallback {
    /**
     * constant
     */
    public static final String EXTRA_PATH_LIST = "path_list";
    public static final String EXTRA_TOTAL_TIME = "total_time";
    public static final String EXTRA_MEDIA_OPTIONS = "media_options";
    private static final String ADJUST_TAB = "adjust_tab";
    private static final String SUBSECTION_TAB = "subsection_tab";
    private static final String WORDS_TAB = "words_tab";
    private static final String TEXTURES_TAB = "textures_tab";
    private static final String ACCOMPANY_SOUND_TAB = "accompany_sound_tab";
    public static final int EXTRA_REQUEST_CODE = 1010;
    public static final String EXTRA_EDIT_DONE = "edit_done";
    // 文字
    private static final String WORD_NONE_TAB = "word_none_tab";
    private static final String WORD_YELLOW_TAB = "word_yellow_tab";
    private static final String WORD_RED_TAB = "word_red_tab";
    private static final String WORD_BUBBLE_TAB = "word_bubble_tab";

    /**
     * data
     */
    private MediaPlayerAPI mPlayer;
    private String[] arr;
    private List<String> pathList;
    private float totalTime;
    private String currentTab = ADJUST_TAB;
    private int videoIndex = 0;
    private VideoProcessController videoProcessController; // 视频编辑控制器
    private MediaCaptureOptions mediaCaptureOptions;
    private VideoItem videoItem;
    private String displayName; // 视频名称
    private List<Bitmap> textureBitmaps;
    // 调整布局参数，亮度，对比度等
    private float brightness = 0.0f;
    private float contrast = 1.0f;
    private float saturation = 1.0f;
    private float colorTemperature = 0;
    private float sharpness = 0;
    // subsection
    private int currentVideoLayout = 0;
    private List<Integer> videoThumbList;
    private int currentPictureLayout = -1;
    private int currentCircle = -1;
    private float volume = 0.3f; // 原声大小
    private boolean isTrasition; // 是否过渡

    // Textures
    private int currentTexturesLayout = -1;
    private Bitmap textureBitmap; // 贴图路径
    private float textureOffset;  //贴图起始时间
    private float textureDuration; // 贴图持续时间

    // accompany_sound
    private int currentSoundLayout = -1;
    private String accompanySoundPath;
    private float accompanyVolume = 0.3f; // 伴音大小

    // 文字
    private String currentWordTab = WORD_NONE_TAB;

    /**
     * view
     */
    // video view
    private NeteaseView videoView;
    // tab
    private ViewGroup adjustLayout;
    private ViewGroup subsectionLayout;
    private ViewGroup wordslayout;
    private ViewGroup texturesLayout;
    private ViewGroup accompanySoundLayout;
    // tab button
    private TextView adjustText;
    private TextView subsectionText;
    private TextView wordsText;
    private TextView texturesText;
    private TextView accompanySoundText;
    // edit root
    private ViewGroup editRoot;
    // 调整布局
    private TwoWaysVerticalSeekBar brightnessSeekbar;
    private TwoWaysVerticalSeekBar contrastSeekbar;
    private TwoWaysVerticalSeekBar saturationSeekbar;
    private TwoWaysVerticalSeekBar colorTemperatureSeekbar;
    private TwoWaysVerticalSeekBar sharpnessSeekbar;
    //textures
    private ViewGroup emptyLayout;
    private ViewGroup kissLayout;
    private ViewGroup knifeLayout;
    private ViewGroup grimaceLayout;
    private MoveImageView big_textures;
//    private RangeSeekBar textureSeekBar;
    private TextView textureMinText;
    private TextView textureMaxText;
    private ViewGroup showTimeLayout;

    // accompany_sound
    private ViewGroup emptySound;
    private ViewGroup sound_1;
    private ViewGroup sound_2;
    private ViewGroup sound_3;
    private SeekBar accompanySeekBar;
    private ViewGroup soundLayout;

    // 拼接等待界面
    private RelativeLayout combinationLayout; // 拼接等待界面
    private ImageView loadingImage; // 拼接等待图片
    private CircleProgressView loadingView; // 拼接loading
    // subsection
    private ViewGroup firstVideoLayout;
    private ViewGroup secondVideoLayout;
    private ViewGroup thirdVideoLayout;
    private ViewGroup subsectionMovementLayout;
    private TextView moveForwardBtn;
    private TextView moveBackwardBtn;
    private ImageView firstVideoThumb;
    private ImageView secondVideoThumb;
    private ImageView thirdVideoThumb;
    private ViewGroup firstChooseLayout;
    private ViewGroup secondChooseLayout;
    private ViewGroup thirdChooseLayout;
    private ImageView firstChooseImage;
    private ImageView secondChooseImage;
    private ImageView thirdChooseImage;
    private TextView trasitionNone; // 过渡，无
    private TextView trasitionFade; // 过渡，淡入淡出
    private SeekBar volumeSeekBar; // 原声进度条

    // 文字布局
    private TextView noneWordBtn; // 不显示文字按钮
    private TextView yellowWordBtn; // 黄色文字背景按钮
    private TextView redWordBtn;  // 红色文字背景按钮
    private TextView bubbleWordBtn; // 气泡文字背景按钮

    private TextView blackBtn; // 文字颜色按钮
    private TextView whiteBtn;
    private TextView yellowBtn;
    private TextView redBtn;
    private TextView blueBtn;
    private TextView greenBtn;

    private AutoResizeEditText wordEdit; // 界面上的文字背景

    private ViewGroup subsectionCircleLayout;

    public static void startActivityForResult(Context context, List<String> pathList,
                                              float totalTime, MediaCaptureOptions mediaCaptureOptions) {
        Intent intent = new Intent();
        intent.setClass(context, ShortVideoEditActivity.class);
        intent.putStringArrayListExtra(EXTRA_PATH_LIST, (ArrayList<String>) pathList);
        intent.putExtra(EXTRA_TOTAL_TIME, totalTime);
        intent.putExtra(EXTRA_MEDIA_OPTIONS, (Serializable) mediaCaptureOptions);
        ((Activity) context).startActivityForResult(intent, EXTRA_REQUEST_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initData();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        stopPlayer();
        super.onDestroy();
    }

    private void stopPlayer() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.unInit();
        }
        mPlayer = null;
    }

    @Override
    protected void handleIntent(Intent intent) {
        pathList = intent.getStringArrayListExtra(EXTRA_PATH_LIST);
        totalTime = intent.getFloatExtra(EXTRA_TOTAL_TIME, 0);
        mediaCaptureOptions = (MediaCaptureOptions) intent.getSerializableExtra(EXTRA_MEDIA_OPTIONS);

        if (pathList != null) {
            LogUtil.i(TAG, "how many videos:" + pathList.size());
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.short_video_edit_activity;
    }

    @Override
    public void onBackPressed() {
        if (combinationLayout.getVisibility() != View.VISIBLE) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_EDIT_DONE, false);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    @Override
    protected void initView() {
        initToolbar();
        initVideoView();
        // tab
        adjustLayout = findView(R.id.adjust_layout);
        subsectionLayout = findView(R.id.subsection_layout);
        wordslayout = findView(R.id.words_layout);
        texturesLayout = findView(R.id.textures_layout);
        accompanySoundLayout = findView(R.id.accompany_sound_layout);

        // tab button
        adjustText = findView(R.id.adjust_text);
        subsectionText = findView(R.id.subsection_text);
        wordsText = findView(R.id.words_text);
        texturesText = findView(R.id.textures_text);
        accompanySoundText = findView(R.id.accompany_sound_text);

        // edit root
        editRoot = findView(R.id.edit_layout);

        // 调整布局
        brightnessSeekbar = findView(R.id.brightness_seekbar);
        brightnessSeekbar.setMaxProgress(1.0);
        brightnessSeekbar.setLowestProgress(-1.0);
        brightnessSeekbar.setDefaultValue(0.0);
        contrastSeekbar = findView(R.id.contrast_seekbar);
        contrastSeekbar.setMaxProgress(4.0);
        contrastSeekbar.setLowestProgress(0.0);
        contrastSeekbar.setDefaultValue(1.0);
        saturationSeekbar = findView(R.id.saturation_seekbar);
        saturationSeekbar.setMaxProgress(2.0);
        saturationSeekbar.setLowestProgress(0.0);
        saturationSeekbar.setDefaultValue(1.0);
        colorTemperatureSeekbar = findView(R.id.color_temperature_seekbar);
        colorTemperatureSeekbar.setMaxProgress(360);
        colorTemperatureSeekbar.setLowestProgress(0);
        colorTemperatureSeekbar.setDefaultValue(0);
        sharpnessSeekbar = findView(R.id.sharpness_seekbar);
        sharpnessSeekbar.setMaxProgress(4.0);
        sharpnessSeekbar.setLowestProgress(-4.0);
        sharpnessSeekbar.setDefaultValue(0.0);

        //textures layout
        emptyLayout = findView(R.id.empty_layout);
        kissLayout = findView(R.id.kiss_layout);
        knifeLayout = findView(R.id.knife_layout);
        grimaceLayout = findView(R.id.grimace_layout);
        big_textures = findView(R.id.big_textures);
//        textureSeekBar = findView(R.id.texture_seekbar);
        textureMinText = findView(R.id.texture_min_time);
        textureMaxText = findView(R.id.texture_max_time);
        textureMinText.setText(TimeUtil.secondToTime(0));
        textureMaxText.setText(TimeUtil.secondToTime((int) totalTime));
        showTimeLayout = findView(R.id.showtime_layout);

        //accompany_sound
        emptySound = findView(R.id.empty_sound);
        sound_1 = findView(R.id.sound_1);
        sound_2 = findView(R.id.sound_2);
        sound_3 = findView(R.id.sound_3);
        soundLayout = findView(R.id.sound_layout);
        accompanySeekBar = findView(R.id.accompany_volume_seekbar);
        accompanySeekBar.setMax(100);
        accompanySeekBar.setProgress(30);


        // 拼接等待界面
        combinationLayout = findView(R.id.combination_layout);
        loadingImage = findView(R.id.loading_image);
        loadingView = findView(R.id.loading_view);
        // subsection layout
        firstVideoLayout = findView(R.id.picture_1_layout);
        secondVideoLayout = findView(R.id.picture_2_layout);
        thirdVideoLayout = findView(R.id.picture_3_layout);
        subsectionMovementLayout = findView(R.id.subsection_imageview_layout);
        moveForwardBtn = findView(R.id.move_forward);
        moveBackwardBtn = findView(R.id.move_backward);
        firstVideoThumb = findView(R.id.picture_1);
        secondVideoThumb = findView(R.id.picture_2);
        thirdVideoThumb = findView(R.id.picture_3);
        firstChooseLayout = findView(R.id.circle_1_layout);
        secondChooseLayout = findView(R.id.circle_2_layout);
        thirdChooseLayout = findView(R.id.circle_3_layout);
        firstChooseImage = findView(R.id.circle_1);
        secondChooseImage = findView(R.id.circle_2);
        thirdChooseImage = findView(R.id.circle_3);
        trasitionNone = findView(R.id.trasition_none);
        trasitionFade = findView(R.id.trasition_fade);
        volumeSeekBar = findView(R.id.volume_seekbar);
        volumeSeekBar.setMax(100);
        volumeSeekBar.setProgress(30);

        subsectionCircleLayout = findView(R.id.subsection_circle_layout);

        // 文字
        wordEdit = findView(R.id.word_edit);
        wordEdit.setCursorVisible(false);

        setListener();
    }

    private void initToolbar() {
        findView(R.id.back_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        findView(R.id.done_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNameDialog();
            }
        });
    }

    private void initVideoView() {
        videoView = findView(R.id.video_view);
        arr = new String[pathList.size()];
        arr = pathList.toArray(arr);
        mPlayer = MediaPlayerAPI.getInstance();
        mPlayer.init(getApplicationContext(), arr, videoView);
        mPlayer.start();

    }

    private void setListener() {
        adjustText.setOnClickListener(this);
        subsectionText.setOnClickListener(this);
        wordsText.setOnClickListener(this);
        texturesText.setOnClickListener(this);
        accompanySoundText.setOnClickListener(this);

        emptyLayout.setOnClickListener(this);
        kissLayout.setOnClickListener(this);
        knifeLayout.setOnClickListener(this);
        grimaceLayout.setOnClickListener(this);
       /* if (totalTime > 1000) {
            textureSeekBar.setRange(0, totalTime / 1000);
            textureSeekBar.setValue(0, totalTime / 1000);
        }*/
       /* textureSeekBar.setOnRangeChangedListener(new RangeSeekBar.OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float min, float max, boolean isFromUser) {
                textureOffset = min;
                textureDuration = max - textureOffset;
            }
        });*/

        // accompany sound
        emptySound.setOnClickListener(this);
        sound_1.setOnClickListener(this);
        sound_2.setOnClickListener(this);
        sound_3.setOnClickListener(this);
        accompanySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                accompanyVolume = (float) progress / 100;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // 调整布局
        brightnessSeekbar.setOnSeekBarChangeListener(new TwoWaysVerticalSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressBefore() {

            }

            @Override
            public void onProgressChanged(TwoWaysVerticalSeekBar seekBar, double progress) {
                brightness = (float) progress;
                if (mPlayer != null) {
                    mPlayer.setBrightness(brightness);
                }
            }

            @Override
            public void onProgressAfter() {

            }
        });
        contrastSeekbar.setOnSeekBarChangeListener(new TwoWaysVerticalSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressBefore() {

            }

            @Override
            public void onProgressChanged(TwoWaysVerticalSeekBar seekBar, double progress) {
                contrast = (float) progress;
                if (mPlayer != null) {
                    mPlayer.setContrast(contrast);
                }
            }

            @Override
            public void onProgressAfter() {

            }
        });
        saturationSeekbar.setOnSeekBarChangeListener(new TwoWaysVerticalSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressBefore() {

            }

            @Override
            public void onProgressChanged(TwoWaysVerticalSeekBar seekBar, double progress) {
                saturation = (float) progress;
                if (mPlayer != null) {
                    mPlayer.setSaturation(saturation);
                }
            }

            @Override
            public void onProgressAfter() {

            }
        });
        colorTemperatureSeekbar.setOnSeekBarChangeListener(new TwoWaysVerticalSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressBefore() {

            }

            @Override
            public void onProgressChanged(TwoWaysVerticalSeekBar seekBar, double progress) {
                colorTemperature = (float) progress;
                if (mPlayer != null) {
                    mPlayer.setHue(colorTemperature);
                }
            }

            @Override
            public void onProgressAfter() {

            }
        });
        sharpnessSeekbar.setOnSeekBarChangeListener(new TwoWaysVerticalSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressBefore() {

            }

            @Override
            public void onProgressChanged(TwoWaysVerticalSeekBar seekBar, double progress) {
                sharpness = (float) progress;
                if (mPlayer != null) {
                    mPlayer.setSharpen(sharpness);
                }
            }

            @Override
            public void onProgressAfter() {

            }
        });
        // subsection
        firstVideoLayout.setOnClickListener(this);
        secondVideoLayout.setOnClickListener(this);
        thirdVideoLayout.setOnClickListener(this);
        moveForwardBtn.setOnClickListener(this);
        moveBackwardBtn.setOnClickListener(this);

        firstChooseLayout.setOnClickListener(this);
        secondChooseLayout.setOnClickListener(this);
        thirdChooseLayout.setOnClickListener(this);

        trasitionNone.setOnClickListener(this);
        trasitionFade.setOnClickListener(this);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                volume = (float) progress / 100;
                if (mPlayer != null) {
                    mPlayer.setVolume(volume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void initData() {
        videoProcessController = new VideoProcessController(this, this);
        videoThumbList = new ArrayList<>();
        videoThumbList.add(R.drawable.ic_first_subsection);
        videoThumbList.add(R.drawable.ic_second_subsection);
        videoThumbList.add(R.drawable.ic_third_subsection);
        textureOffset = 0;
        textureDuration = totalTime;

        textureBitmaps = new ArrayList<>();
        textureBitmaps.add(convertDrawableToBitmap(R.drawable.big_kiss));
        textureBitmaps.add(convertDrawableToBitmap(R.drawable.big_knife));
        textureBitmaps.add(convertDrawableToBitmap(R.drawable.big_grimace));
    }

    private Bitmap convertDrawableToBitmap(int drawable) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), drawable, options);

        return bitmap;
    }

    private void updateUI() {
        if (pathList == null) {
            return;
        }
        firstVideoLayout.setVisibility(pathList.size() > 0 ? View.VISIBLE : View.GONE);
        secondVideoLayout.setVisibility(pathList.size() > 1 ? View.VISIBLE : View.GONE);
        thirdVideoLayout.setVisibility(pathList.size() > 2 ? View.VISIBLE : View.GONE);
        firstChooseImage.setVisibility(pathList.size() > 0 ? View.VISIBLE : View.GONE);
        secondChooseImage.setVisibility(pathList.size() > 1 ? View.VISIBLE : View.GONE);
        thirdChooseImage.setVisibility(pathList.size() > 2 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.adjust_text:
                currentTab = ADJUST_TAB;
                switchTab();
                break;
            case R.id.subsection_text:
                currentTab = SUBSECTION_TAB;
                switchTab();
                break;
            case R.id.words_text:
                currentTab = WORDS_TAB;
                switchTab();
                initWordsLayout();
                break;
            case R.id.textures_text:
                currentTab = TEXTURES_TAB;
                switchTab();
                break;
            case R.id.accompany_sound_text:
                currentTab = ACCOMPANY_SOUND_TAB;
                switchTab();
                break;
            case R.id.empty_layout:
                updateTexturesLayout(1);
                break;
            case R.id.kiss_layout:
                updateTexturesLayout(2);
                break;
            case R.id.knife_layout:
                updateTexturesLayout(3);
                break;
            case R.id.grimace_layout:
                updateTexturesLayout(4);
                break;
            case R.id.picture_1_layout:
                updatePictureLayout(1);
                break;
            case R.id.picture_2_layout:
                updatePictureLayout(2);
                break;
            case R.id.picture_3_layout:
                updatePictureLayout(3);
                break;
            case R.id.move_forward:
                moveVideoForward();
                break;
            case R.id.move_backward:
                moveVideoBackward();
                break;
            case R.id.empty_sound:
                updateSoundLayout(1);
                break;
            case R.id.sound_1:
                updateSoundLayout(2);
                break;
            case R.id.sound_2:
                updateSoundLayout(3);
                break;
            case R.id.sound_3:
                updateSoundLayout(4);
                break;
            case R.id.circle_1_layout:
                updateCircle(1);
                break;
            case R.id.circle_2_layout:
                updateCircle(2);
                break;
            case R.id.circle_3_layout:
                updateCircle(3);
                break;
            case R.id.trasition_none:
                updateTrasition(false);
                break;
            case R.id.trasition_fade:
                updateTrasition(true);
                break;
        }
    }

    private View.OnClickListener wordsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.none_word_btn:
                    currentWordTab = WORD_NONE_TAB;
                    switchWordTab();
                    break;
                case R.id.yellow_word_btn:
                    currentWordTab = WORD_YELLOW_TAB;
                    switchWordTab();
                    break;
                case R.id.red_word_btn:
                    currentWordTab = WORD_RED_TAB;
                    switchWordTab();
                    break;
                case R.id.bubble_word_btn:
                    currentWordTab = WORD_BUBBLE_TAB;
                    switchWordTab();
                    break;
                case R.id.black_btn:
                    wordEdit.setTextColor(Color.BLACK);
                    break;
                case R.id.white_btn:
                    wordEdit.setTextColor(Color.WHITE);
                    break;
                case R.id.yellow_btn:
                    wordEdit.setTextColor(Color.YELLOW);
                    break;
                case R.id.red_btn:
                    wordEdit.setTextColor(Color.RED);
                    break;
                case R.id.blue_btn:
                    wordEdit.setTextColor(Color.BLUE);
                    break;
                case R.id.green_btn:
                    wordEdit.setTextColor(Color.GREEN);
                    break;
            }
        }
    };

    private void switchTab() {
        adjustText.setEnabled(!currentTab.equals(ADJUST_TAB));
        subsectionText.setEnabled(!currentTab.equals(SUBSECTION_TAB));
        wordsText.setEnabled(!currentTab.equals(WORDS_TAB));
        texturesText.setEnabled(!currentTab.equals(TEXTURES_TAB));
        accompanySoundText.setEnabled(!currentTab.equals(ACCOMPANY_SOUND_TAB));

        adjustLayout.setVisibility(currentTab.equals(ADJUST_TAB) ? View.VISIBLE : View.GONE);
        subsectionLayout.setVisibility(currentTab.equals(SUBSECTION_TAB) ? View.VISIBLE : View.GONE);
        wordslayout.setVisibility(currentTab.equals(WORDS_TAB) ? View.VISIBLE : View.GONE);
        texturesLayout.setVisibility(currentTab.equals(TEXTURES_TAB) ? View.VISIBLE : View.GONE);
        accompanySoundLayout.setVisibility(currentTab.equals(ACCOMPANY_SOUND_TAB) ? View.VISIBLE : View.GONE);

        // 文字编辑框是否隐藏
        if (noneWordBtn == null) {
            wordEdit.setVisibility(View.GONE);
        } else {
            wordEdit.setVisibility(noneWordBtn.isEnabled() ? View.VISIBLE : View.GONE);
        }
    }

    // 初始化文字布局
    private void initWordsLayout() {
        if (noneWordBtn == null) {
            noneWordBtn = findView(R.id.none_word_btn);
            yellowWordBtn = findView(R.id.yellow_word_btn);
            redWordBtn = findView(R.id.red_word_btn);
            bubbleWordBtn = findView(R.id.bubble_word_btn);

            blackBtn = findView(R.id.black_btn);
            whiteBtn = findView(R.id.white_btn);
            yellowBtn = findView(R.id.yellow_btn);
            redBtn = findView(R.id.red_btn);
            blueBtn = findView(R.id.blue_btn);
            greenBtn = findView(R.id.green_btn);

            noneWordBtn.setOnClickListener(wordsListener);
            yellowWordBtn.setOnClickListener(wordsListener);
            redWordBtn.setOnClickListener(wordsListener);
            bubbleWordBtn.setOnClickListener(wordsListener);

            blackBtn.setOnClickListener(wordsListener);
            whiteBtn.setOnClickListener(wordsListener);
            yellowBtn.setOnClickListener(wordsListener);
            redBtn.setOnClickListener(wordsListener);
            blueBtn.setOnClickListener(wordsListener);
            greenBtn.setOnClickListener(wordsListener);
        }

    }

    // 切换文字背景框
    private void switchWordTab() {
        noneWordBtn.setEnabled(!currentWordTab.equals(WORD_NONE_TAB));
        yellowWordBtn.setEnabled(!currentWordTab.equals(WORD_YELLOW_TAB));
        redWordBtn.setEnabled(!currentWordTab.equals(WORD_RED_TAB));
        bubbleWordBtn.setEnabled(!currentWordTab.equals(WORD_BUBBLE_TAB));

        wordEdit.setVisibility(noneWordBtn.isEnabled() ? View.VISIBLE : View.GONE);

        if (noneWordBtn.isEnabled()) {
            wordEdit.requestFocus();
        }

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) wordEdit.getLayoutParams();
        if (!yellowWordBtn.isEnabled()) {
            wordEdit.setBackgroundResource(R.drawable.ic_yellow_edit);
            layoutParams.width = ScreenUtil.screenWidth;
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else if (!redWordBtn.isEnabled()) {
            wordEdit.setBackgroundResource(R.drawable.ic_red_edit);
            layoutParams.width = ScreenUtil.dip2px(250);
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else if (!bubbleWordBtn.isEnabled()) {
            wordEdit.setBackgroundResource(R.drawable.ic_bubble_edit);
            layoutParams.width = ScreenUtil.dip2px(100);
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        // 设置文字背景框为固定大小
        wordEdit.setLayoutParams(layoutParams);
    }

    //选中伴音中的哪个伴音
    private void updateSoundLayout(int currentSoundLayout) {
        this.currentSoundLayout = currentSoundLayout;
        soundLayout.setVisibility(1 == currentSoundLayout ? View.GONE : View.VISIBLE);

        emptySound.setBackgroundResource(1 == currentSoundLayout ? R.drawable.border : R.color.color_gray_1affffff);
        sound_1.setBackgroundResource(2 == currentSoundLayout ? R.drawable.border : R.color.color_gray_1affffff);
        sound_2.setBackgroundResource(3 == currentSoundLayout ? R.drawable.border : R.color.color_gray_1affffff);
        sound_3.setBackgroundResource(4 == currentSoundLayout ? R.drawable.border : R.color.color_gray_1affffff);

        switch (currentSoundLayout) {
            case 1:
                accompanySoundPath = null;
                break;
            case 2:
                accompanySoundPath = "sdcard/sound/test_1.mp3";
                break;
            case 3:
                accompanySoundPath = "sdcard/sound/test_2.mp3";
                break;
            case 4:
                accompanySoundPath = "sdcard/sound/test_3.mp3";
                break;
        }
    }

    // 选中贴图中的哪个表情
    private void updateTexturesLayout(int currentTexturesLayout) {
        this.currentTexturesLayout = currentTexturesLayout;
        showTimeLayout.setVisibility(1 == currentTexturesLayout ? View.GONE : View.VISIBLE);
        emptyLayout.setBackgroundResource(1 == currentTexturesLayout ? R.drawable.border : R.color.color_gray_1affffff);
        kissLayout.setBackgroundResource(2 == currentTexturesLayout ? R.drawable.border : R.color.color_gray_1affffff);
        knifeLayout.setBackgroundResource(3 == currentTexturesLayout ? R.drawable.border : R.color.color_gray_1affffff);
        grimaceLayout.setBackgroundResource(4 == currentTexturesLayout ? R.drawable.border : R.color.color_gray_1affffff);

        updateBigTextures(currentTexturesLayout, 1 == currentTexturesLayout);

    }

    //选项图片并且贴到图像上
    private void updateBigTextures(int currentTexturesLayout, boolean isHide) {
        switch (currentTexturesLayout) {
            case 1:
                big_textures.setVisibility(View.GONE);
                if (isHide) {
                    textureBitmap = null;
                }
                break;
            case 2:
                big_textures.setVisibility(View.VISIBLE);
                big_textures.setBackgroundResource(R.drawable.big_kiss);
                textureBitmap = textureBitmaps.get(0);
                break;
            case 3:
                big_textures.setVisibility(View.VISIBLE);
                big_textures.setBackgroundResource(R.drawable.big_knife);
                textureBitmap = textureBitmaps.get(1);
                break;
            case 4:
                big_textures.setVisibility(View.VISIBLE);
                big_textures.setBackgroundResource(R.drawable.big_grimace);
                textureBitmap = textureBitmaps.get(2);
                break;
        }

    }

    //更新Circle的layout
    private void updateCircle(int currentCircle) {
        this.currentCircle = currentCircle;

        firstChooseImage.setImageResource(1 == currentCircle ? R.drawable.ic_blue_circle : R.drawable.gray);
        secondChooseImage.setImageResource(2 == currentCircle ? R.drawable.ic_blue_circle : R.drawable.gray);
        thirdChooseImage.setImageResource(3 == currentCircle ? R.drawable.ic_blue_circle : R.drawable.gray);

        updateCircle();
    }

    private void updateCircle() {
        subsectionCircleLayout.setVisibility(View.VISIBLE);
        subsectionMovementLayout.setVisibility(View.GONE);
        firstVideoLayout.setBackgroundResource(R.color.transparent);
        secondVideoLayout.setBackgroundResource(R.color.transparent);
        thirdVideoLayout.setBackgroundResource(R.color.transparent);
    }

    // 选中哪个视频的背景图显示
    private void updatePictureLayout(int currentVideoLayout) {
        this.currentVideoLayout = currentVideoLayout;

        firstVideoLayout.setBackgroundResource(1 == currentVideoLayout ? R.drawable.blue_border : R.color.transparent);
        secondVideoLayout.setBackgroundResource(2 == currentVideoLayout ? R.drawable.blue_border : R.color.transparent);
        thirdVideoLayout.setBackgroundResource(3 == currentVideoLayout ? R.drawable.blue_border : R.color.transparent);

        updateVideoThumb();
        updateMoveLayout();
    }

    private void updateVideoThumb() {
        // 先清空原来现实图片
        firstVideoThumb.setBackgroundResource(0);
        secondVideoThumb.setBackgroundResource(0);
        thirdVideoThumb.setBackgroundResource(0);

        firstVideoThumb.setImageResource(videoThumbList.get(0));
        secondVideoThumb.setImageResource(videoThumbList.get(1));
        thirdVideoThumb.setImageResource(videoThumbList.get(2));
    }

    // 更新排序，往前，往后布局
    private void updateMoveLayout() {
        subsectionMovementLayout.setVisibility(View.VISIBLE);
        subsectionCircleLayout.setVisibility(View.GONE);
        firstChooseImage.setImageResource(R.drawable.gray);
        secondChooseImage.setImageResource(R.drawable.gray);
        thirdChooseImage.setImageResource(R.drawable.gray);
        moveForwardBtn.setEnabled(currentVideoLayout > 1);
        moveBackwardBtn.setEnabled(currentVideoLayout < pathList.size());
    }

    private void moveVideoForward() {
        if (currentVideoLayout <= 1) {
            return;
        }
        Collections.swap(pathList, currentVideoLayout - 2, currentVideoLayout - 1);
        Collections.swap(videoThumbList, currentVideoLayout - 2, currentVideoLayout - 1);
        currentVideoLayout--;
        updatePictureLayout(currentVideoLayout);
    }

    private void moveVideoBackward() {
        if (currentVideoLayout >= pathList.size() || currentVideoLayout >= videoThumbList.size()) {
            return;
        }
        Collections.swap(pathList, currentVideoLayout - 1, currentVideoLayout);
        Collections.swap(videoThumbList, currentVideoLayout - 1, currentVideoLayout);
        currentVideoLayout++;
        updatePictureLayout(currentVideoLayout);
    }

    // 淡入淡出选择
    private void updateTrasition(boolean isChoose) {
        trasitionNone.setEnabled(isChoose);
        trasitionFade.setEnabled(!isChoose);
        isTrasition = isChoose;
    }

    /**
     * ************* 设置完成，开始拼接视频 *****************
     */
    // 设置视频名称
    private void showNameDialog() {
        final EasyEditDialog requestDialog = new EasyEditDialog(ShortVideoEditActivity.this);
        requestDialog.setEditTextMaxLength(200);
        requestDialog.setTitle(getString(R.string.video_name));
        requestDialog.setEditHint("新视频" + TimeUtil.getMonthTimeString(System.currentTimeMillis()));
        requestDialog.setInputType(InputType.TYPE_CLASS_TEXT);
        requestDialog.setCustomTextWatcher(true);
        requestDialog.addNegativeButtonListener(R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDialog.dismiss();
            }
        });
        requestDialog.addPositiveButtonListener(R.string.save, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayName = requestDialog.getEditMessage();
                if (TextUtils.isEmpty(displayName)) {
                    Toast.makeText(ShortVideoEditActivity.this, "不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                requestDialog.dismiss();
                videoView.setVisibility(View.GONE);
                editRoot.setVisibility(View.GONE);
                DialogMaker.showProgressDialog(ShortVideoEditActivity.this, "等待截图");
                stopPlayer();

                startVideoProcess();
            }
        });
        requestDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
            }
        });
        requestDialog.show();

        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(requestDialog.getmEdit(), 0);
            }
        }, 200);
    }

    private int videoWidth;
    private int videoHeight;

    private void startVideoProcess() {
        TranscodingAPI.SnapshotPara para = new TranscodingAPI.SnapshotPara();
        try {
            para.setFilePath(pathList.get(0));
            MediaMetadata.MetaData metaData = TranscodingAPI.getInstance().getMediaInfo(pathList.get(0));
            if (metaData != null) {
                videoWidth = metaData.width;
                videoHeight = metaData.height;

            }
            para.setStart(1);
            para.setInterval(0);
            LogUtil.i(TAG, "start snapshot， width:" + videoWidth + ", height:" + videoHeight);
            if ((videoWidth < 240 || videoHeight < 320) &&
                    videoWidth > 0 && videoHeight > 0) {
                // 截图的大小 要比源文件小
                para.setOutWidth(videoWidth);
                para.setOutHeight(videoHeight);
            } else {
                para.setOutWidth(240);
                para.setOutHeight(320);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        videoProcessController.startSnapShot(para);
    }

    private String saveBitmapToPath(Bitmap bitmap) {
        String path = FileUtil.getThumbPath(displayName + System.currentTimeMillis(), ".png", StorageType.TYPE_THUMB_IMAGE);
        File dirFile = new File(path);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(dirFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    private void startCombination(String path) {
        VideoProcessOptions videoProcessOptions;
        String outputPath;
        try {
            videoProcessOptions = new VideoProcessOptions(mediaCaptureOptions);
            // 设置待拼接的文件
            TranscodingAPI.TranSource inputFilePara = videoProcessOptions.getSource();
            arr = pathList.toArray(arr);
            inputFilePara.setFilePaths(arr);
            inputFilePara.setMergeWidth(mediaCaptureOptions.mVideoPreviewWidth);
            inputFilePara.setMergeHeight(mediaCaptureOptions.mVideoPreviewHeight);
            // 原声大小
            inputFilePara.setAudioVolume(volume);
            videoProcessOptions.setSource(inputFilePara);
            // 过渡
            if (isTrasition) {
                inputFilePara.setVideoFadeDuration(1000);
            } else {
                inputFilePara.setVideoFadeDuration(100);
            }

            // 设置拼接后文件存储地址
            outputPath = StorageUtil.getWritePath(displayName + ".mp4", StorageType.TYPE_VIDEO);
            TranscodingAPI.TranOut outputFilePara = videoProcessOptions.getOutputFilePara();
            outputFilePara.setFilePath(outputPath);
            videoProcessOptions.setOutputFilePara(outputFilePara);
            // 设置视频亮度，对比度等
            TranscodingAPI.TranFilter filter = videoProcessOptions.getFilter();
            filter.setBrightness(brightness);
            filter.setContrast(contrast);
            filter.setSaturation(saturation);
            filter.setHue(colorTemperature);
            filter.setSharpenness(sharpness);
            videoProcessOptions.setFilter(filter);
            // 伴音
            TranscodingAPI.TranMixAudio mixAudioPara = videoProcessOptions.getMixAudio();
            if (!TextUtils.isEmpty(accompanySoundPath)) {
                mixAudioPara.setFilePath(accompanySoundPath);
                mixAudioPara.setMixVolume(accompanyVolume);
            }
            // 贴图
            setTexture(videoProcessOptions);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        // videoItem
        videoItem = new VideoItem();
        videoItem.setId("local" + System.currentTimeMillis());
        videoItem.setFilePath(outputPath);
        videoItem.setDisplayName(displayName);
        videoItem.setDateTaken(TimeUtil.getNowDatetime());
        videoItem.setUriString(path);

        // 开始拼接
        videoProcessController.startCombination(videoProcessOptions);
    }

    // 设置贴图
    private void setTexture(VideoProcessOptions videoProcessOptions) {
        // 按照原始视频大小贴图
        List<TranscodingAPI.TranWaterMark> waterList = new ArrayList<>();
        TranscodingAPI.TranWaterMark textureMark = null;
        TranscodingAPI.TranWaterMark wordMark = null;
        // 贴图持续时间不能为0
        if (textureBitmap != null && textureDuration != 0) {
            float xpos;
            float ypos;
            textureMark = new TranscodingAPI.TranWaterMark();
            if (big_textures.isMove()) {
                xpos = (big_textures.getLastX() - (big_textures.getWidth() / 2)) * videoWidth / (float) ScreenUtil.screenWidth;
                ypos = (big_textures.getLastY() - (big_textures.getHeight() / 2)) * videoHeight / (float) ScreenUtil.screenHeight;
                textureMark.setX((int) xpos);
                textureMark.setY((int) ypos);
            } else {
                textureMark.setRect(VideoEffect.Rect.center);
            }

            textureMark.setBitmap(textureBitmap);

            // 时间单位，毫秒
            textureMark.setDuration((int) Math.floor(textureDuration));
            textureMark.setStart((int) textureOffset);
            waterList.add(textureMark);
            LogUtil.i(TAG, "texture===totalTime:" + totalTime / 1000 +
                    ", duration:" + ((int) Math.floor(textureDuration))
                    + ", offset:" + textureOffset);
        }
        // 文字贴图
        if (noneWordBtn != null && noneWordBtn.isEnabled()) {
            wordMark = new TranscodingAPI.TranWaterMark();

            int drawable = 0;
            if (!yellowWordBtn.isEnabled()) {
                drawable = R.drawable.ic_yellow_edit;
            } else if (!redWordBtn.isEnabled()) {
                drawable = R.drawable.ic_red_edit;
            } else if (!bubbleWordBtn.isEnabled()) {
                drawable = R.drawable.ic_bubble_edit;
            }
            wordMark.setBitmap(addTextToBitmap(wordEdit.getText().toString(),
                    wordEdit.getTextSize(), wordEdit.getCurrentTextColor(),
                    drawable));

            // 贴在视频中间
            wordMark.setRect(VideoEffect.Rect.center);
            // 时间单位毫秒
            wordMark.setDuration((int) totalTime);
            wordMark.setStart(0);
            waterList.add(wordMark);
        }

        if (waterList.size() > 0) {
            videoProcessOptions.setWaterMarks(waterList);
        }
    }


    private Bitmap textAsBitmap(String text, float textSize, int width, int textColor) {
        TextPaint textPaint = new TextPaint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        StaticLayout layout = new StaticLayout(text, textPaint, width,
                Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        Bitmap bitmap = Bitmap.createBitmap(layout.getWidth() + 10,
                layout.getHeight() + 10, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
        layout.draw(canvas);
        return bitmap;
    }

    //在图片上加文字
    private Bitmap addTextToBitmap(String text, float textSize, int textColor, int background) {
        BitmapFactory.Options waterOption = new BitmapFactory.Options();
        waterOption.inScaled = false; //设置图片缩放
        Bitmap source = BitmapFactory.decodeResource(getResources(), background, waterOption);
        int pictureWidth;
        int pictureHeight;
        if (!bubbleWordBtn.isEnabled()) {
            // bubble背景图，不进行缩放，一加5的红色会变形
            pictureWidth = source.getWidth();
            pictureHeight = source.getHeight();
        } else {
            pictureWidth = wordEdit.getWidth();
            pictureHeight = wordEdit.getHeight();
        }
        Bitmap icon = Bitmap.createBitmap(pictureWidth, pictureHeight, Bitmap.Config.ARGB_8888); //建立一个空的BItMap

        Canvas canvas = new Canvas(icon);//初始化画布 绘制的图像到icon上
        Paint photoPaint = new Paint(); //建立画笔
        photoPaint.setDither(true); //获取跟清晰的图像采样
        photoPaint.setFilterBitmap(true);//过滤一些

        Rect src = new Rect(0, 0, source.getWidth(), source.getHeight());//创建一个指定的新矩形的坐标
        Rect dst = new Rect(0, 0, pictureWidth, pictureHeight);//创建一个指定的新矩形的坐标
        canvas.drawBitmap(source, src, dst, photoPaint);//将photo 缩放或则扩大到 dst使用的填充区photoPaint

        Bitmap textBitmap = textAsBitmap(text, textSize, pictureWidth, textColor);
        src = new Rect(0, 0, textBitmap.getWidth(), textBitmap.getHeight());//创建一个指定的新矩形的坐标
        dst = new Rect(ScreenUtil.dip2px(12), ScreenUtil.dip2px(8),
                pictureWidth - ScreenUtil.dip2px(8), pictureHeight - ScreenUtil.dip2px(8));//创建一个指定的新矩形的坐标
        canvas.drawBitmap(textBitmap, src, dst, photoPaint);//将photo 缩放或则扩大到 dst使用的填充区photoPaint

        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        return icon;
    }

    /**
     * **************** VideoProcessCallback *******************
     */
    @Override
    public void onVideoProcessSuccess() {
        Toast.makeText(this, "保存成功！", Toast.LENGTH_SHORT).show();
        for (String path : pathList) {
            FileUtil.deleteFile(path);
        }
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.putExtra(VideoShootActivity.EXTRA_VIDEO_ITEM, videoItem);
                intent.putExtra(EXTRA_EDIT_DONE, true);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        }, 3000);
    }

    @Override
    public void onVideoProcessFailed(int code) {
        Toast.makeText(this, "视频保存失败:" + code, Toast.LENGTH_SHORT).show();
        // 视频保存失败，直接退出录制界面
        Intent intent = new Intent();
        intent.putExtra(EXTRA_EDIT_DONE, true);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void onVideoSnapshotSuccess(Bitmap bitmap) {
        String path = saveBitmapToPath(bitmap);
        DialogMaker.dismissProgressDialog();
        combinationLayout.setVisibility(View.VISIBLE);
        Glide.with(this).load(pathList.get(0)).into(loadingImage);
        loadingView.setFormat("%");
        loadingView.setProgress(0);
        loadingView.setContent("0");
        startCombination(path);
    }

    @Override
    public void onVideoSnapshotFailed(int code) {
        DialogMaker.dismissProgressDialog();
        Toast.makeText(this, "截图失败:" + code, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onVideoProcessUpdate(int process, int total) {
        loadingView.setMaxProgress(total);
        loadingView.setProgress(process);
        NumberFormat numberFormat = NumberFormat.getInstance();

        // 设置精确到小数点后0位
        numberFormat.setMaximumFractionDigits(0);

        String result = numberFormat.format((float) process / (float) total * 100);
        loadingView.setContent(result);
    }
}
