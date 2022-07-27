package com.example.sealdetect;

import static com.yalantis.ucrop.util.FileUtils.getDataColumn;
import static com.yalantis.ucrop.util.FileUtils.isDownloadsDocument;
import static com.yalantis.ucrop.util.FileUtils.isExternalStorageDocument;
import static com.yalantis.ucrop.util.FileUtils.isMediaDocument;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.net.UriKt;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.ejlchina.data.Array;
import com.ejlchina.data.Mapper;
import com.ejlchina.okhttps.AHttpTask;
import com.ejlchina.okhttps.HttpResult;
import com.luck.picture.lib.basic.PictureSelectionModel;
import com.luck.picture.lib.basic.PictureSelector;
import com.luck.picture.lib.config.SelectMimeType;
import com.luck.picture.lib.decoration.GridSpacingItemDecoration;
import com.luck.picture.lib.engine.ImageEngine;
import com.luck.picture.lib.engine.UriToFileTransformEngine;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.utils.DensityUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import okio.Okio;

public class MainActivity extends AppCompatActivity {

    private final int maxSelectNum = 6;//最多上传6张
    private GridImageAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private ImageEngine imageEngine;
    private ActivityResultLauncher<Intent> launcherResult,launcherSelector;
    private ArrayList<String> picturepath_list;
    private String pdf_path;
    private TextView file_name;
    private File pdf_file;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        picturepath_list = new ArrayList<>();
        file_name = findViewById(R.id.file_name);
        mRecyclerView = findViewById(R.id.deliver);
        imageEngine = GlideEngine.createGlideEngine();
        //Adapter
        mAdapter = new GridImageAdapter(this, new ArrayList<>());
        mAdapter.setSelectMax(maxSelectNum);
        if (savedInstanceState != null && savedInstanceState.getParcelableArrayList("selectorList") != null) {
            mAdapter.getData().clear();
            mAdapter.getData().addAll(savedInstanceState.getParcelableArrayList("selectorList"));
        }

        //设置页面添加图片
        FullyGridLayoutManager manager = new FullyGridLayoutManager(this,
                4, GridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);
        RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        if (itemAnimator != null) {
            ((SimpleItemAnimator) itemAnimator).setSupportsChangeAnimations(false);
        }
        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(4,
                DensityUtil.dip2px(this, 8), false));
        mRecyclerView.setAdapter(mAdapter);
        if (savedInstanceState != null && savedInstanceState.getParcelableArrayList("selectorList") != null) {
            mAdapter.getData().clear();
            mAdapter.getData().addAll(savedInstanceState.getParcelableArrayList("selectorList"));
        }

        launcherResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        int resultCode = result.getResultCode();
                        if (resultCode == RESULT_OK) {
                            ArrayList<LocalMedia> selectList = PictureSelector.obtainSelectorList(result.getData());
                            mAdapter.getData().clear();
                            mAdapter.getData().addAll(selectList);
                            mAdapter.notifyDataSetChanged();
                            picturepath_list.clear();
                            for(LocalMedia picture:mAdapter.getData()){
                                picturepath_list.add(picture.getRealPath());
                            }
                            System.out.println("done");
                        } else if (resultCode == RESULT_CANCELED) {
                            System.out.println("onActivityResult PictureSelector Cancel");
                        }
                    }
                });

        mAdapter.setOnItemClickListener(new GridImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                // 预览图片、视频、音频
                PictureSelector.create(MainActivity.this)
                        .openPreview()
                        .setImageEngine(imageEngine)
                        .startActivityPreview(position, true, mAdapter.getData());
            }

            @Override
            public void openPicture() {
                // 进入相册
                PictureSelectionModel selectionModel = PictureSelector.create(MainActivity.this)
                        .openGallery(SelectMimeType.ofImage())
                        .setMaxSelectNum(maxSelectNum)
                        .isWithSelectVideoImage(true)
                        .isPreviewImage(true)
                        .setImageEngine(imageEngine);
                selectionModel.forResult(launcherResult);
            }

        });

        launcherSelector = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode()==RESULT_OK) {
                            Uri pdf_uri = Uri.parse(result.getData().getData().toString());
                            System.out.println(pdf_uri.toString());
                            try {
                                pdf_file = FileUtil.from(MainActivity.this,pdf_uri);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            pdf_path = pdf_file.getAbsolutePath();
                            System.out.println(pdf_path);
                            file_name.setText(pdf_path);
                        }
                    }
                });
        //上传文件
        findViewById(R.id.select_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 调用系统自带的文件选择器
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/pdf");
                launcherSelector.launch(intent);
            }
        });
        //处理图片
        findViewById(R.id.deal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("上传");
                AHttpTask task = Const.http.async("/upload");
                task.bodyType("application/x-www-form-urlencoded");
                //图片参数
                //图片
                int i= 0;
                String name;
                for(String picture_path:picturepath_list){
                    name = "img_"+i;
                    i+=1;
                    task.addFilePara(name,picture_path);
                }
                System.out.println(pdf_file);
                if(pdf_file!=null){
                    task.addFilePara("pdf",pdf_file.getAbsoluteFile());
                }
                task.setOnResponse(httpResult -> {
                    System.out.println("OK");
                    HttpResult.Body body = httpResult.getBody();
                    Mapper content_mapper = body.toMapper();
                    System.out.println(content_mapper.toString());
                    Array array = content_mapper.getArray("data");
                    System.out.println(array);
                    Intent post_intent = new Intent(MainActivity.this,ResultActivity.class);
                    post_intent.putExtra("data",array.toString());
                    startActivity(post_intent);
                });
                task.post();
            }
        });

    }
}