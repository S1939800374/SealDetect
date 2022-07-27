package com.example.sealdetect;

import android.os.Handler;
import android.os.Looper;

import com.ejlchina.okhttps.HTTP;
import com.ejlchina.okhttps.gson.GsonMsgConvertor;

import java.nio.charset.StandardCharsets;

public class Const {
    //网络接口
    public static String baseurl = "http://192.168.0.161:8080";
    public static HTTP http = HTTP.builder()
            .baseUrl(Const.baseurl)
            .charset(StandardCharsets.UTF_8)
            .callbackExecutor((Runnable run) -> {
                // 实际编码中可以吧 Handler 提出来，不需要每次执行回调都重新创建
                new Handler(Looper.getMainLooper()).post(run); // 在主线程执行
            })
            .addMsgConvertor(new GsonMsgConvertor())
            .build();
}
