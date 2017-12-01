package com.soywiz.kpspemu

import KpspTests
import MyAssert
import com.soywiz.klogger.LogLevel
import com.soywiz.klogger.LoggerManager
import com.soywiz.korio.async.eventLoop
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.coroutine.getCoroutineContext
import com.soywiz.korio.lang.Console
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.toAsync
import com.soywiz.korio.util.hex
import com.soywiz.korio.util.quote
import com.soywiz.korio.vfs.MemoryVfsMix
import com.soywiz.kpspemu.format.elf.loadElfAndSetRegisters
import com.soywiz.kpspemu.hle.registerNativeModules
import org.junit.Test
import kotlin.test.Ignore

class IntegrationTests {
	val TRACE = false
	val TRACE1 = false
	//val TRACE = true
	//val TRACE1 = true

	//@Test fun testDmac() = testFile("dmac/dmactest")

	@Test fun testCpuAlu() = testFile("cpu/cpu_alu/cpu_alu")
	@Test fun testCpuBranch() = testFile("cpu/cpu_alu/cpu_branch")
	@Test fun testCpuBranch2() = testFile("cpu/cpu_alu/cpu_branch2")

	@Test fun testIcache() = testFile("cpu/icache/icache")

	@Test fun testLsu() = testFile("cpu/lsu/lsu")

	@Test fun testFpu() = testFile("cpu/fpu/fpu", ignores = listOf(
		"mul.s 0.296558 * 62.000000, CAST_1 = 18.38657^",
		"mul.s 0.296558 * 62.000000, FLOOR_3 = 18.38657^"
	))

	@Test fun testFcr() = testFile("cpu/fpu/fcr", ignores = listOf(
		"Underflow:\n  fcr0: 00003351, fcr25: 00000000, fcr26: 00000000, fcr27: 00000000, fcr28: 00000000, fcr31: ^^^^^^^^",
		"Inexact:\n  fcr0: 00003351, fcr25: 00000000, fcr26: 00000000, fcr27: 00000000, fcr28: 00000000, fcr31: ^^^^^^^^"
	))

	@Test fun testRtc() = testFile("rtc/rtc")

	@Test fun testThreadsK0() = testFile("threads/k0/k0")

	//@Test fun testKirk() = testFile("kirk/kirk")

	@Ignore
	@Test fun testLoaderBss() = testFile("loader/bss/bss")

	//@Test fun testVfpuColors() = testFile("cpu/vfpu/colors")

	fun testFile(name: String, ignores: List<String> = listOf(), processor: (String) -> String = { it }) = syncTest {
		testFile(
			KpspTests.pspautotests["$name.prx"].readAsSyncStream(),
			KpspTests.pspautotests["$name.expected"].readString(),
			ignores,
			processor
		)
	}

	suspend fun testFile(elf: SyncStream, expected: String, ignores: List<String>, processor: (String) -> String = { it }) {
		val emulator = Emulator(getCoroutineContext())
		emulator.display.exposeDisplay = false
		emulator.registerNativeModules()
		//val info = emulator.loadElfAndSetRegisters(elf, "ms0:/PSP/GAME/EBOOT.PBP")
		emulator.fileManager.currentDirectory = "ms0:/PSP/GAME/virtual"
		emulator.fileManager.executableFile = "ms0:/PSP/GAME/virtual/EBOOT.PBP"
		emulator.deviceManager.mount(emulator.fileManager.currentDirectory, MemoryVfsMix("EBOOT.PBP" to elf.clone().toAsync()))
		val info = emulator.loadElfAndSetRegisters(elf, listOf("ms0:/PSP/GAME/virtual/EBOOT.PBP"))

		if (TRACE1) {
			LoggerManager.defaultLevel = LogLevel.TRACE
		}

		if (TRACE) {
			emulator.threadManager.trace("user_main", trace = true)
			LoggerManager.defaultLevel = LogLevel.TRACE
		} else {
			LoggerManager.setLevel("ElfPsp", LogLevel.ERROR)
		}

		try {
			//println("[1]")
			while (emulator.running) {
				//println("[2] : ${emulator.running}")
				emulator.threadManager.step() // UPDATE THIS
				emulator.display.startVsync()
				getCoroutineContext().eventLoop.step(10)
				emulator.display.startVsync()
				//println("[3]")
				if (TRACE) {
					for (thread in emulator.threadManager.threads) println("PC: ${thread.state.PC.hex} : ${(thread.state.PC - info.baseAddress).hex}")
				}
			}
		} catch (e: Throwable) {
			Console.error("Partial output generated:")
			Console.error("'" + emulator.output.toString() + "'")
			throw e
		}

		val ignoresRegex = ignores.map {
			Regex(Regex.quote(it).replace("\\^", ".")) to it
		}

		fun String.normalize(): String {
			var out = this.replace("\r\n", "\n").replace("\r", "\n").trimEnd()
			for (rex in ignoresRegex) {
				out = out.replace(rex.first, rex.second)
			}
			return out
		}
		MyAssert.assertEquals(expected.normalize(), processor(emulator.output.toString().normalize()))
		//assertEquals(expected.normalize(), processor(emulator.output.toString().normalize()))
	}
}
