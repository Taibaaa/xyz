package com.PdfCreator.Editor.model

data class CheckModel(
    val fileId : Long,
    val checkStatus : Boolean?  = false,
)