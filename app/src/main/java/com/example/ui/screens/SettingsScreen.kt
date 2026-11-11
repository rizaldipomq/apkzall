package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.preferences.BrowserPreferences
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ZallBackground
import com.example.ui.theme.ZallCard
import com.example.ui.theme.ZallCardBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: BrowserPreferences,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    var opacity by remember { mutableFloatStateOf(preferences.opacity) }
    var windowWidth by remember { mutableIntStateOf(preferences.windowWidthDp) }
    var windowHeight by remember { mutableIntStateOf(preferences.windowHeightDp) }

    var stealthSecure by remember { mutableStateOf(preferences.stealthSecure) }
    var stealthNonFocus by remember { mutableStateOf(preferences.stealthNonFocus) }

    var bubbleSnap by remember { mutableStateOf(preferences.bubbleSnap) }
    var bubbleSize by remember { mutableIntStateOf(preferences.bubbleSizeDp) }
    var startAsBubble by remember { mutableStateOf(preferences.startAsBubble) }

    var searchEngine by remember { mutableStateOf(preferences.searchEngine) }
    var homeUrl by remember { mutableStateOf(preferences.homeUrl) }
    var desktopMode by remember { mutableStateOf(preferences.desktopMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZallBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Pengaturan Browser",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = ElectricBlue
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ZallBackground
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Tampilan Jendela
            item {
                SettingsSectionTitle("TAMPILAN JENDELA")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ZallCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZallCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Transparansi Jendela", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("${(opacity * 100).toInt()}%", color = ElectricBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = opacity,
                            onValueChange = {
                                opacity = it
                                preferences.opacity = it
                            },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricBlue,
                                activeTrackColor = ElectricBlue,
                                inactiveTrackColor = Color(0xFF223656)
                            ),
                            modifier = Modifier.testTag("opacity_slider")
                        )

                        Text(
                            text = "Transparansi rendah membantu melihat musuh atau peta saat bermain game.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Ukuran Jendela Default", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetSizeChip("Ringkas", 300, 420, windowWidth, windowHeight) { w, h ->
                                windowWidth = w
                                windowHeight = h
                                preferences.windowWidthDp = w
                                preferences.windowHeightDp = h
                            }
                            PresetSizeChip("Standar", 340, 480, windowWidth, windowHeight) { w, h ->
                                windowWidth = w
                                windowHeight = h
                                preferences.windowWidthDp = w
                                preferences.windowHeightDp = h
                            }
                            PresetSizeChip("Lebar", 380, 560, windowWidth, windowHeight) { w, h ->
                                windowWidth = w
                                windowHeight = h
                                preferences.windowWidthDp = w
                                preferences.windowHeightDp = h
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Lebar: ${windowWidth}dp | Tinggi: ${windowHeight}dp", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            // 2. Anti-Deteksi & Mode Siluman
            item {
                SettingsSectionTitle("KEAMANAN & ANTI-DETEKSI")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ZallCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZallCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Perisai Layar (FLAG_SECURE)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Mencegah aplikasi ujian (Exambrowser) dan perekam layar mendeteksi isi browser.",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = stealthSecure,
                                onCheckedChange = {
                                    stealthSecure = it
                                    preferences.stealthSecure = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricBlue
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fokus Non-Intrusif", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Mencegah game mengalami pause saat browser mengambang terbuka.",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = stealthNonFocus,
                                onCheckedChange = {
                                    stealthNonFocus = it
                                    preferences.stealthNonFocus = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricBlue
                                )
                            )
                        }
                    }
                }
            }

            // 3. Floating Bubble Settings
            item {
                SettingsSectionTitle("PENGATURAN FLOATING BUBBLE")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ZallCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZallCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Menempel ke Tepi Layar", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Bubble otomatis menempel ke tepi kiri atau kanan.", color = TextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = bubbleSnap,
                                onCheckedChange = {
                                    bubbleSnap = it
                                    preferences.bubbleSnap = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricBlue
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mulai Otomatis Sebagai Bubble", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Saat tombol mulai ditekan, langsung buka bubble kecil.", color = TextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = startAsBubble,
                                onCheckedChange = {
                                    startAsBubble = it
                                    preferences.startAsBubble = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricBlue
                                )
                            )
                        }
                    }
                }
            }

            // 4. Browser Preferences
            item {
                SettingsSectionTitle("PREFERENSI BROWSER")

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ZallCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZallCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mesin Pencari Default", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Google", "DuckDuckGo", "Bing").forEach { engine ->
                                val isSelected = engine == searchEngine
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF00384D) else Color(0xFF162035))
                                        .border(1.dp, if (isSelected) ElectricBlue else ZallCardBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            searchEngine = engine
                                            preferences.searchEngine = engine
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = engine,
                                        color = if (isSelected) ElectricBlue else TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Halaman Beranda (Home URL)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = homeUrl,
                            onValueChange = {
                                homeUrl = it
                                preferences.homeUrl = it
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = ZallCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = Color(0xFF0B101D),
                                unfocusedContainerColor = Color(0xFF0B101D)
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Desktop Mode Default", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Meminta tampilan situs desktop secara default.", color = TextMuted, fontSize = 11.sp)
                            }
                            Switch(
                                checked = desktopMode,
                                onCheckedChange = {
                                    desktopMode = it
                                    preferences.desktopMode = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricBlue
                                )
                            )
                        }
                    }
                }
            }

            // 5. Reset Settings Button
            item {
                Button(
                    onClick = {
                        preferences.opacity = 0.95f
                        preferences.windowWidthDp = 340
                        preferences.windowHeightDp = 480
                        preferences.homeUrl = "https://www.google.com"
                        preferences.searchEngine = "Google"
                        preferences.desktopMode = false
                        preferences.stealthSecure = true
                        preferences.stealthNonFocus = true
                        preferences.bubbleSnap = true
                        preferences.startAsBubble = false

                        opacity = 0.95f
                        windowWidth = 340
                        windowHeight = 480
                        stealthSecure = true
                        stealthNonFocus = true
                        bubbleSnap = true
                        startAsBubble = false
                        searchEngine = "Google"
                        homeUrl = "https://www.google.com"
                        desktopMode = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E293B),
                        contentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kembalikan Pengaturan ke Default")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = NeonCyan,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun PresetSizeChip(
    label: String,
    w: Int,
    h: Int,
    currentW: Int,
    currentH: Int,
    onSelect: (Int, Int) -> Unit
) {
    val isSelected = currentW == w && currentH == h
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF00384D) else Color(0xFF162035))
            .border(1.dp, if (isSelected) ElectricBlue else ZallCardBorder, RoundedCornerShape(10.dp))
            .clickable { onSelect(w, h) }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) ElectricBlue else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
