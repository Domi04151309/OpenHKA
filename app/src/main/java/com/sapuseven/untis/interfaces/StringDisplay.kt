package com.sapuseven.untis.interfaces

import com.sapuseven.untis.helpers.strings.StringLoader

interface StringDisplay {
	fun onStringLoaded(string: String)
	fun onStringLoadingError(code: Int, loader: StringLoader)
}
