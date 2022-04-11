package com.sapuseven.untis.widgets

import com.sapuseven.untis.R

class MessagesWidget : BaseWidget() {
	override fun getWidgetType(): Int = WidgetRemoteViewsFactory.WIDGET_TYPE_MESSAGES
	override fun getTitleInt(): Int = R.string.widget_daily_messages
}

class TimetableWidget : BaseWidget() {
	override fun getWidgetType(): Int = WidgetRemoteViewsFactory.WIDGET_TYPE_TIMETABLE
	override fun getTitleInt(): Int = R.string.widget_timetable
}

class MensaWidget : BaseWidget() {
	override fun getWidgetType(): Int = WidgetRemoteViewsFactory.WIDGET_TYPE_MENSA
	override fun getTitleInt(): Int = R.string.widget_mensa
}
