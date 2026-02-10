package com.example.ricarica

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.delay

class DtmfPlayer {
    // MODIFICA QUI:
    // 1. Usa STREAM_MUSIC invece di STREAM_DTMF (l'emulatore lo gestisce meglio)
    // 2. Volume a 100 (massimo)
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100
    )

    suspend fun playSequence(code: String, onNotePlayed: (Int) -> Unit) {
        for ((index, char) in code.withIndex()) {
            val toneType = getToneForChar(char)
            if (toneType != -1) {
                onNotePlayed(index)
                toneGenerator.startTone(toneType, 300) // Durata
                delay(500) // Attesa fine suono
                toneGenerator.stopTone()
                delay(500) // Pausa tra i suoni
            }
        }
        onNotePlayed(-1)
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
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> -1
        }
    }

    fun release() {
        toneGenerator.release()
    }
}