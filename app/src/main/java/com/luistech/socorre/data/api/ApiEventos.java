package com.luistech.socorre.data.api;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;


public interface ApiEventos {

    @POST
    Call<ResponseBody> registrar(@Url String url, @Body RequestBody corpo);
}
