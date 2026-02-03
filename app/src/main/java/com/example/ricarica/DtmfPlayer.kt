package com.example.ricarica
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.delay

class DtmfPlayer {
    // Volume al 100% (STREAM_DTMF o STREAM_MUSIC sono ok, DTMF è specifico per i toni)
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 100)

    suspend fun playSequence(code: String, onNotePlayed: (Int) -> Unit) {
        for ((index, char) in code.withIndex()) {
            val toneType = getToneForChar(char)
            if (toneType != -1) {
                onNotePlayed(index) // Callback per UI
                toneGenerator.startTone(toneType, 200) // 200ms di suono (un po' più lungo per Arduino)
                delay(200) // Aspetta che finisca il suono
                toneGenerator.stopTone()
                delay(100) // 100ms di silenzio tra i toni
            }
        }
        onNotePlayed(-1) // Reset alla fine
    }

    private fun getToneForChar(c: Char): Int {
        return when (c) {
            '0' -> ToneGenerator.TONE_DTMF_0
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '*' -> ToneGenerator.TONE_DTMF_S // S sta per Star (*)
            '#' -> ToneGenerator.TONE_DTMF_P // P sta per Pound (#)
            else -> -1
        }
    }
}