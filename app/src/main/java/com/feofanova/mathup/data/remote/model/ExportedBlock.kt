package com.feofanova.mathup.data.remote.model


data class ExportedBlock(
    val blockID: Int,
    val name: String,
    val videoLink: String?,
    val referenceMaterial: String?,
    val tasks: List<ExportedTask>,
    val formulas: List<ExportedFormula>
)

data class ExportedTask(
    val taskID: Int,
    val description: String,
    val drawingLink: String?,
    val answer: String,
    val hint: String?,
    val given: String?,
    val steps: List<ExportedStep>
)

data class ExportedStep(
    val stepID: Int,
    val solutionVariant: String,
    val stepDescription: String,
    val stepAction: String?
)

data class ExportedFormula(
    val formulaID: Int,
    val name: String,
    val formula: String
)
