package ma.projet.restclient.config;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static String currentFormat = null;

    private static final String BASE_URL = "http://10.0.2.2:8082/";

    public static Retrofit getClient(String converterType) {
        if (retrofit == null || !converterType.equals(currentFormat)) {
            currentFormat = converterType;

            Retrofit.Builder builder = new Retrofit.Builder()
                    .baseUrl(BASE_URL);

            if ("JSON".equals(converterType)) {
                builder.addConverterFactory(GsonConverterFactory.create());
            } else if ("XML".equals(converterType)) {
                builder.addConverterFactory(SimpleXmlConverterFactory.createNonStrict());
            }

            retrofit = builder.build();
        }
        return retrofit;
    }
}