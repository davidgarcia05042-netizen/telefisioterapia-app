package com.example.therapp.domain.use_cases.routines

import com.example.therapp.domain.repository.RoutinesRepository


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 3/5/2026
 * @version 1.0
 */
class RefreshScheduledTraining(
    private val repository: RoutinesRepository
) {
    suspend operator fun invoke() = repository.refreshScheduledTraining()
}