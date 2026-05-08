package com.lastdone.app.data.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class PlusRepository {
    val isPlus: Flow<Boolean> = flowOf(false)
}
