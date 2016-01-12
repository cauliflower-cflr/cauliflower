
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

template<typename A, typename B, unsigned AC, unsigned BC>

struct CauliAA : public ModulePass, public AliasAnalysis, public InstVisitor<CauliAA, void> {
    static char ID;
    unsigned num_allocs = 0;
    int num = 0;

        registrar_group<Value*,unsigned> regs;
        relation_buffer<Value*,unsigned> buf_al;
        relation_buffer<Value*,Value*> buf_as;
        relation_buffer<Value*,Value*> buf_lo;
        relation_buffer<Value*,Value*> buf_st;
        relation_buffer<Value*,unsigned> buf_pt;
        relation_buffer<Value*,Value*> buf_ai;

    CauliAA() : ModulePass(ID), regs(), buf_al(regs.select<0,1>()), buf_as(regs.select<0,0>()), buf_lo(regs.select<0,0>()), buf_st(regs.select<0,0>()), buf_pt(regs.select<0,1>()), buf_ai(regs.select<0,0>()) {}

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
        llvm_unreachable("Unsupported instruction encountered");
    }

    void visitLoadInst(LoadInst& inst){
        errs() << "LOAD: " << inst << "\n";
    }
};

}

char CauliAA::ID = 0;
static RegisterPass<CauliAA> X("cauli-aa", "Cauliflower's implementation of Andersen-style Anlias Analysis", true, true);
static RegisterAnalysisGroup<AliasAnalysis> Y(X);
