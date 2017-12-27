package com.netease.nim.live.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.netease.cloud.nos.android.core.CallRet;
import com.netease.nim.live.DemoCache;
import com.netease.nim.live.R;
import com.netease.nim.live.adapter.VideoAdapter;
import com.netease.nim.live.modle.UploadState;
import com.netease.nim.live.modle.VideoInfoEntity;
import com.netease.nim.live.modle.VideoItem;
import com.netease.nim.live.testuploadvideo.NOSUpload;
import com.netease.nim.live.testuploadvideo.NOSUploadHandler;
import com.netease.nim.live.video.http.UploadType;
import com.netease.nim.live.video.utils.NetworkUtils;
import com.netease.nim.uikit.common.ui.dialog.EasyAlertDialogHelper;
import com.netease.nim.uikit.common.ui.recyclerview.adapter.BaseFetchLoadAdapter;
import com.netease.nim.uikit.common.ui.recyclerview.listener.OnItemClickListener;
import com.netease.nim.uikit.common.ui.recyclerview.loadmore.MsgListFetchLoadMoreView;
import com.netease.nim.uikit.common.util.sys.NetworkUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import view.RecyclerViewEmptySupport;

public class PhotoTakeActivity extends AppCompatActivity implements View.OnClickListener, VideoAdapter.EventListener{

    @InjectView(R.id.take_video_image)
    ImageView takeVideoImage;
    @InjectView(R.id.list_empty)
    LinearLayout listEmpty;
    @InjectView(R.id.video_list)
    RecyclerViewEmptySupport videoListView;
    // data
    private int videoCount; // 已经上传服务器的视频数量
    private List<VideoItem> items; // 视频item列表
    VideoAdapter videoAdapter;
    private File mFile;
    private boolean allowMobileNetwork = false; //是否允许移动网络进行上传
    private boolean needToAlert = true;
    private boolean needResumeUpload; //是否需要恢复上传
    private AlertDialog MobileNetworkDialog; //移动网络的提示框
    /*测试上传*/
    private String mNosToken, mBucket, mObject;
    private NOSUpload nosUpload;
    private NOSUpload.UploadExecutor executor = null;
    private ProgressBar progressBar;
    private TextView txtNetUrl;
    private TextView txtprogress;
    private Button btnCancel;
    private Button btnHttpUpload;

    private static class HandleMsg {
        public static final int MSG_INIT_SUCCESS = 0;
        public static final int MSG_INIT_FAIL = 1;
        public static final int MSG_QUERYVIDEO_SUCCESS = 2;
        public static final int MSG_QUERYVIDEO_FAIL = 3;
    }
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case HandleMsg.MSG_INIT_SUCCESS: {
                    Toast.makeText(PhotoTakeActivity.this, "init success", Toast.LENGTH_SHORT).show();
                    txtNetUrl.setText("http://nos.netease.com/" + mBucket + "/" + mObject);
                    /*判断网络*/
                    checkNetWork();
                    break;
                }
                case HandleMsg.MSG_INIT_FAIL: {
                    int code = msg.arg1;
                    String m = (String) msg.obj;
                    Toast.makeText(PhotoTakeActivity.this, "init fail, code: " + code + ", msg: " + m, Toast.LENGTH_SHORT).show();
                    break;
                }
                case HandleMsg.MSG_QUERYVIDEO_SUCCESS: {
                    List<NOSUploadHandler.VideoQueryCallback.QueryResItem> list = (List< NOSUploadHandler.VideoQueryCallback.QueryResItem> ) msg.obj;
                    Toast.makeText(PhotoTakeActivity.this, "query video success: " + list.toString(), Toast.LENGTH_SHORT).show();
                    break;
                }
                case HandleMsg.MSG_QUERYVIDEO_FAIL: {
                    int code = msg.arg1;
                    String m = (String) msg.obj;
                    Toast.makeText(PhotoTakeActivity.this, "query video fail, code: " + code + ", msg: " + m, Toast.LENGTH_SHORT).show();
                    break;
                }

                default:
                    break;
            }
            return false;
        }
    });

    private void checkNetWork() {
        if (!allowMobileNetwork && NetworkUtils.getNetworkType() == NetworkUtils.TYPE_MOBILE) {

            if (!needToAlert) return;

            needResumeUpload = true;
            try {
                //Context传入DemoCache.getVisibleActivity(), 则在其他页面也可弹窗提示
                AlertDialog.Builder builder = new AlertDialog.Builder(DemoCache.getVisibleActivity() == null ? PhotoTakeActivity.this : DemoCache.getVisibleActivity());
                builder.setMessage("正在使用手机流量上传, 是否继续?");
                builder.setPositiveButton("是",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                allowMobileNetwork = true;
                                httpUpload();
                            }
                        });
                builder.setNegativeButton("否",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //若提示且用户不上传, 则依赖后续网络切换至WIFI自动上传
                                allowMobileNetwork = false;
                                needToAlert = false;
                                Toast.makeText(PhotoTakeActivity.this.getApplicationContext(), "待连接至WIFI网络后,继续上传", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();

                            }
                        });
                builder.setCancelable(false);

                if (MobileNetworkDialog == null || !MobileNetworkDialog.isShowing()) {
                    MobileNetworkDialog = builder.show();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {
            httpUpload();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_take);
        ButterKnife.inject(this);
        progressBar = (ProgressBar) findViewById(R.id.progress_upload);
        txtNetUrl = (TextView) findViewById(R.id.txtNetUrl);
        txtprogress = (TextView) findViewById(R.id.txt_progress);
        (btnCancel = (Button) findViewById(R.id.btn_cancel)).setOnClickListener(this);
        (btnHttpUpload = (Button) findViewById(R.id.btn_http_upload)).setOnClickListener(this);
        progressBar.setMax(1000);
        findViews();
    }

    private void findViews() {
        // adapter
        items = new ArrayList<>();
        videoAdapter = new VideoAdapter(videoListView, R.layout.video_item_layout, items);
        videoAdapter.setEventListener(PhotoTakeActivity.this);
        videoAdapter.setFetchMoreView(new MsgListFetchLoadMoreView());
        videoAdapter.setOnFetchMoreListener(new BaseFetchLoadAdapter.RequestFetchMoreListener() {
            @Override
            public void onFetchMoreRequested() {
//                getVideoList(null);
            }
        });
        videoListView.addOnItemTouchListener(new OnItemClickListener<VideoAdapter>() {
            @Override
            public void onItemClick(VideoAdapter adapter, View view, int position) {
                VideoInfoEntity entity = adapter.getItem(position).getEntity();
                if (entity != null) {
                    if (TextUtils.isEmpty(entity.getSnapshotUrl())) {
                        entity.setSnapshotUrl(adapter.getItem(position).getUriString());
                    }
                   /* VideoDetailInfoActivity.startActivity(getActivity(), adapter.getItem(position).getEntity(),
                            adapter.getItem(position).getState(), false);*/
                }
            }

            @Override
            public void onItemLongClick(VideoAdapter adapter, View view, int position) {
//                onNormalLongClick(position);
            }
        });
        videoListView.setLayoutManager(new LinearLayoutManager(PhotoTakeActivity.this));
        videoListView.setEmptyView(findViewById(R.id.list_empty));
        videoListView.setAdapter(videoAdapter);
    }

    private interface FetchVideoListener {
        void onFetchVideoDone();
    }
/*
    private void getVideoList(final FetchVideoListener fetchVideoListener) {
        DemoServerHttpClient.getInstance().videoInfoGet(null, UploadType.SHORT_VIDEO, new DemoServerHttpClient.DemoServerHttpCallback<List<VideoInfoEntity>>() {
            @Override
            public void onSuccess(List<VideoInfoEntity> entities) {
                List<VideoInfoEntity> videoInfoEntities = new ArrayList<>();
                List<VideoItem> videoItems = new ArrayList<>();
                for (VideoInfoEntity videoInfoEntity : entities) {
                    if (videosFromServer.containsKey(videoInfoEntity.getVid())) {
                        continue;
                    }
                    videoInfoEntities.add(videoInfoEntity);
                    VideoItem videoItem = new VideoItem();
                    videoItem.setEntity(videoInfoEntity);
                    videoItems.add(videoItem);
                    videosFromServer.put(videoInfoEntity.getVid(), videoInfoEntity);
                }

                // 顶部加载
                if (videoInfoEntities.size() <= 0) {
                    videoAdapter.fetchMoreEnd(true);
                } else {
                    videoCount = videoInfoEntities.size();
                    videoAdapter.fetchMoreComplete(videoListView, videoItems);
                }
                if (fetchVideoListener != null) {
                    fetchVideoListener.onFetchVideoDone();
                }
            }

            @Override
            public void onFailed(int code, String errorMsg) {
            }
        });
    }*/



    @OnClick({R.id.take_video_image, R.id.list_empty})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.take_video_image:
                checkTakeVideo();
                break;
            case R.id.video_list:
                break;
            case R.id.list_empty:
                break;
            case R.id.btn_cancel:
                cancelUpload();
                break;
            case R.id.btn_http_upload:
                httpUpload();
                break;


        }
    }

    private void cancelUpload() {
        if (executor != null) {
            executor.cancel();
        }
    }

    /**
     * 确认是否可以开始拍摄视频
     */
    private void checkTakeVideo() {
        /*if (videoCount >= VIDEO_LIMIT) {
            showVideoCountDialog();
        } else {
            VideoShootActivity.startActivityForResult(getActivity());
        }*/
        //进行视频录制
        VideoShootActivity.startActivityForResult(PhotoTakeActivity.this);
    }

    @Override
    public void onRetryUpload(VideoItem videoItem) {

    }

    @Override
    public void onVideoDeleted(int position, VideoItem videoItem) {

    }

    private void uploadFile(final VideoItem videoItem) {
        if (!NetworkUtil.isNetAvailable(PhotoTakeActivity.this)) {
            EasyAlertDialogHelper.showOneButtonDiolag(PhotoTakeActivity.this, null, getString(R.string.network_is_not_available),
                    getString(R.string.i_know), false, null);
        }

        doRealUpload(videoItem);
    }

    /**
     * 调用上传接口，上传视频
     */
    private void doRealUpload(VideoItem videoItem) {
        videoItem.setType(UploadType.SHORT_VIDEO);
        videoItem.setState(UploadState.STATE_WAIT);
        List<VideoItem> videoItemList = new ArrayList<>(1);
        videoItemList.add(videoItem);
//        UploadController.getInstance().uploadLocalItem(videoItemList, UploadType.SHORT_VIDEO, true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case VideoShootActivity.REQUEST_CODE:
                    VideoItem videoItem = (VideoItem) data.getSerializableExtra(VideoShootActivity.EXTRA_VIDEO_ITEM);
//                    uploadFile(videoItem);
                    String filePath = videoItem.getFilePath();
                    mFile = new File(filePath);
                    nosUpload = NOSUpload.getInstace(PhotoTakeActivity.this);
                    if (nosUpload != null) {
                        /** 这里的accid,token需要用户根据文档 http://dev.netease.im/docs/product/%E9%80%9A%E7%94%A8/%E7%82%B9%E6%92%AD%E9%80%9A%E7%94%A8/%E7%A7%BB%E5%8A%A8%E7%AB%AF%E4%B8%8A%E4%BC%A0%E4%BD%BF%E7%94%A8%E8%AF%B4%E6%98%8E
                         中的/app/vod/thirdpart/user/create 接口创建 **/
                        NOSUpload.Config config = new NOSUpload.Config();
                        config.appKey = "55f3fcee14db4682a11e1c633739d314";
                        config.accid = "test_accid_0505";
                        config.token = "b99d5baf7afd461e8b1ca747f112bee80854adf2";
                        nosUpload.setConfig(config);
                    }
                    uploadInit();
                    break;
               /* case VideoDetailInfoActivity.REQUEST_CODE:
                    VideoInfoEntity infoEntity = (VideoInfoEntity) data.getSerializableExtra(VideoDetailInfoActivity.EXTRA_VIDEO_ENTITY);
                    doRealDelete(getItemIndexByVid(infoEntity.getVid()), infoEntity);
                    break;*/
            }
        }
    }
    //测试上传
    private void uploadInit() {
        if (!NetworkUtil.isNetAvailable(PhotoTakeActivity.this)) {
            EasyAlertDialogHelper.showOneButtonDiolag(PhotoTakeActivity.this, null, getString(R.string.network_is_not_available),
                    getString(R.string.i_know), false, null);
            return;
        }
        if(mFile == null){
            Toast.makeText(PhotoTakeActivity.this, "please select file first!", Toast.LENGTH_SHORT).show();
            return;
        }
        String name = mFile.getName();

        nosUpload.fileUploadInit(name, null, -1, -1, null, null, -1, null, new NOSUploadHandler.UploadInitCallback() {
            @Override
            public void onSuccess(String nosToken, String bucket, String object) {
                mNosToken = nosToken;
                mBucket = bucket;
                mObject = object;
                Message msg = Message.obtain(mHandler, HandleMsg.MSG_INIT_SUCCESS);
                mHandler.sendMessage(msg);
            }

            @Override
            public void onFail(int code, String msg) {
                Message m = Message.obtain(mHandler, HandleMsg.MSG_INIT_FAIL);
                m.arg1 = code;
                m.obj = msg;
                mHandler.sendMessage(m);
            }
        });

    }

/*上传视频*/
    private void httpUpload() {

        if (!NetworkUtil.isNetAvailable(PhotoTakeActivity.this)) {
            EasyAlertDialogHelper.showOneButtonDiolag(PhotoTakeActivity.this, null, getString(R.string.network_is_not_available),
                    getString(R.string.i_know), false, null);
            return;
        }
        if(mFile == null){
            Toast.makeText(PhotoTakeActivity.this, "please select file first!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                {
                    String uploadContext = null;
                    /**
                     *  查看一个这个文件是否已经上传过，如果上传过就取出它的uploadcontext, 传给NosUpload.putFileByHttp进行断点续传
                     *  当uploadContext是null时, 就从头开始往上传
                     */
                    String tmp = nosUpload.getUploadContext(mFile);
                    if (tmp != null && !tmp.equals("")) {
                        uploadContext = tmp;
                    }
                    try {

                        executor = nosUpload.putFileByHttp(mFile,
                                uploadContext, mBucket, mObject, mNosToken, new NOSUploadHandler.UploadCallback() {
                                    @Override
                                    public void onUploadContextCreate(
                                            String oldUploadContext,
                                            String newUploadContext) {
                                        /**
                                         *  将新的uploadcontext保存起来
                                         */
                                        nosUpload.setUploadContext(mFile, newUploadContext);
                                    }

                                    @Override
                                    public void onProcess(long current, long total) {
                                        int pro = (int)((1.0f* current / total) * progressBar.getMax());
                                        progressBar.setProgress(pro);
                                         float v = 1.0f * current / total;
                                        /*转化为百分比，保留最后两位小数*/
                                        String result = String.format("%.2f",v*100);
                                        txtprogress.setText(result+"%");
                                    }

                                    @Override
                                    public void onSuccess(CallRet ret) {
                                        executor = null;
                                        /**
                                         *  清除该文件对应的uploadcontext
                                         */
                                        nosUpload.setUploadContext(mFile, "");
                                        Toast.makeText(PhotoTakeActivity.this, "upload success", Toast.LENGTH_SHORT).show();
                                        progressBar.setProgress(0);
                                        txtprogress.setText("");
                                    }

                                    @Override
                                    public void onFailure(CallRet ret) {
                                        executor = null;
                                        Toast.makeText(PhotoTakeActivity.this, "upload fail", Toast.LENGTH_SHORT).show();
                                        progressBar.setProgress(0);
                                    }

                                    @Override
                                    public void onCanceled(CallRet ret) {
                                        executor = null;
                                        Toast.makeText(PhotoTakeActivity.this, "upload cancel", Toast.LENGTH_SHORT).show();
//                                        progressBar.setProgress(0);
                                    }
                                });
                        executor.join();
                    } catch (Exception ex) {
                    }

                }
            }
        }).start();
    }
}
