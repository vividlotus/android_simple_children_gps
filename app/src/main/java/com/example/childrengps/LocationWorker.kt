package com.example.childrengps

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class LocationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        Util.getLocationAndSendToSlack(applicationContext)
        return Result.success()
    }
}
