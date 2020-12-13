package com.davidgyoungtech.advertiseranalyzer

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice
import uk.co.alt236.bluetoothlelib.device.adrecord.AdRecord
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconType
import uk.co.alt236.bluetoothlelib.device.beacon.BeaconUtils
import uk.co.alt236.bluetoothlelib.device.beacon.ibeacon.IBeaconDevice
import uk.co.alt236.bluetoothlelib.util.ByteUtils
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Parses the Manufactured Data field of an iBeacon
 * <p>
 * The parsing is based on the following schema:
 * <pre>
 * Byte|Value
 * -------------------------------------------------
 * 0	4C - Byte 1 (LSB) of Company identifier code
 * 1	00 - Byte 0 (MSB) of Company identifier code (0x004C == Apple)
 * 2	02 - Byte 0 of iBeacon advertisement indicator
 * 3	15 - Byte 1 of iBeacon advertisement indicator
 * 4	e2 |\
 * 5	c5 |\\
 * 6	6d |#\\
 * 7	b5 |##\\
 * 8	df |###\\
 * 9	fb |####\\
 * 10	48 |#####\\
 * 11	d2 |#####|| iBeacon
 * 12	b0 |#####|| Proximity UUID
 * 13	60 |#####//
 * 14	d0 |####//
 * 15	f5 |###//
 * 16	a7 |##//
 * 17	10 |#//
 * 18	96 |//
 * 19	e0 |/
 * 20	00 - major
 * 21	00
 * 22	00 - minor
 * 23	00
 * 24	c5 - The 2's complement of the calibrated Tx Power
 * </pre>
 * @author Alexandros Schillings
 */

class BeaconManager(private val context: Context) {
	var lastChangeDetectionTime: Long = 0
	var lastBinaryString: String = ""
	var servicesForBitPosition = HashMap<Int, ArrayList<String>>()
	var presumedServiceUUidNumber = 0
	var dumped = false
	var onMessage: ((String, String) -> Unit)? = null

	fun startScanning() {
		val bluetoothManager = context.getSystemService(
				Context.BLUETOOTH_SERVICE) as BluetoothManager
		val bluetoothAdapter = bluetoothManager.adapter
		val scanner = bluetoothAdapter.bluetoothLeScanner
		scanner.startScan(bleScannerCallback)
		sendMessage("start", "Start Scanning...")
	}

	private val bleScannerCallback = object : ScanCallback() {
		override fun onScanResult(callbackType: Int, result: ScanResult?) {
			super.onScanResult(callbackType, result)
//			if (result?.rssi?.let { it > -45 } == true) {
			if (result?.rssi != null) {

				val deviceLe = BluetoothLeDevice(
						result.device,
						result.rssi,
						result.scanRecord?.bytes,
						System.currentTimeMillis()
				)
				if (BeaconUtils.getBeaconType(deviceLe) == BeaconType.IBEACON) {
					val iBeacon = IBeaconDevice(deviceLe)
					Log.i(TAG, "iBeacon: $iBeacon")
					sendMessage(iBeacon.address + "-ibeacon",
							"rssi: ${iBeacon.rssi}\ndevice: ${iBeacon.device}\nuuids: ${iBeacon.uuid}\ntxPower: ${iBeacon.calibratedTxPower}\n accuracy: ${iBeacon.accuracy}")
				} else {
					Log.i(TAG, "Bluetooth: ${deviceLe.name}")
				}

				result.scanRecord?.getManufacturerSpecificData(0x004c)
						?.let { manData ->

							val deviceBLe = BluetoothLeDevice(
									result.device,
									result.rssi,
									result.scanRecord?.bytes,
									System.currentTimeMillis()
							)
							val manufacturerData = deviceBLe.adRecordStore.getRecord(
									AdRecord.TYPE_MANUFACTURER_SPECIFIC_DATA).data

							val newManufacturerData = byteArrayOf(
									*manufacturerData,
									0,
									0, // major
									0,
									0, // miner
									0,
									0 // Tx Power
							)
							if (newManufacturerData.size > 20) {
								val uuid = calculateUuidString(
										newManufacturerData.copyOfRange(4, 20))
								Log.v(TAG, "uuid: $uuid")
							}
							Log.v(TAG, "manData(${manData.size}): ${Arrays.toString(manData)}")
							Log.v(TAG, "manufacturerData(${manufacturerData.size}): ${
								Arrays.toString(manufacturerData)
							}")

							val intArray: ByteArray = Arrays.copyOfRange(manufacturerData, 0, 2)
							ByteUtils.invertArray(intArray)

							val companyIdentidier = ByteUtils.getIntFrom2ByteArray(intArray)
							val iBeaconAdvertisment = ByteUtils.getIntFrom2ByteArray(
									Arrays.copyOfRange(manufacturerData, 2, 4))
							Log.v(TAG, "companyIdentidier: $companyIdentidier")
							Log.v(TAG, "iBeaconAdvertisment: $iBeaconAdvertisment")


//							public IBeaconManufacturerData(final byte[] manufacturerData) {
//								super(BeaconType.IBEACON, manufacturerData);
//
//								final byte[] intArray = Arrays.copyOfRange(manufacturerData, 0, 2);
//								ByteUtils.invertArray(intArray);
//
//								mCompanyIdentidier = ByteUtils.getIntFrom2ByteArray(intArray);
//								mIBeaconAdvertisment = ByteUtils.getIntFrom2ByteArray(Arrays.copyOfRange(manufacturerData, 2, 4));
//								mUUID = IBeaconUtils.calculateUuidString(Arrays.copyOfRange(manufacturerData, 4, 20));
//								mMajor = ByteUtils.getIntFrom2ByteArray(Arrays.copyOfRange(manufacturerData, 20, 22));
//								mMinor = ByteUtils.getIntFrom2ByteArray(Arrays.copyOfRange(manufacturerData, 22, 24));
//								mCalibratedTxPower = manufacturerData[24];
//							}

							Log.i(TAG, "result: $result")
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
								Log.i(TAG,
										"rssi: ${result.rssi}\ndevice: ${result.device.address}\nuuids: ${result.scanRecord?.serviceUuids}\ntxPower: ${result.txPower}")
								sendMessage(result.device.address + "-overflow-area-1", "rssi: ${result.rssi}\ndevice: ${result.device.address}\nuuids: ${result.scanRecord?.serviceUuids}\ntxPower: ${result.txPower}")
								sendMessage(result.device.address + "-overflow-area-2", "$result")
							} else {
								Log.i(TAG,
										"rssi: ${result.rssi}\ndevice: ${result.device.address}\nuuids: ${result.scanRecord?.serviceUuids}\ntxPower: ${result.scanRecord?.txPowerLevel}")
								sendMessage(result.device.address + "-overflow-area",
										"rssi: ${result.rssi}\ndevice: ${result.device.address}\nuuids: ${result.scanRecord?.serviceUuids}\ntxPower: ${result.scanRecord?.txPowerLevel}")
							}
							Log.i(TAG, "scanRecord: ${result.scanRecord}")
							if (manData.count() >= 17 && manData[0].toUByte() == 1.toUByte()) {
								// We have found an apple background advertisement
								var bytesAsBinary = ""
								var bytesAsBinaryFormatted = ""
								for (byteIndex in 1..16) {
									var byteAsUnsignedInt = manData[byteIndex].toInt()
									if (byteAsUnsignedInt < 0) {
										byteAsUnsignedInt += 256
									}
									val binaryString = String.format("%8s", Integer.toBinaryString(
											byteAsUnsignedInt))
											.replace(" ", "0")
									bytesAsBinary += binaryString
									bytesAsBinaryFormatted += "$binaryString "
								}
								val firstBitSet = bytesAsBinary.indexOf("1")
								val lastBitSet = bytesAsBinary.lastIndexOf("1")
								if (lastBitSet != firstBitSet) {
									Log.e(TAG, "Two bits set")
									sendMessage("bitsset", "Two bits set")
								}
								if (lastBinaryString != bytesAsBinary) {
									if (lastBinaryString != "") {
										if (System.currentTimeMillis() - lastChangeDetectionTime > 1700) {
											val msg = "Failed to detect an advertisement change in " + (System.currentTimeMillis() - lastChangeDetectionTime) + " millis"
											Log.e(TAG, msg)
											sendMessage("failed", msg)
										}
										presumedServiceUUidNumber += 1
									}
									lastChangeDetectionTime = System.currentTimeMillis()
									lastBinaryString = bytesAsBinary
									val calculatedServiceUuid = formatServiceNumber(
											presumedServiceUUidNumber)
									var services = servicesForBitPosition[firstBitSet]
									if (services == null) {
										services = ArrayList()
										servicesForBitPosition[firstBitSet] = services
									} else {
										val msg = "Collision detected for bit $firstBitSet.  Colliding service UUIDS:"
										Log.d(TAG, msg)
										sendMessage("collision", msg)
										for ((index, service) in services.withIndex()) {
											Log.d(TAG, service)
											sendMessage("collision-${index}", service)
										}
										Log.d(TAG, calculatedServiceUuid)
										sendMessage("calculated", calculatedServiceUuid)
									}
									services.add(calculatedServiceUuid)
									val appleBitFor = "iOS bit for $calculatedServiceUuid is ${
										String.format(
												"%3d", firstBitSet)
									}: $bytesAsBinaryFormatted"
									val found = "Found ${servicesForBitPosition.count()} of 128"
									Log.d(TAG, appleBitFor)
									Log.d(TAG, found)
									sendMessage("appleBitFor", appleBitFor)
									sendMessage("found", found)
									if (servicesForBitPosition.count() == 128) {
										if (!dumped) {
											dumped = true
											dumpTable()
										}
									}
								}

							}
						}
			}
		}

		fun dumpTable() {
			Log.d(TAG,
					"// Table of known service UUIDs by position in Apple's proprietary background service advertisement")
			sendMessage("table-of-know",
					"// Table of known service UUIDs by position in Apple's proprietary background service advertisement")
			for (bitPosition in 0..127) {
				val uuid = servicesForBitPosition[bitPosition]
				val first = uuid?.get(0)
				Log.d(TAG, "\"$first\",")
				sendMessage("first-$bitPosition", "\"$first\",")
			}

		}

		fun formatServiceNumber(number: Int): String {
			val serviceHex = String.format("%032X", number)
			val idBuff = StringBuffer(serviceHex)
			idBuff.insert(20, '-')
			idBuff.insert(16, '-')
			idBuff.insert(12, '-')
			idBuff.insert(8, '-')
			return idBuff.toString()
		}

		override fun onBatchScanResults(results: MutableList<ScanResult>?) {
			super.onBatchScanResults(results)
			Log.d(TAG, "onBatchScanResults:${results.toString()}")
			sendMessage("onBatchScanResults", "onBatchScanResults:${results.toString()}")
		}

		override fun onScanFailed(errorCode: Int) {
			super.onScanFailed(errorCode)
			Log.d(TAG, "onScanFailed: $errorCode")
			sendMessage("onScanFailed", "onScanFailed: $errorCode")
		}
	}

	private fun sendMessage(uuid: String = "", message: String) {
		onMessage?.invoke(uuid, message)
	}


	fun calculateUuidString(uuid: ByteArray): String {
		val sb = StringBuilder()
		for (i in uuid.indices) {
			if (i == 4) {
				sb.append('-')
			}
			if (i == 6) {
				sb.append('-')
			}
			if (i == 8) {
				sb.append('-')
			}
			if (i == 10) {
				sb.append('-')
			}
			val intFromByte = ByteUtils.getIntFromByte(uuid[i])
			if (intFromByte <= 0xF) {
				sb.append('0')
			}
			sb.append(Integer.toHexString(intFromByte))
		}
		return sb.toString()
	}

	companion object {
		const val TAG = "AdvertiserAnalyzerApp"
	}
}