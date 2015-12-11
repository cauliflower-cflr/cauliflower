
#include "llvm/Analysis/AliasAnalysis.h"
#include "llvm/IR/InstVisitor.h"
#include "llvm/IR/Module.h"
#include "llvm/Pass.h"
#include "llvm/Support/raw_ostream.h"

using namespace llvm;

namespace {
struct CauliAA : public ModulePass, public AliasAnalysis, public InstVisitor<CauliAA, void> {
    static char ID;
    int num = 0;
    CauliAA() : ModulePass(ID) {}

    ~CauliAA() {
        errs() << "TOLD " << num << "\n";
    }

    //
    // Module Pass
    //

    bool runOnModule(Module& m) override {
        InitializeAliasAnalysis(this);
        errs() << "Hello: \n";
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
