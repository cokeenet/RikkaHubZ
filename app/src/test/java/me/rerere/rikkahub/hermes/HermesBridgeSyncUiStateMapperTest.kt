package me.rerere.rikkahub.hermes

import me.rerere.rikkahub.ui.pages.setting.hermes.HermesBridgeSyncUiState
import me.rerere.rikkahub.ui.pages.setting.hermes.HermesBridgeSyncUiStateMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class HermesBridgeSyncUiStateMapperTest {
    @Test
    fun fromStatus_mapsRunningStatus() {
        val state = HermesBridgeSyncUiStateMapper.fromStatus(
            HermesSyncStatusResponse(
                isRunning = true,
                currentPhase = "git-push",
                lastTrigger = "api",
            )
        )

        assertEquals("电脑端正在同步", state.status.headline)
        assertEquals("阶段: git-push", state.status.diagnosticMessage)
    }

    @Test
    fun fromRunResponse_marksAlreadyRunningConflictAsActionMessage() {
        val state = HermesBridgeSyncUiStateMapper.fromRunResponse(
            started = false,
            status = HermesSyncStatusResponse(
                isRunning = true,
                currentPhase = "export",
                lastTrigger = "worker",
            )
        )

        assertEquals("电脑端已有同步任务在运行", state.actionMessage)
        assertEquals("电脑端正在同步", state.status.headline)
    }

    @Test
    fun fromStatus_keepsFailureDiagnosticError() {
        val state = HermesBridgeSyncUiStateMapper.fromStatus(
            HermesSyncStatusResponse(
                lastSucceeded = false,
                lastError = "git push failed",
                lastTrigger = "api",
            )
        )

        assertEquals("电脑端上次同步失败", state.status.headline)
        assertEquals("git push failed", state.status.diagnosticMessage)
    }
}
