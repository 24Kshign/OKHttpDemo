package cn.jack.okhttpdemo.interceptor;

import android.util.Log;

import java.io.IOException;

import cn.jack.okhttp.okhttp3.Interceptor;
import cn.jack.okhttp.okhttp3.Request;
import cn.jack.okhttp.okhttp3.Response;

public class MyRetryInterceptor implements Interceptor {

    public int maxRetry;//最大重试次数
    private int retryNum = 0;//假如设置为3次重试的话，则最大可能请求4次（默认1次+3次重试）

    public MyRetryInterceptor(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response;
        while (true) {
            Log.e("TAG", "retryNum--->" + retryNum);
            try {
                response = chain.proceed(request);
                return response;
            } catch (Exception e) {
                if (++retryNum > maxRetry) {
                    Log.e("TAG", "e--->" + e.getMessage());
                    throw e;
                }
            }
        }
    }
}
