package com.sapuseven.untis.activities

import android.os.Bundle
import android.view.View.GONE
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.untis.R
import com.sapuseven.untis.adapters.ErrorsAdapter
import com.sapuseven.untis.dialogs.ErrorReportingDialog
import com.sapuseven.untis.helpers.ZipUtils
import com.sapuseven.untis.helpers.issues.GithubIssue
import com.sapuseven.untis.helpers.issues.Issue
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.io.File

class ErrorsActivity : BaseActivity() {
	companion object {
		const val EXTRA_BOOLEAN_SHOW_CRASH_MESSAGE = "com.sapuseven.activities.errors.crashmessage"
	}

	private lateinit var constraintLayoutRoot: ConstraintLayout
	private lateinit var recyclerViewErrors: RecyclerView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.activity_errors)

		constraintLayoutRoot = findViewById(R.id.constraintlayout_root)
		recyclerViewErrors = findViewById(R.id.recyclerview_errors)

		if (!intent.getBooleanExtra(EXTRA_BOOLEAN_SHOW_CRASH_MESSAGE, false)) {
			findViewById<TextView>(R.id.textview_crash_title).visibility = GONE
			findViewById<TextView>(R.id.textview_crash_message).visibility = GONE
			with(ConstraintSet()) {
				clone(constraintLayoutRoot)
				connect(
					findViewById<TextView>(R.id.textview_errors_report).id,
					ConstraintSet.TOP,
					constraintLayoutRoot.id,
					ConstraintSet.TOP
				)
				applyTo(constraintLayoutRoot)
			}
		}

		findViewById<Button>(R.id.button_dismiss).setOnClickListener {
			deleteLogFiles()
			finish()
		}

		loadErrorList()

		findViewById<Button>(R.id.button_report).setOnClickListener {
			val zipFile = zipLogFiles()
			deleteLogFiles()
			GithubIssue(
				Issue.Type.CRASH,
				getString(R.string.errors_github_attach_file_message, zipFile.absolutePath)
			).launch(this)
		}
	}

	private fun deleteLogFiles() {
		File(filesDir, "logs").deleteRecursively()
	}

	private fun zipLogFiles(): File {
		return File(getExternalFilesDir(null), "logs-" + System.currentTimeMillis() + ".zip").also {
			ZipUtils.zip(File(filesDir, "logs"), it)
		}
	}

	private fun loadErrorList() {
		recyclerViewErrors.layoutManager = LinearLayoutManager(this)
		recyclerViewErrors.adapter =
			ErrorsAdapter(File(filesDir, "logs").listFiles()?.let { files ->
				files.sortedDescending()
					.map {
						val timestamp =
							it.name.replace(Regex("""^_?(\d+)(-\d+)?.log$"""), "$1").toLongOrNull()

						ErrorData(
							readCrashData(it),
							timestamp?.let { DateTime(timestamp).toString(DateTimeFormat.mediumDateTime()) }
								?: "(unknown date)" // TODO: Extract string resource
						)
					}
					.groupingBy { it }
					.eachCount()
					.map {
						ErrorData(
							it.key.log,
							if (it.value > 1) "${it.key.time} (${it.value})" else it.key.time
						)
					}
			} ?: emptyList())
		(recyclerViewErrors.adapter as ErrorsAdapter).setOnItemClickListener { item ->
			ErrorReportingDialog(this).showGenericErrorDialog(item.log)
		}
	}

	data class ErrorData(
		val log: String,
		val time: String
	)
}
