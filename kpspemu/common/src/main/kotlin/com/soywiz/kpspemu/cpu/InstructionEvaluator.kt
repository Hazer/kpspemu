package com.soywiz.kpspemu.cpu

open class InstructionEvaluator<T> : InstructionDecoder() {
	open fun unimplemented(s: T, i: InstructionType): Unit = TODO("unimplemented: ${i.name} : " + i)

	open fun add(s: T): Unit = unimplemented(s, Instructions.add)
	open fun addu(s: T): Unit = unimplemented(s, Instructions.addu)
	open fun addi(s: T): Unit = unimplemented(s, Instructions.addi)
	open fun addiu(s: T): Unit = unimplemented(s, Instructions.addiu)
	open fun sub(s: T): Unit = unimplemented(s, Instructions.sub)
	open fun subu(s: T): Unit = unimplemented(s, Instructions.subu)
	open fun and(s: T): Unit = unimplemented(s, Instructions.and)
	open fun andi(s: T): Unit = unimplemented(s, Instructions.andi)
	open fun nor(s: T): Unit = unimplemented(s, Instructions.nor)
	open fun or(s: T): Unit = unimplemented(s, Instructions.or)
	open fun ori(s: T): Unit = unimplemented(s, Instructions.ori)
	open fun xor(s: T): Unit = unimplemented(s, Instructions.xor)
	open fun xori(s: T): Unit = unimplemented(s, Instructions.xori)
	open fun sll(s: T): Unit = unimplemented(s, Instructions.sll)
	open fun sllv(s: T): Unit = unimplemented(s, Instructions.sllv)
	open fun sra(s: T): Unit = unimplemented(s, Instructions.sra)
	open fun srav(s: T): Unit = unimplemented(s, Instructions.srav)
	open fun srl(s: T): Unit = unimplemented(s, Instructions.srl)
	open fun srlv(s: T): Unit = unimplemented(s, Instructions.srlv)
	open fun rotr(s: T): Unit = unimplemented(s, Instructions.rotr)
	open fun rotrv(s: T): Unit = unimplemented(s, Instructions.rotrv)
	open fun slt(s: T): Unit = unimplemented(s, Instructions.slt)
	open fun slti(s: T): Unit = unimplemented(s, Instructions.slti)
	open fun sltu(s: T): Unit = unimplemented(s, Instructions.sltu)
	open fun sltiu(s: T): Unit = unimplemented(s, Instructions.sltiu)
	open fun lui(s: T): Unit = unimplemented(s, Instructions.lui)
	open fun seb(s: T): Unit = unimplemented(s, Instructions.seb)
	open fun seh(s: T): Unit = unimplemented(s, Instructions.seh)
	open fun bitrev(s: T): Unit = unimplemented(s, Instructions.bitrev)
	open fun max(s: T): Unit = unimplemented(s, Instructions.max)
	open fun min(s: T): Unit = unimplemented(s, Instructions.min)
	open fun div(s: T): Unit = unimplemented(s, Instructions.div)
	open fun divu(s: T): Unit = unimplemented(s, Instructions.divu)
	open fun mult(s: T): Unit = unimplemented(s, Instructions.mult)
	open fun multu(s: T): Unit = unimplemented(s, Instructions.multu)
	open fun madd(s: T): Unit = unimplemented(s, Instructions.madd)
	open fun maddu(s: T): Unit = unimplemented(s, Instructions.maddu)
	open fun msub(s: T): Unit = unimplemented(s, Instructions.msub)
	open fun msubu(s: T): Unit = unimplemented(s, Instructions.msubu)
	open fun mfhi(s: T): Unit = unimplemented(s, Instructions.mfhi)
	open fun mflo(s: T): Unit = unimplemented(s, Instructions.mflo)
	open fun mthi(s: T): Unit = unimplemented(s, Instructions.mthi)
	open fun mtlo(s: T): Unit = unimplemented(s, Instructions.mtlo)
	open fun movz(s: T): Unit = unimplemented(s, Instructions.movz)
	open fun movn(s: T): Unit = unimplemented(s, Instructions.movn)
	open fun ext(s: T): Unit = unimplemented(s, Instructions.ext)
	open fun ins(s: T): Unit = unimplemented(s, Instructions.ins)
	open fun clz(s: T): Unit = unimplemented(s, Instructions.clz)
	open fun clo(s: T): Unit = unimplemented(s, Instructions.clo)
	open fun wsbh(s: T): Unit = unimplemented(s, Instructions.wsbh)
	open fun wsbw(s: T): Unit = unimplemented(s, Instructions.wsbw)
	open fun beq(s: T): Unit = unimplemented(s, Instructions.beq)
	open fun beql(s: T): Unit = unimplemented(s, Instructions.beql)
	open fun bgez(s: T): Unit = unimplemented(s, Instructions.bgez)
	open fun bgezl(s: T): Unit = unimplemented(s, Instructions.bgezl)
	open fun bgezal(s: T): Unit = unimplemented(s, Instructions.bgezal)
	open fun bgezall(s: T): Unit = unimplemented(s, Instructions.bgezall)
	open fun bltz(s: T): Unit = unimplemented(s, Instructions.bltz)
	open fun bltzl(s: T): Unit = unimplemented(s, Instructions.bltzl)
	open fun bltzal(s: T): Unit = unimplemented(s, Instructions.bltzal)
	open fun bltzall(s: T): Unit = unimplemented(s, Instructions.bltzall)
	open fun blez(s: T): Unit = unimplemented(s, Instructions.blez)
	open fun blezl(s: T): Unit = unimplemented(s, Instructions.blezl)
	open fun bgtz(s: T): Unit = unimplemented(s, Instructions.bgtz)
	open fun bgtzl(s: T): Unit = unimplemented(s, Instructions.bgtzl)
	open fun bne(s: T): Unit = unimplemented(s, Instructions.bne)
	open fun bnel(s: T): Unit = unimplemented(s, Instructions.bnel)
	open fun j(s: T): Unit = unimplemented(s, Instructions.j)
	open fun jr(s: T): Unit = unimplemented(s, Instructions.jr)
	open fun jalr(s: T): Unit = unimplemented(s, Instructions.jalr)
	open fun jal(s: T): Unit = unimplemented(s, Instructions.jal)
	open fun bc1f(s: T): Unit = unimplemented(s, Instructions.bc1f)
	open fun bc1t(s: T): Unit = unimplemented(s, Instructions.bc1t)
	open fun bc1fl(s: T): Unit = unimplemented(s, Instructions.bc1fl)
	open fun bc1tl(s: T): Unit = unimplemented(s, Instructions.bc1tl)
	open fun lb(s: T): Unit = unimplemented(s, Instructions.lb)
	open fun lh(s: T): Unit = unimplemented(s, Instructions.lh)
	open fun lw(s: T): Unit = unimplemented(s, Instructions.lw)
	open fun lwl(s: T): Unit = unimplemented(s, Instructions.lwl)
	open fun lwr(s: T): Unit = unimplemented(s, Instructions.lwr)
	open fun lbu(s: T): Unit = unimplemented(s, Instructions.lbu)
	open fun lhu(s: T): Unit = unimplemented(s, Instructions.lhu)
	open fun sb(s: T): Unit = unimplemented(s, Instructions.sb)
	open fun sh(s: T): Unit = unimplemented(s, Instructions.sh)
	open fun sw(s: T): Unit = unimplemented(s, Instructions.sw)
	open fun swl(s: T): Unit = unimplemented(s, Instructions.swl)
	open fun swr(s: T): Unit = unimplemented(s, Instructions.swr)
	open fun ll(s: T): Unit = unimplemented(s, Instructions.ll)
	open fun sc(s: T): Unit = unimplemented(s, Instructions.sc)
	open fun lwc1(s: T): Unit = unimplemented(s, Instructions.lwc1)
	open fun swc1(s: T): Unit = unimplemented(s, Instructions.swc1)
	open fun add_s(s: T): Unit = unimplemented(s, Instructions.add_s)
	open fun sub_s(s: T): Unit = unimplemented(s, Instructions.sub_s)
	open fun mul_s(s: T): Unit = unimplemented(s, Instructions.mul_s)
	open fun div_s(s: T): Unit = unimplemented(s, Instructions.div_s)
	open fun sqrt_s(s: T): Unit = unimplemented(s, Instructions.sqrt_s)
	open fun abs_s(s: T): Unit = unimplemented(s, Instructions.abs_s)
	open fun mov_s(s: T): Unit = unimplemented(s, Instructions.mov_s)
	open fun neg_s(s: T): Unit = unimplemented(s, Instructions.neg_s)
	open fun round_w_s(s: T): Unit = unimplemented(s, Instructions.round_w_s)
	open fun trunc_w_s(s: T): Unit = unimplemented(s, Instructions.trunc_w_s)
	open fun ceil_w_s(s: T): Unit = unimplemented(s, Instructions.ceil_w_s)
	open fun floor_w_s(s: T): Unit = unimplemented(s, Instructions.floor_w_s)
	open fun cvt_s_w(s: T): Unit = unimplemented(s, Instructions.cvt_s_w)
	open fun cvt_w_s(s: T): Unit = unimplemented(s, Instructions.cvt_w_s)
	open fun mfc1(s: T): Unit = unimplemented(s, Instructions.mfc1)
	open fun mtc1(s: T): Unit = unimplemented(s, Instructions.mtc1)
	open fun cfc1(s: T): Unit = unimplemented(s, Instructions.cfc1)
	open fun ctc1(s: T): Unit = unimplemented(s, Instructions.ctc1)
	open fun c_f_s(s: T): Unit = unimplemented(s, Instructions.c_f_s)
	open fun c_un_s(s: T): Unit = unimplemented(s, Instructions.c_un_s)
	open fun c_eq_s(s: T): Unit = unimplemented(s, Instructions.c_eq_s)
	open fun c_ueq_s(s: T): Unit = unimplemented(s, Instructions.c_ueq_s)
	open fun c_olt_s(s: T): Unit = unimplemented(s, Instructions.c_olt_s)
	open fun c_ult_s(s: T): Unit = unimplemented(s, Instructions.c_ult_s)
	open fun c_ole_s(s: T): Unit = unimplemented(s, Instructions.c_ole_s)
	open fun c_ule_s(s: T): Unit = unimplemented(s, Instructions.c_ule_s)
	open fun c_sf_s(s: T): Unit = unimplemented(s, Instructions.c_sf_s)
	open fun c_ngle_s(s: T): Unit = unimplemented(s, Instructions.c_ngle_s)
	open fun c_seq_s(s: T): Unit = unimplemented(s, Instructions.c_seq_s)
	open fun c_ngl_s(s: T): Unit = unimplemented(s, Instructions.c_ngl_s)
	open fun c_lt_s(s: T): Unit = unimplemented(s, Instructions.c_lt_s)
	open fun c_nge_s(s: T): Unit = unimplemented(s, Instructions.c_nge_s)
	open fun c_le_s(s: T): Unit = unimplemented(s, Instructions.c_le_s)
	open fun c_ngt_s(s: T): Unit = unimplemented(s, Instructions.c_ngt_s)
	open fun syscall(s: T): Unit = unimplemented(s, Instructions.syscall)
	open fun cache(s: T): Unit = unimplemented(s, Instructions.cache)
	open fun sync(s: T): Unit = unimplemented(s, Instructions.sync)
	open fun _break(s: T): Unit = unimplemented(s, Instructions._break)
	open fun dbreak(s: T): Unit = unimplemented(s, Instructions.dbreak)
	open fun halt(s: T): Unit = unimplemented(s, Instructions.halt)
	open fun dret(s: T): Unit = unimplemented(s, Instructions.dret)
	open fun eret(s: T): Unit = unimplemented(s, Instructions.eret)
	open fun mfic(s: T): Unit = unimplemented(s, Instructions.mfic)
	open fun mtic(s: T): Unit = unimplemented(s, Instructions.mtic)
	open fun mfdr(s: T): Unit = unimplemented(s, Instructions.mfdr)
	open fun mtdr(s: T): Unit = unimplemented(s, Instructions.mtdr)
	open fun cfc0(s: T): Unit = unimplemented(s, Instructions.cfc0)
	open fun ctc0(s: T): Unit = unimplemented(s, Instructions.ctc0)
	open fun mfc0(s: T): Unit = unimplemented(s, Instructions.mfc0)
	open fun mtc0(s: T): Unit = unimplemented(s, Instructions.mtc0)
	open fun mfv(s: T): Unit = unimplemented(s, Instructions.mfv)
	open fun mfvc(s: T): Unit = unimplemented(s, Instructions.mfvc)
	open fun mtv(s: T): Unit = unimplemented(s, Instructions.mtv)
	open fun mtvc(s: T): Unit = unimplemented(s, Instructions.mtvc)
	open fun lv_s(s: T): Unit = unimplemented(s, Instructions.lv_s)
	open fun lv_q(s: T): Unit = unimplemented(s, Instructions.lv_q)
	open fun lvl_q(s: T): Unit = unimplemented(s, Instructions.lvl_q)
	open fun lvr_q(s: T): Unit = unimplemented(s, Instructions.lvr_q)
	open fun sv_q(s: T): Unit = unimplemented(s, Instructions.sv_q)
	open fun vdot(s: T): Unit = unimplemented(s, Instructions.vdot)
	open fun vscl(s: T): Unit = unimplemented(s, Instructions.vscl)
	open fun vsge(s: T): Unit = unimplemented(s, Instructions.vsge)
	open fun vslt(s: T): Unit = unimplemented(s, Instructions.vslt)
	open fun vrot(s: T): Unit = unimplemented(s, Instructions.vrot)
	open fun vzero(s: T): Unit = unimplemented(s, Instructions.vzero)
	open fun vone(s: T): Unit = unimplemented(s, Instructions.vone)
	open fun vmov(s: T): Unit = unimplemented(s, Instructions.vmov)
	open fun vabs(s: T): Unit = unimplemented(s, Instructions.vabs)
	open fun vneg(s: T): Unit = unimplemented(s, Instructions.vneg)
	open fun vocp(s: T): Unit = unimplemented(s, Instructions.vocp)
	open fun vsgn(s: T): Unit = unimplemented(s, Instructions.vsgn)
	open fun vrcp(s: T): Unit = unimplemented(s, Instructions.vrcp)
	open fun vrsq(s: T): Unit = unimplemented(s, Instructions.vrsq)
	open fun vsin(s: T): Unit = unimplemented(s, Instructions.vsin)
	open fun vcos(s: T): Unit = unimplemented(s, Instructions.vcos)
	open fun vexp2(s: T): Unit = unimplemented(s, Instructions.vexp2)
	open fun vlog2(s: T): Unit = unimplemented(s, Instructions.vlog2)
	open fun vsqrt(s: T): Unit = unimplemented(s, Instructions.vsqrt)
	open fun vasin(s: T): Unit = unimplemented(s, Instructions.vasin)
	open fun vnrcp(s: T): Unit = unimplemented(s, Instructions.vnrcp)
	open fun vnsin(s: T): Unit = unimplemented(s, Instructions.vnsin)
	open fun vrexp2(s: T): Unit = unimplemented(s, Instructions.vrexp2)
	open fun vsat0(s: T): Unit = unimplemented(s, Instructions.vsat0)
	open fun vsat1(s: T): Unit = unimplemented(s, Instructions.vsat1)
	open fun vcst(s: T): Unit = unimplemented(s, Instructions.vcst)
	open fun vmmul(s: T): Unit = unimplemented(s, Instructions.vmmul)
	open fun vhdp(s: T): Unit = unimplemented(s, Instructions.vhdp)
	open fun vcrs_t(s: T): Unit = unimplemented(s, Instructions.vcrs_t)
	open fun vcrsp_t(s: T): Unit = unimplemented(s, Instructions.vcrsp_t)
	open fun vi2c(s: T): Unit = unimplemented(s, Instructions.vi2c)
	open fun vi2uc(s: T): Unit = unimplemented(s, Instructions.vi2uc)
	open fun vtfm2(s: T): Unit = unimplemented(s, Instructions.vtfm2)
	open fun vtfm3(s: T): Unit = unimplemented(s, Instructions.vtfm3)
	open fun vtfm4(s: T): Unit = unimplemented(s, Instructions.vtfm4)
	open fun vhtfm2(s: T): Unit = unimplemented(s, Instructions.vhtfm2)
	open fun vhtfm3(s: T): Unit = unimplemented(s, Instructions.vhtfm3)
	open fun vhtfm4(s: T): Unit = unimplemented(s, Instructions.vhtfm4)
	open fun vsrt3(s: T): Unit = unimplemented(s, Instructions.vsrt3)
	open fun vfad(s: T): Unit = unimplemented(s, Instructions.vfad)
	open fun vmin(s: T): Unit = unimplemented(s, Instructions.vmin)
	open fun vmax(s: T): Unit = unimplemented(s, Instructions.vmax)
	open fun vadd(s: T): Unit = unimplemented(s, Instructions.vadd)
	open fun vsub(s: T): Unit = unimplemented(s, Instructions.vsub)
	open fun vdiv(s: T): Unit = unimplemented(s, Instructions.vdiv)
	open fun vmul(s: T): Unit = unimplemented(s, Instructions.vmul)
	open fun vidt(s: T): Unit = unimplemented(s, Instructions.vidt)
	open fun vmidt(s: T): Unit = unimplemented(s, Instructions.vmidt)
	open fun viim(s: T): Unit = unimplemented(s, Instructions.viim)
	open fun vmmov(s: T): Unit = unimplemented(s, Instructions.vmmov)
	open fun vmzero(s: T): Unit = unimplemented(s, Instructions.vmzero)
	open fun vmone(s: T): Unit = unimplemented(s, Instructions.vmone)
	open fun vnop(s: T): Unit = unimplemented(s, Instructions.vnop)
	open fun vsync(s: T): Unit = unimplemented(s, Instructions.vsync)
	open fun vflush(s: T): Unit = unimplemented(s, Instructions.vflush)
	open fun vpfxd(s: T): Unit = unimplemented(s, Instructions.vpfxd)
	open fun vpfxs(s: T): Unit = unimplemented(s, Instructions.vpfxs)
	open fun vpfxt(s: T): Unit = unimplemented(s, Instructions.vpfxt)
	open fun vdet(s: T): Unit = unimplemented(s, Instructions.vdet)
	open fun vrnds(s: T): Unit = unimplemented(s, Instructions.vrnds)
	open fun vrndi(s: T): Unit = unimplemented(s, Instructions.vrndi)
	open fun vrndf1(s: T): Unit = unimplemented(s, Instructions.vrndf1)
	open fun vrndf2(s: T): Unit = unimplemented(s, Instructions.vrndf2)
	open fun vcmp(s: T): Unit = unimplemented(s, Instructions.vcmp)
	open fun vcmovf(s: T): Unit = unimplemented(s, Instructions.vcmovf)
	open fun vcmovt(s: T): Unit = unimplemented(s, Instructions.vcmovt)
	open fun vavg(s: T): Unit = unimplemented(s, Instructions.vavg)
	open fun vf2id(s: T): Unit = unimplemented(s, Instructions.vf2id)
	open fun vf2in(s: T): Unit = unimplemented(s, Instructions.vf2in)
	open fun vf2iu(s: T): Unit = unimplemented(s, Instructions.vf2iu)
	open fun vf2iz(s: T): Unit = unimplemented(s, Instructions.vf2iz)
	open fun vi2f(s: T): Unit = unimplemented(s, Instructions.vi2f)
	open fun vscmp(s: T): Unit = unimplemented(s, Instructions.vscmp)
	open fun vmscl(s: T): Unit = unimplemented(s, Instructions.vmscl)
	open fun vt4444_q(s: T): Unit = unimplemented(s, Instructions.vt4444_q)
	open fun vt5551_q(s: T): Unit = unimplemented(s, Instructions.vt5551_q)
	open fun vt5650_q(s: T): Unit = unimplemented(s, Instructions.vt5650_q)
	open fun vmfvc(s: T): Unit = unimplemented(s, Instructions.vmfvc)
	open fun vmtvc(s: T): Unit = unimplemented(s, Instructions.vmtvc)
	open fun mfvme(s: T): Unit = unimplemented(s, Instructions.mfvme)
	open fun mtvme(s: T): Unit = unimplemented(s, Instructions.mtvme)
	open fun sv_s(s: T): Unit = unimplemented(s, Instructions.sv_s)
	open fun vfim(s: T): Unit = unimplemented(s, Instructions.vfim)
	open fun svl_q(s: T): Unit = unimplemented(s, Instructions.svl_q)
	open fun svr_q(s: T): Unit = unimplemented(s, Instructions.svr_q)
	open fun vbfy1(s: T): Unit = unimplemented(s, Instructions.vbfy1)
	open fun vbfy2(s: T): Unit = unimplemented(s, Instructions.vbfy2)
	open fun vf2h(s: T): Unit = unimplemented(s, Instructions.vf2h)
	open fun vh2f(s: T): Unit = unimplemented(s, Instructions.vh2f)
	open fun vi2s(s: T): Unit = unimplemented(s, Instructions.vi2s)
	open fun vi2us(s: T): Unit = unimplemented(s, Instructions.vi2us)
	open fun vlgb(s: T): Unit = unimplemented(s, Instructions.vlgb)
	open fun vqmul(s: T): Unit = unimplemented(s, Instructions.vqmul)
	open fun vs2i(s: T): Unit = unimplemented(s, Instructions.vs2i)
	open fun vc2i(s: T): Unit = unimplemented(s, Instructions.vc2i)
	open fun vuc2i(s: T): Unit = unimplemented(s, Instructions.vuc2i)
	open fun vsbn(s: T): Unit = unimplemented(s, Instructions.vsbn)
	open fun vsbz(s: T): Unit = unimplemented(s, Instructions.vsbz)
	open fun vsocp(s: T): Unit = unimplemented(s, Instructions.vsocp)
	open fun vsrt1(s: T): Unit = unimplemented(s, Instructions.vsrt1)
	open fun vsrt2(s: T): Unit = unimplemented(s, Instructions.vsrt2)
	open fun vsrt4(s: T): Unit = unimplemented(s, Instructions.vsrt4)
	open fun vus2i(s: T): Unit = unimplemented(s, Instructions.vus2i)
	open fun vwbn(s: T): Unit = unimplemented(s, Instructions.vwbn)
	open fun bvf(s: T): Unit = unimplemented(s, Instructions.bvf)
	open fun bvt(s: T): Unit = unimplemented(s, Instructions.bvt)
	open fun bvfl(s: T): Unit = unimplemented(s, Instructions.bvfl)
	open fun bvtl(s: T): Unit = unimplemented(s, Instructions.bvtl)
}
