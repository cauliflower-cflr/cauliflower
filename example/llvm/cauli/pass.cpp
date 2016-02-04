
#include "llvm/Pass.h"
#include "llvm/IR/Function.h"
#include "llvm/Support/raw_ostream.h"

using namespace llvm;

namespace {
struct Hello : public FunctionPass {
    static char ID;
    Hello() : FunctionPass(ID) {}

    bool runOnFunction(Function &F) override {
        errs() << "Hello: ";
        errs().write_escaped(F.getName()) << '\n';
        Hello h;
        return false;
    }
};
}

char Hello::ID = 0;
static RegisterPass<Hello> Z("hello", "Hello World Pass", false, false);
