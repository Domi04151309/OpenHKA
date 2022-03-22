package com.sapuseven.untis.data.connectivity

object UntisApiConstants {
	const val DEFAULT_WEBUNTIS_PROTOCOL = "https://"
	const val DEFAULT_WEBUNTIS_HOST = "mobile.webuntis.com"
	const val DEFAULT_WEBUNTIS_PATH = "/ms/app/"

	const val METHOD_GET_APP_SHARED_SECRET = "getAppSharedSecret"
	const val METHOD_GET_MESSAGES = "getMessagesOfDay2017"
	const val METHOD_GET_TIMETABLE = "getTimetable2017"
	const val METHOD_GET_USER_DATA = "getUserData2017"
	const val METHOD_SEARCH_SCHOOLS = "searchSchool"

	const val CAN_READ_STUDENT_ABSENCE = "READ_STUD_ABSENCE"
	const val CAN_WRITE_STUDENT_ABSENCE = "WRITE_STUD_ABSENCE"
	const val CAN_READ_LESSON_TOPIC = "READ_LESSONTOPIC"
	const val CAN_WRITE_LESSON_TOPIC = "WRITE_LESSONTOPIC"

	const val SCHOOL_SEARCH_URL = "$DEFAULT_WEBUNTIS_PROTOCOL$DEFAULT_WEBUNTIS_HOST/ms/schoolquery2/"
}
