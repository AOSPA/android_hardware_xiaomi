/*
 * Copyright (C) 2023-25 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.xiaomi.preference

import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Toast
import androidx.core.os.postDelayed
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragment
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import co.aospa.dolby.xiaomi.DolbyConstants
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_BASS
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_DIALOGUE
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_DIALOGUE_AMOUNT
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_ENABLE
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_HP_VIRTUALIZER
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_IEQ
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_PRESET
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_PROFILE
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_RESET
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_SPK_VIRTUALIZER
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_STEREO_WIDENING
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.PREF_VOLUME
import co.aospa.dolby.xiaomi.DolbyConstants.Companion.dlog
import co.aospa.dolby.xiaomi.DolbyController
import co.aospa.dolby.xiaomi.R
import com.android.settingslib.widget.MainSwitchPreference

class DolbySettingsFragment : PreferenceFragment(),
    OnPreferenceChangeListener, OnCheckedChangeListener {

    private val switchBar by lazy {
        findPreference<MainSwitchPreference>(PREF_ENABLE)!!
    }
    private val profilePref by lazy {
        findPreference<ListPreference>(PREF_PROFILE)!!
    }
    private val presetPref by lazy {
        findPreference<Preference>(PREF_PRESET)!!
    }
    private val ieqPref by lazy {
        findPreference<DolbyIeqPreference>(PREF_IEQ)!!
    }
    private val dialoguePref by lazy {
        findPreference<SwitchPreferenceCompat>(PREF_DIALOGUE)!!
    }
    private val dialogueAmountPref by lazy {
        findPreference<SeekBarPreference>(PREF_DIALOGUE_AMOUNT)!!
    }
    private val bassPref by lazy {
        findPreference<SwitchPreferenceCompat>(PREF_BASS)!!
    }
    private val hpVirtPref by lazy {
        findPreference<SwitchPreferenceCompat>(PREF_HP_VIRTUALIZER)!!
    }
    private val spkVirtPref by lazy {
        findPreference<SwitchPreferenceCompat>(PREF_SPK_VIRTUALIZER)!!
    }
    private val volumePref by lazy {
        findPreference<SwitchPreferenceCompat>(PREF_VOLUME)!!
    }
    private val settingsCategory by lazy {
        findPreference<PreferenceCategory>("dolby_category_settings")!!
    }
    private val advSettingsCategory by lazy {
        findPreference<PreferenceCategory>("dolby_category_adv_settings")!!
    }
    private val advSettingsFooter by lazy {
        findPreference<Preference>("dolby_adv_settings_footer")!!
    }
    private var stereoPref: SeekBarPreference? = null

    private val dolbyController by lazy { DolbyController.getInstance(context) }
    private val audioManager by lazy { context.getSystemService(AudioManager::class.java)!! }
    private val handler = Handler()

    private var isOnSpeaker = true
        set(value) {
            if (field == value) return
            field = value
            dlog(TAG, "setIsOnSpeaker($value)")
            updateProfileSpecificPrefs()
        }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            dlog(TAG, "onAudioDevicesAdded")
            updateSpeakerState()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            dlog(TAG, "onAudioDevicesRemoved")
            updateSpeakerState()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        dlog(TAG, "onCreatePreferences")
        addPreferencesFromResource(R.xml.dolby_settings)

        stereoPref = findPreference<SeekBarPreference>(PREF_STEREO_WIDENING)!!
        if (!context.getResources().getBoolean(R.bool.dolby_stereo_widening_supported)) {
            settingsCategory.removePreference(stereoPref!!)
            stereoPref = null
        }

        preferenceManager.preferenceDataStore = DolbyPreferenceStore(context).also {
            it.profile = dolbyController.profile
        }

        val dsOn = dolbyController.dsOn
        switchBar.addOnSwitchChangeListener(this)
        switchBar.setChecked(dsOn)

        profilePref.onPreferenceChangeListener = this
        hpVirtPref.onPreferenceChangeListener = this
        spkVirtPref.onPreferenceChangeListener = this
        stereoPref?.apply {
            onPreferenceChangeListener = this@DolbySettingsFragment
            min = context.resources.getInteger(R.integer.stereo_widening_min)
            max = context.resources.getInteger(R.integer.stereo_widening_max)
        }
        dialoguePref.onPreferenceChangeListener = this
        dialogueAmountPref.apply {
            onPreferenceChangeListener = this@DolbySettingsFragment
            min = context.resources.getInteger(R.integer.dialogue_enhancer_min)
            max = context.resources.getInteger(R.integer.dialogue_enhancer_max)
        }
        bassPref.onPreferenceChangeListener = this
        volumePref.onPreferenceChangeListener = this
        ieqPref.onPreferenceChangeListener = this

        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
        updateSpeakerState()
        updateProfileSpecificPrefsImmediate()
    }

    override fun onDestroyView() {
        dlog(TAG, "onDestroyView")
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        updateProfileSpecificPrefsImmediate()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        dlog(TAG, "onPreferenceChange: key=${preference.key} value=$newValue")
        when (preference.key) {
            PREF_PROFILE -> {
                val profile = newValue.toString().toInt()
                dolbyController.profile = profile
                updateProfileSpecificPrefs()
            }

            PREF_SPK_VIRTUALIZER -> {
                dolbyController.setSpeakerVirtEnabled(newValue as Boolean)
            }

            PREF_HP_VIRTUALIZER -> {
                dolbyController.setHeadphoneVirtEnabled(newValue as Boolean)
            }

            PREF_STEREO_WIDENING -> {
                dolbyController.setStereoWideningAmount(newValue as Int)
            }

            PREF_DIALOGUE -> {
                dolbyController.setDialogueEnhancerEnabled(newValue as Boolean)
            }

            PREF_DIALOGUE_AMOUNT -> {
                dolbyController.setDialogueEnhancerAmount(newValue as Int)
            }

            PREF_BASS -> {
                dolbyController.setBassEnhancerEnabled(newValue as Boolean)
            }

            PREF_VOLUME -> {
                dolbyController.setVolumeLevelerEnabled(newValue as Boolean)
            }

            PREF_IEQ -> {
                dolbyController.setIeqPreset(newValue.toString().toInt())
            }

            else -> return false
        }
        return true
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        dlog(TAG, "onCheckedChanged($isChecked)")
        dolbyController.dsOn = isChecked
        updateProfileSpecificPrefs()
    }

    private fun updateSpeakerState() {
        val device = audioManager.getDevicesForAttributes(ATTRIBUTES_MEDIA)[0]
        isOnSpeaker = (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
    }

    private fun updateProfileSpecificPrefs() {
        handler.postDelayed(100) { updateProfileSpecificPrefsImmediate() }
    }

    private fun updateProfileSpecificPrefsImmediate() {
        if (context == null) return
        if (!dolbyController.dsOn) {
            dlog(TAG, "updateProfileSpecificPrefs: Dolby is off")
            advSettingsCategory.isVisible = false
            return
        }

        val unknownRes = context.getString(R.string.dolby_unknown)
        val headphoneRes = context.getString(R.string.dolby_connect_headphones)
        val currentProfile = dolbyController.profile
        val isDynamicProfile = currentProfile == 0
        (preferenceManager.preferenceDataStore as DolbyPreferenceStore).profile = currentProfile

        dlog(
            TAG, "updateProfileSpecificPrefs: currentProfile=$currentProfile"
                    + " isOnSpeaker=$isOnSpeaker"
        )

        profilePref.apply {
            if (entryValues.contains(currentProfile.toString())) {
                summary = "%s"
                value = currentProfile.toString()
            } else {
                summary = unknownRes
                dlog(TAG, "current profile $currentProfile unknown")
            }
        }

        // hide advanced settings on dynamic profile
        advSettingsCategory.isVisible = !isDynamicProfile
        advSettingsFooter.isVisible = isDynamicProfile

        presetPref.summary = dolbyController.getPresetName()
        bassPref.isChecked = dolbyController.getBassEnhancerEnabled(currentProfile)

        // below prefs are not visible on dynamic profile
        if (isDynamicProfile) return

        val ieqValue = dolbyController.getIeqPreset(currentProfile)
        ieqPref.apply {
            if (entryValues.contains(ieqValue.toString())) {
                summary = "%s"
                value = ieqValue.toString()
            } else {
                summary = unknownRes
                dlog(TAG, "ieq value $ieqValue unknown")
            }
        }

        dialoguePref.isChecked = dolbyController.getDialogueEnhancerEnabled(currentProfile)
        dialogueAmountPref.value = dolbyController.getDialogueEnhancerAmount(currentProfile)

        spkVirtPref.isChecked = dolbyController.getSpeakerVirtEnabled(currentProfile)
        volumePref.isChecked = dolbyController.getVolumeLevelerEnabled(currentProfile)

        // below prefs are not enabled on loudspeaker
        if (isOnSpeaker) {
            hpVirtPref.apply {
                isEnabled = false
                summary = headphoneRes
            }
            return
        }

        hpVirtPref.apply {
            isEnabled = true
            isChecked = dolbyController.getHeadphoneVirtEnabled(currentProfile)
            summary = null
        }

        stereoPref?.value = dolbyController.getStereoWideningAmount(currentProfile)
    }

    companion object {
        private const val TAG = "DolbySettingsFragment"
        private val ATTRIBUTES_MEDIA = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
    }
}
