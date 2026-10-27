package com.example.therapp.domain.use_cases.routines

import com.example.therapp.domain.repository.RoutinesRepository


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 9/19/2025
 * @version 1.0
 */
class GetScheduledTraining(
    private val repository: RoutinesRepository
) {
    operator fun invoke() = repository.getScheduledTraining()
}