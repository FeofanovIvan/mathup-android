package com.feofanova.mathup.data.repository

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.feofanova.mathup.SyncMetadata
import com.feofanova.mathup.data.local.dao.AppDao
import com.feofanova.mathup.data.local.db.MathUpDatabase
import com.feofanova.mathup.data.local.db.MathUpOgeDatabase
import com.feofanova.mathup.data.local.entities.*
import com.feofanova.mathup.data.remote.model.ExportedBlock
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
                Log.d("DataSync", "📥 Начало загрузки JSON с $JSON_URL")

                val json = URL(JSON_URL).readText()
                Log.d("DataSync", "📦 Размер JSON-файла: ${json.length} символов")

                val gson = Gson()
                val blockListType = object : TypeToken<List<ExportedBlock>>() {}.type
                val blocks: List<ExportedBlock> = gson.fromJson(json, blockListType)


                if (blocks.isEmpty()) {
                    Log.w("DataSync", "⚠️ Получен пустой список блоков!")
                }

                database.withTransaction {
                    val dao = database.appDao()

                    dao.clearBlocks()
                    dao.clearTasks()
                    dao.clearSteps()
                    dao.clearFormulas()
                    Log.d("DataSync", "🧹 Старые данные удалены")

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
                            Log.d("DataSync", "   • Шагов: ${stepEntities.size}")
                            dao.insertSteps(stepEntities)

                            val formulaEntities = block.formulas.map { formula ->

                                FormulaEntity(
                                    formulaID = formula.formulaID,
                                    name = formula.name,
                                    formula = formula.formula,
                                    blockOwnerID = block.blockID
                                )
                            }
                            Log.d("DataSync", "   • Формул: ${formulaEntities.size}")
                            dao.insertFormulas(formulaEntities)

                        } catch (e: Exception) {
                            Log.e("DataSync", "❌ Ошибка при обработке блока ${index + 1}: ${block.name}", e)
                        }
                    }
                }

                Log.d("DataSync", "✅ Импорт завершён: ${blocks.size} блоков")
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val egeVersion = firestore.collection("sync_metadata")
                        .document("MathUpDatabase").get().await().getLong("version")?.toInt() ?: 1

                    val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

                    // Читаем старый metadata (если есть)
                    val existing = prefs.getString("sync_metadata", null)
                    val oldMetadata = existing?.let {
                        try {
                            Gson().fromJson(it, SyncMetadata::class.java)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    val updatedMetadata = oldMetadata?.copy(version_ege = egeVersion)
                        ?: SyncMetadata(version_ege = egeVersion)

                    prefs.edit { putString("sync_metadata", Gson().toJson(updatedMetadata)) }

                    Log.d("DataSync", "💾 Версия EGE из Firestore сохранена: $egeVersion")

                } catch (e: Exception) {
                    Log.e("DataSync", "⚠️ Не удалось обновить версию EGE из Firestore", e)
                }

            } catch (e: Exception) {
                Log.e("DataSync", "❌ Ошибка при синхронизации", e)
                throw e // ← пробрасываем ошибку
            }
        }
    }
    private const val OGE_JSON_URL =
        "https://firebasestorage.googleapis.com/v0/b/mathappforrus.appspot.com/o/databases%2Fdata_export_oge.json?alt=media"

    suspend fun syncOgeFromRemote(context: Context, database: MathUpOgeDatabase) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("DataSyncOGE", "📥 Начало загрузки JSON с $OGE_JSON_URL")

                val json = URL(OGE_JSON_URL).readText()
                Log.d("DataSyncOGE", "📦 Размер JSON-файла: ${json.length} символов")

                val gson = Gson()
                val blockListType = object : TypeToken<List<ExportedBlock>>() {}.type
                val blocks: List<ExportedBlock> = gson.fromJson(json, blockListType)

                Log.d("DataSyncOGE", "🔍 Декодировано блоков: ${blocks.size}")
                if (blocks.isEmpty()) {
                    Log.w("DataSyncOGE", "⚠️ Получен пустой список блоков!")
                }

                database.withTransaction {
                    val dao = database.appDao() // ✅ теперь унифицировано


                    dao.clearBlocks()
                    dao.clearTasks()
                    dao.clearSteps()
                    dao.clearFormulas()
                    Log.d("DataSyncOGE", "🧹 Старые данные удалены")

                    blocks.forEachIndexed { index, block ->
                        try {
                            Log.d("DataSync", "📄 Обработка блока ${index + 1}: ${block.name} (ID=${block.blockID})")

                            val blockEntity = QuestionBlockEntity(
                                blockID = block.blockID,
                                name = block.name,
                                videoLink = block.videoLink,
                                referenceMaterial = block.referenceMaterial
                            )
                            dao.insertBlocks(listOf(blockEntity))

                            val taskEntities = block.tasks.map { task ->
                                Log.d("DataSync", "   ↪️ Задача: id=${task.taskID}, text=${task.description}")
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
                            Log.d("DataSync", "   • Задач: ${taskEntities.size}")
                            dao.insertTasks(taskEntities)

                            val stepEntities = block.tasks.flatMap { task ->
                                task.steps.map { step ->
                                    Log.d("DataSync", "   ↪️ Шаг: id=${step.stepID}, text=${step.stepDescription}")
                                    StepEntity(
                                        stepID = step.stepID,
                                        taskOwnerID = task.taskID,
                                        solutionVariant = step.solutionVariant,
                                        stepDescription = step.stepDescription,
                                        stepAction = step.stepAction ?: ""
                                    )
                                }
                            }
                            Log.d("DataSync", "   • Шагов: ${stepEntities.size}")
                            dao.insertSteps(stepEntities)

                            val formulaEntities = block.formulas.map { formula ->
                                Log.d("DataSync", "   ↪️ Формула: id=${formula.formulaID}, name=${formula.name}")
                                FormulaEntity(
                                    formulaID = formula.formulaID,
                                    name = formula.name,
                                    formula = formula.formula,
                                    blockOwnerID = block.blockID
                                )
                            }
                            Log.d("DataSync", "   • Формул: ${formulaEntities.size}")
                            dao.insertFormulas(formulaEntities)

                        } catch (e: Exception) {
                            Log.e("DataSync", "❌ Ошибка при обработке блока ${index + 1}: ${block.name}", e)
                        }
                    }
                }

                Log.d("DataSyncOGE", "✅ Импорт завершён: ${blocks.size} блоков")

                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val ogeVersion = firestore.collection("sync_metadata")
                        .document("MathUpOgeDatabase").get().await().getLong("version")?.toInt() ?: 1

                    val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
                    val existing = prefs.getString("sync_metadata", null)
                    val oldMetadata = existing?.let {
                        try { Gson().fromJson(it, SyncMetadata::class.java) } catch (_: Exception) { null }
                    }

                    val updated = oldMetadata?.copy(version_oge = ogeVersion)
                        ?: SyncMetadata(version_oge = ogeVersion)

                    prefs.edit { putString("sync_metadata", Gson().toJson(updated)) }

                    Log.d("DataSyncOGE", "💾 Версия OGE обновлена: $ogeVersion")
                } catch (e: Exception) {
                    Log.e("DataSyncOGE", "⚠️ Ошибка при обновлении версии OGE", e)
                }
            } catch (e: Exception) {
                Log.e("DataSyncOGE", "❌ Ошибка при синхронизации", e)
                throw e
            }
        }
    }
    suspend fun syncFromAssets(context: Context, database: MathUpDatabase) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("DataSync", "📥 Загрузка JSON из assets/data_export.json")

                val json = context.assets.open("data_export.json").bufferedReader().use { it.readText() }
                Log.d("DataSync", "📦 Размер JSON-файла: ${json.length} символов")

                val gson = Gson()
                val blockListType = object : TypeToken<List<ExportedBlock>>() {}.type
                val blocks: List<ExportedBlock> = gson.fromJson(json, blockListType)

                Log.d("DataSync", "🔍 Блоков: ${blocks.size}")
                if (blocks.isEmpty()) Log.w("DataSync", "⚠️ JSON пуст")

                database.withTransaction {
                    val dao = database.appDao()
                    dao.clearBlocks()
                    dao.clearTasks()
                    dao.clearSteps()
                    dao.clearFormulas()
                    Log.d("DataSync", "🧹 Очистка завершена")

                    insertBlocksToDb(dao, blocks)
                }

                Log.d("DataSync", "✅ Импорт из assets завершён")

            } catch (e: Exception) {
                Log.e("DataSync", "❌ Ошибка импорта из assets", e)
                throw e
            }
        }
    }

    suspend fun syncOgeFromAssets(context: Context, database: MathUpOgeDatabase) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("DataSyncOGE", "📥 Загрузка JSON из assets/data_export_oge.json")

                val json = context.assets.open("data_export_oge.json").bufferedReader().use { it.readText() }
                Log.d("DataSyncOGE", "📦 Размер JSON-файла: ${json.length} символов")

                val gson = Gson()
                val blockListType = object : TypeToken<List<ExportedBlock>>() {}.type
                val blocks: List<ExportedBlock> = gson.fromJson(json, blockListType)

                Log.d("DataSyncOGE", "🔍 Блоков: ${blocks.size}")
                if (blocks.isEmpty()) Log.w("DataSyncOGE", "⚠️ JSON пуст")

                database.withTransaction {
                    val dao = database.appDao()
                    dao.clearBlocks()
                    dao.clearTasks()
                    dao.clearSteps()
                    dao.clearFormulas()
                    Log.d("DataSyncOGE", "🧹 Очистка завершена")

                    insertBlocksToDb(dao, blocks)
                }

                Log.d("DataSyncOGE", "✅ Импорт из assets завершён")

            } catch (e: Exception) {
                Log.e("DataSyncOGE", "❌ Ошибка импорта из assets", e)
                throw e
            }
        }
    }
    private suspend fun insertBlocksToDb(dao: AppDao, blocks: List<ExportedBlock>) {
        blocks.forEachIndexed { index, block ->
            try {
                Log.d("DataSync", "📄 Блок ${index + 1}: ${block.name} (ID=${block.blockID})")

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
                Log.e("DataSync", "❌ Ошибка при обработке блока ${block.name}", e)
            }
        }
    }
}
