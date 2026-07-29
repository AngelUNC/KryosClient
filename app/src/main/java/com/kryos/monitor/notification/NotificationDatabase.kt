package com.kryos.monitor.notification

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class NotificationDatabase(context: Context) :
    SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION
    ) {

    companion object {
        private const val DATABASE_NAME = "notifications.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE = "offline_notifications"

        private const val COL_ID = "id"
        private const val COL_DEVICE_ID = "device_id"
        private const val COL_DEVICE_NAME = "device_name"
        private const val COL_PACKAGE = "package_name"
        private const val COL_NOTIFICATION_ID = "notification_id"
        private const val COL_TAG = "tag"
        private const val COL_KEY = "notification_key"
        private const val COL_TITLE = "title"
        private const val COL_TEXT = "text"
        private const val COL_POST_TIME = "post_time"
        private const val COL_RECEIVED_AT = "received_at"
        private const val COL_ONGOING = "is_ongoing"
        private const val COL_CLEARABLE = "is_clearable"
        private const val COL_SYNCED = "synced"
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_DEVICE_ID TEXT NOT NULL,
                $COL_DEVICE_NAME TEXT NOT NULL,
                $COL_PACKAGE TEXT NOT NULL,
                $COL_NOTIFICATION_ID INTEGER NOT NULL,
                $COL_TAG TEXT,
                $COL_KEY TEXT NOT NULL,
                $COL_TITLE TEXT,
                $COL_TEXT TEXT,
                $COL_POST_TIME INTEGER NOT NULL,
                $COL_RECEIVED_AT INTEGER NOT NULL,
                $COL_ONGOING INTEGER NOT NULL,
                $COL_CLEARABLE INTEGER NOT NULL,
                $COL_SYNCED INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun saveNotification(notification: NotificationEntity) {

        val values = ContentValues().apply {

            put(COL_DEVICE_ID, notification.deviceId)
            put(COL_DEVICE_NAME, notification.deviceName)
            put(COL_PACKAGE, notification.packageName)
            put(COL_NOTIFICATION_ID, notification.notificationId)
            put(COL_TAG, notification.tag)
            put(COL_KEY, notification.key)
            put(COL_TITLE, notification.title)
            put(COL_TEXT, notification.text)
            put(COL_POST_TIME, notification.postTime)
            put(COL_RECEIVED_AT, notification.receivedAt)
            put(COL_ONGOING, if (notification.isOngoing) 1 else 0)
            put(COL_CLEARABLE, if (notification.isClearable) 1 else 0)
            put(COL_SYNCED, if (notification.synced) 1 else 0)
        }

        writableDatabase.insert(TABLE, null, values)
    }

    fun getUnsyncedNotifications(limit: Int = 100): List<NotificationEntity> {

        val list = mutableListOf<NotificationEntity>()

        val cursor = readableDatabase.query(
            TABLE,
            null,
            "$COL_SYNCED=0",
            null,
            null,
            null,
            "$COL_RECEIVED_AT ASC",
            limit.toString()
        )

        cursor.use {

            while (it.moveToNext()) {

                list.add(
                    NotificationEntity(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        deviceId = it.getString(it.getColumnIndexOrThrow(COL_DEVICE_ID)),
                        deviceName = it.getString(it.getColumnIndexOrThrow(COL_DEVICE_NAME)),
                        packageName = it.getString(it.getColumnIndexOrThrow(COL_PACKAGE)),
                        notificationId = it.getInt(it.getColumnIndexOrThrow(COL_NOTIFICATION_ID)),
                        tag = it.getString(it.getColumnIndexOrThrow(COL_TAG)),
                        key = it.getString(it.getColumnIndexOrThrow(COL_KEY)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                        text = it.getString(it.getColumnIndexOrThrow(COL_TEXT)),
                        postTime = it.getLong(it.getColumnIndexOrThrow(COL_POST_TIME)),
                        receivedAt = it.getLong(it.getColumnIndexOrThrow(COL_RECEIVED_AT)),
                        isOngoing = it.getInt(it.getColumnIndexOrThrow(COL_ONGOING)) == 1,
                        isClearable = it.getInt(it.getColumnIndexOrThrow(COL_CLEARABLE)) == 1,
                        synced = false
                    )
                )
            }
        }

        return list
    }

    fun markSynced(ids: List<Long>) {

        val db = writableDatabase

        ids.forEach { id ->

            val values = ContentValues().apply {
                put(COL_SYNCED, 1)
            }

            db.update(
                TABLE,
                values,
                "$COL_ID=?",
                arrayOf(id.toString())
            )
        }
    }

    fun getPendingCount(): Int {

        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE WHERE $COL_SYNCED=0",
            null
        )

        cursor.use {

            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }

        return 0
    }

    fun cleanupOldSynced() {

        writableDatabase.delete(
            TABLE,
            "$COL_SYNCED=1",
            null
        )
    }
}
