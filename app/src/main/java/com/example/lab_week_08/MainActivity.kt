package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.*
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    private val workManager by lazy { WorkManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //Handle system insets (status/navigation bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Ask for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        //Define first worker
        val firstRequest = OneTimeWorkRequestBuilder<FirstWorker>()
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        //Define second worker
        val secondRequest = OneTimeWorkRequestBuilder<SecondWorker>()
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        //Chain workers: First -> Second
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        //Observe first worker
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("First process is done")
                }
            }

        //Observe second worker
        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info != null && info.state.isFinished) {
                    showResult("Second process is done")
                    launchNotificationService()
                }
            }

        //Observe first notification service
        NotificationService.trackingCompletion.observe(this) { channelId ->
            showResult("NotificationService finished for Channel $channelId")

            //Define third worker
            val thirdRequest = OneTimeWorkRequestBuilder<ThirdWorker>()
                .setConstraints(networkConstraints)
                .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, "003"))
                .build()

            //Enqueue third worker
            workManager.enqueue(thirdRequest)

            //Observe third worker
            workManager.getWorkInfoByIdLiveData(thirdRequest.id)
                .observe(this) { info ->
                    if (info != null && info.state.isFinished) {
                        showResult("Third process is done")
                        launchSecondNotificationService()
                    }
                }
        }

        //Observe second notification service
        SecondNotificationService.trackingCompletion.observe(this) { Id ->
            showResult("Process for Second Notification Channel ID $Id is done!")
        }
    }

    private fun getIdInputData(idKey: String, idValue: String): Data {
        return Data.Builder()
            .putString(idKey, idValue)
            .build()
    }

    //Start the first notification service
    private fun launchNotificationService() {
        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(NotificationService.EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    //Start the second notification service
    private fun launchSecondNotificationService() {
        val serviceIntent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(SecondNotificationService.EXTRA_ID, "002")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun enableEdgeToEdge() {
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}
