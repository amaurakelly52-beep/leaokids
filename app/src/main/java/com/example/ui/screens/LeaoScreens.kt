package com.example.ui.screens

import android.accounts.AccountManager
import android.app.Activity
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.graphics.Bitmap
import android.widget.Toast
import java.util.regex.Pattern
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ChildProfile
import com.example.data.KidVideo
import com.example.ui.LeaoScreen
import com.example.ui.LeaoViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- Branded Theme Colors ---
val PrimaryOrange = Color(0xFFFF7A00)
val PrimaryOrangeDark = Color(0xFF994700)
val SecondaryBlue = Color(0xFF0061a4)
val SecondaryBlueContainer = Color(0xFF33a0fd)
val LightSurface = Color(0xFFFFF8F5)
val LightSurfaceDim = Color(0xFFEDD5CA)
val TertiaryYellow = Color(0xFFFABD00)
val AccentPink = Color(0xFFFFD1DC)
val AccentLilac = Color(0xFFE6E6FA)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LeaoMainContent(viewModel: LeaoViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LightSurface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width / 2 } + fadeIn() with
                            slideOutHorizontally { width -> -width / 2 } + fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    LeaoScreen.Splash -> SplashScreen(viewModel)
                    LeaoScreen.ProfileSelection -> ProfileSelectionScreen(viewModel)
                    LeaoScreen.ChildHome -> ChildHomeScreen(viewModel)
                    LeaoScreen.Player -> VideoPlayerScreen(viewModel)
                    LeaoScreen.ParentGate -> ParentGateScreen(viewModel)
                    LeaoScreen.ParentDashboard -> ParentDashboardScreen(viewModel)
                    LeaoScreen.PianoGame -> PianoGameScreen(viewModel)
                    LeaoScreen.PuzzleGame -> PuzzleGameScreen(viewModel)
                    LeaoScreen.LimitsExceeded -> TimeoutWarningScreen(viewModel)
                }
            }
        }
    }
}

// --- Dynamic Button Style: Physical "Squishy" depress effect ---
@Composable
fun SquishyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PrimaryOrange,
    shadowColor: Color = PrimaryOrangeDark,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    verticalPadding: androidx.compose.ui.unit.Dp = 18.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 32.dp,
    testTag: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val translationY by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "PressDepress"
    )

    val shadowHeight by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        label = "ShadowDepress"
    )

    Box(
        modifier = modifier
            .padding(bottom = 6.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        propagateMinConstraints = true
    ) {
        // Bottom Elevation Shadow block
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shadowHeight)
                .background(shadowColor, shape)
        )

        // Tonal Front Face Block
        Row(
            modifier = Modifier
                .offset(y = translationY)
                .background(backgroundColor, shape)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// --- 01. SPLASH SCREEN ---
@Composable
fun SplashScreen(viewModel: LeaoViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "FloatMascot")

    val bounceAnim by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BounceY"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFEADF), LightSurface),
                    radius = 1200f
                )
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Center Identity Block
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = bounceAnim.dp)
        ) {
            // Sunburst background visual decoration
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color(0x33FFB68B), CircleShape)
                )

                // High quality logo illustration
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Leão Kids Logo",
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(180.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text display name
            Text(
                text = "Leão Kids",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryOrange,
                textAlign = TextAlign.Center,
                lineHeight = 48.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Aventura e diversão no lugar mais seguro da internet!",
                fontSize = 18.sp,
                color = Color(0xFF584235),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(280.dp),
                lineHeight = 24.sp
            )
        }

        // Action block
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            SquishyButton(
                onClick = { viewModel.navigateTo(LeaoScreen.ProfileSelection) },
                backgroundColor = PrimaryOrange,
                shadowColor = PrimaryOrangeDark,
                modifier = Modifier.fillMaxWidth(0.95f),
                testTag = "comecar_button"
            ) {
                Text(
                    text = "Começar",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Proteção Parental Ativa",
                    tint = SecondaryBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AMBIENTE 100% MONITORADO PELOS PAIS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryBlue,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// --- 02. PROFILE SELECTION SCREEN ---
@Composable
fun ProfileSelectionScreen(viewModel: LeaoViewModel) {
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var inputProfileName by remember { mutableStateOf("") }
    var selectedBoyOption by remember { mutableStateOf(true) } // default true-boy
    var selectedAvatarUrl by remember { mutableStateOf(viewModel.availableAvatars.first().first) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(48.dp))

                Text(
                    text = "Quem vai brincar?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF251912)
                )

                // Top Parental lock key config
                IconButton(
                    onClick = { viewModel.prepareParentGate(LeaoScreen.ParentDashboard) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFFEADF), CircleShape)
                        .testTag("parental_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Configuração dos Pais",
                        tint = PrimaryOrange
                    )
                }
            }

            // Central Profiles Grid
            if (profiles.isEmpty()) {
                // If empty profiles, friendly guide empty state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Face,
                        contentDescription = "Sem Perfis",
                        tint = Color(0x33994700),
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Não há perfil cadastrado!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF584235)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    profiles.forEach { profile ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.92f else 1.0f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
                            label = "ProfilePressScale"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .scale(scale)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { viewModel.selectProfileAndNavigate(profile) }
                                )
                                .testTag("profile_item_${profile.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .background(
                                            if (profile.isBoy) SecondaryBlueContainer else AccentPink,
                                            CircleShape
                                        )
                                        .border(
                                            width = 6.dp,
                                            color = if (profile.isBoy) SecondaryBlue else Color(0xFFFF69B4),
                                            shape = CircleShape
                                        )
                                        .padding(8.dp)
                                ) {
                                    AsyncImage(
                                        model = profile.avatarUrl,
                                        contentDescription = profile.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                // Theme Indicator Badges (Star / Heart)
                                Icon(
                                    imageVector = if (profile.isBoy) Icons.Filled.Star else Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (profile.isBoy) SecondaryBlue else Color(0xFFFF69B4),
                                            CircleShape
                                        )
                                        .padding(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = profile.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF251912)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (profile.isBoy) "Astronauta" else "Fadinha",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = (if (profile.isBoy) SecondaryBlue else Color(0xFFFF69B4))
                            )
                        }
                    }
                }
            }

            // Add Profile triggers
            SquishyButton(
                onClick = {
                    inputProfileName = ""
                    selectedBoyOption = true
                    showAddProfileDialog = true
                },
                backgroundColor = SecondaryBlueContainer,
                shadowColor = SecondaryBlue,
                modifier = Modifier.fillMaxWidth(0.92f),
                testTag = "add_profile_button"
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Criar Perfil",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Dialog for adding children profile
        if (showAddProfileDialog) {
            AlertDialog(
                onDismissRequest = { showAddProfileDialog = false },
                title = {
                    Text(
                        text = "Criar Novo Perfil",
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryOrange
                    )
                },
                text = {
                    Column(modifier = Modifier.padding(2.dp)) {
                        OutlinedTextField(
                            value = inputProfileName,
                            onValueChange = { inputProfileName = it },
                            label = { Text("Nome da Criança") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_profile_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedIndicatorColor = PrimaryOrange
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Tema do Ambiente:",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF251912),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Boy Button Cosmic Option
                            Button(
                                onClick = { 
                                    selectedBoyOption = true
                                    selectedAvatarUrl = viewModel.availableAvatars[0].first
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedBoyOption) SecondaryBlue else Color(0x1A0061A4),
                                    contentColor = if (selectedBoyOption) Color.White else SecondaryBlue
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cosmos (Menino)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Girl Button Magical Option
                            Button(
                                onClick = { 
                                    selectedBoyOption = false 
                                    selectedAvatarUrl = viewModel.availableAvatars[1].first
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!selectedBoyOption) Color(0xFFFF69B4) else Color(0x1AFF69B4),
                                    contentColor = if (!selectedBoyOption) Color.White else Color(0xFFFF69B4)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Magia (Menina)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Escolha a Imagem do Perfil:",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF251912),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(viewModel.availableAvatars) { (url, label) ->
                                val isSelected = selectedAvatarUrl == url
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) (if (selectedBoyOption) SecondaryBlue else Color(0xFFFF69B4)) else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { 
                                            selectedAvatarUrl = url
                                            if (url == viewModel.availableAvatars[0].first) selectedBoyOption = true
                                            if (url == viewModel.availableAvatars[1].first) selectedBoyOption = false
                                        }
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = label,
                                        modifier = Modifier.size(46.dp).clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (inputProfileName.trim().isNotEmpty()) {
                                viewModel.createChildProfile(inputProfileName.trim(), selectedBoyOption, selectedAvatarUrl)
                                showAddProfileDialog = false
                            }
                        },
                        modifier = Modifier.testTag("save_profile_dialog_button")
                    ) {
                        Text("Salvar", color = PrimaryOrange, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddProfileDialog = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = LightSurface
            )
        }
    }
}

// --- 03. CHILD HOME ENVIRONMENT ---
@Composable
fun ChildHomeScreen(viewModel: LeaoViewModel) {
    val currentProfile by viewModel.currentProfile.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val currentCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isSmartCurating by viewModel.isSmartCurating.collectAsStateWithLifecycle()
    val isCategoryLoading by viewModel.isCategoryLoading.collectAsStateWithLifecycle()
    val unsafeResponse by viewModel.unsafeSearchResponse.collectAsStateWithLifecycle()
    val favorites by viewModel.favoritesList.collectAsStateWithLifecycle()

    val kidsProfile = currentProfile ?: return

    val listState = rememberLazyListState()

    // Dynamic brand backgrounds inspired by profile specifications
    val bannerBg = if (kidsProfile.isBoy) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF001D36), Color(0xFF00497D))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(AccentPink, AccentLilac)
        )
    }

    val bannerTextColor = if (kidsProfile.isBoy) Color.White else Color(0xFF5C2800)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface),
        state = listState,
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Upper Dashboard Appbar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.navigateTo(LeaoScreen.ProfileSelection) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (kidsProfile.isBoy) SecondaryBlue else Color(0xFFFF69B4),
                                CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        AsyncImage(
                            model = kidsProfile.avatarUrl,
                            contentDescription = "Avatar do perfil",
                            modifier = Modifier
                                .clip(CircleShape)
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = kidsProfile.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF251912)
                        )
                        Text(
                            text = if (kidsProfile.isBoy) "Planeta Cosmos 🚀" else "Mundo Mágico ✨",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (kidsProfile.isBoy) SecondaryBlue else Color(0xFFFF69B4)
                        )
                    }
                }

                // Clock lock out-gate parental gate
                IconButton(
                    onClick = { viewModel.prepareParentGate(LeaoScreen.ParentDashboard) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFFEADF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Configuração dos Pais",
                        tint = PrimaryOrange
                    )
                }
            }
        }

        // Hello Mascot Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(bannerBg)
                    .border(
                        width = 4.dp,
                        color = if (kidsProfile.isBoy) SecondaryBlue else Color.White,
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Olá, ${kidsProfile.name}!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = bannerTextColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (kidsProfile.isBoy) {
                                "Vamos decolar no foguete e descobrir planetas e dinossauros incríveis hoje?"
                            } else {
                                "Pronta para cantar lindas musiquinhas e desenhar estrelas brilhantes?"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = bannerTextColor.copy(alpha = 0.85f),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SquishyButton(
                            onClick = {
                                val firstVideo = viewModel.customApprovedVideos.value.firstOrNull() ?: viewModel.presetVideos.firstOrNull()
                                if (firstVideo != null) {
                                    viewModel.selectVideoAndNavigate(firstVideo)
                                }
                            },
                            backgroundColor = if (kidsProfile.isBoy) SecondaryBlueContainer else PrimaryOrange,
                            shadowColor = if (kidsProfile.isBoy) SecondaryBlue else PrimaryOrangeDark,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                "Continuar Jogando",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuAhEYselL__exfw8GMaFTrTKUlRDpC_ykPSiiGT-QLt_UFyqjr-9xFZ8CvuijGgO9XMoJokSWWydMxXtUcQc6WPUmij7lHNwwiaudfSQ_UUwy8lLdxL71zJjIEudO7fbuZaZBMCCp81d2DL2PeeQCBm6OF2POAAevnYStrtqS8JtH0tfyCJ-qDc9VD-vMmiAY8x0hsPIA7TbasanetrzC6avj3eFug9NvDuaiwJWAQIIu17VICDRhLSUmxrj9wHM0XhClVnTXLc7no",
                        contentDescription = "Mascote Leãozinho",
                        modifier = Modifier
                            .size(100.dp)
                            .rotate(5f)
                    )
                }
            }
        }

        // Custom search bar input
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Pesquisar vídeos incríveis...", fontSize = 15.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Pesquisar",
                            tint = PrimaryOrange
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Limpar busca", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kids_search_bar"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.performKidsSearch(searchQuery) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFFFECE3),
                        unfocusedContainerColor = Color(0xFFFFECE3),
                        focusedIndicatorColor = PrimaryOrange,
                        unfocusedIndicatorColor = Color(0xFFFFDFC8)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.performKidsSearch(searchQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Buscar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Safety Unsafe Search Blocking Alerts!
        if (unsafeResponse != null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color(0xFFFFDAD6), RoundedCornerShape(24.dp))
                        .border(4.dp, Color(0xFFBA1A1A), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Alerta de Bloqueio",
                        tint = Color(0xFFBA1A1A),
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ops! Esse assunto não está liberado.",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF410002),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = unsafeResponse?.reason ?: "Termo bloqueado no filtro parental.",
                        fontSize = 13.sp,
                        color = Color(0xFF410002).copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    unsafeResponse?.suggestedAlternative?.let { alternative ->
                        if (alternative.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Dica do Leãozinho: $alternative",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryBlue,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SquishyButton(
                        onClick = { viewModel.clearUnsafeSearchBlock() },
                        backgroundColor = Color(0xFFBA1A1A),
                        shadowColor = Color(0xFF93000A),
                        shape = RoundedCornerShape(16.dp),
                        testTag = "dismiss_safety_warning"
                    ) {
                        Text("Voltar ao Início", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        } else {
            // Category Slider block
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val categories = listOf("Todas", "Astronomia", "Dinossauros", "Ciências", "Música", "Artes")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = currentCategory == category
                            val itemColor = if (isSelected) PrimaryOrange else Color.White
                            val strokeColor = if (isSelected) PrimaryOrangeDark else Color(0xFFFFEADF)
                            val textColor = if (isSelected) Color.White else Color(0xFF584235)

                            Box(
                                modifier = Modifier
                                    .shadow(elevation = if (isSelected) 4.dp else 1.dp, shape = RoundedCornerShape(20.dp))
                                    .background(itemColor, RoundedCornerShape(20.dp))
                                    .border(2.dp, strokeColor, RoundedCornerShape(20.dp))
                                    .clickable { viewModel.onCategoryChange(category) }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                                    .testTag("category_tab_${category.lowercase()}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = when (category) {
                                        "Todas" -> Icons.Filled.List
                                        "Astronomia" -> Icons.Filled.Star
                                        "Dinossauros" -> Icons.Filled.Star
                                        "Ciências" -> Icons.Filled.Star
                                        "Música" -> Icons.Filled.Star
                                        "Artes" -> Icons.Filled.Star
                                        else -> Icons.Filled.Star
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else PrimaryOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = category,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // DYNAMIC CATEGORY LOADER
            if (isCategoryLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PrimaryOrange)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Carregando novidades direto do YouTube...",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF584235),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isSmartCurating) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PrimaryOrange)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "O Leãozinho está checando se seu termo é seguro...",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF584235),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Video list cards grid
                if (searchResults.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.List,
                                contentDescription = "Sem videos",
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhum vídeo liberado nesta categoria!",
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Vídeos Recomendados",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF251912),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    items(searchResults) { video ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .shadow(2.dp, RoundedCornerShape(24.dp))
                                .background(Color.White, RoundedCornerShape(24.dp))
                                .border(3.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                                .clickable {
                                    viewModel.selectVideoAndNavigate(video)
                                }
                                .testTag("video_card_${video.id}")
                        ) {
                            Column {
                                // Thumbnail display
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = video.thumbnailUrl,
                                        contentDescription = video.title,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(topStart = 21.dp, topEnd = 21.dp)),
                                        contentScale = ContentScale.Crop
                                    )

                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Assistir",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .shadow(2.dp, CircleShape)
                                    )

                                    // Duration stamp
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(12.dp)
                                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = video.durationText,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = video.title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF251912),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Canal: ${video.channelName}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF584235).copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // Action buttons for child home cards
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(video) }
                                    ) {
                                        val isFav = favorites.any { it.videoId == video.id }
                                        Icon(
                                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = "Favoritar",
                                            tint = if (isFav) Color(0xFFFF007F) else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DYNAMIC GAMES PANEL ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Jogos Divertidos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF251912)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Piano Mágico Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                            .shadow(3.dp, RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFFF69B4), Color(0xFFFFC0CB))
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.navigateTo(LeaoScreen.PianoGame) }
                            .padding(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                    .padding(10.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Piano Mágico",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Mascot Jigsaw Puzzle Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                            .shadow(3.dp, RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(SecondaryBlue, SecondaryBlueContainer)
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.navigateTo(LeaoScreen.PuzzleGame) }
                            .padding(16.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                    .padding(10.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Quebra-Cabeça",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Floating static overlay navbar
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color(0xFFFFECE3), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Home, contentDescription = null, tint = PrimaryOrange)
                Text("Início", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryOrange)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(LeaoScreen.PianoGame) }
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color.Gray)
                Text("Jogos", fontSize = 11.sp, color = Color.Gray)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(LeaoScreen.ProfileSelection) }
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Face, contentDescription = null, tint = Color.Gray)
                Text("Perfis", fontSize = 11.sp, color = Color.Gray)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { viewModel.prepareParentGate(LeaoScreen.ParentDashboard) }
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color.Gray)
                Text("Pais", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun RecommendedVideoCard(
    video: KidVideo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .padding(end = 12.dp)
            .clickable(onClick = onClick)
            .testTag("recommended_card_${video.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Duration tag in recommended
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.durationText,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = video.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF251912),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = video.channelName,
                    fontSize = 11.sp,
                    color = SecondaryBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// --- 04. KID SAFE MOUNTED PLAYER SCREEN ---
@Composable
fun VideoPlayerScreen(viewModel: LeaoViewModel) {
    val activeVideo by viewModel.activeVideo.collectAsStateWithLifecycle()
    val spentSeconds by viewModel.screenTimeSpentSeconds.collectAsStateWithLifecycle()
    val limitMinutes by viewModel.parentConfig.map { it.screenTimeLimitMinutes }.collectAsStateWithLifecycle(60)
    val recommendations by viewModel.recommendedVideos.collectAsStateWithLifecycle()

    val video = activeVideo ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(LeaoScreen.ChildHome) },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFFFEADF), CircleShape)
                    .testTag("back_to_home_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = PrimaryOrange
                )
            }

            val totalLimitSeconds = limitMinutes * 60
            val remainingSecs = maxOf(0, totalLimitSeconds - spentSeconds)
            val minutesLeft = remainingSecs / 60
            val secondsLeft = remainingSecs % 60

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFF001D36), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = String.format("%02d:%02d", minutesLeft, secondsLeft),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return true
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            domStorageEnabled = true
                        }
                        loadUrl("https://www.youtube.com/embed/${video.id}?autoplay=1&controls=0&rel=0&modestbranding=1&showInfo=0")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF251912)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Canal: ${video.channelName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryBlue
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleFavorite(video) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFFEADF), CircleShape)
                ) {
                    val isFav = viewModel.isFavorite(video.id)
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFav) Color(0xFFFF007F) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = Color(0xFFFFEADF))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Descrição do Vídeo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF584235)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = video.description,
                fontSize = 14.sp,
                color = Color(0xFF584235).copy(alpha = 0.8f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Próximos Vídeos Recomendados",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF251912)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recommendations.isEmpty()) {
                Text(
                    text = "Não há outros vídeos recomendados para este perfil no momento.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(recommendations) { recVideo ->
                        RecommendedVideoCard(
                            video = recVideo,
                            onClick = { viewModel.selectVideoAndNavigate(recVideo) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFFFEC), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Aprovado no Filtro Seguro Leão Kids (Sem anúncios externos ou comentários)",
                    color = Color(0xFF2E7D32),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// --- 05. PARENTAL LOCK SECURITY CHALLENGE GATE ---
@Composable
fun ParentGateScreen(viewModel: LeaoViewModel) {
    val num1 by viewModel.firstGateNum.collectAsStateWithLifecycle()
    val num2 by viewModel.secondGateNum.collectAsStateWithLifecycle()
    val inputVal by viewModel.gateAnswerInput.collectAsStateWithLifecycle()
    val errorMsg by viewModel.gateErrorMsg.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color(0xFFFFEADF), CircleShape)
            )
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = PrimaryOrange,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Área Exclusiva para Pais",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF251912)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "As crianças não podem passar daqui sem resolver o problema matemático abaixo:",
            fontSize = 14.sp,
            color = Color(0xFF584235),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(300.dp),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(3.dp, Color(0xFFFFDFC8), RoundedCornerShape(24.dp))
                .padding(horizontal = 32.dp, vertical = 20.dp)
        ) {
            Text(
                text = "$num1 x $num2 = ",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryOrange
            )

            OutlinedTextField(
                value = inputVal,
                onValueChange = { viewModel.gateAnswerInput.value = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(90.dp)
                    .testTag("parent_gate_input"),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = PrimaryOrange
                )
            )
        }

        if (errorMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMsg,
                color = Color(0xFFBA1A1A),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(0.9f)) {
            Button(
                onClick = { viewModel.navigateTo(LeaoScreen.ProfileSelection) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Voltar", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.verifyParentGate() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                modifier = Modifier
                    .weight(1.5f)
                    .padding(start = 8.dp)
                    .height(48.dp)
                    .testTag("parent_gate_confirm_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirmar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- 06. PARENTS CONTROL DASHBOARD PANEL ---
@Suppress("ModifierParameter")
@Composable
fun ParentDashboardScreen(viewModel: LeaoViewModel) {
    val pConfig by viewModel.parentConfig.collectAsStateWithLifecycle()
    val wordsState by viewModel.blockedWords.collectAsStateWithLifecycle()
    val channelsState by viewModel.blockedChannels.collectAsStateWithLifecycle()
    val allowedState by viewModel.allowedChannels.collectAsStateWithLifecycle()
    val searchLogsState by viewModel.blockedSearchAttempts.collectAsStateWithLifecycle()
    val childProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()

    var inputWord by remember { mutableStateOf("") }
    var inputChannel by remember { mutableStateOf("") }
    var inputAllowedChannel by remember { mutableStateOf("") }

    var editingProfile by remember { mutableStateOf<ChildProfile?>(null) }
    var editNameInput by remember { mutableStateOf("") }
    var editBoyOption by remember { mutableStateOf(true) }
    var editAvatarUrl by remember { mutableStateOf("") }

    var activeTab by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    val accountChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val accountName = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (accountName != null) {
                viewModel.connectMockGoogleAccount(accountName, accountName.substringBefore("@"))
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- CABEÇALHO ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo(LeaoScreen.ProfileSelection) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFFEADF), CircleShape)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = PrimaryOrange)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Dashboard dos Pais",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF251912)
                    )
                }

                if (pConfig.connectedEmail != null) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEFFFEC), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Conta Premium Ativa", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }
            }
        }

        // --- SELEÇÃO DE ABAS ---
        item {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.White,
                contentColor = PrimaryOrange,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Configurações", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Pesquisa YouTube", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Navegar no YouTube", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }
        }

        // --- ABA 0: CONFIGURAÇÕES E AMBIENTES ---
        if (activeTab == 0) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Conectar Conta do Responsável",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF251912)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (pConfig.connectedEmail != null) {
                                    "${pConfig.connectedName ?: "Responsável"} (${pConfig.connectedEmail})\nModo Premium Ativado ✨"
                                } else {
                                    "Sincronize com o Google para monitoramento premium."
                                },
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                        }

                        SquishyButton(
                            onClick = {
                                if (pConfig.connectedEmail != null) {
                                    viewModel.disconnectGoogleAccount()
                                } else {
                                    try {
                                        val intent = AccountManager.newChooseAccountIntent(
                                            null,
                                            null,
                                            arrayOf("com.google"),
                                            true,
                                            null,
                                            null,
                                            null,
                                            null
                                        )
                                        accountChooserLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        // Fallback manual if account picker is not available
                                        viewModel.connectMockGoogleAccount("premium@youtube.com", "Responsável")
                                    }
                                }
                            },
                            backgroundColor = if (pConfig.connectedEmail != null) Color.Gray else SecondaryBlue,
                            shadowColor = if (pConfig.connectedEmail != null) Color.DarkGray else SecondaryBlue,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.wrapContentSize(),
                            testTag = "google_connect_button"
                        ) {
                            Text(
                                text = if (pConfig.connectedEmail != null) "Desconectar" else "Entrar com Google",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Gerenciar Perfis das Crianças",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF251912)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (childProfiles.isEmpty()) {
                        Text("Nenhum perfil cadastrado.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            childProfiles.forEach { child ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFAFAFA), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(16.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = child.avatarUrl,
                                            contentDescription = "Avatar de ${child.name}",
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, if (child.isBoy) SecondaryBlue else Color(0xFFFF69B4), CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(child.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF251912))
                                            Text(
                                                text = if (child.isBoy) "Tema: Cosmos (Espaço)" else "Tema: Magia (Fadas)",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingProfile = child
                                                editNameInput = child.name
                                                editBoyOption = child.isBoy
                                                editAvatarUrl = child.avatarUrl
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFFE3F2FD), CircleShape)
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Editar Perfil", tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteChildProfile(child.id) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFFFFEBEE), CircleShape)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Excluir Perfil", tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Controle de Tempo de Tela",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF251912)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val times = listOf(1, 15, 30, 60, 120)
                        times.forEach { time ->
                            val isSel = pConfig.screenTimeLimitMinutes == time
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSel) PrimaryOrange else Color(0xFFFFECE3),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.updateScreenTimeTimer(time) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (time == 1) "1 Min (Teste)" else "${time}m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else PrimaryOrange
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Modos de Segurança",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF251912)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modo Restrito de Canais", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Somente canais aprovados serão visíveis no app.", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = pConfig.isStrictChannelMode,
                            onCheckedChange = { viewModel.changeStrictChannelMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryOrange, checkedTrackColor = Color(0xFFFFECE3))
                        )
                    }

                    Divider(color = Color(0xFFFFEADF))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modo Curadoria Inteligente (IA)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Utiliza modelos Google Gemini para filtrar todas as buscas das crianças.", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = pConfig.isSmartCuratorMode,
                            onCheckedChange = { viewModel.changeSmartCuratorMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryOrange, checkedTrackColor = Color(0xFFFFECE3)),
                            modifier = Modifier.testTag("ai_curator_mode_switch")
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Blacklist de Palavras",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF251912)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputWord,
                            onValueChange = { inputWord = it },
                            placeholder = { Text("Adicionar palavra...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_banned_word_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputWord.trim().isNotEmpty()) {
                                    viewModel.addBlockedWord(inputWord.trim())
                                    inputWord = ""
                                }
                            },
                            modifier = Modifier
                                .background(PrimaryOrange, CircleShape)
                                .testTag("submit_banned_word_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        wordsState.forEach { word ->
                            AssistChip(
                                onClick = { viewModel.removeBlockedWord(word.id) },
                                label = { Text(word.word) },
                                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Canais Bloqueados",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF251912)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputChannel,
                            onValueChange = { inputChannel = it },
                            placeholder = { Text("Adicionar Canal...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputChannel.trim().isNotEmpty()) {
                                    viewModel.addBlockedChannel(inputChannel.trim())
                                    inputChannel = ""
                                }
                            },
                            modifier = Modifier.background(PrimaryOrange, CircleShape)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        channelsState.forEach { channel ->
                            AssistChip(
                                onClick = { viewModel.removeBlockedChannel(channel.id) },
                                label = { Text(channel.channelName) },
                                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Canais Permitidos (Modo Aprovados)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF251912)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputAllowedChannel,
                            onValueChange = { inputAllowedChannel = it },
                            placeholder = { Text("Permitir Canal...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputAllowedChannel.trim().isNotEmpty()) {
                                    viewModel.addAllowedChannel(inputAllowedChannel.trim())
                                    inputAllowedChannel = ""
                                }
                            },
                            modifier = Modifier.background(PrimaryOrange, CircleShape)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allowedState.forEach { ch ->
                            AssistChip(
                                onClick = { viewModel.removeAllowedChannel(ch.id) },
                                label = { Text(ch.channelName) },
                                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(12.dp)) }
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tentativas de Busca Bloqueadas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF251912)
                        )

                        if (searchLogsState.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearBlockedSearchLogs() }) {
                                Text("Limpar", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (searchLogsState.isEmpty()) {
                        Text(
                            "Nenhuma atividade suspeita registrada. As crianças estão seguras! 🎉",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            searchLogsState.take(10).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFDAD6), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Pesquisa: '${item.query}'",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF93000a)
                                        )
                                        Text(
                                            text = "Filtrado pelo Leãozinho Inteligente",
                                            fontSize = 10.sp,
                                            color = Color.Red.copy(alpha = 0.7f)
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Bloqueado",
                                        tint = Color.Red,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Zona de Perigo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ações administrativas para depurar ou redefinir a base do aplicativo.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val context = LocalContext.current
                    Button(
                        onClick = {
                            viewModel.importSampleVideos()
                            Toast.makeText(context, "Vídeos de exemplo importados com sucesso! 🎉", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Importar Vídeos Educativos de Exemplo", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.resetApp() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Limpar Todos os Dados (Redefinir)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // --- ABA 1: CURADORIA ATIVA (PESQUISA DO YOUTUBE) ---
        if (activeTab == 1) {
            item {
                val youtubeSearchQuery by viewModel.parentYoutubeSearchQuery.collectAsStateWithLifecycle()
                val youtubeSearchResults by viewModel.parentYoutubeSearchResults.collectAsStateWithLifecycle()
                val customApproved by viewModel.customApprovedVideos.collectAsStateWithLifecycle()
                var localSearchInput by remember { mutableStateOf(youtubeSearchQuery) }

                Column(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gerenciador YouTube Premium",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF251912)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pesquise e libere vídeos do YouTube para sincronizar diretamente com o catálogo infantil do app.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = localSearchInput,
                            onValueChange = { localSearchInput = it },
                            placeholder = { Text("Pesquisar vídeos ou canais...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.searchYoutubeAsParent(localSearchInput) })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.searchYoutubeAsParent(localSearchInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Buscar", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (youtubeSearchResults.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Resultados Encontrados (${youtubeSearchResults.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF251912)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            youtubeSearchResults.forEach { video ->
                                val isAlreadyApproved = customApproved.any { it.id == video.id }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFAFAFA), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        AsyncImage(
                                            model = video.thumbnailUrl,
                                            contentDescription = video.title,
                                            modifier = Modifier
                                                .size(80.dp, 60.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(video.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF251912), maxLines = 2)
                                            Text("Canal: ${video.channelName} • ${video.durationText}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(
                                                onClick = { viewModel.addBlockedChannel(video.channelName) },
                                                modifier = Modifier.height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) {
                                                Text("Bloquear Canal", color = Color(0xFFD32F2F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            TextButton(
                                                onClick = { viewModel.addBlockedWord(video.title.split(" ").firstOrNull() ?: video.title) },
                                                modifier = Modifier.height(28.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) {
                                                Text("Bloquear Título", color = Color(0xFFD32F2F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (isAlreadyApproved) {
                                            Button(
                                                onClick = { viewModel.removeApprovedVideo(video.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                            ) {
                                                Text("Liberado ✨", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Button(
                                                onClick = { viewModel.approveVideoForApp(video) },
                                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                            ) {
                                                Text("Liberar no App", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- VÍDEOS LIBERADOS LIST ---
                    if (customApproved.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Vídeos Liberados no App (${customApproved.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            customApproved.forEach { video ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = video.thumbnailUrl,
                                        contentDescription = video.title,
                                        modifier = Modifier
                                            .size(60.dp, 45.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(video.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, color = Color(0xFF2E7D32))
                                        Text("Canal: ${video.channelName}", fontSize = 9.sp, color = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeApprovedVideo(video.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remover", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- ABA 2: NAVEGAR NO YOUTUBE (WEBVIEW) ---
        if (activeTab == 2) {
            item {
                Column(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(650.dp)
                        .shadow(1.dp, RoundedCornerShape(24.dp))
                        .background(Color.White, RoundedCornerShape(24.dp))
                        .border(2.dp, Color(0xFFFFEADF), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    var webViewRef by remember { mutableStateOf<WebView?>(null) }
                    var webUrl by remember { mutableStateOf("https://m.youtube.com") }
                    var webTitle by remember { mutableStateOf("YouTube Mobile") }
                    var canGoBack by remember { mutableStateOf(false) }
                    var canGoForward by remember { mutableStateOf(false) }

                    // Browser Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack,
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (canGoBack) Color(0xFFFFECE3) else Color(0xFFF5F5F5), CircleShape)
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = if (canGoBack) PrimaryOrange else Color.Gray)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward,
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (canGoForward) Color(0xFFFFECE3) else Color(0xFFF5F5F5), CircleShape)
                        ) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "Avançar", tint = if (canGoForward) PrimaryOrange else Color.Gray)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.size(36.dp).background(Color(0xFFFFECE3), CircleShape)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Recarregar", tint = PrimaryOrange)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = webTitle,
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = Color.DarkGray
                            )
                        }
                    }

                    // WebView Container
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            if (url != null) {
                                                webUrl = url
                                                canGoBack = view?.canGoBack() == true
                                                canGoForward = view?.canGoForward() == true
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            if (url != null) {
                                                webUrl = url
                                                webTitle = view?.title ?: url
                                                canGoBack = view?.canGoBack() == true
                                                canGoForward = view?.canGoForward() == true
                                            }
                                        }
                                    }
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        mediaPlaybackRequiresUserGesture = false
                                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Mobile Safari/537.36"
                                    }
                                    loadUrl(webUrl)
                                    webViewRef = this
                                }
                            },
                            update = { view ->
                                webViewRef = view
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Approval Action Footer
                    val videoId = com.example.data.GeminiService.extractYoutubeVideoId(webUrl)
                    if (videoId != null) {
                        val context = LocalContext.current
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    Toast.makeText(context, "Buscando detalhes do vídeo...", Toast.LENGTH_SHORT).show()
                                    val videoDetails = withContext(Dispatchers.IO) {
                                        com.example.data.GeminiService.getVideoDetailsViaAI(videoId, webTitle)
                                    }
                                    viewModel.approveVideoForApp(videoDetails)
                                    Toast.makeText(context, "Vídeo liberado com sucesso: ${videoDetails.title}", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Liberar este vídeo no Leão Kids! ✨", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF9C4), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFFBC02D), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Navegue pelo YouTube e abra um vídeo para poder liberá-lo no app!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }


    // Dialog for editing children profile details
    if (editingProfile != null) {
        AlertDialog(
            onDismissRequest = { editingProfile = null },
            title = {
                Text(
                    text = "Editar Perfil do Pequeno",
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryOrange
                )
            },
            text = {
                Column(modifier = Modifier.padding(2.dp)) {
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Nome da Criança") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = PrimaryOrange
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Tema do Ambiente:",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF251912),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { 
                                editBoyOption = true
                                editAvatarUrl = viewModel.availableAvatars[0].first
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (editBoyOption) SecondaryBlue else Color(0x1A0061A4),
                                contentColor = if (editBoyOption) Color.White else SecondaryBlue
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        ) {
                            Text("Cosmos", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { 
                                editBoyOption = false 
                                editAvatarUrl = viewModel.availableAvatars[1].first
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!editBoyOption) Color(0xFFFF69B4) else Color(0x1AFF69B4),
                                contentColor = if (!editBoyOption) Color.White else Color(0xFFFF69B4)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        ) {
                            Text("Magia", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Escolha a Imagem do Perfil:",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF251912),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        items(viewModel.availableAvatars) { (url, label) ->
                            val isSelected = editAvatarUrl == url
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) (if (editBoyOption) SecondaryBlue else Color(0xFFFF69B4)) else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { 
                                        editAvatarUrl = url
                                        if (url == viewModel.availableAvatars[0].first) editBoyOption = true
                                        if (url == viewModel.availableAvatars[1].first) editBoyOption = false
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = label,
                                    modifier = Modifier.size(46.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentEditing = editingProfile
                        if (currentEditing != null && editNameInput.trim().isNotEmpty()) {
                            viewModel.updateChildProfile(
                                id = currentEditing.id,
                                name = editNameInput.trim(),
                                isBoy = editBoyOption,
                                avatarUrl = editAvatarUrl
                            )
                            editingProfile = null
                        }
                    }
                ) {
                    Text("Atualizar", color = PrimaryOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProfile = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = LightSurface
        )
    }
}

// --- 07. PIANO MAGICO MINI JOGOS WITH AUDIO SYNTH ---
@Composable
fun PianoGameScreen(viewModel: LeaoViewModel) {
    var activeNoteKey by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val notes = listOf(
        Pair("Dó", 261.63),
        Pair("Ré", 293.66),
        Pair("Mi", 329.63),
        Pair("Fá", 349.23),
        Pair("Sol", 392.00),
        Pair("Lá", 440.00),
        Pair("Si", 493.88),
        Pair("Dó+", 523.25)
    )

    val colors = listOf(
        Color(0xFFFF0000), Color(0xFFFF7A00), Color(0xFFFABD00),
        Color(0xFF4CAF50), Color(0xFF00E5FF), Color(0xFF2979FF),
        Color(0xFF651FFF), Color(0xFFD500F9)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(LeaoScreen.ChildHome) },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }

            Text(
                text = "Piano Mágico 🎹",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.width(44.dp))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            Text(
                text = activeNoteKey ?: "Toque nas teclas coloridas!",
                fontSize = 24.sp,
                color = if (activeNoteKey != null) PrimaryOrange else Color.Gray,
                fontWeight = FontWeight.Bold
            )
            if (activeNoteKey != null) {
                Text(
                    text = "Lindo som harmônico!",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            notes.forEachIndexed { idx, note ->
                val color = colors[idx]
                val keyStatePressed = activeNoteKey == note.first

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(elevation = if (keyStatePressed) 1.dp else 6.dp, shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(
                            if (keyStatePressed) color.copy(alpha = 0.7f) else color,
                            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .clickable {
                            scope.launch {
                                activeNoteKey = note.first
                                try {
                                    val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                                    toneGen.startTone(ToneGenerator.TONE_DTMF_1, 150)
                                    delay(150)
                                    toneGen.release()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                activeNoteKey = null
                            }
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = note.first,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color.White.copy(alpha = 0.7f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

// --- 08. MASCOT MOUNTED PUZZLE MATCHING GAME ---
@Suppress("MutableCollectionState")
@Composable
fun PuzzleGameScreen(viewModel: LeaoViewModel) {
    var puzzleState by remember {
        mutableStateOf(mutableListOf(0, 1, 2, 5, 4, 3, 6, 7, 8))
    }
    var selectedPieceIdx by remember { mutableStateOf<Int?>(null) }
    var congratMsg by remember { mutableStateOf("") }

    val imagesParts = listOf(
        "https://images.unsplash.com/photo-1546182990-dffeafbe841d?q=80&w=200&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1546182990-dffeafbe841d?q=80&w=200&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1546182990-dffeafbe841d?q=80&w=200&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1546182990-dffeafbe841d?q=80&w=200&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1546182990-dffeafbe841d?q=80&w=200&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1546182990-dffeafbe841d?q=80&w=200&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1546182990-dffeafbe841d?q=80&w=200&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1546182990-dffeafbe841d?q=80&w=200&auto=format&fit=crop",
        "https://images.unsplash.com/photo-1546182990-dffeafbe841d?q=80&w=200&auto=format&fit=crop"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(LeaoScreen.ChildHome) },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFFFEADF), CircleShape)
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = PrimaryOrange)
            }

            Text(
                text = "Quebra-Cabeça do Leãozinho 🦁",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF251912)
            )

            Spacer(modifier = Modifier.width(44.dp))
        }

        congratMsg.let { msg ->
            if (msg.isNotEmpty()) {
                Text(
                    text = msg,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFFFEC), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                )
            } else {
                Text(
                    text = "Toque em duas peças para trocar de lugar e montar o Leãozinho!",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(
            modifier = Modifier
                .size(300.dp)
                .background(Color.White, RoundedCornerShape(24.dp))
                .border(4.dp, Color(0xFFFFDFC8), RoundedCornerShape(24.dp))
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (row in 0..2) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (col in 0..2) {
                            val idx = row * 3 + col
                            val activeVal = puzzleState[idx]
                            val isSelected = selectedPieceIdx == idx

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                                    .border(
                                        width = if (isSelected) 4.dp else 1.dp,
                                        color = if (isSelected) PrimaryOrange else Color.LightGray,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (selectedPieceIdx == null) {
                                            selectedPieceIdx = idx
                                        } else {
                                            val list = puzzleState.toMutableList()
                                            val oldIdx = selectedPieceIdx!!
                                            val oldVal = list[oldIdx]
                                            list[oldIdx] = list[idx]
                                            list[idx] = oldVal
                                            puzzleState = list
                                            selectedPieceIdx = null

                                            val sorted = (0..8).toList()
                                            if (puzzleState == sorted) {
                                                congratMsg = "Uau! Você montou o Leãozinho! Parabéns! 🦁🎉✨"
                                            } else {
                                                congratMsg = ""
                                            }
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = imagesParts[activeVal],
                                    contentDescription = null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .fillMaxSize(),
                                    colorFilter = ColorFilter.tint(Color(0xFFD32F2F)),
                                    contentScale = ContentScale.Crop
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${activeVal + 1}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                puzzleState = mutableListOf(0, 1, 2, 3, 4, 5, 6, 7, 8).apply { shuffle() }
                congratMsg = ""
                selectedPieceIdx = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Embaralhar Peças", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// --- 09. SCREEN TIMEOUT LIMIT WARNING OUT EXCEEDED ---
@Composable
fun TimeoutWarningScreen(viewModel: LeaoViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF001D36))
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = TertiaryYellow,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        AsyncImage(
            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuAhEYselL__exfw8GMaFTrTKUlRDpC_ykPSiiGT-QLt_UFyqjr-9xFZ8CvuijGgO9XMoJokSWWydMxXtUcQc6WPUmij7lHNwwiaudfSQ_UUwy8lLdxL71zJjIEudO7fbuZaZBMCCp81d2DL2PeeQCBm6OF2POAAevnYStrtqS8JtH0tfyCJ-qDc9VD-vMmiAY8x0hsPIA7TbasanetrzC6avj3eFug9NvDuaiwJWAQIIu17VICDRhLSUmxrj9wHM0XhClVnTXLc7no",
            contentDescription = "Mascote Leãozinho descansando",
            modifier = Modifier
                .size(160.dp)
                .rotate(8f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Hora de descansar!",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Seu limite diário de tela terminou. Vamos descansar os olhinhos e brincar lá fora? Amanhã tem mais!",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(300.dp),
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        SquishyButton(
            onClick = { viewModel.navigateTo(LeaoScreen.ProfileSelection) },
            backgroundColor = PrimaryOrange,
            shadowColor = PrimaryOrangeDark,
            modifier = Modifier.fillMaxWidth(0.8f),
            testTag = "timeout_go_back_button"
        ) {
            Text(
                "Voltar amanhã",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
