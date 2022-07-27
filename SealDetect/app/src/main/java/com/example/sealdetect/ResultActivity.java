package com.example.sealdetect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;

import com.ejlchina.data.Array;
import com.ejlchina.data.DataSet;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;
    private ImgAdapter adapter;
    private ArrayList<Img> imgs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        imgs = new ArrayList<Img>();
        mRecyclerView = findViewById(R.id.result_recyclerview);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(ResultActivity.this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(ResultActivity.this,DividerItemDecoration.VERTICAL));
        Intent intent = getIntent();
        String array = intent.getStringExtra("data");
        try {
            JSONArray jsonArray = new JSONArray(array);
            System.out.println(jsonArray.toString());
            for (int i=0;i<jsonArray.length();i++){
                Img img = Img.jsonToObject(jsonArray.getJSONObject(i));
                imgs.add(img);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        adapter = new ImgAdapter(ResultActivity.this,imgs);
        mRecyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
}