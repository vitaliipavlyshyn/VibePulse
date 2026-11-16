package com.vpch.vibepulse.domain.usecase

import com.vpch.vibepulse.domain.coordinator.ActivityCoordinator
import com.vpch.vibepulse.domain.repository.AuthRepository
import com.vpch.vibepulse.domain.repository.PlaybackRepository
import com.vpch.vibepulse.domain.repository.UserRepository
import com.vpch.vibepulse.service.RunTrackerNotifier

class LogoutUseCase(
    private val runTrackerNotifier: RunTrackerNotifier,
    private val playbackRepository: PlaybackRepository,
    private val activityCoordinator: ActivityCoordinator,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke() {
        runTrackerNotifier.onRunStopped()
        playbackRepository.pauseImmediate()
        playbackRepository.pause()
        activityCoordinator.endSession()
        authRepository.clearSession()
        userRepository.clearCache()
    }
}