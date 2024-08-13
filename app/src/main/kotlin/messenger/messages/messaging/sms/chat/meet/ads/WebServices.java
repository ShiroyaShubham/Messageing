package messenger.messages.messaging.sms.chat.meet.ads;


import messenger.messages.messaging.sms.chat.meet.model.AdModel;
import retrofit2.Call;
import retrofit2.http.GET;

public interface WebServices {

    @GET("codzyer/api/ads/setting/get/Messages:%20Text%20SMS%20&%20Chat%20App")
    Call<AdModel> GetAdvertisement();
}
