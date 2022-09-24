package com.sapuseven.untis

object Helpers {

    fun getFileContents(path: String): String {
        return javaClass.getResource(path)?.readText() ?: throw IllegalStateException()
    }
}
