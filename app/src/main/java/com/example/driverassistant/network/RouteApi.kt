package com.example.driverassistant.network

import com.example.driverassistant.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface RouteApi {
    @POST("directions/v2:computeRoutes")
    suspend fun computeRoute(@Body body: Map<String, Any>): RouteResponse
}

data class RouteResponse(val routes: List<RouteItem> = emptyList())
data class RouteItem(val distanceMeters: Int = 0, val duration: String = "0s")

object RetrofitProvider {
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-Goog-Api-Key", BuildConfig.ROUTES_API_KEY)
            .addHeader("X-Goog-FieldMask", "routes.distanceMeters,routes.duration")
            .build()
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    val routeApi: RouteApi = Retrofit.Builder()
        .baseUrl("https://routes.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RouteApi::class.java)
}
