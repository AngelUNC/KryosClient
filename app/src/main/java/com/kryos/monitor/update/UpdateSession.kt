package com.kryos.monitor.update

import android.content.Context

class UpdateSession(context: Context) {

    companion object {
        private const val PREF_NAME = "kryos_update"

        private const val KEY_STATE = "state"
        private const val KEY_URL = "url"
        private const val KEY_PROGRESS = "progress"
    }

    private val prefs =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveState(state: UpdateState) {
        prefs.edit()
            .putString(KEY_STATE, state.name)
            .apply()
    }

    fun getState(): UpdateState {
        val value = prefs.getString(KEY_STATE, UpdateState.IDLE.name)
            ?: UpdateState.IDLE.name

        return UpdateState.valueOf(value)
    }

    fun saveUrl(url: String) {
        prefs.edit()
            .putString(KEY_URL, url)
            .apply()
    }

    fun getUrl(): String? {
        return prefs.getString(KEY_URL, null)
    }

    fun saveProgress(progress: Int) {
        prefs.edit()
            .putInt(KEY_PROGRESS, progress)
            .apply()
    }

    fun getProgress(): Int {
        return prefs.getInt(KEY_PROGRESS, 0)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
