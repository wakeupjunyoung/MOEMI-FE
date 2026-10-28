package com.example.moemi.data

import com.example.moemi.data.remote.HazardSearchResponse
import com.example.moemi.data.remote.MoemiApi

/** WGS84 좌표 */
data class GeoPoint(val lat: Double, val lng: Double)

/** 주변 위험 정보(국가교통정보센터 돌발상황)를 MOEMI-BE(/api/hazards)를 거쳐 가져온다. */
object HazardRepository {
    /** 위치 권한·주소 검색을 붙이기 전까지 쓰는 기준 위치 (광주 광산구 수완동 부근) */
    val DEFAULT_CENTER = GeoPoint(35.19, 126.82)

    suspend fun search(center: GeoPoint, radiusM: Int): HazardSearchResponse =
        MoemiApi.service.hazards(center.lat, center.lng, radiusM)
}
