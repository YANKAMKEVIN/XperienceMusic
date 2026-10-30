package com.xperiencemusic.shared.domain.learning

import com.xperiencemusic.music.pitch.Pitch
import com.xperiencemusic.music.pitch.WavEncoder
import com.xperiencemusic.shared.domain.repository.AudioPlayer
import com.xperiencemusic.shared.domain.repository.FileStore
import com.xperiencemusic.shared.domain.repository.NowPlaying
import com.xperiencemusic.shared.domain.usecase.PlaybackController
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Locally synthesized examples; short key sounds never play a reward over the exercise. */
class PlayLearningExample(private val files:FileStore,private val player:AudioPlayer,private val playback:PlaybackController) {
    operator fun invoke(notes:List<Int>)=play(notes,false,1.4)
    fun key(midi:Int)=play(listOf(midi),false,0.35)
    fun broken(notes:List<Int>)=play(notes,true,0.5)
    fun sequence(notes:List<Int>,bpm:Int) { require(bpm in 40..120); play(notes,true,60.0/bpm) }
    fun reward()=play(listOf(72,76,79),true,0.13)
    fun metronome(bpm:Int) {
        val rate=24_000; val beat=60.0/bpm
        val samples=FloatArray((rate*beat*8).toInt()) { i ->
            val t=(i.toDouble()/rate)%beat
            if(t<0.035) (sin(2*PI*1100*t)*0.2*(1-t/0.035)).toFloat() else 0f
        }
        output("click-$bpm",WavEncoder.encode(samples,rate))
    }
    private fun play(notes:List<Int>,broken:Boolean,seconds:Double) {
        if(notes.isEmpty())return
        val rate=24_000;val duration=if(broken)seconds*notes.size else seconds
        val samples=FloatArray((rate*duration).toInt()) { i ->
            val t=i.toDouble()/rate
            val active=if(broken)listOf(notes[(t/seconds).toInt().coerceAtMost(notes.lastIndex)]) else notes
            val local=if(broken)t%seconds else t
            val envelope=min(1.0,min(local,seconds-local)/0.015).coerceAtLeast(0.0)
            (active.sumOf { midi -> sin(2*PI*Pitch.midiToFrequency(midi.toDouble())*local) }/active.size*0.32*envelope).toFloat()
        }
        output("learning-${notes.joinToString("-")}-$broken-$seconds",WavEncoder.encode(samples,rate))
    }
    private fun output(id:String,bytes:ByteArray) {
        playback.stop()
        player.play(files.write("tones/$id.wav",bytes),NowPlaying("Exemple musical","XperienceMusic"))
    }
    fun stop()=playback.stop()
}
