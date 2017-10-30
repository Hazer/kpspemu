package com.soywiz.kpspemu.cpu.interpreter

import com.soywiz.korio.lang.format
import com.soywiz.korio.util.IntEx
import com.soywiz.korio.util.udiv
import com.soywiz.korio.util.urem
import com.soywiz.kpspemu.cpu.*
import com.soywiz.kpspemu.cpu.dis.disasmMacro
import com.soywiz.kpspemu.util.imul32_64
import com.soywiz.kpspemu.util.umul32_64

class CpuInterpreter(var cpu: CpuState, var trace: Boolean = false) {
	val dispatcher = InstructionDispatcher(InstructionInterpreter)

	fun step() {
		if (cpu._PC == 0) throw IllegalStateException("Trying to execute PC=0")
		if (trace) println("%08X: %s".format(cpu._PC, cpu.mem.disasmMacro(cpu._PC)))
		dispatcher.dispatch(cpu)
	}

	fun steps(count: Int) {
		for (n in 0 until count) step()
	}
}

// http://www.mrc.uidaho.edu/mrc/people/jff/digital/MIPSir.html
object InstructionInterpreter : InstructionEvaluator<CpuState>() {
	override fun unimplemented(s: CpuState, i: InstructionType): Unit = TODO("unimplemented: ${i.name} : " + i + " at ${"%08X".format(s._PC)}")

	val itemp = IntArray(2)

	// ALU
	override fun lui(s: CpuState) = s { RT = (U_IMM16 shl 16) }

	override fun movz(s: CpuState) = s { if (RT == 0) RD = RS }
	override fun movn(s: CpuState) = s { if (RT != 0) RD = RS }

	override fun add(s: CpuState) = s { RD = RS + RT }
	override fun addu(s: CpuState) = s { RD = RS + RT }
	override fun subu(s: CpuState) = s { RD = RS - RT }
	override fun addiu(s: CpuState) = s { RT = RS + S_IMM16 }
	override fun divu(s: CpuState) = s { LO = RS udiv RT; HI = RS urem RT }
	override fun mult(s: CpuState) = s {
		imul32_64(RS, RT, itemp)
		this.LO = itemp[0]
		this.HI = itemp[1]
	}

	override fun multu(s: CpuState) = s {
		umul32_64(RS, RT, itemp)
		this.LO = itemp[0]
		this.HI = itemp[1]
	}

	override fun mflo(s: CpuState) = s { RD = LO }
	override fun mfhi(s: CpuState) = s { RD = HI }
	override fun mfic(s: CpuState) = s { RT = IC }

	override fun mtlo(s: CpuState) = s { LO = RS }
	override fun mthi(s: CpuState) = s { HI = RS }
	override fun mtic(s: CpuState) = s { IC = RT }

	// ALU: Bit
	override fun or(s: CpuState) = s { RD = RS or RT }

	override fun xor(s: CpuState) = s { RD = RS xor RT }
	override fun and(s: CpuState) = s { RD = RS and RT }
	override fun nor(s: CpuState) = s { RD = (RS or RT).inv() }

	override fun ori(s: CpuState) = s { RT = RS or U_IMM16 }
	override fun xori(s: CpuState) = s { RT = RS xor U_IMM16 }
	override fun andi(s: CpuState) = s { RT = RS and U_IMM16 }

	override fun sll(s: CpuState) = s { RD = RT shl POS }
	override fun sra(s: CpuState) = s { RD = RT shr POS }
	override fun srl(s: CpuState) = s { RD = RT ushr POS }

	override fun sllv(s: CpuState) = s { RD = RT shl (RS and 0b11111) }
	override fun srav(s: CpuState) = s { RD = RT shr (RS and 0b11111) }
	override fun srlv(s: CpuState) = s { RD = RT ushr (RS and 0b11111) }

	// Memory
	override fun lb(s: CpuState) = s { RT = mem.lb(RS_IMM16) }

	override fun lbu(s: CpuState) = s { RT = mem.lbu(RS_IMM16) }
	override fun lh(s: CpuState) = s { RT = mem.lh(RS_IMM16) }
	override fun lhu(s: CpuState) = s { RT = mem.lhu(RS_IMM16) }
	override fun lw(s: CpuState) = s { RT = mem.lw(RS_IMM16) }

	override fun sb(s: CpuState) = s { mem.sb(RS_IMM16, RT) }
	override fun sh(s: CpuState) = s { mem.sh(RS_IMM16, RT) }
	override fun sw(s: CpuState) = s { mem.sw(RS_IMM16, RT) }

	// Special
	override fun syscall(s: CpuState) = s { syscall(SYSCALL) }

	override fun _break(s: CpuState) = s { throw CpuBreak(SYSCALL) }

	// Set less
	override fun slt(s: CpuState) = s { RD = if (IntEx.compare(RS, RT) < 0) 1 else 0 }

	override fun sltu(s: CpuState) = s { RD = if (IntEx.compareUnsigned(RS, RT) < 0) 1 else 0 }

	override fun slti(s: CpuState) = s { RT = if (IntEx.compare(RS, S_IMM16) < 0) 1 else 0 }
	override fun sltiu(s: CpuState) = s { RT = if (IntEx.compareUnsigned(RS, S_IMM16) < 0) 1 else 0 }


	// Branch
	override fun beq(s: CpuState) = s.branch { RS == RT }

	override fun bne(s: CpuState) = s.branch { RS != RT }
	override fun bltz(s: CpuState) = s.branch { RS < 0 }
	override fun blez(s: CpuState) = s.branch { RS <= 0 }
	override fun bgtz(s: CpuState) = s.branch { RS > 0 }
	override fun bgez(s: CpuState) = s.branch { RS >= 0 }

	override fun beql(s: CpuState) = s.branchLikely { RS == RT }
	override fun bnel(s: CpuState) = s.branchLikely { RS != RT }
	override fun bltzl(s: CpuState) = s.branchLikely { RS < 0 }
	override fun blezl(s: CpuState) = s.branchLikely { RS <= 0 }
	override fun bgtzl(s: CpuState) = s.branchLikely { RS > 0 }
	override fun bgezl(s: CpuState) = s.branchLikely { RS >= 0 }

	override fun j(s: CpuState) = s.none { _PC = _nPC; _nPC = (_PC and 0xf0000000.toInt()) or (JUMP_ADDRESS) }
	// $31 = PC + 8 (or nPC + 4); PC = nPC; nPC = (PC & 0xf0000000) | (target << 2);
	override fun jal(s: CpuState) = s.none { RA = _nPC + 4; _PC = _nPC; _nPC = (_PC and 0xf0000000.toInt()) or (JUMP_ADDRESS) }

	override fun jr(s: CpuState) = s.none { _PC = _nPC; _nPC = RS }
}

