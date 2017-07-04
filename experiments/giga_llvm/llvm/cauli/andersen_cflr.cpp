
#include <chrono>
#include <iostream>

#include "llvm/Analysis/AliasAnalysis.h"
#include "llvm/IR/InstVisitor.h"
#include "llvm/IR/Module.h"
#include "llvm/Pass.h"
#include "llvm/Support/raw_ostream.h"

#include "relation_buffer.h"
#include "andersen.h"

using namespace llvm;
using namespace cflr;

namespace {

void dumpV(const Value* val){
    if(val->hasName()) errs() << val->getName();
    else errs() << *val;
}

void dumpVV(std::string name, const relation<cflr::andersen_semi_naive::adt_t>& rel, registrar_group<const Value*,unsigned>& regs){
    relation_buffer<const Value*,const Value*> tmp(regs.select<0,0>());
    rel.export_buffer(tmp);
    for(auto& row : tmp.data){
        errs() << "  ";
        dumpV(std::get<0>(regs.group).get(row[0]));
        errs() << " # " << name << " # ";
        dumpV(std::get<0>(regs.group).get(row[1]));
        errs() << "\n";
    }
}
void dumpVH(std::string name, const relation<cflr::andersen_semi_naive::adt_t>& rel, registrar_group<const Value*,unsigned>& regs){
    relation_buffer<const Value*,unsigned> tmp(regs.select<0,1>());
    rel.export_buffer(tmp);
    for(auto& row : tmp.data){
        errs() << "  ";
        dumpV(std::get<0>(regs.group).get(row[0]));
        errs() << " # " << name << " # H" << std::get<1>(regs.group).get(row[1]) << "\n";
    }
}

struct CauliAA : public ModulePass, public AliasAnalysis, public InstVisitor<CauliAA, void> {
    typedef andersen_semi_naive P;
    static char ID;
    unsigned num_allocs = 0;

    registrar_group<const Value*,unsigned> regs;
    relation_buffer<const Value*,unsigned> buf_re;
    relation_buffer<const Value*,const Value*> buf_as;
    relation_buffer<const Value*,const Value*> buf_lo;
    relation_buffer<const Value*,const Value*> buf_st;
    relation_buffer<const Value*,unsigned> buf_pt;
    relation_buffer<const Value*,const Value*> buf_al;
    P::rels_t relations;

    CauliAA() : ModulePass(ID), regs(), buf_re(regs.select<0,1>()), buf_as(regs.select<0,0>()), buf_lo(regs.select<0,0>()), buf_st(regs.select<0,0>()), buf_pt(regs.select<0,1>()), buf_al(regs.select<0,0>()), relations{relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1)} {}

    //
    // Module Pass
    //

    bool runOnModule(Module& m) override {
        InitializeAliasAnalysis(this);
        // The group of string registrars
        unsigned count = 0;
        for(auto& func : m.functions()) {
            (errs() << "  - ").write_escaped(func.getName()) << "\n";
            for(auto& block : func.getBasicBlockList()) {
                for(auto& inst : block.getInstList()) {
                    //exclude non-useful edges
                    bool IsNonInvokeTerminator = isa<TerminatorInst>(inst) && !isa<InvokeInst>(inst);
                    if(isa<CmpInst>(inst) || isa<FenceInst>(inst) || IsNonInvokeTerminator) continue;
                    this->visit(&inst);
                }
            }
            count++;
        }
        errs() << "==" << count << "==\n";
        //errs().write_escaped(m.getName()) << '\n';
        // import as relations
        P::vols_t vols = regs.volumes();
        relations[0].import_buffer(buf_re);
        relations[1].import_buffer(buf_as);
        relations[2].import_buffer(buf_lo);
        relations[3].import_buffer(buf_st);
        relations[4].import_buffer(buf_pt);
        relations[5].import_buffer(buf_al);
        //solve
        P::solve(vols, relations);
        //print the relations
        // dumpVH("ref", relations[0], regs);
        // dumpVV("assign", relations[1], regs);
        // dumpVV("load", relations[2], regs);
        // dumpVV("store", relations[3], regs);
        // dumpVH("pointsto", relations[4], regs);
        // dumpVV("alias", relations[5], regs);
        return false;
    }

    //
    // Alias Analysis
    //

    void getAnalysisUsage(AnalysisUsage &AU) const override {
        AliasAnalysis::getAnalysisUsage(AU);
    }

    void *getAdjustedAnalysisPointer(const void *ID) override {
        if (ID == &AliasAnalysis::ID)
            return (AliasAnalysis *)this;
        return this;
    }

    AliasAnalysis::AliasResult alias(const Location& a, const Location& b) override {
        auto aval = std::get<0>(regs.group).get_or_add(a.Ptr);
        auto bval = std::get<0>(regs.group).get_or_add(b.Ptr);
        if(!relations[5].adts[0].query(aval, bval)) return AliasResult::NoAlias;
        return AliasAnalysis::alias(a, b);
    }

    //
    // Instruction Visitor
    //

    void visitInstruction(Instruction& inst) {
        errs() << "UNSUPPORTED: " << inst << "\n";
        //llvm_unreachable("Unsupported instruction encountered");
    }

    template <typename InstT> void visitCallLikeInst(InstT &Inst) {
        if (auto *Fn = Inst.getCalledFunction()) {
            if (Fn->isDeclaration() || Fn->isVarArg()) return;
            //parameters assigned to arguments
            unsigned cur = 0;
            for(auto i = Fn->arg_begin(), e = Fn->arg_end(); i!=e; ++i){
                buf_as.add(relation_buffer<const Value*, const Value*>::outer_type {Inst.getArgOperand(cur), &(*i)});
                ++cur;
            }
            assert(cur == Inst.getNumArgOperands());
            //intrinsic return (the function itself) assigned to return
            buf_as.add(relation_buffer<const Value*, const Value*>::outer_type {Fn, &Inst});
        } else {
            //TODO unknown functions
            errs() << "UNLINKED: " << Inst << "\n";
            //llvm_unreachable("Function with unknown linkage");
            return;
        }
    }

    void visitCallInst(CallInst &Inst) {
        visitCallLikeInst(Inst);
    }

    void visitInvokeInst(InvokeInst &Inst) {
        visitCallLikeInst(Inst);
    }

    void visitReturnInst(ReturnInst& i) {
        if(Value* val = i.getReturnValue()){
            if (!dyn_cast<Constant>(val)){
                buf_as.add(relation_buffer<Value*, Value*>::outer_type {val, i.getParent()->getParent()});
            }
        }
    }

    void visitCastInst(CastInst &Inst) {
        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(0)});
    }
    void visitBinaryOperator(BinaryOperator &Inst) {
        auto *Op1 = Inst.getOperand(0);
        auto *Op2 = Inst.getOperand(1);
        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Op1});
        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Op2});
    }

    void visitAtomicCmpXchgInst(AtomicCmpXchgInst &i) {
        buf_st.add(relation_buffer<Value*, Value*>::outer_type {i.getPointerOperand(), i.getNewValOperand()});
    }

    void visitAtomicRMWInst(AtomicRMWInst &i) {
        buf_st.add(relation_buffer<Value*, Value*>::outer_type {i.getPointerOperand(), i.getValOperand()});
    }

    void visitPHINode(PHINode &Inst) {
        for (unsigned I = 0, E = Inst.getNumIncomingValues(); I != E; ++I) {
            buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getIncomingValue(I)});
        }
    }

    void visitGetElementPtrInst(GetElementPtrInst &Inst) {
        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getPointerOperand()});
        for (auto I = Inst.idx_begin(), E = Inst.idx_end(); I != E; ++I)
            buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, *I});
    }

    void visitSelectInst(SelectInst &i) {
        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&i, i.getCondition()});
        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&i, i.getTrueValue()});
        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&i, i.getFalseValue()});
    }

    void visitAllocaInst(AllocaInst &i) {
        buf_re.add(relation_buffer<Value*, unsigned>::outer_type {&i, num_allocs});
        num_allocs++;
    }

    void visitLoadInst(LoadInst& i) {
        buf_lo.add(relation_buffer<Value*, Value*>::outer_type {&i, i.getPointerOperand()});
    }

    void visitStoreInst(StoreInst &i) {
        buf_st.add(relation_buffer<Value*, Value*>::outer_type {i.getPointerOperand(), i.getValueOperand()});
    }

    void visitExtractElementInst(ExtractElementInst &i) {
        buf_lo.add(relation_buffer<Value*, Value*>::outer_type {&i, i.getVectorOperand()});
    }

    void visitInsertElementInst(InsertElementInst &Inst) {
        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(0)});
        buf_st.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(1)});
    }

    void visitInsertValueInst(InsertValueInst &Inst) {
        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(0)});
        buf_st.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(1)});
    }

    void visitExtractValueInst(ExtractValueInst &i) {
        buf_lo.add(relation_buffer<Value*, Value*>::outer_type {&i, i.getAggregateOperand()});
    }

    void visitShuffleVectorInst(ShuffleVectorInst &Inst) {
        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(0)});
        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(1)});
    }
};

}

char CauliAA::ID = 0;
static RegisterPass<CauliAA> X("cauli-aa", "Cauliflower's implementation of Andersen-style Anlias Analysis", true, true);
static RegisterAnalysisGroup<AliasAnalysis> Y(X);
