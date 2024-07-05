/*
 * Copyright (C) 2023-24 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioManager.AudioPlaybackCallback
import android.media.AudioPlaybackConfiguration
import android.media.session.MediaSessionManager
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import co.aospa.dolby.xiaomi.DolbyConstants.DsParam
import co.aospa.dolby.xiaomi.R

internal class DolbyController private constructor(
    private val context: Context
) {
    private var dolbyEffect = DolbyAudioEffect(EFFECT_PRIORITY, audioSession = 0)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler()

    // Restore current profile on every media session
    private val playbackCallback = object : AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            val isPlaying = configs.any {
                it.playerState == AudioPlaybackConfiguration.PLAYER_STATE_STARTED
            }
            dlog("onPlaybackConfigChanged: isPlaying=$isPlaying")
            if (isPlaying)
                setCurrentProfile()
        }
    }

    // Restore current profile on audio device change
    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            dlog("onAudioDevicesAdded")
            setCurrentProfile()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            dlog("onAudioDevicesRemoved")
            setCurrentProfile()
        }
    }

    private var registerCallbacks = false
        set(value) {
            if (field == value) return
            field = value
            dlog("setRegisterCallbacks($value)")
            if (value) {
                audioManager.registerAudioPlaybackCallback(playbackCallback, handler)
                audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
            } else {
                audioManager.unregisterAudioPlaybackCallback(playbackCallback)
                audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            }
        }

    var dsOn: Boolean
        get() =
            dolbyEffect.dsOn.also {
                dlog("getDsOn: $it")
            }
        set(value) {
            dlog("setDsOn: $value")
            checkEffect()
            dolbyEffect.dsOn = value
            registerCallbacks = value
            if (value)
                setCurrentProfile()
        }

    var profile: Int
        get() =
            dolbyEffect.profile.also {
                dlog("getProfile: $it")
            }
        set(value) {
            dlog("setProfile: $value")
            checkEffect()
            dolbyEffect.profile = value
        }

    var preset: String
        get() {
            val gains = dolbyEffect.getDapParameter(DsParam.GEQ_BAND_GAINS)
            return gains.joinToString(separator = ",").also {
                dlog("getPreset: $it")
            }
        }
        set(value) {
            dlog("setPreset: $value")
            checkEffect()
            val gains = value.split(",")
                    .map { it.toInt() }
                    .toIntArray()
            dolbyEffect.setDapParameter(DsParam.GEQ_BAND_GAINS, gains)
        }

    var headphoneVirtEnabled: Boolean
        get() =
            dolbyEffect.getDapParameterBool(DsParam.HEADPHONE_VIRTUALIZER).also {
                dlog("getHeadphoneVirtEnabled: $it")
            }
        set(value) {
            dlog("setHeadphoneVirtEnabled: $value")
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.HEADPHONE_VIRTUALIZER, value)
        }

    var speakerVirtEnabled: Boolean
        get() =
            dolbyEffect.getDapParameterBool(DsParam.SPEAKER_VIRTUALIZER).also {
                dlog("getSpeakerVirtEnabled: $it")
            }
        set(value) {
            dlog("setSpeakerVirtEnabled: $value")
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.SPEAKER_VIRTUALIZER, value)
        }

    var bassEnhancerEnabled: Boolean
        get() =
            dolbyEffect.getDapParameterBool(DsParam.BASS_ENHANCER_ENABLE).also {
                dlog("getBassEnhancerEnabled: $it")
            }
        set(value) {
            dlog("setBassEnhancerEnabled: $value")
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.BASS_ENHANCER_ENABLE, value)
        }

    var volumeLevelerEnabled: Boolean
        get() {
            val enabled: Boolean = dolbyEffect.getDapParameterBool(DsParam.VOLUME_LEVELER_ENABLE)
            val amount: Int = dolbyEffect.getDapParameterInt(DsParam.VOLUME_LEVELER_AMOUNT)
            dlog("getVolumeLevelerEnabled: enabled=$enabled amount=$amount")
            return enabled && amount > 0
        }
        set(value) {
            dlog("setVolumeLevelerEnabled: $value")
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.VOLUME_LEVELER_ENABLE, value)
            dolbyEffect.setDapParameter(DsParam.VOLUME_LEVELER_AMOUNT,
                    if (value) VOLUME_LEVELER_AMOUNT else 0)
        }

    var stereoWideningAmount: Int
        get() =
            dolbyEffect.getDapParameterInt(DsParam.STEREO_WIDENING_AMOUNT).also {
                dlog("getStereoWideningAmount: $it")
            }
        set(value) {
            dlog("setStereoWideningAmount: $value")
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.STEREO_WIDENING_AMOUNT, value)
        }

    var dialogueEnhancerAmount: Int
        get() {
            val enabled: Boolean = dolbyEffect.getDapParameterBool(DsParam.DIALOGUE_ENHANCER_ENABLE)
            val amount: Int = if (enabled) {
                dolbyEffect.getDapParameterInt(DsParam.DIALOGUE_ENHANCER_AMOUNT)
            } else 0
            dlog("getDialogueEnhancerAmount: enabled=$enabled amount=$amount")
            return amount
        }
        set(value) {
            dlog("setDialogueEnhancerAmount: $value")
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.DIALOGUE_ENHANCER_ENABLE, (value > 0))
            dolbyEffect.setDapParameter(DsParam.DIALOGUE_ENHANCER_AMOUNT, value)
        }

    init {
        dlog("initialized")
    }

    fun onBootCompleted() {
        dlog("onBootCompleted")

        // Restore current profile now and on certain audio changes.
        dolbyEffect.enabled = dsOn
        registerCallbacks = dsOn
        if (dsOn)
            setCurrentProfile()
    }

    private fun checkEffect() {
        if (!dolbyEffect.hasControl()) {
            Log.w(TAG, "lost control, recreating effect")
            dolbyEffect.release()
            dolbyEffect = DolbyAudioEffect(EFFECT_PRIORITY, 0)
        }
    }

    private fun setCurrentProfile() {
        if (!dsOn) {
            dlog("setCurrentProfile: skip, dolby is off")
            return
        }
        dlog("setCurrentProfile")
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        profile = prefs.getString(DolbyConstants.PREF_PROFILE, "0" /*dynamic*/).toInt()
    }

    fun getProfileName(): String? {
        val profile = dolbyEffect.profile.toString()
        val profiles = context.resources.getStringArray(R.array.dolby_profile_values)
        val profileIndex = profiles.indexOf(profile)
        dlog("getProfileName: profile=$profile index=$profileIndex")
        return if (profileIndex == -1) null else context.resources.getStringArray(
                R.array.dolby_profile_entries)[profileIndex]
    }

    fun resetProfileSpecificSettings() {
        checkEffect()
        dolbyEffect.resetProfileSpecificSettings()
    }

    companion object {
        private const val TAG = "DolbyController"
        private const val EFFECT_PRIORITY = 100
        private const val VOLUME_LEVELER_AMOUNT = 2

        @Volatile private var instance: DolbyController? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: DolbyController(context).also { instance = it }
            }

        private fun dlog(msg: String) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, msg)
            }
        }
    }
}
