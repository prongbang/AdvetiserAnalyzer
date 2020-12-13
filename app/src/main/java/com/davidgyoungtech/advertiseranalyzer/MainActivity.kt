package com.davidgyoungtech.advertiseranalyzer

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.davidgyoungtech.advertiseranalyzer.databinding.MainBinding
import com.davidgyoungtech.advertiseranalyzer.message.MessageAdapter
import com.davidgyoungtech.advertiseranalyzer.message.MessageEvent


class MainActivity : Activity() {

	private val binding by lazy { MainBinding.inflate(layoutInflater) }
	private val messageAdapter by lazy { MessageAdapter() }
	private val beaconManager by lazy { BeaconManager(this) }

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
				Manifest.permission.ACCESS_BACKGROUND_LOCATION)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			for (permission in permissions) {
				if (ActivityCompat.checkSelfPermission(this.applicationContext,
								permission) != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(this, permissions, 1)
				}
			}
		}
		setContentView(binding.root)

		initLoad()
		initView()
	}

	private fun initLoad() {
		beaconManager.apply {
			onMessage = { uuid, event ->
				messageAdapter.appendToList(uuid, MessageEvent(event))
			}
			startScanning()
		}
	}

	private fun initView() {
		binding.apply {
			messageRecyclerView.apply {
				adapter = messageAdapter
				layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL,
						false)
			}
		}
	}

}