package com.seekersofjustice.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.seekersofjustice.app.ui.casedetail.CaseDetailScreen
import com.seekersofjustice.app.ui.cases.CaseListScreen
import com.seekersofjustice.app.ui.memo.MemoDetailScreen
import com.seekersofjustice.app.ui.record.RecordMemoScreen

@Composable
fun AppNavHost(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = CaseListRoute) {
        composable<CaseListRoute> {
            CaseListScreen(
                onCaseClick = { caseId -> navController.navigate(CaseDetailRoute(caseId)) },
                onSignOut = onSignOut,
            )
        }
        composable<CaseDetailRoute> {
            CaseDetailScreen(
                onBack = { navController.popBackStack() },
                onAddMemo = { caseId -> navController.navigate(RecordMemoRoute(caseId)) },
                onMemoClick = { memoId -> navController.navigate(MemoDetailRoute(memoId)) },
            )
        }
        composable<RecordMemoRoute> {
            RecordMemoScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable<MemoDetailRoute> {
            MemoDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
