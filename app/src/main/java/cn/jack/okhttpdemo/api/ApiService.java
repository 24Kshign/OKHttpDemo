package cn.jack.okhttpdemo.api;

import com.squareup.retrofit2.retrofit.Call;
import com.squareup.retrofit2.retrofit.http.GET;

import cn.jack.okhttpdemo.bean.UserInfo;

/**
 * Created by cyg on 2019-08-28.
 */
public interface ApiService {

    @GET("userInfo")
    Call<UserInfo> getUserInfo();
}