@file:Suppress("DEPRECATION")

package com.feofanova.mathup.ui.navigation

import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import androidx.compose.animation.*
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.data.local.db.MathUpOgeDatabase
import com.feofanova.mathup.data.stats.StatsDatabase
import com.feofanova.mathup.ui.components.MathKeyboardCoordinator
import com.feofanova.mathup.ui.screens.news.NewsScreen
import com.feofanova.mathup.ui.screens.auth.AuthScreen
import com.feofanova.mathup.ui.screens.exam.ExamScreen
import com.feofanova.mathup.ui.screens.main.MainScreen
import com.feofanova.mathup.ui.screens.main.ProfileViewModel
import com.feofanova.mathup.ui.screens.preparation.PreparationScreen
import com.feofanova.mathup.ui.screens.reference.ReferenceScreen
import com.feofanova.mathup.ui.screens.rules.RulesScreen
import com.feofanova.mathup.ui.screens.settings.SettingsScreen
import com.feofanova.mathup.ui.screens.splash.SplashScreen
import com.feofanova.mathup.ui.screens.videos.VideoScreen
import com.feofanova.mathup.ui.components.VideoPlayerScreen
import com.feofanova.mathup.ui.screens.formulas.FormulasScreen
import com.feofanova.mathup.ui.screens.game.GamesScreen
import com.feofanova.mathup.ui.screens.preparation.FormulasPreparationScreen
import com.feofanova.mathup.ui.screens.preparation.TaskScreen
import com.feofanova.mathup.ui.screens.preparation.TaskViewModel
import com.feofanova.mathup.ui.screens.preparation.TaskViewModelFactory
import com.feofanova.mathup.ui.screens.reference.ReferenceDetailScreen


object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val MAIN = "main"
    const val NEWS = "news"
    const val SETTINGS = "settings"
    const val RULES = "rules"
    const val EXAM = "exam"
    const val PREPARATION = "preparation"
    const val FORMULAS_DETAIL = "formulasDetail"
    const val TASKS = "tasks"
    const val REFERENCE = "reference"
    const val REFERENCE_DETAIL = "referenceDetail"
    const val FORMULAS = "formulas"
    const val VIDEO = "video"
    const val VIDEO_PLAYER = "videoPlayer"
    const val GAMES = "games"
    const val TEST = "test"

}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    //startDestination: String = Routes.TEST,
    startDestination: String = Routes.SPLASH,
    dao: AppDao,
    profileViewModel: ProfileViewModel,
    isSoundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit
) {
    val selectedProfile by profileViewModel.selectedProfile.collectAsStateWithLifecycle()

    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {

        composable(Routes.SPLASH) { SplashScreen(navController) }
        composable(Routes.AUTH) { AuthScreen(navController) }
        composable(Routes.MAIN) { MainScreen(
            navController = navController,
            isSoundEnabled = isSoundEnabled,
            onSoundToggle = onSoundToggle) }
        composable(Routes.NEWS) { NewsScreen(navController) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                navController = navController,
                isSoundEnabled = isSoundEnabled,
                onSoundToggle = onSoundToggle
            )
        }


        composable("${Routes.RULES}/{profile}") { backStackEntry ->
            val profile = backStackEntry.arguments?.getString("profile") ?: ""
            RulesScreen(
                profile = profile,
                onBack = { navController.popBackStack() }
            )
        }

        composable("${Routes.EXAM}/{profile}") { backStackEntry ->
            val profile = backStackEntry.arguments?.getString("profile") ?: ""
            val coordinator = remember { MathKeyboardCoordinator() }

            val context = LocalContext.current
            val baseDao = remember { MathUpDatabase.getInstance(context).appDao() }
            val ogeDao = remember { MathUpOgeDatabase.getInstance(context).appDao() }

            ExamScreen(
                profile = profile,
                baseDao = baseDao,
                ogeDao = ogeDao,
                coordinator = coordinator
            )
        }


        composable("${Routes.PREPARATION}/{profile}") { backStackEntry ->
            val profile = backStackEntry.arguments?.getString("profile") ?: ""
            val context = LocalContext.current

            val baseDao = remember { MathUpDatabase.getInstance(context).appDao() }
            val ogeDao = remember { MathUpOgeDatabase.getInstance(context).appDao() }
            val statsDao = remember { StatsDatabase.getInstance(context).statsDao() }

            val factory = remember { TaskViewModelFactory(baseDao, ogeDao, statsDao, profile) }
            val viewModel: TaskViewModel = viewModel(factory = factory)

            PreparationScreen(
                profile = profile,
                onBack = { navController.popBackStack() },
                navController = navController,
                viewModel = viewModel
            )
        }

        composable(
            route = "${Routes.TASKS}/{blockId}/{profile}",
            arguments = listOf(
                navArgument("blockId") { type = NavType.IntType },
                navArgument("profile") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val blockId = backStackEntry.arguments!!.getInt("blockId")
            val profile = backStackEntry.arguments!!.getString("profile") ?: ""
            val context = LocalContext.current

            val baseDao = remember { MathUpDatabase.getInstance(context).appDao() }
            val ogeDao = remember { MathUpOgeDatabase.getInstance(context).appDao() }

            TaskScreen(
                blockId = blockId,
                profile = profile,
                onBack = { navController.popBackStack() },
                baseDao = baseDao,
                ogeDao = ogeDao
            )
        }


        composable(
            route = "${Routes.FORMULAS_DETAIL}/{blockId}/{profile}",
            arguments = listOf(
                navArgument("blockId") { type = NavType.LongType },
                navArgument("profile") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val blockId = backStackEntry.arguments!!.getLong("blockId")
            val profile = backStackEntry.arguments!!.getString("profile") ?: ""
            FormulasPreparationScreen(
                blockId = blockId,
                profile = profile,
                onBack = { navController.popBackStack() }
            )
        }

        composable("${Routes.REFERENCE}/{profile}") { backStackEntry ->
            val profile = backStackEntry.arguments?.getString("profile") ?: ""
            ReferenceScreen(
                profile = profile,
                onNavigateToDetail = { blockId ->
                    navController.navigate("${Routes.REFERENCE_DETAIL}/$blockId/$profile")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.REFERENCE_DETAIL}/{blockId}/{profile}",
            arguments = listOf(
                navArgument("blockId") { type = NavType.LongType },
                navArgument("profile") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val blockId = backStackEntry.arguments!!.getLong("blockId")
            val profile = backStackEntry.arguments!!.getString("profile") ?: ""
            ReferenceDetailScreen(blockId = blockId, profile = profile) {
                navController.popBackStack()
            }
        }


        composable("${Routes.FORMULAS}/{profile}") { backStackEntry ->
            val profile = backStackEntry.arguments?.getString("profile") ?: ""
            FormulasScreen(
                profile = profile,
                onBack = { navController.popBackStack() }
            )
        }


        // Список видео с переходом на встроенный плеер
        composable("${Routes.VIDEO}/{profile}") { backStackEntry ->
            val profile = backStackEntry.arguments?.getString("profile") ?: ""
            VideoScreen(
                profile = profile,
                onBack = { navController.popBackStack() },
                navController = navController
            )
        }

        // Экран плеера принимает URL видео как аргумент
        composable(
            route = "${Routes.VIDEO_PLAYER}/{videoUrl}",
            arguments = listOf(navArgument("videoUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoUrl = Uri.decode(backStackEntry.arguments!!.getString("videoUrl")!!)
            VideoPlayerScreen(
                videoUrl = videoUrl,
                onBack = { navController.popBackStack() }
            )
        }

        composable("${Routes.GAMES}/{profile}") { backStackEntry ->
            val profile = backStackEntry.arguments?.getString("profile") ?: ""
            GamesScreen(
                profile = profile,
                onBack = { navController.popBackStack() })
        }
        composable(Routes.TEST) {
            com.feofanova.mathup.testing.TestScreen()
        }

    }
}
