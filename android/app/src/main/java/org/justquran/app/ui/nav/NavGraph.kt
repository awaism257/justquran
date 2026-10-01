package org.justquran.app.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.justquran.app.AppContainer
import org.justquran.app.ui.about.AboutScreen
import org.justquran.app.ui.book.BookIndexScreen
import org.justquran.app.ui.book.BookReaderScreen
import org.justquran.app.ui.bookmarks.BookmarksScreen
import org.justquran.app.ui.help.HelpScreen
import org.justquran.app.ui.home.HomeScreen
import org.justquran.app.ui.index.JuzIndexScreen
import org.justquran.app.ui.index.SurahIndexScreen
import org.justquran.app.ui.khatm.KhatmScreen
import org.justquran.app.ui.reader.ReaderScreen
import org.justquran.app.ui.recitation.RecitationScreen
import org.justquran.app.ui.search.SearchScreen
import org.justquran.app.ui.settings.SettingsScreen

@Composable
fun JustQuranNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val audioController = container.audioController
    val openReq by audioController.openVerseRequest.collectAsState()
    LaunchedEffect(openReq) {
        val req = openReq ?: return@LaunchedEffect
        audioController.clearOpenVerseRequest()
        val currentDest = navController.currentDestination?.route
        val transLang = audioController.translationLang.value
        if (currentDest == Routes.BOOK || transLang != null) {
            val lang = transLang ?: navController.currentBackStackEntry?.arguments?.getString("lang") ?: "en"
            navController.navigate(Routes.book(lang, req.s)) {
                popUpTo(Routes.BOOK) { inclusive = true }
            }
        } else {
            navController.navigate(Routes.reader(req.s, req.v))
        }
    }

    val pending by audioController.pendingAdvance.collectAsState()
    LaunchedEffect(pending) {
        val p = pending ?: return@LaunchedEffect
        audioController.clearPendingAdvance()
        val currentDest = navController.currentDestination?.route
        val transLang = audioController.translationLang.value
        if (currentDest == Routes.BOOK || transLang != null) {
            val lang = transLang ?: navController.currentBackStackEntry?.arguments?.getString("lang") ?: "en"
            navController.navigate(Routes.book(lang, p.surah)) {
                popUpTo(Routes.BOOK) { inclusive = true }
            }
        } else {
            navController.navigate(Routes.reader(p.surah, 1))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideInHorizontally(animationSpec = tween(280)) { it }
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = tween(280)) { -it / 4 }
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = tween(280)) { -it / 4 }
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = tween(280)) { it }
        }
    ) {
        composable(Routes.HOME) {
            HomeScreen(container, navController)
        }
        composable(Routes.SURAHS) {
            SurahIndexScreen(container, navController)
        }
        composable(Routes.JUZ) {
            JuzIndexScreen(container, navController)
        }
        composable(
            route = Routes.READER,
            arguments = listOf(
                navArgument("n") { type = NavType.IntType },
                navArgument("v") {
                    type = NavType.IntType
                    defaultValue = -1
                },
                navArgument("end") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { entry ->
            val args = entry.arguments
            val n = args?.getInt("n") ?: 1
            val v = args?.getInt("v")?.takeIf { it > 0 }
            val end = args?.getInt("end") == 1
            ReaderScreen(container, navController, n, v, end)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                container = container,
                onBack = { navController.popBackStack() },
                onAbout = { navController.navigate(Routes.ABOUT) },
                onRecitation = { navController.navigate(Routes.RECITATION) }
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(container, navController)
        }
        composable(Routes.BOOKMARKS) {
            BookmarksScreen(container, navController)
        }
        composable(Routes.RECITATION) {
            RecitationScreen(container, onBack = { navController.popBackStack() })
        }
        composable(Routes.ABOUT) {
            AboutScreen(container, onBack = { navController.popBackStack() })
        }
        composable(Routes.HELP) {
            HelpScreen(container, onBack = { navController.popBackStack() })
        }
        composable(Routes.KHATM) {
            KhatmScreen(container, onBack = { navController.popBackStack() })
        }
        composable(Routes.BOOK_HOME) {
            BookIndexScreen(container, navController)
        }
        composable(
            route = Routes.BOOK,
            arguments = listOf(
                navArgument("lang") { type = NavType.StringType },
                navArgument("n") { type = NavType.IntType }
            )
        ) { entry ->
            val lang = entry.arguments?.getString("lang") ?: "en"
            val n = entry.arguments?.getInt("n") ?: 1
            BookReaderScreen(container, navController, lang, n)
        }
    }
}
