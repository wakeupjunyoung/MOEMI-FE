package com.example.moemi.data.remote

import android.content.Context
import com.example.moemi.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/** MOEMI-BE 엔드포인트 (컨트롤러 기준) */
interface MoemiService {

    @GET("api/life-index/regions")
    suspend fun regions(): List<RegionResponse>

    /** from/to를 생략하면 서버가 오늘부터 예보 일수만큼 돌려준다. refresh=true면 캐시를 무시하고 기상청을 다시 조회한다. */
    @GET("api/life-index")
    suspend fun lifeIndex(
        @Query("regionCode") regionCode: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("refresh") refresh: Boolean = false,
    ): LifeIndexCalendarResponse

    /** 날짜(+일정)의 체크리스트를 돌려준다. 처음이면 생활수치 추천 항목을 넣어 만든다. */
    @POST("api/checklists")
    suspend fun openChecklist(@Body request: OpenChecklistRequest): ChecklistResponse

    @GET("api/checklists")
    suspend fun checklists(@Query("date") date: String): List<ChecklistResponse>

    @GET("api/checklists/{checklistId}")
    suspend fun checklist(@Path("checklistId") checklistId: Long): ChecklistResponse

    /** regionCode를 주면 그 지역으로 바꾼 뒤 추천을 다시 계산한다. */
    @POST("api/checklists/{checklistId}/recommendations")
    suspend fun refreshRecommendations(
        @Path("checklistId") checklistId: Long,
        @Query("regionCode") regionCode: String? = null,
    ): ChecklistResponse

    @POST("api/checklists/{checklistId}/items")
    suspend fun addItem(@Path("checklistId") checklistId: Long, @Body request: AddItemRequest): ChecklistResponse

    @PATCH("api/checklists/{checklistId}/items/{itemId}")
    suspend fun updateItem(
        @Path("checklistId") checklistId: Long,
        @Path("itemId") itemId: Long,
        @Body request: UpdateItemRequest,
    ): ChecklistResponse

    @DELETE("api/checklists/{checklistId}/items/{itemId}")
    suspend fun removeItem(@Path("checklistId") checklistId: Long, @Path("itemId") itemId: Long): ChecklistResponse

    /** radius 100~5000m. type은 생략하면 전체, 여러 개면 반복해서 보낸다. */
    @GET("api/hazards")
    suspend fun hazards(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int,
        @Query("type") types: List<String>? = null,
    ): HazardSearchResponse
}

object MoemiApi {
    private const val DEVICE_ID_HEADER = "X-Device-Id"

    internal val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** 로그인 기능이 생기기 전까지 체크리스트 주인을 구분하는 값. init 전에는 헤더 없이 보낸다. */
    @Volatile
    private var deviceId: String? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("moemi", Context.MODE_PRIVATE)
        deviceId = prefs.getString("device_id", null)
            ?: UUID.randomUUID().toString().also { prefs.edit().putString("device_id", it).apply() }
    }

    private val client = OkHttpClient.Builder()
        // Cloudtype 컨테이너가 깨어나는 데 시간이 걸릴 수 있어 넉넉히 둔다.
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
            deviceId?.let { request.header(DEVICE_ID_HEADER, it) }
            chain.proceed(request.build())
        }
        .build()

    val service: MoemiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create()
    }

    /** 화면에 보여줄 에러 문구. 서버가 보낸 ErrorResponse.message가 있으면 그대로 쓴다. */
    fun messageOf(error: Throwable): String = when (error) {
        is HttpException -> error.response()?.errorBody()?.string()
            ?.let { body -> runCatching { json.decodeFromString<ErrorResponse>(body).message }.getOrNull() }
            ?: "서버 오류가 발생했어요 (${error.code()})"
        is IOException -> "서버에 연결할 수 없어요. 네트워크를 확인해 주세요."
        else -> "알 수 없는 오류가 발생했어요."
    }
}
