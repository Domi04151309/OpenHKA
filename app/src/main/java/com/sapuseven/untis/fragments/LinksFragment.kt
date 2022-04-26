package com.sapuseven.untis.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.sapuseven.untis.R


class LinksFragment : Fragment() {

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val root = inflater.inflate(
			R.layout.fragment_links,
			container,
			false
		)

		root.findViewById<Button>(R.id.btn_website).setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.h-ka.de/")))
		}

		root.findViewById<Button>(R.id.btn_qis).setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qis2.hs-karlsruhe.de/")))
		}

		root.findViewById<Button>(R.id.btn_hisinone).setOnClickListener {
			startActivity(
				Intent(
					Intent.ACTION_VIEW,
					Uri.parse("https://hisinone.extern-hs-karlsruhe.de/")
				)
			)
		}

		root.findViewById<Button>(R.id.btn_ilias).setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ilias.h-ka.de/")))
		}

		root.findViewById<Button>(R.id.btn_webmail).setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://webmail.h-ka.de/")))
		}

		root.findViewById<Button>(R.id.btn_lsf).setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://lsf.hs-karlsruhe.de/")))
		}

		root.findViewById<Button>(R.id.btn_bw_sync_and_share).setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bwsyncandshare.kit.edu/")))
		}

		return root
	}
}
