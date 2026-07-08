package com.example.oclockbell

/**
 * 알람 기본값 단일 관리.
 *
 * ⚠️ 웹 constants.js 의 OCB.DEFAULT_START_HOUR / DEFAULT_END_HOUR 와 동일하게 유지할 것.
 * (Kotlin 이라 물리적으로 공유되지 않으므로 값이 어긋나지 않도록 수동 동기화)
 */
object AlarmDefaults {
    const val START_HOUR = 8    // 알람 활성 시작 시각 (08:00)
    const val END_HOUR   = 22   // 알람 활성 종료 시각 (22:00, 미포함)
}
