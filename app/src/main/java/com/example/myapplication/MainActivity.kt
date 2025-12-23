package com.example.myapplication

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Объявляем переменные
    private lateinit var player: MediaPlayer
    private lateinit var playBtn: Button
    private lateinit var nextBtn: Button
    private lateinit var prevBtn: Button
    private lateinit var seekBar: SeekBar
    private lateinit var songTitle: TextView
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView

    private var isPlaying = false
    private var currentSongIndex = 0

    // СПИСОК ПЕСЕН - ТЕПЕРЬ ОБЪЯВЛЕН ПРАВИЛЬНО
    private val songs = mutableListOf<Song>()

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализируем список песен
        loadSongsFromRaw()

        // Проверяем, есть ли песни
        if (songs.isEmpty()) {
            Toast.makeText(this, "Нет песен в папке res/raw", Toast.LENGTH_SHORT).show()
            return
        }

        initViews()
        setupPlayer()
        updateSongInfo()
    }

    private fun loadSongsFromRaw() {
        try {
            // Получаем все ресурсы из папки raw
            val resources = resources
            val fields = R.raw::class.java.fields

            for (field in fields) {
                val resourceId = field.getInt(null)
                val resourceName = resources.getResourceEntryName(resourceId)
                // Форматируем название песни
                val title = resourceName
                    .replace("_", " ")
                    .replace(".mp3", "")
                    .replaceFirstChar { it.uppercaseChar() }

                songs.add(Song(title, resourceId))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Если не получилось автоматически, добавляем песню вручную
            songs.add(Song("My Song", R.raw.lover_you_ve_should_come_over))
        }
    }

    private fun initViews() {
        playBtn = findViewById(R.id.playBtn)
        nextBtn = findViewById(R.id.nextBtn)
        prevBtn = findViewById(R.id.prevBtn)
        seekBar = findViewById(R.id.seekBar)
        songTitle = findViewById(R.id.songTitle)
        currentTime = findViewById(R.id.currentTime)
        totalTime = findViewById(R.id.totalTime)
    }

    private fun setupPlayer() {
        player = MediaPlayer.create(this, songs[currentSongIndex].resourceId)

        playBtn.setOnClickListener { togglePlayPause() }
        nextBtn.setOnClickListener { playNext() }
        prevBtn.setOnClickListener { playPrevious() }

        player.setOnCompletionListener {
            if (currentSongIndex < songs.size - 1) {
                playNext()
            } else {
                playBtn.text = "▶"
                isPlaying = false
                seekBar.progress = 0
                currentTime.text = "0:00"
            }
        }

        seekBar.max = player.duration
        totalTime.text = formatTime(player.duration.toLong())

        updateRunnable = Runnable {
            if (player.isPlaying) {
                seekBar.progress = player.currentPosition
                currentTime.text = formatTime(player.currentPosition.toLong())
                handler.postDelayed(updateRunnable, 1000)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player.seekTo(progress)
                    currentTime.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun togglePlayPause() {
        if (!isPlaying) {
            player.start()
            playBtn.text = "⏸"
            isPlaying = true
            handler.post(updateRunnable)
        } else {
            player.pause()
            playBtn.text = "▶"
            isPlaying = false
            handler.removeCallbacks(updateRunnable)
        }
    }

    private fun playNext() {
        if (currentSongIndex < songs.size - 1) {
            currentSongIndex++
            restartPlayer()
        }
    }

    private fun playPrevious() {
        if (player.currentPosition > 3000) {
            // Если проигрывается больше 3 секунд, перемотать в начало
            player.seekTo(0)
            seekBar.progress = 0
            currentTime.text = "0:00"
        } else if (currentSongIndex > 0) {
            // Если это начало песни, перейти к предыдущей
            currentSongIndex--
            restartPlayer()
        } else {
            // Первая песня - перемотать в начало
            player.seekTo(0)
            seekBar.progress = 0
            currentTime.text = "0:00"
        }
    }

    private fun restartPlayer() {
        player.release()
        player = MediaPlayer.create(this, songs[currentSongIndex].resourceId)
        setupPlayer()
        updateSongInfo()

        if (isPlaying) {
            player.start()
            handler.post(updateRunnable)
        }
    }

    private fun updateSongInfo() {
        songTitle.text = songs[currentSongIndex].title
        seekBar.max = player.duration
        totalTime.text = formatTime(player.duration.toLong())
        seekBar.progress = 0
        currentTime.text = "0:00"
    }

    private fun formatTime(milliseconds: Long): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        handler.removeCallbacks(updateRunnable)
    }
}

data class Song(val title: String, val resourceId: Int)