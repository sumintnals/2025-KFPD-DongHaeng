package com.kfpd_donghaeng_fe.data.remote.dto

import com.google.gson.annotations.SerializedName

// qr 인증 정보 조회 (시작,종료)

//1. 시작 qr 조회
//GET /api/matches/{match_id}/qr/start

//2.종료 qr 조회
//GET /api/matches/{match_id}/qr/end

data class QRDto(
    //@SerializedName("matchId") val matchId :Long,
    @SerializedName("qr_image_url") val qrCodeImg: String,
    //@SerializedName("qrData") val qrData:String,
    @SerializedName ("auth_type") val qrType : String,//start or end
    @SerializedName("scanned") val qrScanned: Boolean, // 스캔 여부

)



//qr 스캔 후
data class AfterQRScanDto(
    @SerializedName("qrCode")  val qrCode: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)



//qr 스캔응답 (시작, 종료)
//POST /api/qr/scan

data class QRScanResponseDto(
    @SerializedName("matchId") val matchId: Int,
    @SerializedName("authType") val authType: String, // start or end
    @SerializedName("scannedAt") val scannedAt: String,
    @SerializedName("status") val status: String, // ongoing or completed

    // --- '종료' 응답에만 있는 Nullable 필드 ---
    @SerializedName("actualDurationMinutes") val actualDurationMinutes: Int?,
    @SerializedName("earnedPoints") val earnedPoints: Int?,
    @SerializedName("earnedVolunteerMinutes") val earnedVolunteerMinutes: Int?
)








// 동행 시작(QR스캔)
data class QRScanRequest(
    @SerializedName("qrCode") val qrCode: String,      // 스캔한 QR 코드 문자열
    @SerializedName("latitude") val latitude: Double,   // 스캔한 위치 위도
    @SerializedName("longitude") val longitude: Double  // 스캔한 위치 경도
)