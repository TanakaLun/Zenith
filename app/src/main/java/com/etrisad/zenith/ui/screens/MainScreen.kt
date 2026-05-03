package com.etrisad.zenith.ui.screens

import androidx.compose.animation.*
import androidx.compose.ui.unit.IntOffset
 import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.etrisad.zenith.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.etrisad.zenith.ui.components.PermissionBottomSheet
import com.etrisad.zenith.ui.components.UserBottomSheet
import com.etrisad.zenith.ui.components.ZenithHeader
import com.etrisad.zenith.ui.components.ConfirmBottomSheet
import com.etrisad.zenith.ui.navigation.Screen
import com.etrisad.zenith.ui.navigation.navItems
import com.etrisad.zenith.ui.screens.focus.FocusScreen
import com.etrisad.zenith.ui.screens.home.HomeScreen
import com.etrisad.zenith.ui.screens.home.UsageStatsScreen
import com.etrisad.zenith.ui.screens.bedtime.BedtimeScreen
import com.etrisad.zenith.ui.screens.settings.SettingsScreen
import com.etrisad.zenith.ui.viewmodel.FocusViewModel
import com.etrisad.zenith.ui.viewmodel.HomeViewModel
import com.etrisad.zenith.ui.viewmodel.BedtimeViewModel
import com.etrisad.zenith.ui.viewmodel.BedtimeViewModelFactory

import com.etrisad.zenith.data.preferences.UserPreferences
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    focusViewModel: FocusViewModel,
    userPreferencesRepository: UserPreferencesRepository,
    windowSizeClass: WindowSizeClass
) {
    val bedtimeViewModel: BedtimeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = BedtimeViewModelFactory(userPreferencesRepository)
    )
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { navItems.size })

    val smoothTweenSpec = tween<Float>(
        durationMillis = 500,
        easing = FastOutSlowInEasing
    )

    val smoothIntOffsetTweenSpec = tween<IntOffset>(
        durationMillis = 500,
        easing = FastOutSlowInEasing
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val effectiveRoute = if (currentRoute == "main_tabs") {
        navItems[pagerState.currentPage].route
    } else {
        currentRoute
    }

    val isDeepScreen =
        currentRoute == Screen.UsageStats.route || 
        currentRoute == Screen.Bedtime.route ||
        currentRoute?.startsWith("app_detail") == true

    val enterAlwaysScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val pinnedScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val scrollBehavior = if (isDeepScreen) pinnedScrollBehavior else enterAlwaysScrollBehavior

    val preferences by userPreferencesRepository.userPreferencesFlow.collectAsState(
        initial = UserPreferences()
    )
    val homeUiState by homeViewModel.uiState.collectAsState()

    val useNavigationRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    // Bedtime Switch Animation States
    var bedtimeSwitchVisible by remember { mutableStateOf(false) }
    var bedtimeSwitchInLayout by remember { mutableStateOf(false) }
    var showPauseSheet by remember { mutableStateOf(false) }

    LaunchedEffect(effectiveRoute, preferences.bedtimeEnabled) {
        val isBedtimeScreen = effectiveRoute == Screen.Bedtime.route
        val shouldShow = isBedtimeScreen && preferences.bedtimeEnabled
        
        if (shouldShow) {
            if (!bedtimeSwitchInLayout) {
                bedtimeSwitchInLayout = true
                delay(1200)
            }
            bedtimeSwitchVisible = true
        } else {
            if (bedtimeSwitchVisible) {
                bedtimeSwitchVisible = false
                delay(800)
            }
            if (bedtimeSwitchInLayout) {
                delay(1500)
                bedtimeSwitchInLayout = false
            }
        }
    }

    var showPermissionSheet by remember { mutableStateOf(false) }
    var showUserSheet by remember { mutableStateOf(false) }

    fun checkPermissions() {
        val hasUsageStats = com.etrisad.zenith.util.hasUsageStatsPermission(context)
        val hasOverlay = android.provider.Settings.canDrawOverlays(context)
        val hasAccessibility = com.etrisad.zenith.util.isAccessibilityServiceEnabled(context)

        val allGranted =
            hasUsageStats && hasOverlay && (hasAccessibility || preferences.accessibilityDisabled)
        showPermissionSheet = !allGranted
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, preferences.accessibilityDisabled) {
        checkPermissions()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showPermissionSheet) {
        PermissionBottomSheet(
            preferencesRepository = userPreferencesRepository,
            onDismissRequest = {
                showPermissionSheet = false
            },
            onAllPermissionsGranted = {
                showPermissionSheet = false
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        if (useNavigationRail) {
            val showNavRail =
                currentRoute != Screen.UsageStats.route && 
                currentRoute != Screen.Bedtime.route &&
                currentRoute?.startsWith("app_detail") == false

            AnimatedVisibility(
                visible = showNavRail,
                enter = slideInHorizontally(initialOffsetX = { -it }) + expandHorizontally(expandFrom = Alignment.Start),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + shrinkHorizontally(shrinkTowards = Alignment.Start)
            ) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    header = {
                    }
                ) {
                    val currentDestination = navBackStackEntry?.destination
                    navItems.forEach { screen ->
                        val selected = if (currentRoute == "main_tabs") {
                            navItems[pagerState.currentPage].route == screen.route
                        } else {
                            currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        }
                        NavigationRailItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = null
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                val index = navItems.indexOf(screen)
                                if (index != -1) {
                                    if (currentRoute != "main_tabs") {
                                        navController.navigate("main_tabs") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            index,
                                            animationSpec = smoothTweenSpec
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                ZenithHeader(
                    currentRoute = effectiveRoute,
                    scrollBehavior = scrollBehavior,
                    isNavRailVisible = useNavigationRail && !isDeepScreen,
                    userName = preferences.userName,
                    onBack = { navController.popBackStack() },
                    actions = {
                        Box(contentAlignment = Alignment.CenterEnd) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !isDeepScreen,
                                enter = fadeIn(tween(400)) + 
                                        scaleIn(initialScale = 0.9f, animationSpec = tween(400)) +
                                        slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = tween(400)),
                                exit = fadeOut(tween(400)) + 
                                       scaleOut(targetScale = 0.9f) +
                                       slideOutHorizontally(targetOffsetX = { it / 2 }, animationSpec = smoothIntOffsetTweenSpec)
                            ) {
                                IconButton(
                                    onClick = { showUserSheet = true },
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountCircle,
                                        contentDescription = "User Profile"
                                    )
                                }
                            }
                            
                            if (bedtimeSwitchInLayout) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .width(52.dp)
                                        .height(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = bedtimeSwitchVisible,
                                        enter = fadeIn(animationSpec = tween(400)) + 
                                                scaleIn(initialScale = 0.8f, animationSpec = tween(400)) +
                                                slideInHorizontally(initialOffsetX = { it / 2 }, animationSpec = tween(400)),
                                        exit = fadeOut(animationSpec = tween(400)) + 
                                               scaleOut(targetScale = 0.8f, animationSpec = tween(400)) +
                                               slideOutHorizontally(targetOffsetX = { it / 2 }, animationSpec = tween(400))
                                    ) {
                                        Switch(
                                            checked = preferences.bedtimeEnabled,
                                            onCheckedChange = { 
                                                if (preferences.bedtimeEnabled) {
                                                    showPauseSheet = true
                                                } else {
                                                    bedtimeViewModel.setBedtimeEnabled(true)
                                                }
                                            },
                                            thumbContent = {
                                                Icon(
                                                    imageVector = Icons.Default.Bedtime,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (showPauseSheet) {
                ConfirmBottomSheet(
                    onDismiss = { showPauseSheet = false },
                    onConfirm = { _ ->
                        bedtimeViewModel.setBedtimeEnabled(false)
                        showPauseSheet = false
                    },
                    leverCount = 10,
                    puzzleTimeoutSeconds = 10,
                    showTimeSelection = false
                )
            }
            if (showUserSheet) {
                UserBottomSheet(
                    userName = preferences.userName,
                    currentStreak = homeUiState.globalCurrentStreak,
                    bestStreak = homeUiState.globalBestStreak,
                    repository = userPreferencesRepository,
                    onDismissRequest = { showUserSheet = false }
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = "main_tabs",
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        val initialRoute = initialState.destination.route
                        val targetRoute = targetState.destination.route

                        val isTargetDeep =
                            targetRoute == Screen.UsageStats.route || 
                            targetRoute == Screen.Bedtime.route ||
                            targetRoute?.startsWith("app_detail") == true
                        val isInitialDeep =
                            initialRoute == Screen.UsageStats.route || 
                            initialRoute == Screen.Bedtime.route ||
                            initialRoute?.startsWith("app_detail") == true

                        if (isTargetDeep && !isInitialDeep) {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = smoothIntOffsetTweenSpec
                            ) + fadeIn(animationSpec = smoothTweenSpec)
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { if (isTargetDeep) it else -it },
                                animationSpec = smoothIntOffsetTweenSpec
                            ) + fadeIn(animationSpec = smoothTweenSpec)
                        }
                    },
                    exitTransition = {
                        val targetRoute = targetState.destination.route

                        val isTargetDeep =
                            targetRoute == Screen.UsageStats.route || 
                            targetRoute == Screen.Bedtime.route ||
                            targetRoute?.startsWith("app_detail") == true

                        if (isTargetDeep) {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = smoothIntOffsetTweenSpec
                            ) + fadeOut(animationSpec = smoothTweenSpec)
                        } else {
                            slideOutHorizontally(
                                targetOffsetX = { it / 3 },
                                animationSpec = smoothIntOffsetTweenSpec
                            ) + fadeOut(animationSpec = smoothTweenSpec)
                        }
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it / 3 },
                            animationSpec = smoothIntOffsetTweenSpec
                        ) + fadeIn(animationSpec = smoothTweenSpec)
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = smoothIntOffsetTweenSpec
                        ) + fadeOut(animationSpec = smoothTweenSpec)
                    }
                ) {
                    composable("main_tabs") {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 2,
                            userScrollEnabled = true,
                            flingBehavior = PagerDefaults.flingBehavior(
                                state = pagerState,
                                snapAnimationSpec = smoothTweenSpec
                            )
                        ) { page ->
                            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val pageAlpha = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                                        val pageScale = 0.95f + (0.05f * pageAlpha)
                                        alpha = pageAlpha
                                        scaleX = pageScale
                                        scaleY = pageScale
                                    }
                            ) {
                                when (navItems[page]) {
                                    Screen.Home -> HomeScreen(
                                        homeViewModel,
                                        userPreferencesRepository,
                                        innerPadding,
                                        onSeeFullList = { navController.navigate(Screen.UsageStats.route) },
                                        onAppClick = { packageName ->
                                            navController.navigate(Screen.AppDetail.createRoute(packageName))
                                        },
                                        onBedtimeClick = { navController.navigate(Screen.Bedtime.route) }
                                    )
                                    Screen.Focus -> FocusScreen(
                                        focusViewModel,
                                        innerPadding,
                                        onAppClick = { packageName ->
                                            navController.navigate(Screen.AppDetail.createRoute(packageName))
                                        }
                                    )
                                    Screen.Settings -> SettingsScreen(userPreferencesRepository, innerPadding)
                                    else -> {}
                                }
                            }
                        }
                    }
                    composable(Screen.Bedtime.route) {
                        BedtimeScreen(bedtimeViewModel, innerPadding)
                    }
                    composable(Screen.UsageStats.route) {
                        UsageStatsScreen(
                            viewModel = homeViewModel,
                            innerPadding = innerPadding,
                            onAppClick = { packageName ->
                                navController.navigate(Screen.AppDetail.createRoute(packageName))
                            }
                        )
                    }
                    composable(
                        route = Screen.AppDetail.route,
                        arguments = listOf(androidx.navigation.navArgument("packageName") {
                            type = androidx.navigation.NavType.StringType
                        })
                    ) { backStackEntry ->
                        val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                        com.etrisad.zenith.ui.screens.home.AppDetailScreen(
                            packageName = packageName,
                            viewModel = homeViewModel,
                            innerPadding = innerPadding
                        )
                    }
                }

                if (!useNavigationRail) {
                    val showBottomBar =
                        currentRoute != Screen.UsageStats.route && 
                        currentRoute != Screen.Bedtime.route &&
                        currentRoute?.startsWith("app_detail") == false

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showBottomBar,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = slideInVertically(initialOffsetY = { it }) + expandVertically(expandFrom = Alignment.Top),
                        exit = slideOutVertically(targetOffsetY = { it }) + shrinkVertically(shrinkTowards = Alignment.Top)
                    ) {
                        AnimatedContent(
                            targetState = preferences.floatingTabBarEnabled,
                            transitionSpec = {
                                (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                                        scaleIn(initialScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessLow)))
                                    .togetherWith(fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                                            scaleOut(targetScale = 0.92f, animationSpec = spring(stiffness = Spring.StiffnessLow)))
                            },
                            label = "TabBarTransition"
                        ) { isFloating ->
                            if (isFloating) {
                                val currentDestination = navBackStackEntry?.destination
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 36.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    HorizontalFloatingToolbar(
                                        expanded = true,
                                        modifier = Modifier
                                            .animateContentSize()
                                            .height(68.dp),
                                        shape = RoundedCornerShape(100),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                                            toolbarContainerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        navItems.forEach { screen ->
                                            val selected = if (currentRoute == "main_tabs") {
                                                navItems[pagerState.currentPage].route == screen.route
                                            } else {
                                                currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                            }
                                            ShortNavigationBarItem(
                                                selected = selected,
                                                onClick = {
                                                    val index = navItems.indexOf(screen)
                                                    if (index != -1) {
                                                        if (currentRoute != "main_tabs") {
                                                            navController.navigate("main_tabs") {
                                                                popUpTo(navController.graph.findStartDestination().id) {
                                                                    saveState = true
                                                                }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        }
                                                        scope.launch {
                                                            pagerState.animateScrollToPage(
                                                                index,
                                                                animationSpec = smoothTweenSpec
                                                            )
                                                        }
                                                    }
                                                },
                                                icon = {
                                                    val extraHeight by animateDpAsState(
                                                        targetValue = if (selected) 16.dp else 0.dp,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        ),
                                                        label = "indicatorHeight"
                                                    )
                                                    Box(
                                                        modifier = Modifier.height(26.dp + extraHeight),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                                            contentDescription = screen.title,
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                    }
                                                },
                                                label = {
                                                    androidx.compose.animation.AnimatedVisibility(
                                                        visible = selected,
                                                        enter = fadeIn() + slideInHorizontally(
                                                            initialOffsetX = { -15 },
                                                            animationSpec = spring(
                                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                                stiffness = Spring.StiffnessLow
                                                            )
                                                        ) + expandHorizontally(expandFrom = Alignment.Start),
                                                        exit = fadeOut() + slideOutHorizontally(
                                                            targetOffsetX = { -15 }) + shrinkHorizontally(
                                                            shrinkTowards = Alignment.Start
                                                        )
                                                    ) {
                                                        Text(
                                                            text = screen.title,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            maxLines = 1,
                                                            modifier = Modifier.padding(start = 2.dp, end = 4.dp)
                                                        )
                                                    }
                                                },
                                                iconPosition = NavigationItemIconPosition.Start,
                                                colors = ShortNavigationBarItemDefaults.colors(
                                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                                    selectedIndicatorColor = MaterialTheme.colorScheme.primary,
                                                    unselectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    unselectedTextColor = Color.Transparent
                                                ),
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .fillMaxHeight()
                                            )
                                        }
                                    }
                                }
                            } else {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    val currentDestination = navBackStackEntry?.destination
                                    navItems.forEach { screen ->
                                        val selected = if (currentRoute == "main_tabs") {
                                            navItems[pagerState.currentPage].route == screen.route
                                        } else {
                                            currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                        }
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                                    contentDescription = null
                                                )
                                            },
                                            label = { Text(screen.title) },
                                            selected = selected,
                                            onClick = {
                                                val index = navItems.indexOf(screen)
                                                if (index != -1) {
                                                    if (currentRoute != "main_tabs") {
                                                        navController.navigate("main_tabs") {
                                                            popUpTo(navController.graph.findStartDestination().id) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(
                                                            index,
                                                            animationSpec = smoothTweenSpec
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
