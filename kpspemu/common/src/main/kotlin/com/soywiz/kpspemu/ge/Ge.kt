package com.soywiz.kpspemu.ge

import com.soywiz.korio.async.Signal
import com.soywiz.kpspemu.Emulator
import com.soywiz.kpspemu.WithEmulator
import com.soywiz.kpspemu.gpu
import com.soywiz.kpspemu.util.ResourceItem
import com.soywiz.kpspemu.util.ResourceList
import com.soywiz.kpspemu.util.Signal2

class Ge(override val emulator: Emulator) : WithEmulator {
	val state = GeState()
	val queue = arrayListOf<GeList>()
	val lists = ResourceList<GeList>("GeList") { GeList(this, it) }
	val onCompleted = Signal2<Unit>()

	fun listEnqueue(start: Int, stall: Int, callback: GeCallback, pspGeListArgs: Int): GeList {
		val list = lists.alloc().apply {
			this.start = start
			this.stall = stall
			this.callback = callback
			this.pspGeListArgs = pspGeListArgs
			this.PC = start
			reset()
		}
		queue += list
		return list
	}

	fun run() {
		while (queue.isNotEmpty()) {
			val item = queue.first()
			item.run()
			if (item.completed) {
				lists.free(item)
				queue.removeAt(0)
			} else {
				break
			}
		}

		//if (queue.isEmpty()) onCompleted(Unit)
		onCompleted(Unit)
	}

	fun emitBatch(batch: GeBatchData) {
		gpu.addBatch(batch)
		//println("BATCH: $batch")
	}

	fun sync(syncType: Int) {
		run()
	}

	//suspend fun sync(syncType: Int) {
	//	onCompleted.waitOne()
	//}
//
	//fun syncAsync(syncType: Int): Promise<Unit> {
	//	val deferred = Promise.Deferred<Unit>()
	//	onCompleted.once { deferred.resolve(Unit) }
	//	return deferred.promise
	//}
}

data class GeCallback(override val id: Int) : ResourceItem {
	var signal_func: Int = 0
	var signal_arg: Int = 0
	var finish_func: Int = 0
	var finish_arg: Int = 0
}

enum class PrimAction { NOTHING, FLUSH_PRIM }

enum class ListSyncKind { DONE, QUEUED, DRAWING_DONE, STALL_REACHED, CANCEL_DONE }
