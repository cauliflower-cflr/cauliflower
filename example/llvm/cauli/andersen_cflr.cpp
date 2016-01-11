
#include <forward_list>

#include "llvm/ADT/DenseMap.h"
#include "llvm/ADT/None.h"
#include "llvm/ADT/Optional.h"
#include "llvm/Analysis/AliasAnalysis.h"
#include "llvm/IR/InstVisitor.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/ValueHandle.h"
#include "llvm/Pass.h"
#include "llvm/Support/raw_ostream.h"

#include "StratifiedSets.h"

using namespace llvm;

namespace {

struct CauliAA : public ModulePass, public AliasAnalysis {

    static char ID;
    int num = 0;
    CauliAA() : ModulePass(ID) {}

    ~CauliAA() {
        errs() << "TOLD " << num << "\n";
    }

    // 
    // Analysis
    //
    //
    // Module Pass
    //

    bool runOnModule(Module& m) override {
        InitializeAliasAnalysis(this);
        errs() << "Hello: \n";
        unsigned count = 0;
        for(auto& func : m.functions()) {
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
