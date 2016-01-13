
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

struct CauliAA : public ModulePass, public AliasAnalysis, public InstVisitor<CauliAA, void> {
    static char ID;
    unsigned num_allocs = 0;
    int num = 0;

    registrar_group<Value*,unsigned> regs;
    relation_buffer<Value*,unsigned> buf_re;
    relation_buffer<Value*,Value*> buf_as;
    relation_buffer<Value*,Value*> buf_lo;
    relation_buffer<Value*,Value*> buf_st;
    relation_buffer<Value*,unsigned> buf_pt;
    relation_buffer<Value*,Value*> buf_al;

    CauliAA() : ModulePass(ID), regs(), buf_re(regs.select<0,1>()), buf_as(regs.select<0,0>()), buf_lo(regs.select<0,0>()), buf_st(regs.select<0,0>()), buf_pt(regs.select<0,1>()), buf_al(regs.select<0,0>()) {}

    ~CauliAA() {
        errs() << "TOLD " << num << "\n";
    }

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
                    this->visit(&inst);
                }
            }
            count++;
        }
        errs() << "==" << count << "==\n";
        //errs().write_escaped(m.getName()) << '\n';
        // import as relations
        typedef andersen_semi_naive P;
        P::vols_t vols = regs.volumes();
        P::rels_t relations = {relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1)};
        relations[0].import_buffer(buf_re);
        relations[1].import_buffer(buf_as);
        relations[2].import_buffer(buf_lo);
        relations[3].import_buffer(buf_st);
        relations[4].import_buffer(buf_pt);
        relations[5].import_buffer(buf_al);
        //solve
        //print the relations
        std::cerr << "__ref__\n";
        relations[0].dump(std::cerr);
        std::cerr << "__assign__\n";
        relations[1].dump(std::cerr);
        std::cerr << "__load__\n";
        relations[2].dump(std::cerr);
        std::cerr << "__store__\n";
        relations[3].dump(std::cerr);
        std::cerr << "__pointsto__\n";
        relations[4].dump(std::cerr);
        std::cerr << "__alias__\n";
        relations[5].dump(std::cerr);
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
        this->num++;
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
                buf_as.add(relation_buffer<Value*, Value*>::outer_type {Inst.getArgOperand(cur), &(*i)});
                ++cur;
            }
            assert(cur == Inst.getNumArgOperands());
            //intrinsic return (the function itself) assigned to return
            buf_as.add(relation_buffer<Value*, Value*>::outer_type {Fn, &Inst});
        } else {
            //TODO unknown functions
            llvm_unreachable("Function with unknown linkage");
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
