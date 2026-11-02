package com.example.moemi.data.remote

import com.example.moemi.data.DayAvailability
import com.example.moemi.data.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** MOEMI-BE 응답 모양(record 필드, ISO 날짜 문자열)을 앱 DTO가 그대로 읽는지 확인한다. */
class ApiModelsTest {

    private val json = MoemiApi.json

    @Test
    fun lifeIndexCalendar() {
        val body = """
            {"regionCode":"1233000000","regionName":"광주 광산구","status":"OK","source":"기상청 생활기상지수 조회서비스",
             "fetchedAt":"2026-10-27T14:03:12",
             "days":[
               {"date":"2026-10-27","availability":"AVAILABLE","overallLevel":"CAUTION","indices":[
                 {"type":"UV","name":"자외선지수","value":8,"unit":null,"grade":"매우높음","level":"CAUTION","issuedAt":"2026-10-27T06:00:00"},
                 {"type":"AIR_STAGNATION","name":"대기정체지수","value":37.5,"unit":null,"grade":"보통","level":"NORMAL","issuedAt":"2026-10-27T06:00:00"}]},
               {"date":"2026-10-31","availability":"NO_DATA","overallLevel":null,"indices":[]}]}
        """.trimIndent()

        val res = json.decodeFromString<LifeIndexCalendarResponse>(body)

        assertEquals(LifeIndexStatus.OK, res.status)
        assertEquals(Level.CAUTION, res.days[0].overallLevel)
        assertEquals(8.0, res.days[0].indices[0].value, 0.0)
        assertEquals(37.5, res.days[0].indices[1].value, 0.0)
        assertEquals(DayAvailability.NO_DATA, res.days[1].availability)
        assertNull(res.days[1].overallLevel)
    }

    @Test
    fun checklist() {
        val body = """
            {"id":3,"date":"2026-10-27","scheduleId":1,"regionCode":"1233000000","recommendationStatus":"OK",
             "recommendedAt":"2026-10-27T14:05:00","totalCount":2,"checkedCount":1,"remainingCount":1,
             "items":[{"id":10,"name":"선크림","checked":true,"source":"RECOMMENDED","reason":"자외선 높음"},
                      {"id":11,"name":"노트북","checked":false,"source":"CUSTOM","reason":null}]}
        """.trimIndent()

        val res = json.decodeFromString<ChecklistResponse>(body)

        assertEquals(ItemSource.RECOMMENDED, res.items[0].source)
        assertNull(res.items[1].reason)
    }

    @Test
    fun hazardSearch() {
        val body = """
            {"status":"OK","source":"국가교통정보센터 돌발상황정보","fetchedAt":"2026-10-27T14:03:00","radiusM":2000,
             "hazards":[{"id":"a1b2","type":"CONSTRUCTION","state":"ACTIVE","title":"하남대로 도로포장","description":"도로포장 작업",
               "lat":35.1921,"lng":126.8189,"distanceM":350,"startAt":"2026-10-20T09:00:00","endAt":null,"roadName":"하남대로",
               "direction":null,"blockType":null,"blockedLanes":"1차로","sourceEventType":"공사","sourceEventDetailType":"도로포장"}]}
        """.trimIndent()

        val res = json.decodeFromString<HazardSearchResponse>(body)

        assertEquals(HazardState.ACTIVE, res.hazards[0].state)
        assertNull(res.hazards[0].endAt)
    }

    @Test
    fun partialUpdateSendsOnlyChangedField() {
        assertEquals("""{"checked":true}""", json.encodeToString(UpdateItemRequest(checked = true)))
    }
}
