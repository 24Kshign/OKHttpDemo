package cn.jack.okhttpdemo;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import com.google.gson.Gson;
import com.squareup.retrofit2.converter.GsonConverterFactory;
import com.squareup.retrofit2.retrofit.Retrofit;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import cn.jack.okhttp.okhttp3.Cache;
import cn.jack.okhttp.okhttp3.Call;
import cn.jack.okhttp.okhttp3.Callback;
import cn.jack.okhttp.okhttp3.OkHttpClient;
import cn.jack.okhttp.okhttp3.Request;
import cn.jack.okhttp.okhttp3.Response;
import cn.jack.okhttpdemo.api.ApiService;
import cn.jack.okhttpdemo.bean.UserInfo;
import cn.jack.okhttpdemo.interceptor.MyCacheInterceptor;
import cn.jack.okhttpdemo.interceptor.MyRetryInterceptor;
import cn.jack.okhttpdemo.interceptor.ResponseCacheInterceptor;
import cn.jack.okhttpdemo.util.UnicodeUtil;

public class MainActivity extends FragmentActivity {

    private TextView tvRequest;
    private TextView tvResult;
    private Cache cache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvRequest = findViewById(R.id.am_tv_request);
        tvResult = findViewById(R.id.am_tv_result);
        tvRequest.setOnClickListener(v -> {
            requestData();
//            requestRetrofitData();
        });

        //缓存文件夹
        File cacheFile = new File(Objects.requireNonNull(getExternalCacheDir()).toString(), "demoCache");
        //缓存大小为10M
        int cacheSize = 10 * 1024 * 1024;
        //创建缓存对象
        cache = new Cache(cacheFile, cacheSize);
    }

    private void requestRetrofitData() {
        Retrofit retrofit = new Retrofit.Builder()
                .client(new OkHttpClient())
                .baseUrl("http://192.168.1.197:8888/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService service = retrofit.create(ApiService.class);

        service.getUserInfo().enqueue(new com.squareup.retrofit2.retrofit.Callback<UserInfo>() {
            @Override
            public void onResponse(com.squareup.retrofit2.retrofit.Call<UserInfo> call, com.squareup.retrofit2.retrofit.Response<UserInfo> response) {
                tvResult.setText(new Gson().toJson(response.body()));
            }

            @Override
            public void onFailure(com.squareup.retrofit2.retrofit.Call<UserInfo> call, Throwable t) {
                tvResult.setText(t.getMessage());
            }
        });
    }

    private void requestData() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new MyCacheInterceptor())
                .cache(cache)
                .build();
        Request request = new Request.Builder()
                .url("http://192.168.0.130:8080/userInfo")
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    tvResult.setText(e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    try {
                        tvResult.setText(UnicodeUtil.decodeUnicode(response.body().string()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }
}
