package messenger.messages.messaging.sms.chat.meet.ads;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WebApiClient {
    public static WebServices getInstance() {
        //http://ambtechapplication.com/api/ads/setting/get/resume-builder
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(httpLoggingInterceptor);  // <-- this is the important line!

        httpClient.readTimeout(180, TimeUnit.SECONDS);
        httpClient.connectTimeout(180, TimeUnit.SECONDS);
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        Retrofit mRetrofit = new Retrofit.Builder()
                .baseUrl("https://ambtechapplication.com/")
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return mRetrofit.create(WebServices.class);
    }
}
