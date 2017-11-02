package com.soywiz.kpspemu

import com.soywiz.korag.AG
import com.soywiz.korag.shader.*
import com.soywiz.korge.Korge
import com.soywiz.korge.input.onKeyDown
import com.soywiz.korge.input.onKeyUp
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.scene.Module
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.texture
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korio.JvmStatic
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.util.OS
import com.soywiz.korio.vfs.IsoVfs
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korio.vfs.applicationVfs
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.SizeInt
import com.soywiz.kpspemu.ctrl.PspCtrlButtons
import com.soywiz.kpspemu.format.Pbp
import com.soywiz.kpspemu.format.elf.PspElf
import com.soywiz.kpspemu.format.elf.loadElfAndSetRegisters
import com.soywiz.kpspemu.ge.*
import com.soywiz.kpspemu.hle.registerNativeModules
import com.soywiz.kpspemu.mem.Memory
import com.soywiz.kpspemu.util.*
import com.soywiz.kpspemu.util.io.ZipVfs2
import kotlin.reflect.KClass

const val DIRECT_FAST_SHARP_RENDERING = false
//const val DIRECT_FAST_SHARP_RENDERING = true

fun main(args: Array<String>) = Main.main(args)

object Main {
	@JvmStatic
	fun main(args: Array<String>) = Korge(KpspemuModule, injector = AsyncInjector()
		.mapPrototype(KpspemuMainScene::class) { KpspemuMainScene() }
	)
}

object KpspemuModule : Module() {
	//override val clearEachFrame: Boolean = false
	override val clearEachFrame: Boolean = true
	override val mainScene: KClass<out Scene> = KpspemuMainScene::class
	override val title: String = "kpspemu"
	override val size: SizeInt get() = SizeInt(480, 272)
}

class KpspemuMainScene : Scene(), WithEmulator {
	lateinit override var emulator: Emulator

	class KorgeRenderer(val scene: KpspemuMainScene) : View(scene.views), GpuRenderer, WithEmulator {
		val logger = PspLogger("KorgeRenderer")

		override val emulator: Emulator get() = scene.emulator
		val batchesQueue = arrayListOf<List<GeBatch>>()

		override fun render(batches: List<GeBatch>) {
			batchesQueue += batches
			//println("KorgeRenderer.render: $batches")
			//display.rawDisplay = false
		}

		val tempBmp = Bitmap32(512, 272)

		val u_modelViewProjMatrix = Uniform("u_modelViewProjMatrix", VarType.Mat4)

		data class ProgramLayout(val program: Program, val layout: VertexLayout)

		val programLayoutByVertexType = LinkedHashMap<Int, ProgramLayout>()

		fun getProgramLayout(state: GeState): ProgramLayout {
			return programLayoutByVertexType.getOrPut(state.vertexType) { createProgramLayout(state) }
		}

		fun createProgramLayout(state: GeState): ProgramLayout {
			val vtype = VertexType().init(state)
			val COUNT2 = listOf(VarType.VOID, VarType.BYTE(2), VarType.SHORT(2), VarType.FLOAT(2))
			val COUNT3 = listOf(VarType.VOID, VarType.BYTE(3), VarType.SHORT(3), VarType.FLOAT(3))

			//val COLORS = listOf(VarType.VOID, VarType.VOID, VarType.VOID, VarType.VOID, VarType.SHORT(1), VarType.SHORT(1), VarType.SHORT(1), VarType.BYTE(4))
			val COLORS = listOf(VarType.VOID, VarType.VOID, VarType.VOID, VarType.VOID, VarType.SHORT(1), VarType.SHORT(1), VarType.SHORT(1), VarType.Byte4)

			//val a_Tex = Attribute("a_Tex", VarType.Float2, normalized = false)
			val a_Tex = if (vtype.hasTexture) Attribute("a_Tex", COUNT2[vtype.tex.id], normalized = false, offset = vtype.texOffset) else null
			val a_Col = if (vtype.hasColor) Attribute("a_Col", COLORS[vtype.col.id], normalized = true, offset = vtype.colOffset) else null
			val a_Pos = Attribute("a_Pos", COUNT3[vtype.pos.id], normalized = false, offset = vtype.posOffset)
			val v_Col = Varying("v_Col", VarType.Byte4)

			val layout = VertexLayout(listOf(a_Tex, a_Col, a_Pos).filterNotNull(), vtype.size())

			val program = Program(
				name = "$vtype",
				vertex = VertexShader {
					//SET(out, vec4(a_Pos, 0f.lit, 1f.lit) * u_ProjMat)
					//SET(out, u_modelViewProjMatrix * vec4(a_Pos, 1f.lit))
					SET(out, u_modelViewProjMatrix * vec4(a_Pos, 1f.lit))
					if (a_Col != null) {
						SET(v_Col, a_Col[if (OS.isJs) "bgra" else "rgba"])
					}
					//SET(out, vec4(a_Pos, 1f.lit))
				},
				fragment = FragmentShader {
					if (a_Col != null) {
						SET(out, v_Col)
					}
				}
			)

			return ProgramLayout(program, layout)
		}

		private var indexBuffer: AG.Buffer? = null
		private var vertexBuffer: AG.Buffer? = null

		override fun render(ctx: RenderContext, m: Matrix2d) {
			val ag = ctx.ag
			ctx.flush()
			if (DIRECT_FAST_SHARP_RENDERING) {
				if (batchesQueue.isNotEmpty()) {
					try {
						for (batches in batchesQueue) for (batch in batches) renderBatch(ag, batch)
					} finally {
						batchesQueue.clear()
					}
				}
			} else {
				if (batchesQueue.isNotEmpty()) {
					try {
						mem.read(display.address, tempBmp.data)
						ag.renderToBitmapEx(tempBmp) {
							ag.drawBmp(tempBmp)
							for (batches in batchesQueue) for (batch in batches) renderBatch(ag, batch)
							//val depth = FloatArray(512 * 272)
							//readDepth(512, 272, depth)
							//println(depth.toList())
						}
						mem.write(display.address, tempBmp.data)
					} finally {
						batchesQueue.clear()
					}
				}

				if (display.rawDisplay) {
					display.decodeToBitmap32(display.bmp)
					display.bmp.setAlpha(0xFF)
					scene.tex.update(display.bmp)
				}

				ctx.batch.drawQuad(scene.tex, m = m, blendFactors = AG.Blending.NONE, filtering = false)
			}
		}

		private val renderState = AG.RenderState()

		fun TestFunctionEnum.toAg() = when (this) {
			TestFunctionEnum.NEVER -> AG.CompareMode.NEVER
			TestFunctionEnum.ALWAYS -> AG.CompareMode.ALWAYS
			TestFunctionEnum.EQUAL -> AG.CompareMode.EQUAL
			TestFunctionEnum.NOT_EQUAL -> AG.CompareMode.NOT_EQUAL
			TestFunctionEnum.LESS -> AG.CompareMode.LESS
			TestFunctionEnum.LESS_OR_EQUAL -> AG.CompareMode.LESS_EQUAL
			TestFunctionEnum.GREATER -> AG.CompareMode.GREATER
			TestFunctionEnum.GREATER_OR_EQUAL -> AG.CompareMode.GREATER_EQUAL
		}

		private fun renderBatch(ag: AG, batch: GeBatch) {
			//if (batch.vertexCount < 10) return

			if (indexBuffer == null) indexBuffer = ag.createIndexBuffer()
			if (vertexBuffer == null) vertexBuffer = ag.createVertexBuffer()

			val state = batch.state

			indexBuffer!!.upload(batch.indices)
			vertexBuffer!!.upload(batch.vertices)

			//logger.level = PspLogLevel.TRACE
			logger.trace { "----------------" }
			logger.trace { "indices: ${batch.indices.toList()}" }
			logger.trace { "primitive: ${batch.primType.toAg()}" }
			logger.trace { "vertexCount: ${batch.vertexCount}" }
			logger.trace { "vertexType: ${batch.state.vertexType.hex}" }
			logger.trace { "vertices: ${batch.vertices.hex}" }
			logger.trace { "matrix: ${batch.modelViewProjMatrix}" }

			logger.trace {
				val vr = VertexReader()
				"" + vr.read(batch.vtype, batch.vertices.size / batch.vtype.size(), batch.vertices.openSync())
			}

			renderState.depthNear = state.depthTest.rangeNear.toFloat()
			renderState.depthFar = state.depthTest.rangeFar.toFloat()

			if (state.clearing) {
				renderState.depthMask = false
				renderState.depthFunc = AG.CompareMode.ALWAYS
				if (state.clearFlags hasFlag ClearBufferSet.DepthBuffer) {
					renderState.depthMask = true
				}
			} else {
				renderState.depthMask = state.depthTest.mask == 0
				renderState.depthFunc = when {
					state.depthTest.enabled -> state.depthTest.func.toAg()
					else -> AG.CompareMode.ALWAYS
				}
			}

			val pl = getProgramLayout(state)
			ag.draw(
				type = batch.primType.toAg(),
				vertices = vertexBuffer!!,
				indices = indexBuffer!!,
				program = pl.program,
				vertexLayout = pl.layout,
				vertexCount = batch.vertexCount,
				uniforms = mapOf(
					u_modelViewProjMatrix to batch.modelViewProjMatrix
				),
				blending = AG.Blending.NONE,
				renderState = renderState
			)
		}
	}

	val tex by lazy { views.texture(display.bmp) }

	suspend override fun sceneInit(sceneView: Container) {
		val samplesFolder = when {
			OS.isJs -> applicationVfs
		//else -> ResourcesVfs
			else -> applicationVfs["samples"].jail()
		}

		//val exeFile = samplesFolder["minifire.elf"]
		//val exeFile = samplesFolder["HelloWorldPSP.elf"]
		//val exeFile = samplesFolder["rtctest.elf"]
		//val exeFile = samplesFolder["compilerPerf.elf"]
		//val exeFile = samplesFolder["cube.elf"]
		//val exeFile = samplesFolder["ortho.elf"]
		//val exeFile = samplesFolder["mytest.elf"]
		//val exeFile = samplesFolder["counter.elf"]
		//val exeFile = samplesFolder["controller.elf"]
		//val exeFile = samplesFolder["fputest.elf"]
		//val exeFile = samplesFolder["lines.elf"]
		//val exeFile = samplesFolder["lines.pbp"]
		//val exeFile = samplesFolder["polyphonic.elf"]
		val exeFile = samplesFolder["cube.iso"]
		//val exeFile = samplesFolder["lights.pbp"]
		//val exeFile = samplesFolder["cwd.elf"]
		//val exeFile = samplesFolder["nehetutorial03.pbp"]
		//val exeFile = samplesFolder["polyphonic.elf"]
		//val exeFile = samplesFolder["text.elf"]
		//val exeFile = samplesFolder["cavestory.iso"]
		//val exeFile = samplesFolder["cavestory.zip"]
		//val exeFile = samplesFolder["TrigWars.iso"]
		//val exeFile = samplesFolder["TrigWars.zip"]

		val renderView = KorgeRenderer(this)

		emulator = Emulator(coroutineContext, mem = Memory(), gpuRenderer = renderView).apply {
			registerNativeModules()
			loadExecutableAndStart(exeFile)
			//threadManager.trace("_start")
			//threadManager.trace("user_main")
		}

		var running = true
		var ended = false

		sceneView.addUpdatable {
			//controller.updateButton(PspCtrlButtons.cross, true) // auto press X

			if (running && emulator.running) {
				try {
					emulator.frameStep()
				} catch (e: Throwable) {
					e.printStackTrace()
					running = false
				}
			} else {
				if (!ended) {
					ended = true
					println("COMPLETED")
				}
			}
		}

		val keys = BooleanArray(256)

		fun updateKey(keyCode: Int, pressed: Boolean) {
			//println("updateKey: $keyCode, $pressed")
			keys[keyCode and 0xFF] = pressed
			when (keyCode) {
				10 -> controller.updateButton(PspCtrlButtons.start, pressed) // return
				32 -> controller.updateButton(PspCtrlButtons.select, pressed) // space
				87 -> controller.updateButton(PspCtrlButtons.triangle, pressed) // W
				65 -> controller.updateButton(PspCtrlButtons.square, pressed) // A
				83 -> controller.updateButton(PspCtrlButtons.cross, pressed) // S
				68 -> controller.updateButton(PspCtrlButtons.circle, pressed) // D
				81 -> controller.updateButton(PspCtrlButtons.leftTrigger, pressed) // Q
				69 -> controller.updateButton(PspCtrlButtons.rightTrigger, pressed) // E
				37 -> controller.updateButton(PspCtrlButtons.left, pressed) // LEFT
				38 -> controller.updateButton(PspCtrlButtons.up, pressed) // UP
				39 -> controller.updateButton(PspCtrlButtons.right, pressed) // RIGHT
				40 -> controller.updateButton(PspCtrlButtons.down, pressed) // DOWN
				in 73..76 -> Unit // IJKL (analog)
				else -> println("Unhandled($pressed): $keyCode")
			}

			controller.updateAnalog(
				x = when { keys[74] -> -1f; keys[76] -> +1f; else -> 0f; },
				y = when { keys[73] -> +1f; keys[75] -> -1f; else -> 0f; }
			)
		}

		sceneView.onKeyDown { updateKey(it.keyCode, true) }
		sceneView.onKeyUp { updateKey(it.keyCode, false) }

		sceneView += renderView
	}
}

private fun PrimitiveType.toAg(): AG.DrawType = when (this) {
	PrimitiveType.POINTS -> AG.DrawType.POINTS
	PrimitiveType.LINES -> AG.DrawType.LINES
	PrimitiveType.LINE_STRIP -> AG.DrawType.LINE_STRIP
	PrimitiveType.TRIANGLES -> AG.DrawType.TRIANGLES
	PrimitiveType.TRIANGLE_STRIP -> AG.DrawType.TRIANGLE_STRIP
	PrimitiveType.TRIANGLE_FAN -> AG.DrawType.TRIANGLE_FAN
	PrimitiveType.SPRITES -> {
		//invalidOp("Can't handle sprite primitives")
		AG.DrawType.TRIANGLES
	}
}

suspend fun Emulator.loadExecutableAndStart(file: VfsFile): PspElf {
	when (file.extensionLC) {
		"elf", "prx", "bin" -> return loadElfAndSetRegisters(file.readAll().openSync())
		"pbp" -> return loadExecutableAndStart(Pbp.load(file.open())[Pbp.PSP_DATA]!!.asVfsFile("executable.elf"))
		"iso", "zip" -> {
			val iso = when (file.extensionLC) {
				"iso" -> IsoVfs(file)
				"zip" -> ZipVfs2(file.open(), file)
				else -> invalidOp("UNEXPECTED")
			}
			val paramSfo = iso["PSP_GAME/PARAM.SFO"]

			val files = listOf(
				iso["PSP_GAME/SYSDIR/BOOT.BIN"],
				iso["EBOOT.ELF"],
				iso["EBOOT.PBP"]
			)

			for (f in files) {
				if (f.exists()) {
					if (f.parent.path.isEmpty()) {
						fileManager.currentDirectory = "umd0:/"
						deviceManager.mount(fileManager.currentDirectory, iso)
						deviceManager.mount("game0:/", iso)
						deviceManager.mount("umd0:/", iso)
					}
					return loadExecutableAndStart(f)
				}
			}
			invalidOp("Can't find any possible executalbe in ISO ($files)")
		}
		else -> {
			invalidOp("Don't know how to load executable file $file")
		}
	}
}
