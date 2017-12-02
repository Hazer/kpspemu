package com.soywiz.kpspemu.hle.modules

import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.waitOne
import com.soywiz.korio.util.hex
import com.soywiz.kpspemu.*
import com.soywiz.kpspemu.cpu.CpuState
import com.soywiz.kpspemu.cpu.GP
import com.soywiz.kpspemu.cpu.K0
import com.soywiz.kpspemu.hle.SceModule
import com.soywiz.kpspemu.hle.error.SceKernelErrors
import com.soywiz.kpspemu.hle.error.sceKernelException
import com.soywiz.kpspemu.hle.manager.*
import com.soywiz.kpspemu.mem.Ptr
import com.soywiz.kpspemu.mem.isNotNull
import com.soywiz.kpspemu.mem.readBytes
import com.soywiz.kpspemu.mem.write
import com.soywiz.kpspemu.util.*
import kotlin.math.min

@Suppress("UNUSED_PARAMETER", "MemberVisibilityCanPrivate")
class ThreadManForUser(emulator: Emulator)
	: SceModule(emulator, "ThreadManForUser", 0x40010011, "threadman.prx", "sceThreadManager")
	, ThreadManForUser_EventFlags
	, ThreadManForUser_Fpl
{
	val eventFlags = ResourceList("EventFlag") { PspEventFlag(it) }

	fun sceKernelCreateThread(name: String?, entryPoint: Int, initPriority: Int, stackSize: Int, attributes: Int, optionPtr: Ptr): Int {
		logger.trace { "sceKernelCreateThread: '$name', ${entryPoint.hex}, $initPriority, $stackSize, ${attributes.hex}, $optionPtr" }
		val thread = threadManager.create(name ?: "unknown", entryPoint, initPriority, stackSize, attributes, optionPtr)
		val k0Struct = K0Structure(
			threadId = thread.id,
			stackAddr = thread.stack.low.toInt(),
			f1 = -1,
			f2 = -1
		)
		mem.sw(thread.stack.low.toInt(), thread.id)
		thread.state.K0 = thread.putDataInStack(K0Structure.toByteArray(k0Struct)).low
		//println("sceKernelCreateThread: ${thread.id}")
		return thread.id
	}

	fun sceKernelStartThread(currentThread: PspThread, threadId: Int, userDataLength: Int, userDataPtr: Ptr): Int {
		logger.trace { "sceKernelStartThread: $threadId, $userDataLength, $userDataPtr" }
		//println("sceKernelStartThread: $threadId")
		val thread = threadManager.getById(threadId)
		if (userDataPtr.isNotNull) {
			val localUserDataPtr = thread.putDataInStack(userDataPtr.readBytes(userDataLength))
			thread.state.r4 = userDataLength
			thread.state.r5 = localUserDataPtr.addr
		} else {
			thread.state.r4 = 0
			thread.state.r5 = 0
		}
		thread.state.GP = currentThread.state.GP
		thread.start()
		return 0
	}

	fun sceKernelExitThread(thread: PspThread, exitStatus: Int): Unit {
		thread.exitStatus = exitStatus
		thread.stop()
		threadManager.suspend()
	}

	fun sceKernelChangeThreadPriority(threadId: Int, priority: Int): Int {
		val thread = threadManager.tryGetById(threadId) ?: return SceKernelErrors.ERROR_KERNEL_NOT_FOUND_THREAD
		thread.priority = priority
		return 0
	}

	fun sceKernelDeleteThread(threadId: Int): Int {
		val thread = threadManager.tryGetById(threadId) ?: return SceKernelErrors.ERROR_KERNEL_NOT_FOUND_THREAD
		thread.delete()
		return 0
	}

	fun _sceKernelSleepThread(currentThread: PspThread, cb: Boolean): Int {
		currentThread.suspend(WaitObject.SLEEP, cb = cb)
		return 0
	}

	fun sceKernelSleepThread(currentThread: PspThread): Int = _sceKernelSleepThread(currentThread, cb = false)
	fun sceKernelSleepThreadCB(currentThread: PspThread): Int = _sceKernelSleepThread(currentThread, cb = true)

	fun sceKernelGetThreadCurrentPriority(thread: PspThread): Int = thread.priority

	fun sceKernelGetSystemTimeWide(): Long = rtc.getTimeInMicroseconds()
	fun sceKernelGetSystemTimeLow(): Int = rtc.getTimeInMicrosecondsInt()

	fun sceKernelCreateCallback(name: String?, func: Ptr, arg: Int): Int {
		val callback = callbackManager.create(name ?: "callback", func, arg)
		return callback.id
	}

	fun _sceKernelDelayThread(thread: PspThread, microseconds: Int, cb: Boolean): Int {
		thread.suspend(WaitObject.TIME(rtc.getTimeInMicrosecondsDouble() + microseconds), cb = cb)
		return 0
	}

	fun sceKernelDelayThreadCB(thread: PspThread, microseconds: Int): Int = _sceKernelDelayThread(thread, microseconds, cb = true)
	fun sceKernelDelayThread(thread: PspThread, microseconds: Int): Int = _sceKernelDelayThread(thread, microseconds, cb = false)

	suspend fun _sceKernelWaitThreadEnd(currentThread: PspThread, threadId: Int, timeout: Ptr, cb: Boolean): Int {
		val thread = threadManager.tryGetById(threadId)
		if (thread == null) {
			logger.warn { "_sceKernelWaitThreadEnd: Thread not found! $threadId" }
			return SceKernelErrors.ERROR_KERNEL_NOT_FOUND_THREAD
		}
		currentThread.waitInfo = threadId
		thread.onEnd.add {
			println("ENDED!")
		}
		thread.onEnd.waitOne()
		println("Resumed!")
		return 0
	}

	suspend fun sceKernelWaitThreadEnd(currentThread: PspThread, threadId: Int, timeout: Ptr): Int = _sceKernelWaitThreadEnd(currentThread, threadId, timeout, cb = false)
	suspend fun sceKernelWaitThreadEndCB(currentThread: PspThread, threadId: Int, timeout: Ptr): Int = _sceKernelWaitThreadEnd(currentThread, threadId, timeout, cb = true)

	fun sceKernelReferThreadStatus(currentThread: PspThread, threadId: Int, out: Ptr): Int {
		val actualThreadId = if (threadId == -1) currentThread.id else threadId
		val thread = threadManager.tryGetById(actualThreadId) ?: return SceKernelErrors.ERROR_KERNEL_NOT_FOUND_THREAD

		val info = SceKernelThreadInfo()

		info.size = SceKernelThreadInfo.size

		info.name = thread.name
		info.attributes = thread.attributes
		info.status = thread.status
		info.threadPreemptionCount = thread.preemptionCount
		info.entryPoint = thread.entryPoint
		info.stackPointer = thread.stack.high.toInt()
		info.stackSize = thread.stack.size.toInt()
		info.GP = thread.state.GP

		info.priorityInit = thread.initPriority
		info.priority = thread.priority
		info.waitType = 0
		info.waitId = 0
		info.wakeupCount = 0
		info.exitStatus = thread.exitStatus
		info.runClocksLow = 0
		info.runClocksHigh = 0
		info.interruptPreemptionCount = 0
		info.threadPreemptionCount = 0
		info.releaseCount = 0

		out.write(SceKernelThreadInfo, info)

		return 0
	}

	fun sceKernelGetThreadId(thread: PspThread): Int = thread.id

	fun sceKernelTerminateThread(threadId: Int): Int {
		val newThread = threadManager.tryGetById(threadId) ?: return SceKernelErrors.ERROR_KERNEL_NOT_FOUND_THREAD
		newThread.stop("_sceKernelTerminateThread")
		newThread.exitStatus = 0x800201ac.toInt()
		return 0
	}

	class Semaphore(
		manager: SemaphoreManager, id: Int, name: String
	) : Resource(manager, id, name) {
		var waitingThreads: Int = 0
		var attribute: Int = 0
		var initialCount: Int = 0
		var count: Int = 0
		var maxCount: Int = 0
		var active: Boolean = false
		val signal = Signal2<Unit>()
	}

	class SemaphoreManager(emulator: Emulator) : Manager<Semaphore>("SemaphoreManager", emulator)

	val semaphoreManager by lazy { SemaphoreManager(emulator) }

	fun sceKernelCreateSema(name: String?, attribute: Int, initialCount: Int, maxCount: Int, options: Ptr): Int {
		val id = semaphoreManager.allocId()
		val sema = semaphoreManager.put(Semaphore(semaphoreManager, id, name ?: "sema$id"))
		sema.attribute = attribute
		sema.initialCount = initialCount
		sema.count = initialCount
		sema.maxCount = maxCount
		sema.signal.clear()
		sema.active = true
		return sema.id
	}

	suspend fun _sceKernelWaitSema(currentThread: PspThread, id: Int, expectedCount: Int, timeout: Ptr): Int {
		val sema = semaphoreManager.getById(id)
		while (sema.active && sema.count < expectedCount) {
			sema.waitingThreads++
			try {
				sema.signal.waitOne()
			} finally {
				sema.waitingThreads--
			}
		}
		sema.count -= expectedCount
		return 0
	}

	fun sceKernelSignalSema(currentThread: PspThread, id: Int, signal: Int): Int {
		val sema = semaphoreManager.getById(id)

		sema.count = min(sema.maxCount, sema.count + signal)
		sema.signal(Unit)

		return 0
	}

	fun sceKernelDeleteSema(id: Int): Int {
		val sema = semaphoreManager.getById(id)
		sema.active = false
		sema.signal(Unit)
		sema.free()
		return 0
	}

	fun sceKernelReferSemaStatus(id: Int, infoStream: Ptr): Int {
		val semaphore: Semaphore = semaphoreManager.tryGetById(id) ?: sceKernelException(SceKernelErrors.ERROR_KERNEL_NOT_FOUND_SEMAPHORE)
		val semaphoreInfo = SceKernelSemaInfo()
		semaphoreInfo.size = SceKernelSemaInfo.size
		semaphoreInfo.attributes = semaphore.attribute
		semaphoreInfo.currentCount = semaphore.count
		semaphoreInfo.initialCount = semaphore.initialCount
		semaphoreInfo.maximumCount = semaphore.maxCount
		semaphoreInfo.name = semaphore.name
		semaphoreInfo.numberOfWaitingThreads = semaphore.waitingThreads
		infoStream.write(SceKernelSemaInfo, semaphoreInfo)
		return 0
	}

	fun sceKernelChangeCurrentThreadAttr(currentThread: PspThread, removeAttributes: Int, addAttributes: Int): Int {
		currentThread.attributes = (currentThread.attributes and removeAttributes.inv()) or addAttributes
		return 0
	}

	fun sceKernelGetVTimerTime(cpu: CpuState): Unit = UNIMPLEMENTED(0x034A921F)
	fun sceKernelRegisterThreadEventHandler(cpu: CpuState): Unit = UNIMPLEMENTED(0x0C106E53)
	fun sceKernelPollMbx(cpu: CpuState): Unit = UNIMPLEMENTED(0x0D81716A)
	fun sceKernelTryLockMutex(cpu: CpuState): Unit = UNIMPLEMENTED(0x0DDCD2C9)
	fun _sceKernelReturnFromTimerHandler(cpu: CpuState): Unit = UNIMPLEMENTED(0x0E927AED)
	fun sceKernelUSec2SysClock(cpu: CpuState): Unit = UNIMPLEMENTED(0x110DEC9A)
	fun sceKernelDelaySysClockThreadCB(cpu: CpuState): Unit = UNIMPLEMENTED(0x1181E963)
	fun sceKernelReceiveMbx(cpu: CpuState): Unit = UNIMPLEMENTED(0x18260574)
	fun sceKernelCreateLwMutex(cpu: CpuState): Unit = UNIMPLEMENTED(0x19CFF145)
	fun sceKernelDonateWakeupThread(cpu: CpuState): Unit = UNIMPLEMENTED(0x1AF94D03)
	fun sceKernelCancelVpl(cpu: CpuState): Unit = UNIMPLEMENTED(0x1D371B8A)
	fun sceKernelCreateVTimer(cpu: CpuState): Unit = UNIMPLEMENTED(0x20FFF560)
	fun sceKernelResumeDispatchThread(cpu: CpuState): Unit = UNIMPLEMENTED(0x27E22EC2)
	fun sceKernelGetCallbackCount(cpu: CpuState): Unit = UNIMPLEMENTED(0x2A3D44FF)
	fun sceKernelReleaseWaitThread(cpu: CpuState): Unit = UNIMPLEMENTED(0x2C34E053)
	fun ThreadManForUser_31327F19(cpu: CpuState): Unit = UNIMPLEMENTED(0x31327F19)
	fun sceKernelDeleteVTimer(cpu: CpuState): Unit = UNIMPLEMENTED(0x328F9E52)
	fun sceKernelReferMsgPipeStatus(cpu: CpuState): Unit = UNIMPLEMENTED(0x33BE4024)
	fun sceKernelCancelMsgPipe(cpu: CpuState): Unit = UNIMPLEMENTED(0x349B864D)
	fun sceKernelCheckCallback(cpu: CpuState): Unit = UNIMPLEMENTED(0x349D6D6C)
	fun sceKernelReferThreadEventHandlerStatus(cpu: CpuState): Unit = UNIMPLEMENTED(0x369EEB6B)
	fun sceKernelTerminateDeleteThread(cpu: CpuState): Unit = UNIMPLEMENTED(0x383F7BCC)
	fun sceKernelReferVplStatus(cpu: CpuState): Unit = UNIMPLEMENTED(0x39810265)
	fun sceKernelSuspendDispatchThread(cpu: CpuState): Unit = UNIMPLEMENTED(0x3AD58B8C)
	fun sceKernelGetThreadExitStatus(cpu: CpuState): Unit = UNIMPLEMENTED(0x3B183E26)
	fun sceKernelReferLwMutexStatusByID(cpu: CpuState): Unit = UNIMPLEMENTED(0x4C145944)
	fun sceKernelGetThreadStackFreeSize(cpu: CpuState): Unit = UNIMPLEMENTED(0x52089CA1)
	fun _sceKernelExitThread(cpu: CpuState): Unit = UNIMPLEMENTED(0x532A522E)
	fun sceKernelSetVTimerHandlerWide(cpu: CpuState): Unit = UNIMPLEMENTED(0x53B00E9A)
	fun sceKernelSetVTimerTime(cpu: CpuState): Unit = UNIMPLEMENTED(0x542AD630)
	fun sceKernelCreateVpl(cpu: CpuState): Unit = UNIMPLEMENTED(0x56C039B5)
	fun sceKernelGetThreadmanIdType(cpu: CpuState): Unit = UNIMPLEMENTED(0x57CF62DD)
	fun sceKernelPollSema(cpu: CpuState): Unit = UNIMPLEMENTED(0x58B1F937)
	fun sceKernelLockMutexCB(cpu: CpuState): Unit = UNIMPLEMENTED(0x5BF4DD27)
	fun sceKernelReferVTimerStatus(cpu: CpuState): Unit = UNIMPLEMENTED(0x5F32BEAA)
	fun sceKernelDeleteLwMutex(cpu: CpuState): Unit = UNIMPLEMENTED(0x60107536)
	fun sceKernelReferSystemStatus(cpu: CpuState): Unit = UNIMPLEMENTED(0x627E6F3A)
	fun sceKernelReferThreadProfiler(cpu: CpuState): Unit = UNIMPLEMENTED(0x64D4540E)
	fun sceKernelSetAlarm(cpu: CpuState): Unit = UNIMPLEMENTED(0x6652B8CA)
	fun sceKernelUnlockMutex(cpu: CpuState): Unit = UNIMPLEMENTED(0x6B30100F)
	fun _sceKernelReturnFromCallback(cpu: CpuState): Unit = UNIMPLEMENTED(0x6E9EA350)
	fun ThreadManForUser_71040D5C(cpu: CpuState): Unit = UNIMPLEMENTED(0x71040D5C)
	fun sceKernelReleaseThreadEventHandler(cpu: CpuState): Unit = UNIMPLEMENTED(0x72F3C145)
	fun sceKernelReferCallbackStatus(cpu: CpuState): Unit = UNIMPLEMENTED(0x730ED8BC)
	fun sceKernelReceiveMsgPipe(cpu: CpuState): Unit = UNIMPLEMENTED(0x74829B76)
	fun sceKernelResumeThread(cpu: CpuState): Unit = UNIMPLEMENTED(0x75156E8F)
	fun sceKernelCreateMsgPipe(cpu: CpuState): Unit = UNIMPLEMENTED(0x7C0DC2A0)
	fun sceKernelSendMsgPipeCB(cpu: CpuState): Unit = UNIMPLEMENTED(0x7C41F2C2)
	fun ThreadManForUser_7CFF8CF3(cpu: CpuState): Unit = UNIMPLEMENTED(0x7CFF8CF3)
	fun sceKernelCancelAlarm(cpu: CpuState): Unit = UNIMPLEMENTED(0x7E65B999)
	fun sceKernelExitDeleteThread(cpu: CpuState): Unit = UNIMPLEMENTED(0x809CE29B)
	fun sceKernelCreateMbx(cpu: CpuState): Unit = UNIMPLEMENTED(0x8125221D)
	fun sceKernelReferGlobalProfiler(cpu: CpuState): Unit = UNIMPLEMENTED(0x8218B4DD)
	fun sceKernelDeleteMbx(cpu: CpuState): Unit = UNIMPLEMENTED(0x86255ADA)
	fun ThreadManForUser_8672E3D0(cpu: CpuState): Unit = UNIMPLEMENTED(0x8672E3D0)
	fun sceKernelSendMsgPipe(cpu: CpuState): Unit = UNIMPLEMENTED(0x876DBFAD)
	fun sceKernelCancelReceiveMbx(cpu: CpuState): Unit = UNIMPLEMENTED(0x87D4DD36)
	fun sceKernelCancelMutex(cpu: CpuState): Unit = UNIMPLEMENTED(0x87D9223C)
	fun sceKernelTrySendMsgPipe(cpu: CpuState): Unit = UNIMPLEMENTED(0x884C9F90)
	fun sceKernelDeleteVpl(cpu: CpuState): Unit = UNIMPLEMENTED(0x89B3D48C)
	fun sceKernelCancelSema(cpu: CpuState): Unit = UNIMPLEMENTED(0x8FFDF9A2)
	fun sceKernelRotateThreadReadyQueue(cpu: CpuState): Unit = UNIMPLEMENTED(0x912354A7)
	fun sceKernelGetThreadmanIdList(cpu: CpuState): Unit = UNIMPLEMENTED(0x94416130)
	fun sceKernelSuspendThread(cpu: CpuState): Unit = UNIMPLEMENTED(0x9944F31F)
	fun sceKernelSleepThread(cpu: CpuState): Unit = UNIMPLEMENTED(0x9ACE131E)
	fun sceKernelReferMbxStatus(cpu: CpuState): Unit = UNIMPLEMENTED(0xA8E8C846)
	fun sceKernelReferMutexStatus(cpu: CpuState): Unit = UNIMPLEMENTED(0xA9C2CB9A)
	fun sceKernelTryAllocateVpl(cpu: CpuState): Unit = UNIMPLEMENTED(0xAF36D708)
	fun sceKernelLockMutex(cpu: CpuState): Unit = UNIMPLEMENTED(0xB011B11F)
	fun sceKernelSetSysClockAlarm(cpu: CpuState): Unit = UNIMPLEMENTED(0xB2C25152)
	fun sceKernelGetVTimerBase(cpu: CpuState): Unit = UNIMPLEMENTED(0xB3A59970)
	fun sceKernelFreeVpl(cpu: CpuState): Unit = UNIMPLEMENTED(0xB736E9FF)
	fun sceKernelGetVTimerBaseWide(cpu: CpuState): Unit = UNIMPLEMENTED(0xB7C18B77)
	fun sceKernelCreateMutex(cpu: CpuState): Unit = UNIMPLEMENTED(0xB7D098C6)
	fun sceKernelCancelCallback(cpu: CpuState): Unit = UNIMPLEMENTED(0xBA4051D6)
	fun sceKernelSysClock2USec(cpu: CpuState): Unit = UNIMPLEMENTED(0xBA6B92E2)
	fun sceKernelDelaySysClockThread(cpu: CpuState): Unit = UNIMPLEMENTED(0xBD123D9E)
	fun sceKernelAllocateVpl(cpu: CpuState): Unit = UNIMPLEMENTED(0xBED27435)
	fun ThreadManForUser_BEED3A47(cpu: CpuState): Unit = UNIMPLEMENTED(0xBEED3A47)
	fun sceKernelGetVTimerTimeWide(cpu: CpuState): Unit = UNIMPLEMENTED(0xC0B3FFD2)
	fun sceKernelNotifyCallback(cpu: CpuState): Unit = UNIMPLEMENTED(0xC11BA8C4)
	fun sceKernelStartVTimer(cpu: CpuState): Unit = UNIMPLEMENTED(0xC68D9437)
	fun sceKernelUSec2SysClockWide(cpu: CpuState): Unit = UNIMPLEMENTED(0xC8CD158C)
	fun sceKernelStopVTimer(cpu: CpuState): Unit = UNIMPLEMENTED(0xD0AEEE87)
	fun sceKernelCheckThreadStack(cpu: CpuState): Unit = UNIMPLEMENTED(0xD13BDE95)
	fun sceKernelCancelVTimerHandler(cpu: CpuState): Unit = UNIMPLEMENTED(0xD2D615EF)
	fun sceKernelWakeupThread(cpu: CpuState): Unit = UNIMPLEMENTED(0xD59EAD2F)
	fun sceKernelSetVTimerHandler(cpu: CpuState): Unit = UNIMPLEMENTED(0xD8B299AE)
	fun sceKernelReferAlarmStatus(cpu: CpuState): Unit = UNIMPLEMENTED(0xDAA3F564)
	fun sceKernelGetSystemTime(cpu: CpuState): Unit = UNIMPLEMENTED(0xDB738F35)
	fun sceKernelTryReceiveMsgPipe(cpu: CpuState): Unit = UNIMPLEMENTED(0xDF52098F)
	fun sceKernelSysClock2USecWide(cpu: CpuState): Unit = UNIMPLEMENTED(0xE1619D7C)
	fun sceKernelSendMbx(cpu: CpuState): Unit = UNIMPLEMENTED(0xE9B3061E)
	fun sceKernelAllocateVplCB(cpu: CpuState): Unit = UNIMPLEMENTED(0xEC0A693F)
	fun sceKernelDeleteCallback(cpu: CpuState): Unit = UNIMPLEMENTED(0xEDBA5844)
	fun sceKernelDeleteMsgPipe(cpu: CpuState): Unit = UNIMPLEMENTED(0xF0B7DA1C)
	fun sceKernelReceiveMbxCB(cpu: CpuState): Unit = UNIMPLEMENTED(0xF3986382)
	fun sceKernelDeleteMutex(cpu: CpuState): Unit = UNIMPLEMENTED(0xF8170FBE)
	fun sceKernelSetVTimerTimeWide(cpu: CpuState): Unit = UNIMPLEMENTED(0xFB6425C3)
	fun sceKernelReceiveMsgPipeCB(cpu: CpuState): Unit = UNIMPLEMENTED(0xFBFA697D)
	fun sceKernelCancelWakeupThread(cpu: CpuState): Unit = UNIMPLEMENTED(0xFCCFAD26)
	fun sceKernelReferThreadRunStatus(cpu: CpuState): Unit = UNIMPLEMENTED(0xFFC36A14)

	override fun registerModule() {
		registerModuleEventFlags()
		registerModuleFpl()

		// Time
		registerFunctionLong("sceKernelGetSystemTimeWide", 0x82BC5777, since = 150) { sceKernelGetSystemTimeWide() }
		registerFunctionInt("sceKernelGetSystemTimeLow", 0x369ED59D, since = 150) { sceKernelGetSystemTimeLow() }

		// Thread
		registerFunctionInt("sceKernelCreateThread", 0x446D8DE6, since = 150) { sceKernelCreateThread(str, int, int, int, int, ptr) }
		registerFunctionInt("sceKernelStartThread", 0xF475845D, since = 150) { sceKernelStartThread(thread, int, int, ptr) }
		registerFunctionInt("sceKernelGetThreadCurrentPriority", 0x94AA61EE, since = 150) { sceKernelGetThreadCurrentPriority(thread) }
		registerFunctionInt("sceKernelSleepThread", 0x9ACE131E, since = 150) { sceKernelSleepThread(thread) }
		registerFunctionInt("sceKernelSleepThreadCB", 0x82826F70, since = 150) { sceKernelSleepThreadCB(thread) }
		registerFunctionInt("sceKernelDelayThreadCB", 0x68DA9E36, since = 150) { sceKernelDelayThreadCB(thread, int) }
		registerFunctionInt("sceKernelDelayThread", 0xCEADEB47, since = 150) { sceKernelDelayThread(thread, int) }
		registerFunctionSuspendInt("sceKernelWaitThreadEnd", 0x278C0DF5, since = 150) { sceKernelWaitThreadEnd(thread, int, ptr) }
		registerFunctionSuspendInt("sceKernelWaitThreadEndCB", 0x840E8133, since = 150) { sceKernelWaitThreadEndCB(thread, int, ptr) }
		registerFunctionInt("sceKernelReferThreadStatus", 0x17C1684E, since = 150) { sceKernelReferThreadStatus(thread, int, ptr) }
		registerFunctionInt("sceKernelGetThreadId", 0x293B45B8, since = 150) { sceKernelGetThreadId(thread) }
		registerFunctionInt("sceKernelTerminateThread", 0x616403BA, since = 150) { sceKernelTerminateThread(int) }
		registerFunctionInt("sceKernelDeleteThread", 0x9FA03CD3, since = 150) { sceKernelDeleteThread(int) }
		registerFunctionVoid("sceKernelExitThread", 0xAA73C935, since = 150) { sceKernelExitThread(thread, int) }
		registerFunctionInt("sceKernelChangeCurrentThreadAttr", 0xEA748E31, since = 150) { sceKernelChangeCurrentThreadAttr(thread, int, int) }
		registerFunctionInt("sceKernelChangeThreadPriority", 0x71BC9871, since = 150) { sceKernelChangeThreadPriority(int, int) }

		// Callbacks
		registerFunctionInt("sceKernelCreateCallback", 0xE81CAF8F, since = 150) { sceKernelCreateCallback(str, ptr, int) }

		// Semaphores
		registerFunctionInt("sceKernelCreateSema", 0xD6DA4BA1, since = 150) { sceKernelCreateSema(str, int, int, int, ptr) }
		registerFunctionSuspendInt("sceKernelWaitSema", 0x4E3A1105, since = 150, cb = false) { _sceKernelWaitSema(thread, int, int, ptr) }
		registerFunctionSuspendInt("sceKernelWaitSemaCB", 0x6D212BAC, since = 150, cb = true) { _sceKernelWaitSema(thread, int, int, ptr) }
		registerFunctionInt("sceKernelSignalSema", 0x3F53E640, since = 150) { sceKernelSignalSema(thread, int, int) }
		registerFunctionInt("sceKernelDeleteSema", 0x28B6489C, since = 150) { sceKernelDeleteSema(int) }
		registerFunctionInt("sceKernelReferSemaStatus", 0xBC6FEBC5, since = 150) { sceKernelReferSemaStatus(int, ptr) }

		registerFunctionRaw("sceKernelGetVTimerTime", 0x034A921F, since = 150) { sceKernelGetVTimerTime(it) }
		registerFunctionRaw("sceKernelRegisterThreadEventHandler", 0x0C106E53, since = 150) { sceKernelRegisterThreadEventHandler(it) }
		registerFunctionRaw("sceKernelPollMbx", 0x0D81716A, since = 150) { sceKernelPollMbx(it) }
		registerFunctionRaw("sceKernelTryLockMutex", 0x0DDCD2C9, since = 150) { sceKernelTryLockMutex(it) }
		registerFunctionRaw("_sceKernelReturnFromTimerHandler", 0x0E927AED, since = 150) { _sceKernelReturnFromTimerHandler(it) }
		registerFunctionRaw("sceKernelUSec2SysClock", 0x110DEC9A, since = 150) { sceKernelUSec2SysClock(it) }
		registerFunctionRaw("sceKernelDelaySysClockThreadCB", 0x1181E963, since = 150) { sceKernelDelaySysClockThreadCB(it) }
		registerFunctionRaw("sceKernelReceiveMbx", 0x18260574, since = 150) { sceKernelReceiveMbx(it) }
		registerFunctionRaw("sceKernelCreateLwMutex", 0x19CFF145, since = 150) { sceKernelCreateLwMutex(it) }
		registerFunctionRaw("sceKernelDonateWakeupThread", 0x1AF94D03, since = 150) { sceKernelDonateWakeupThread(it) }
		registerFunctionRaw("sceKernelCancelVpl", 0x1D371B8A, since = 150) { sceKernelCancelVpl(it) }
		registerFunctionRaw("sceKernelCreateVTimer", 0x20FFF560, since = 150) { sceKernelCreateVTimer(it) }
		registerFunctionRaw("sceKernelResumeDispatchThread", 0x27E22EC2, since = 150) { sceKernelResumeDispatchThread(it) }
		registerFunctionRaw("sceKernelGetCallbackCount", 0x2A3D44FF, since = 150) { sceKernelGetCallbackCount(it) }
		registerFunctionRaw("sceKernelReleaseWaitThread", 0x2C34E053, since = 150) { sceKernelReleaseWaitThread(it) }
		registerFunctionRaw("ThreadManForUser_31327F19", 0x31327F19, since = 150) { ThreadManForUser_31327F19(it) }
		registerFunctionRaw("sceKernelDeleteVTimer", 0x328F9E52, since = 150) { sceKernelDeleteVTimer(it) }
		registerFunctionRaw("sceKernelReferMsgPipeStatus", 0x33BE4024, since = 150) { sceKernelReferMsgPipeStatus(it) }
		registerFunctionRaw("sceKernelCancelMsgPipe", 0x349B864D, since = 150) { sceKernelCancelMsgPipe(it) }
		registerFunctionRaw("sceKernelCheckCallback", 0x349D6D6C, since = 150) { sceKernelCheckCallback(it) }
		registerFunctionRaw("sceKernelReferThreadEventHandlerStatus", 0x369EEB6B, since = 150) { sceKernelReferThreadEventHandlerStatus(it) }
		registerFunctionRaw("sceKernelTerminateDeleteThread", 0x383F7BCC, since = 150) { sceKernelTerminateDeleteThread(it) }
		registerFunctionRaw("sceKernelReferVplStatus", 0x39810265, since = 150) { sceKernelReferVplStatus(it) }
		registerFunctionRaw("sceKernelSuspendDispatchThread", 0x3AD58B8C, since = 150) { sceKernelSuspendDispatchThread(it) }
		registerFunctionRaw("sceKernelGetThreadExitStatus", 0x3B183E26, since = 150) { sceKernelGetThreadExitStatus(it) }
		registerFunctionRaw("sceKernelReferLwMutexStatusByID", 0x4C145944, since = 150) { sceKernelReferLwMutexStatusByID(it) }
		registerFunctionRaw("sceKernelGetThreadStackFreeSize", 0x52089CA1, since = 150) { sceKernelGetThreadStackFreeSize(it) }
		registerFunctionRaw("_sceKernelExitThread", 0x532A522E, since = 150) { _sceKernelExitThread(it) }
		registerFunctionRaw("sceKernelSetVTimerHandlerWide", 0x53B00E9A, since = 150) { sceKernelSetVTimerHandlerWide(it) }
		registerFunctionRaw("sceKernelSetVTimerTime", 0x542AD630, since = 150) { sceKernelSetVTimerTime(it) }
		registerFunctionRaw("sceKernelCreateVpl", 0x56C039B5, since = 150) { sceKernelCreateVpl(it) }
		registerFunctionRaw("sceKernelGetThreadmanIdType", 0x57CF62DD, since = 150) { sceKernelGetThreadmanIdType(it) }
		registerFunctionRaw("sceKernelPollSema", 0x58B1F937, since = 150) { sceKernelPollSema(it) }
		registerFunctionRaw("sceKernelLockMutexCB", 0x5BF4DD27, since = 150) { sceKernelLockMutexCB(it) }
		registerFunctionRaw("sceKernelReferVTimerStatus", 0x5F32BEAA, since = 150) { sceKernelReferVTimerStatus(it) }
		registerFunctionRaw("sceKernelDeleteLwMutex", 0x60107536, since = 150) { sceKernelDeleteLwMutex(it) }
		registerFunctionRaw("sceKernelReferSystemStatus", 0x627E6F3A, since = 150) { sceKernelReferSystemStatus(it) }
		registerFunctionRaw("sceKernelReferThreadProfiler", 0x64D4540E, since = 150) { sceKernelReferThreadProfiler(it) }
		registerFunctionRaw("sceKernelSetAlarm", 0x6652B8CA, since = 150) { sceKernelSetAlarm(it) }
		registerFunctionRaw("sceKernelUnlockMutex", 0x6B30100F, since = 150) { sceKernelUnlockMutex(it) }
		registerFunctionRaw("_sceKernelReturnFromCallback", 0x6E9EA350, since = 150) { _sceKernelReturnFromCallback(it) }
		registerFunctionRaw("ThreadManForUser_71040D5C", 0x71040D5C, since = 150) { ThreadManForUser_71040D5C(it) }
		registerFunctionRaw("sceKernelReleaseThreadEventHandler", 0x72F3C145, since = 150) { sceKernelReleaseThreadEventHandler(it) }
		registerFunctionRaw("sceKernelReferCallbackStatus", 0x730ED8BC, since = 150) { sceKernelReferCallbackStatus(it) }
		registerFunctionRaw("sceKernelReceiveMsgPipe", 0x74829B76, since = 150) { sceKernelReceiveMsgPipe(it) }
		registerFunctionRaw("sceKernelResumeThread", 0x75156E8F, since = 150) { sceKernelResumeThread(it) }
		registerFunctionRaw("sceKernelCreateMsgPipe", 0x7C0DC2A0, since = 150) { sceKernelCreateMsgPipe(it) }
		registerFunctionRaw("sceKernelSendMsgPipeCB", 0x7C41F2C2, since = 150) { sceKernelSendMsgPipeCB(it) }
		registerFunctionRaw("ThreadManForUser_7CFF8CF3", 0x7CFF8CF3, since = 150) { ThreadManForUser_7CFF8CF3(it) }
		registerFunctionRaw("sceKernelCancelAlarm", 0x7E65B999, since = 150) { sceKernelCancelAlarm(it) }
		registerFunctionRaw("sceKernelExitDeleteThread", 0x809CE29B, since = 150) { sceKernelExitDeleteThread(it) }
		registerFunctionRaw("sceKernelCreateMbx", 0x8125221D, since = 150) { sceKernelCreateMbx(it) }
		registerFunctionRaw("sceKernelReferGlobalProfiler", 0x8218B4DD, since = 150) { sceKernelReferGlobalProfiler(it) }
		registerFunctionRaw("sceKernelDeleteMbx", 0x86255ADA, since = 150) { sceKernelDeleteMbx(it) }
		registerFunctionRaw("ThreadManForUser_8672E3D0", 0x8672E3D0, since = 150) { ThreadManForUser_8672E3D0(it) }
		registerFunctionRaw("sceKernelSendMsgPipe", 0x876DBFAD, since = 150) { sceKernelSendMsgPipe(it) }
		registerFunctionRaw("sceKernelCancelReceiveMbx", 0x87D4DD36, since = 150) { sceKernelCancelReceiveMbx(it) }
		registerFunctionRaw("sceKernelCancelMutex", 0x87D9223C, since = 150) { sceKernelCancelMutex(it) }
		registerFunctionRaw("sceKernelTrySendMsgPipe", 0x884C9F90, since = 150) { sceKernelTrySendMsgPipe(it) }
		registerFunctionRaw("sceKernelDeleteVpl", 0x89B3D48C, since = 150) { sceKernelDeleteVpl(it) }
		registerFunctionRaw("sceKernelCancelSema", 0x8FFDF9A2, since = 150) { sceKernelCancelSema(it) }
		registerFunctionRaw("sceKernelRotateThreadReadyQueue", 0x912354A7, since = 150) { sceKernelRotateThreadReadyQueue(it) }
		registerFunctionRaw("sceKernelGetThreadmanIdList", 0x94416130, since = 150) { sceKernelGetThreadmanIdList(it) }
		registerFunctionRaw("sceKernelSuspendThread", 0x9944F31F, since = 150) { sceKernelSuspendThread(it) }
		registerFunctionRaw("sceKernelReferMbxStatus", 0xA8E8C846, since = 150) { sceKernelReferMbxStatus(it) }
		registerFunctionRaw("sceKernelReferMutexStatus", 0xA9C2CB9A, since = 150) { sceKernelReferMutexStatus(it) }
		registerFunctionRaw("sceKernelTryAllocateVpl", 0xAF36D708, since = 150) { sceKernelTryAllocateVpl(it) }
		registerFunctionRaw("sceKernelLockMutex", 0xB011B11F, since = 150) { sceKernelLockMutex(it) }
		registerFunctionRaw("sceKernelSetSysClockAlarm", 0xB2C25152, since = 150) { sceKernelSetSysClockAlarm(it) }
		registerFunctionRaw("sceKernelGetVTimerBase", 0xB3A59970, since = 150) { sceKernelGetVTimerBase(it) }
		registerFunctionRaw("sceKernelFreeVpl", 0xB736E9FF, since = 150) { sceKernelFreeVpl(it) }
		registerFunctionRaw("sceKernelGetVTimerBaseWide", 0xB7C18B77, since = 150) { sceKernelGetVTimerBaseWide(it) }
		registerFunctionRaw("sceKernelCreateMutex", 0xB7D098C6, since = 150) { sceKernelCreateMutex(it) }
		registerFunctionRaw("sceKernelCancelCallback", 0xBA4051D6, since = 150) { sceKernelCancelCallback(it) }
		registerFunctionRaw("sceKernelSysClock2USec", 0xBA6B92E2, since = 150) { sceKernelSysClock2USec(it) }
		registerFunctionRaw("sceKernelDelaySysClockThread", 0xBD123D9E, since = 150) { sceKernelDelaySysClockThread(it) }
		registerFunctionRaw("sceKernelAllocateVpl", 0xBED27435, since = 150) { sceKernelAllocateVpl(it) }
		registerFunctionRaw("ThreadManForUser_BEED3A47", 0xBEED3A47, since = 150) { ThreadManForUser_BEED3A47(it) }
		registerFunctionRaw("sceKernelGetVTimerTimeWide", 0xC0B3FFD2, since = 150) { sceKernelGetVTimerTimeWide(it) }
		registerFunctionRaw("sceKernelNotifyCallback", 0xC11BA8C4, since = 150) { sceKernelNotifyCallback(it) }
		registerFunctionRaw("sceKernelStartVTimer", 0xC68D9437, since = 150) { sceKernelStartVTimer(it) }
		registerFunctionRaw("sceKernelUSec2SysClockWide", 0xC8CD158C, since = 150) { sceKernelUSec2SysClockWide(it) }
		registerFunctionRaw("sceKernelStopVTimer", 0xD0AEEE87, since = 150) { sceKernelStopVTimer(it) }
		registerFunctionRaw("sceKernelCheckThreadStack", 0xD13BDE95, since = 150) { sceKernelCheckThreadStack(it) }
		registerFunctionRaw("sceKernelCancelVTimerHandler", 0xD2D615EF, since = 150) { sceKernelCancelVTimerHandler(it) }
		registerFunctionRaw("sceKernelWakeupThread", 0xD59EAD2F, since = 150) { sceKernelWakeupThread(it) }
		registerFunctionRaw("sceKernelSetVTimerHandler", 0xD8B299AE, since = 150) { sceKernelSetVTimerHandler(it) }
		registerFunctionRaw("sceKernelReferAlarmStatus", 0xDAA3F564, since = 150) { sceKernelReferAlarmStatus(it) }
		registerFunctionRaw("sceKernelGetSystemTime", 0xDB738F35, since = 150) { sceKernelGetSystemTime(it) }
		registerFunctionRaw("sceKernelTryReceiveMsgPipe", 0xDF52098F, since = 150) { sceKernelTryReceiveMsgPipe(it) }
		registerFunctionRaw("sceKernelSysClock2USecWide", 0xE1619D7C, since = 150) { sceKernelSysClock2USecWide(it) }
		registerFunctionRaw("sceKernelSendMbx", 0xE9B3061E, since = 150) { sceKernelSendMbx(it) }
		registerFunctionRaw("sceKernelAllocateVplCB", 0xEC0A693F, since = 150) { sceKernelAllocateVplCB(it) }
		registerFunctionRaw("sceKernelDeleteCallback", 0xEDBA5844, since = 150) { sceKernelDeleteCallback(it) }
		registerFunctionRaw("sceKernelDeleteMsgPipe", 0xF0B7DA1C, since = 150) { sceKernelDeleteMsgPipe(it) }
		registerFunctionRaw("sceKernelReceiveMbxCB", 0xF3986382, since = 150) { sceKernelReceiveMbxCB(it) }
		registerFunctionRaw("sceKernelDeleteMutex", 0xF8170FBE, since = 150) { sceKernelDeleteMutex(it) }
		registerFunctionRaw("sceKernelSetVTimerTimeWide", 0xFB6425C3, since = 150) { sceKernelSetVTimerTimeWide(it) }
		registerFunctionRaw("sceKernelReceiveMsgPipeCB", 0xFBFA697D, since = 150) { sceKernelReceiveMsgPipeCB(it) }
		registerFunctionRaw("sceKernelCancelWakeupThread", 0xFCCFAD26, since = 150) { sceKernelCancelWakeupThread(it) }
		registerFunctionRaw("sceKernelReferThreadRunStatus", 0xFFC36A14, since = 150) { sceKernelReferThreadRunStatus(it) }
	}
}

class SceKernelSemaInfo(
	var size: Int = 0,
	var name: String = "",
	var attributes: Int = 0, // SemaphoreAttribute
	var initialCount: Int = 0,
	var currentCount: Int = 0,
	var maximumCount: Int = 0,
	var numberOfWaitingThreads: Int = 0
) {
	companion object : Struct<SceKernelSemaInfo>({ SceKernelSemaInfo() },
		SceKernelSemaInfo::size AS INT32,
		SceKernelSemaInfo::name AS STRINGZ(32),
		SceKernelSemaInfo::attributes AS INT32,
		SceKernelSemaInfo::initialCount AS INT32,
		SceKernelSemaInfo::currentCount AS INT32,
		SceKernelSemaInfo::maximumCount AS INT32,
		SceKernelSemaInfo::numberOfWaitingThreads AS INT32
	)
}

class K0Structure(
	var unk: IntArray = IntArray(48), // +0000
	var threadId: Int = 0, // +00C0
	var unk1: Int = 0, // +00C4
	var stackAddr: Int = 0, // +00C8
	var unk3: IntArray = IntArray(11),// +00CC
	var f1: Int = 0, // +00F8
	var f2: Int = 0 // +00FC
) {
	companion object : Struct<K0Structure>({ K0Structure() },
		K0Structure::unk AS INTLIKEARRAY(INT32, 48),
		K0Structure::threadId AS INT32,
		K0Structure::unk1 AS INT32,
		K0Structure::stackAddr AS INT32,
		K0Structure::unk3 AS INTLIKEARRAY(INT32, 11),
		K0Structure::f1 AS INT32,
		K0Structure::f2 AS INT32
	)
}