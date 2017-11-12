package com.soywiz.kpspemu.ge

import com.soywiz.kmem.FastMemory
import com.soywiz.kmem.get
import com.soywiz.kmem.set
import com.soywiz.kpspemu.mem
import kotlin.math.max

class GeBatchBuilder(val ge: Ge) {
	val state = ge.state
	val mem = ge.mem
	var primBatchPrimitiveType: Int = -1
	var primitiveType: PrimitiveType? = null
	var vertexType: VertexType = VertexType(-1)
	var vertexCount: Int = 0
	var vertexSize: Int = 0

	val vertexBuffer = ByteArray(0x10000 * 16)
	val vertexBufferMem = FastMemory.wrap(vertexBuffer)
	val vertexBufferI8 = vertexBufferMem.i8
	val vertexBufferI16 = vertexBufferMem.i16
	val vertexBufferI32 = vertexBufferMem.i32
	var vertexBufferPos = 0
	val indexBuffer = ShortArray(0x10000)
	var indexBufferPos = 0

	fun reset() {
		//println("reset")
		flush()
		primBatchPrimitiveType = -1
		primitiveType = null
		vertexType.v = -1
		vertexCount = 0
		vertexBufferPos = 0
		indexBufferPos = 0
		vertexSize = 0
	}

	fun setVertexKind(primitiveType: PrimitiveType, state: GeState) {
		if (this.primitiveType != primitiveType || this.vertexType.v != state.vertexType) flush()
		vertexType.init(state)
		this.primitiveType = primitiveType
		this.vertexSize = vertexType.size
	}

	var texVersion = 0

	fun tflush() {
		texVersion++
		//println("tflush")
	}

	fun tsync() {
		//println("tsync")
	}

	fun flush() {
		//println("flush: $indexBufferPos")
		if (indexBufferPos > 0) {
			ge.emitBatch(GeBatchData(ge.state.data.copyOf(), primitiveType ?: PrimitiveType.TRIANGLES, indexBufferPos, vertexBuffer.copyOf(vertexBufferPos), indexBuffer.copyOf(indexBufferPos), texVersion))
			vertexCount = 0
			vertexBufferPos = 0
			indexBufferPos = 0
		}
	}

	fun putVertex(address: Int) {
		//println("putVertex: ${address.hexx}, $vertexSize")
		mem.read(address, vertexBuffer, vertexBufferPos, vertexSize)
		vertexBufferPos += vertexSize
		vertexCount++
	}

	fun putIndex(index: Int) {
		indexBuffer[indexBufferPos++] = index.toShort()
	}

	fun addIndices(count: Int) {
		when (primitiveType) {
			PrimitiveType.SPRITES -> this.addIndicesSprites(count)
			else -> this.addIndicesNormal(count)
		}
	}

	fun addIndicesSprites(count: Int) {
		val nsprites = count / 2

		when (vertexType.index) {
			IndexEnum.VOID -> {
				// 0..3
				// 2..1
				var bp = this.indexBufferPos
				val start = vertexCount
				val end = start + nsprites * 2
				for (n in 0 until nsprites) {
					val start2 = start + n * 2
					val end2 = end + n * 2
					indexBuffer[bp++] = (start2 + 0).toShort()
					indexBuffer[bp++] = (end2 + 1).toShort()
					indexBuffer[bp++] = (end2 + 0).toShort()
					indexBuffer[bp++] = (end2 + 0).toShort()
					indexBuffer[bp++] = (end2 + 1).toShort()
					indexBuffer[bp++] = (start2 + 1).toShort()
				}
				this.indexBufferPos = bp
			}
			else -> TODO("addIndicesSprites: ${vertexType.index}, $count")
		}

		// Vertices
		val vertexSize_0 = vertexSize * 0
		val vertexSize_1 = vertexSize * 1
		val vertexSize_2 = vertexSize * 2

		val posSize = vertexType.pos.nbytes
		val posOffsetX = vertexType.posOffset
		val posOffsetY = vertexType.posOffset + posSize
		val texSize = vertexType.tex.nbytes
		val texOffsetX = vertexType.texOffset
		val texOffsetY = vertexType.texOffset + texSize
		val colSize = vertexType.colSize
		val colOffset = vertexType.colOffset
		val svpos = vertexBufferPos
		val dvpos = vertexBufferPos + nsprites * vertexSize_2

		// Copy raw sprite vertices at once
		mem.read(state.vertexAddress, vertexBuffer, svpos, nsprites * vertexSize_2)
		mem.read(state.vertexAddress, vertexBuffer, dvpos, nsprites * vertexSize_2)

		state.vertexAddress += nsprites * vertexSize_2

		for (n in 0 until nsprites) {
			val TLpos = svpos + (n * vertexSize_2)
			val BRpos = TLpos + vertexSize_1

			val ssvpos = svpos + (n * vertexSize_2)
			val dsvpos = dvpos + (n * vertexSize_2)

			putGenVertexColor(ssvpos + vertexSize_0, BRpos, TLpos, TLpos, posSize, posOffsetX, posOffsetY, texSize, texOffsetX, texOffsetY, colOffset, colSize)
			putGenVertexColor(ssvpos + vertexSize_1, BRpos, BRpos, BRpos, posSize, posOffsetX, posOffsetY, texSize, texOffsetX, texOffsetY, colOffset, colSize)
			putGenVertex(dsvpos + vertexSize_0, BRpos, TLpos, BRpos, posSize, posOffsetX, posOffsetY, texSize, texOffsetX, texOffsetY, colOffset, colSize)
			putGenVertex(dsvpos + vertexSize_1, BRpos, BRpos, TLpos, posSize, posOffsetX, posOffsetY, texSize, texOffsetX, texOffsetY, colOffset, colSize)
		}
		vertexCount += nsprites * 4
		this.vertexBufferPos = vertexBufferPos + nsprites * vertexSize * 4

	}

	private fun putGenVertex(dest: Int, base: Int, gx: Int, gy: Int, posSize: Int, posOffsetX: Int, posOffsetY: Int, texSize: Int, texOffsetX: Int, texOffsetY: Int, colOffset: Int, colSize: Int) {
		//arraycopy(vertexBuffer, base, vertexBuffer, dest, vertexSize) // Copy one full

		if (vertexType.hasColor) {
			smallVBCopy(base + colOffset, dest + colOffset, colSize)
		}

		if (vertexType.hasPosition) {
			smallVBCopy(gx + posOffsetX, dest + posOffsetX, posSize)
			smallVBCopy(gy + posOffsetY, dest + posOffsetY, posSize)
		}

		if (vertexType.hasTexture) {
			smallVBCopy(gx + texOffsetX, dest + texOffsetX, texSize)
			smallVBCopy(gy + texOffsetY, dest + texOffsetY, texSize)
		}
	}

	private fun putGenVertexColor(dest: Int, base: Int, gx: Int, gy: Int, posSize: Int, posOffsetX: Int, posOffsetY: Int, texSize: Int, texOffsetX: Int, texOffsetY: Int, colOffset: Int, colSize: Int) {
		if (vertexType.hasColor) {
			smallVBCopy(base + colOffset, dest + colOffset, colSize)
		}
	}

	private fun smallVBCopy(src: Int, dst: Int, size: Int) {
		when (size) {
			1 -> vertexBufferI8[dst] = vertexBufferI8[src]
			2 -> vertexBufferI16[dst ushr 1] = vertexBufferI16[src ushr 1]
			4 -> vertexBufferI32[dst ushr 2] = vertexBufferI32[src ushr 2]
		}
	}

	fun addIndicesNormal(count: Int) {
		var maxIdx = 0

		//println("addIndices: size=$size, count=$count")
		when (vertexType.index) {
			IndexEnum.VOID -> {
				val vertexCount = vertexCount
				for (n in 0 until count) indexBuffer[indexBufferPos + n] = (vertexCount + n).toShort()
				indexBufferPos += count
				maxIdx = count
			}
			IndexEnum.SHORT -> {
				val iaddr = state.indexAddress
				for (n in 0 until count) {
					val idx = mem.lhu(iaddr + n * 2)
					maxIdx = max(maxIdx, idx + 1)
					putIndex(idx)
				}
				//println("maxIdx: $maxIdx")
				//state.indexAddress += count * 2
			}
			else -> TODO("addIndices: ${vertexType.index}, $count")
		}

		// Vertices
		mem.read(state.vertexAddress, vertexBuffer, vertexBufferPos, vertexSize * maxIdx)
		vertexBufferPos += vertexSize * maxIdx
		state.vertexAddress += vertexSize * maxIdx
		vertexCount += maxIdx

	}
}
