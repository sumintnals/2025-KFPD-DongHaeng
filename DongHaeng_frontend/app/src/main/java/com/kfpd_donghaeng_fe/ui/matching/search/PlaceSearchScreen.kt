package com.kfpd_donghaeng_fe.ui.matching.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel



import com.kfpd_donghaeng_fe.domain.entity.PlaceSearchResult
import com.kfpd_donghaeng_fe.ui.theme.AppColors
import com.kfpd_donghaeng_fe.viewmodel.matching.PlaceSearchViewModel
import com.kfpd_donghaeng_fe.R
import com.kfpd_donghaeng_fe.domain.entity.LocationType
import com.kfpd_donghaeng_fe.domain.entity.toRouteLocation
import com.kfpd_donghaeng_fe.ui.matching.components.HomeCompanyTag

/**
 * 재사용 가능한 장소 검색 화면
 * @param searchType "도착지" 또는 "경유지"
 * @param onPlaceSelected 장소 선택 시 콜백
 * @param onBackPressed 뒤로가기 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceSearchScreen(
    searchType: String, // "출발지" or "도착지"
    onPlaceSelected: (PlaceSearchResult) -> Unit,
    onBackPressed: () -> Unit,
    viewModel: PlaceSearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchHistories by viewModel.searchHistories.collectAsState()

    val itemClickAction: (PlaceSearchResult) -> Unit = { place ->
        viewModel.addToHistory(place)
        viewModel.setDetailPlace(place) // 1. 상세 정보(State) 업데이트 (지도 마커 표시용)
        onPlaceSelected(place)          // 2. 부모에게 "클릭됨" 알림 (핵심!)
    }

    // 💡 이미지와 동일하게 Full Screen Search UI 구성
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Spacer(modifier = Modifier.height(16.dp))
        // 1. 상단 검색바/네비게이션 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp) // TopAppBar 높이
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(10.dp),
                        clip = false
                    )
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = AppColors.PrimaryDarkText
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 뒤로가기 아이콘 (텍스트필드 안에)
                            IconButton(
                                onClick = onBackPressed,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_chevron_left),
                                    contentDescription = "뒤로가기",
                                    tint = AppColors.PrimaryDarkText,
                                    modifier = Modifier.size(11.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                // Placeholder
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "장소, 버스, 지하철, 주소 검색",
                                        fontSize = 16.sp,
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                // 실제 입력 필드
                                innerTextField()
                            }

                            // 검색 아이콘 (텍스트필드 안에)
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "검색",
                                tint = AppColors.SecondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 우측 주황색 아이콘 (ic_send)
            IconButton(onClick = { /* 검색 실행 또는 다른 액션 */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_go),
                    contentDescription = "전송",
                    tint = AppColors.AccentColor, // 주황색,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // 2. 홈/회사 태그 및 최근 검색
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // 홈/회사 버튼 (PathInputBox에서 재사용)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeCompanyTag("집", R.drawable.ic_home)
                HomeCompanyTag("회사", R.drawable.ic_company)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 검색 결과 또는 히스토리
            if (searchQuery.isBlank()) {
                // 히스토리 표시
                if (searchHistories.isNotEmpty()) {
                    Text(
                        text = "최근 검색어",
                        fontSize = 14.sp,
                        color = AppColors.SecondaryText,
                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                    )
                    LazyColumn {
                        items(searchHistories) { place ->
                            HistoryItem(place = place, onClick = { itemClickAction(place) })
                        }
                    }
                }
            } else {
                // 검색 결과
                if (isLoading) {
                    // ... 로딩 인디케이터
                } else if (searchResults.isEmpty()) {
                    Text(
                        text = "검색 결과가 없습니다",
                        fontSize = 14.sp,
                        color = AppColors.SecondaryText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // 검색 결과 리스트
                    LazyColumn {
                        items(searchResults) { place ->
                            PlaceItem(place = place, onClick = { itemClickAction(place) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceItem(
    place: PlaceSearchResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = place.placeName,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.PrimaryDarkText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (place.roadAddressName.isNotEmpty())
                place.roadAddressName
            else
                place.addressName,
            fontSize = 14.sp,
            color = AppColors.SecondaryText
        )
        if (place.categoryName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = place.categoryName.split(">").lastOrNull()?.trim() ?: "",
                fontSize = 12.sp,
                color = AppColors.SecondaryText.copy(alpha = 0.7f)
            )
        }
    }
    Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
}

@Composable
fun HistoryItem(
    place: PlaceSearchResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 핀 아이콘
            Icon(
                painter = painterResource(id = R.drawable.ic_pin),
                contentDescription = "위치 핀",
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 장소명
            Text(
                text = place.placeName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.PrimaryDarkText
            )
        }

        // 구분선
        Divider(
            color = Color(0xFFE0E0E0),
            thickness = 1.dp
        )
    }
}