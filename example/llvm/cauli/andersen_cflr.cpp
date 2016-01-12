
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
        std::cerr << "__load__\n";
        relations[2].dump(std::cerr);
        std::cerr << "__store__\n";
        relations[3].dump(std::cerr);
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

    void visitLoadInst(LoadInst& inst) {
        Value* ptr = inst.getPointerOperand();
        Value* val = &inst;
        buf_lo.add(relation_buffer<Value*, Value*>::outer_type {val, ptr});
    }

    void visitStoreInst(StoreInst &Inst) {
        auto *Ptr = Inst.getPointerOperand();
        auto *Val = Inst.getValueOperand();
        buf_st.add(relation_buffer<Value*, Value*>::outer_type {Ptr, Val});
    }
};

}

char CauliAA::ID = 0;
static RegisterPass<CauliAA> X("cauli-aa", "Cauliflower's implementation of Andersen-style Anlias Analysis", true, true);
static RegisterAnalysisGroup<AliasAnalysis> Y(X);
