package nay.kirill.workmanagersample

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.encodeToString
import nay.kirill.workmanagersample.databinding.ActivityMainBinding
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var _viewBinding: ActivityMainBinding? = null
    private val viewBinding: ActivityMainBinding
        get() = _viewBinding ?: error("ViewBinding is not initialized yet!")

    private val sharedPrefs by lazy { getSharedPreferences("user_data", Context.MODE_PRIVATE) }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Разрешение на уведомления отклонено!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        checkNotificationPermission()
        createNotificationChannel()

        loadDraft()

        viewBinding.submitButton.setOnClickListener {
            saveFinalForm()
            cancelReminders()
        }

        cancelReminders()
    }

    override fun onStop() {
        super.onStop()
        saveDraft()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33 (Android 13)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun saveDraft() {
        if (viewBinding.nameEditText.text.isNotEmpty() || viewBinding.surnameEditText.text.isNotEmpty() || viewBinding.descriptionEditText.text.isNotEmpty()) {
            val formData = FormData(
                viewBinding.nameEditText.text.toString(),
                viewBinding.surnameEditText.text.toString(),
                viewBinding.descriptionEditText.text.toString()
            )
            val draftFile = File(filesDir, "draft_form.json")
            FileWriter(draftFile).use { it.write(encodeToString(FormData.serializer(), formData)) }

            sharedPrefs.edit().putString("draft_file", draftFile.absolutePath).apply()
            Toast.makeText(this, "Черновик сохранен", Toast.LENGTH_SHORT).show()
            scheduleReminderWorker()
        }
    }

    private fun loadDraft() {
        val draftFile = File(filesDir, "draft_form.json")
        if (draftFile.exists()) {
            val formData = Json.decodeFromString<FormData>(FileReader(draftFile).readText())
            viewBinding.nameEditText.setText(formData.name)
            viewBinding.surnameEditText.setText(formData.surname)
            viewBinding.descriptionEditText.setText(formData.description)
        }
    }

    private fun saveFinalForm() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val finalFile = File(filesDir, "form_$timeStamp.json")
        val formData = FormData(
            viewBinding.nameEditText.text.toString(),
            viewBinding.surnameEditText.text.toString(),
            viewBinding.descriptionEditText.text.toString()
        )
        FileWriter(finalFile).use { it.write(encodeToString(FormData.serializer(), formData)) }

        val draftFile = File(filesDir, "draft_form.json")
        if (draftFile.exists()) {
            draftFile.delete()
        }

        Toast.makeText(this, "Заявление подано!", Toast.LENGTH_SHORT).show()
        clearForm()
    }

    private fun clearForm() {
        viewBinding.nameEditText.text.clear()
        viewBinding.surnameEditText.text.clear()
        viewBinding.descriptionEditText.text.clear()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Reminder Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для напоминаний о заполнении формы"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleReminderWorker() {
        Log.i("WorkManagerSample", "Schedule worker")
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(90, TimeUnit.SECONDS)
            .setInitialDelay(15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "reminder_work",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun cancelReminders() {
        Log.i("WorkManagerSample", "Cancel worker")
        WorkManager.getInstance(this).cancelUniqueWork("reminder_work")
    }
}
