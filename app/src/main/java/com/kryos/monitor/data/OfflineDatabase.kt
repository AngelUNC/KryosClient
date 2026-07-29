package com.kryos.monitor.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken





class OfflineDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "kryos_offline.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_OFFLINE = "offline_reports"

        private const val COL_ID = "id"
        private const val COL_JSON_DATA = "json_data"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_SYNCED = "synced"

        private val gson = Gson()
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_OFFLINE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_JSON_DATA TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_SYNCED INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTable)

        
        val createIndex = "CREATE INDEX IF NOT EXISTS idx_synced ON $TABLE_OFFLINE($COL_SYNCED)"
        db.execSQL(createIndex)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OFFLINE")
        onCreate(db)
    }

    


    fun saveReport(report: DeviceReport): Long {
        val json = gson.toJson(report)
        val values = ContentValues().apply {
            put(COL_JSON_DATA, json)
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_SYNCED, 0)
        }

        return writableDatabase.insert(TABLE_OFFLINE, null, values)
    }

    


    fun getUnsyncedReports(limit: Int = 100): List<DeviceReport> {
        val reports = mutableListOf<DeviceReport>()
        val cursor = readableDatabase.query(
            TABLE_OFFLINE,
            arrayOf(COL_JSON_DATA),
            "$COL_SYNCED = ?",
            arrayOf("0"),
            null,
            null,
            "$COL_TIMESTAMP ASC",
            limit.toString()
        )

        cursor.use {
            val jsonIndex = it.getColumnIndex(COL_JSON_DATA)
            while (it.moveToNext()) {
                val json = it.getString(jsonIndex)
                try {
                    val report = gson.fromJson(json, DeviceReport::class.java)
                    reports.add(report)
                } catch (_: Exception) {
                    
                }
            }
        }

        return reports
    }

    


    fun getPendingCount(): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_OFFLINE WHERE $COL_SYNCED = 0",
            null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    


    fun markSynced(count: Int) {
        val ids = mutableListOf<Long>()
        val cursor = readableDatabase.query(
            TABLE_OFFLINE,
            arrayOf(COL_ID),
            "$COL_SYNCED = ?",
            arrayOf("0"),
            null,
            null,
            "$COL_TIMESTAMP ASC",
            count.toString()
        )

        cursor.use {
            val idIndex = it.getColumnIndex(COL_ID)
            while (it.moveToNext()) {
                ids.add(it.getLong(idIndex))
            }
        }

        if (ids.isNotEmpty()) {
            val idList = ids.joinToString(",")
            writableDatabase.execSQL(
                "UPDATE $TABLE_OFFLINE SET $COL_SYNCED = 1 WHERE $COL_ID IN ($idList)"
            )
        }
    }

    


    fun cleanupOldSynced() {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        writableDatabase.delete(
            TABLE_OFFLINE,
            "$COL_SYNCED = ? AND $COL_TIMESTAMP < ?",
            arrayOf("1", sevenDaysAgo.toString())
        )
    }

    


    fun clearAll() {
        writableDatabase.delete(TABLE_OFFLINE, null, null)
    }
}