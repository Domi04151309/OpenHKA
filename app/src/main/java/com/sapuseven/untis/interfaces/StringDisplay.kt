package com.sapuseven.untis.interfaces

interface StringDisplay {
	fun onStringLoaded(string: String)
	fun onStringLoadingError(code: Int)
}
