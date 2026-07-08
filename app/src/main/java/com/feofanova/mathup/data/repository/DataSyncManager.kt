package com.feofanova.mathup.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.data.local.db.MathUpOgeDatabase
import com.feofanova.mathup.data.local.entities.*
import com.feofanova.mathup.data.remote.model.ExportedBlock
import com.feofanova.mathup.sync.SyncMetadata
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL
import androidx.core.content.edit


object DataSyncManager {

    private const val JSON_URL =
        "https://firebasestorage.googleapis.com/v0/b/mathappforrus.appspot.com/o/databases%2Fdata_export.json?alt=media"

    suspend fun syncFromRemote(context: Context, database: MathUpDatabase) {
        withContext(Dispatchers.IO) {
            try {

                val json = URL(JSON_URL).readText()

                val gson = Gson()
                val blockListType = object : TypeToken<List<ExportedBlock>>() {}.type
                val blocks: List<ExportedBlock> = gson.fromJson(json, blockListType)


                if (blocks.isEmpty()) {
                    Log.w("DataSync", "Получен пустой список блоков")
                }

                database.withTransaction {
                    val dao = database.appDao()

                    dao.clearBlocks()
                    dao.clearTasks()
                    dao.clearSteps()
                    dao.clearFormulas()

                    blocks.forEachIndexed { index, block ->
                        try {


                            val blockEntity = QuestionBlockEntity(
                                blockID = block.blockID,
                                name = block.name,
                                videoLink = block.videoLink,
                                referenceMaterial = block.referenceMaterial
                            )
                            dao.insertBlocks(listOf(blockEntity))

                            val taskEntities = block.tasks.map { task ->

                                TaskEntity(
                                    taskID = task.taskID,
                                    description = task.description,
                                    drawingLink = task.drawingLink,
                                    answer = task.answer,
                                    hint = task.hint,
                                    given = task.given,
                                    blockOwnerID = block.blockID
                                )
                            }

                            dao.insertTasks(taskEntities)

                            val stepEntities = block.tasks.flatMap { task ->
                                task.steps.map { step ->

                                    StepEntity(
                                        stepID = step.stepID,
                                        taskOwnerID = task.taskID,
                                        solutionVariant = step.solutionVariant,
                                        stepDescription = step.stepDescription,
                                        stepAction = step.stepAction ?: ""
                                    )
                                }
                            }
                            dao.insertSteps(stepEntities)

                            val formulaEntities = block.formulas.map { formula ->

                                FormulaEntity(
                                    formulaID = formula.formulaID,
                                    name = formula.name,
                                    formula = formula.formula,
                                    blockOwnerID = block.blockID
                                )
                            }
                            dao.insertFormulas(formulaEntities)

                        } catch (e: Exception) {
                            Log.e("DataSync", "Ошибка при обработке блока ${index + 1}: ${block.name}", e)
                        }
                    }
                }

                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val egeVersion = firestore.collection("sync_metadata")
                        .document("MathUpDatabase").get().await().getLong("version")?.toInt() ?: 1

                    val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

                    val existing = prefs.getString("sync_metadata", null)
                    val oldMetadata = existing?.let {
                        try {
                            Gson().fromJson(it, SyncMetadata::class.java)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    val updatedMetadata = oldMetadata?.copy(versionEge = egeVersion)
                        ?: SyncMetadata(versionEge = egeVersion)

                    prefs.edit { putString("sync_metadata", Gson().toJson(updatedMetadata)) }


                } catch (e: Exception) {
                    Log.e("DataSync", "Не удалось обновить версию EGE из Firestore", e)
                }

            } catch (e: Exception) {
                Log.e("DataSync", "Ошибка при синхронизации", e)
                throw e
            }
        }
    }
    private const val OGE_JSON_URL =
        "https://firebasestorage.googleapis.com/v0/b/mathappforrus.appspot.com/o/databases%2Fdata_export_oge.json?alt=media"

    suspend fun syncOgeFromRemote(context: Context, database: MathUpOgeDatabase) {
        withContext(Dispatchers.IO) {
            try {

                val json = URL(OGE_JSON_URL).readText()

                val gson = Gson()
                val blockListType = object : TypeToken<List<ExportedBlock>>() {}.type
                val blocks: List<ExportedBlock> = gson.fromJson(json, blockListType)

                if (blocks.isEmpty()) {
                    Log.w("DataSyncOGE", "Получен пустой список блоков")
                }

                database.withTransaction {
                    val dao = database.appDao()


                    dao.clearBlocks()
                    dao.clearTasks()
                    dao.clearSteps()
                    dao.clearFormulas()

                    blocks.forEachIndexed { index, block ->
                        try {

                            val blockEntity = QuestionBlockEntity(
                                blockID = block.blockID,
                                name = block.name,
                                videoLink = block.videoLink,
                                referenceMaterial = block.referenceMaterial
                            )
                            dao.insertBlocks(listOf(blockEntity))

                            val taskEntities = block.tasks.map { task ->
                                TaskEntity(
                                    taskID = task.taskID,
                                    description = task.description,
                                    drawingLink = task.drawingLink,
                                    answer = task.answer,
                                    hint = task.hint,
                                    given = task.given,
                                    blockOwnerID = block.blockID
                                )
                            }
                            dao.insertTasks(taskEntities)

                            val stepEntities = block.tasks.flatMap { task ->
                                task.steps.map { step ->
                                    StepEntity(
                                        stepID = step.stepID,
                                        taskOwnerID = task.taskID,
                                        solutionVariant = step.solutionVariant,
                                        stepDescription = step.stepDescription,
                                        stepAction = step.stepAction ?: ""
                                    )
                                }
                            }
                            dao.insertSteps(stepEntities)

                            val formulaEntities = block.formulas.map { formula ->
                                FormulaEntity(
                                    formulaID = formula.formulaID,
                                    name = formula.name,
                                    formula = formula.formula,
                                    blockOwnerID = block.blockID
                                )
                            }
                            dao.insertFormulas(formulaEntities)

                        } catch (e: Exception) {
                            Log.e("DataSync", "Ошибка при обработке блока ${index + 1}: ${block.name}", e)
                        }
                    }
                }


                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val ogeVersion = firestore.collection("sync_metadata")
                        .document("MathUpOgeDatabase").get().await().getLong("version")?.toInt() ?: 1

                    val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                    val existing = prefs.getString("sync_metadata", null)
                    val oldMetadata = existing?.let {
                        try { Gson().fromJson(it, SyncMetadata::class.java) } catch (_: Exception) { null }
                    }

                    val updated = oldMetadata?.copy(versionOge = ogeVersion)
                        ?: SyncMetadata(versionOge = ogeVersion)

                    prefs.edit { putString("sync_metadata", Gson().toJson(updated)) }

                } catch (e: Exception) {
                    Log.e("DataSyncOGE", "Ошибка при обновлении версии OGE", e)
                }
            } catch (e: Exception) {
                Log.e("DataSyncOGE", "Ошибка при синхронизации", e)
                throw e
            }
        }
    }
    suspend fun syncFromAssets(context: Context, database: MathUpDatabase) {
        withContext(Dispatchers.IO) {
            try {

                val json = context.assets.open("data_export.json").bufferedReader().use { it.readText() }

                val gson = Gson()
                val blockListType = object : TypeToken<List<ExportedBlock>>() {}.type
                val blocks: List<ExportedBlock> = gson.fromJson(json, blockListType)

                if (blocks.isEmpty()) Log.w("DataSync", "JSON пуст")

                database.withTransaction {
                    val dao = database.appDao()
                    dao.clearBlocks()
                    dao.clearTasks()
                    dao.clearSteps()
                    dao.clearFormulas()

                    insertBlocksToDb(dao, blocks)
                }


            } catch (e: Exception) {
                Log.e("DataSync", "Ошибка импорта из assets", e)
                throw e
            }
        }
    }

    suspend fun syncOgeFromAssets(context: Context, database: MathUpOgeDatabase) {
        withContext(Dispatchers.IO) {
            try {

                val json = context.assets.open("data_export_oge.json").bufferedReader().use { it.readText() }

                val gson = Gson()
                val blockListType = object : TypeToken<List<ExportedBlock>>() {}.type
                val blocks: List<ExportedBlock> = gson.fromJson(json, blockListType)

                if (blocks.isEmpty()) Log.w("DataSyncOGE", "JSON пуст")

                database.withTransaction {
                    val dao = database.appDao()
                    dao.clearBlocks()
                    dao.clearTasks()
                    dao.clearSteps()
                    dao.clearFormulas()

                    insertBlocksToDb(dao, blocks)
                }


            } catch (e: Exception) {
                Log.e("DataSyncOGE", "Ошибка импорта из assets", e)
                throw e
            }
        }
    }
    private suspend fun insertBlocksToDb(dao: AppDao, blocks: List<ExportedBlock>) {
        blocks.forEachIndexed { index, block ->
            try {

                dao.insertBlocks(listOf(QuestionBlockEntity(
                    blockID = block.blockID,
                    name = block.name,
                    videoLink = block.videoLink,
                    referenceMaterial = block.referenceMaterial
                )))

                dao.insertTasks(block.tasks.map { task ->
                    TaskEntity(
                        taskID = task.taskID,
                        description = task.description,
                        drawingLink = task.drawingLink,
                        answer = task.answer,
                        hint = task.hint,
                        given = task.given,
                        blockOwnerID = block.blockID
                    )
                })

                dao.insertSteps(block.tasks.flatMap { task ->
                    task.steps.map { step ->
                        StepEntity(
                            stepID = step.stepID,
                            taskOwnerID = task.taskID,
                            solutionVariant = step.solutionVariant,
                            stepDescription = step.stepDescription,
                            stepAction = step.stepAction ?: ""
                        )
                    }
                })

                dao.insertFormulas(block.formulas.map { formula ->
                    FormulaEntity(
                        formulaID = formula.formulaID,
                        name = formula.name,
                        formula = formula.formula,
                        blockOwnerID = block.blockID
                    )
                })

            } catch (e: Exception) {
                Log.e("DataSync", "Ошибка при обработке блока ${block.name}", e)
            }
        }
    }
}
