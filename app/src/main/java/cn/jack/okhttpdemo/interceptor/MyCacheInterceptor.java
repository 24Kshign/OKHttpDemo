package cn.jack.okhttpdemo.interceptor;

import android.util.Log;

import java.io.IOException;

import cn.jack.okhttp.okhttp3.CacheControl;
import cn.jack.okhttp.okhttp3.Interceptor;
import cn.jack.okhttp.okhttp3.Request;
import cn.jack.okhttp.okhttp3.Response;
import cn.jack.okio.app.LKUtil;

/**
 * Created by cyg on 2019-09-09.
 */
public class MyCacheInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        int max = 0;
        if (!LKUtil.isNetworkConnected()) {
            request = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build();
            Log.e("TAG", "no network");
        }
        Response response = chain.proceed(request);
        if (LKUtil.isNetworkConnected()) {
            response.newBuilder()
                    .header("Cache-Control", "public, max-age=" + max)
                    .removeHeader("Pragma")
                    .build();
        } else {
            max = 60 * 60 * 24; // 无网络时，设置超时为1天
            response.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=" + max)
                    .removeHeader("Pragma")
                    .build();
        }
        return response;
    }
}
