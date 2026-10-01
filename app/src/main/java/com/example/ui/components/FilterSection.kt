package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NetworkFilterType
import com.example.data.model.SortOption
import com.example.data.model.TimeRangeFilter
import com.example.ui.theme.BorderGray
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.GoogleUIBlue
import com.example.ui.theme.LocalGoogleColors

@Composable
fun FilterSection(
    selectedTimeRange: TimeRangeFilter,
    onTimeRangeSelected: (TimeRangeFilter) -> Unit,
    selectedNetworkFilter: NetworkFilterType,
    onNetworkFilterSelected: (NetworkFilterType) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val googleColors = LocalGoogleColors.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Time Range Selector (Horizontal scrolling chips)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeRangeFilter.entries.forEach { range ->
                val isSelected = range == selectedTimeRange
                Box(
                    modifier = Modifier
                        .testTag("time_chip_${range.name.lowercase()}")
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) googleColors.infoBg else MaterialTheme.colorScheme.surface)
                        .clickable { onTimeRangeSelected(range) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) GoogleUIBlue else googleColors.darkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Network Type Tab Row (All, Cellular, Wi-Fi)
        TabRow(
            selectedTabIndex = selectedNetworkFilter.ordinal,
            containerColor = Color.Transparent,
            contentColor = GoogleUIBlue,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedNetworkFilter.ordinal]),
                    color = GoogleUIBlue,
                    height = 3.dp
                )
            }
        ) {
            NetworkFilterType.entries.forEach { filter ->
                val isSelected = filter == selectedNetworkFilter
                Tab(
                    selected = isSelected,
                    onClick = { onNetworkFilterSelected(filter) },
                    modifier = Modifier.testTag("network_tab_${filter.name.lowercase()}"),
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (filter) {
                                NetworkFilterType.ALL -> {}
                                NetworkFilterType.MOBILE -> {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCell,
                                        contentDescription = null,
                                        tint = if (isSelected) GoogleGreen else googleColors.mediumGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                NetworkFilterType.WIFI -> {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = if (isSelected) GoogleBlue else googleColors.mediumGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }
                            Text(
                                text = filter.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) GoogleUIBlue else googleColors.mediumGray
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Search Bar + Sort Action Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_apps_input"),
                placeholder = {
                    Text(
                        text = "Search apps, packages, UIDs...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = googleColors.mediumGray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = googleColors.mediumGray,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = googleColors.mediumGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoogleUIBlue,
                    unfocusedBorderColor = BorderGray,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Sort Dropdown Box
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { sortMenuExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = GoogleUIBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (selectedSort) {
                                SortOption.TOTAL_DESC -> "Most Data"
                                SortOption.RX_DESC -> "Download"
                                SortOption.TX_DESC -> "Upload"
                                SortOption.NAME_ASC -> "A-Z"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GoogleUIBlue
                        )
                    }
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (option == selectedSort) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == selectedSort) GoogleUIBlue else googleColors.darkGray
                                )
                            },
                            onClick = {
                                onSortSelected(option)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
