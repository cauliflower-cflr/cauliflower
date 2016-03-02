
#include <chrono>
#include <fstream>
#include <iostream>
#include <map>
#include <sstream>

#include "llvm/Analysis/AliasAnalysis.h"
#include "llvm/IR/InstVisitor.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/Operator.h"
#include "llvm/Pass.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/CommandLine.h"

using namespace llvm;

// A command line option to control the location of the output files
cl::opt<std::string> fact_dump_dir("fact-dump-dir", cl::desc("Directory to output program facts to"), cl::value_desc("directory"), cl::init("."));

namespace {

/**
 * Converts a non-formatted atom to its formatted version
 */
template<typename T> std::string format_atom_as_string(const T& atom);

/**
 * Prints a tuple to the output stream of a given relation
 */
template<typename, typename...> struct tuple_printer;

/**
 * Utility string replace_all
 * Thanks to StackOverflow: user283145 and Michael Mrozek
 */
void replace_all(std::string& str, const std::string& from, const std::string& to) {
    if(from.empty()) return;
    size_t start_pos = 0;
    while((start_pos = str.find(from, start_pos)) != std::string::npos) {
        str.replace(start_pos, from.length(), to);
        start_pos += to.length(); // In case 'to' contains 'from', like replacing 'x' with 'yx'
    }
}

/**
 * Removes special characters which are illegal in a CSV file
 */
std::string desugar(const std::string& sugared){
    std::string ret = sugared;
    if(ret.find('^') != std::string::npos) llvm_unreachable("input string contained \'^\' character");
    std::replace(ret.begin(), ret.end(), '\\', '^'); // turn backslash temporarily into ^
    replace_all(ret, ",", "\\c");
    replace_all(ret, "\"", "\\q");
    replace_all(ret, "^", "\\\\"); // return ^ to double backslash
    return ret;
}
/**
 * Re-inserts special characters which are illegal in a CSV file
 */

/**
 * Module pass which crawls over a module and writes program facts to output csv relations
 */
struct fact_dumper : public ModulePass, public InstVisitor<fact_dumper, void> {
    static char ID;

    std::map<std::string, std::ofstream> outputMap;

    fact_dumper() : ModulePass(ID), outputMap() {}

    /**
     * Writes the fact relations to the output relation file
     */
    template<typename...Fs> void dumpFacts(const std::string& relation, const Fs&...facts){
        //special case for error printing
        if(relation == ""){
            tuple_printer<llvm::raw_ostream, Fs...>::print(errs(), facts...);
        } else {
            //if(outputMap.find(relation) == outputMap.end()){
            //    std::string file_loc = std::string(fact_dump_dir.c_str()) + "/" + relation + ".csv";
            //    auto new_stream = outputMap.insert(std::map<std::string, std::ofstream>::value_type{relation, std::ofstream(file_loc)});
            //    if(!new_stream.first->second.good()) llvm_unreachable(("Failed to open relation " + relation + " at " + file_loc).c_str());
            //}
            //tuple_printer<std::ofstream, Fs...>::print(outputMap[relation], facts...);
            // TODO uncomment ^^^
            tuple_printer<llvm::raw_ostream, std::string, Fs...>::print(errs(), relation, facts...);
        }
    }

    //
    // Module Pass
    //

    bool runOnModule(Module& m) override {
        // The group of string registrars
        for(auto& func : m.functions()) {
            dumpFacts("module_functions", m, func);
            unsigned ac = 0;
            for(const auto& arg : func.args()){
                dumpFacts("formal_parameter", func, arg, ac++);
            }
            for(auto& block : func.getBasicBlockList()) {
                dumpFacts("function_blocks", func, block);
                for(auto& inst : block.getInstList()) {
                    dumpFacts("block_instructions", block, inst);
                    //exclude non-useful edges
                    bool IsNonInvokeTerminator = isa<TerminatorInst>(inst) && !isa<InvokeInst>(inst);
                    if(isa<CmpInst>(inst) || isa<FenceInst>(inst) || IsNonInvokeTerminator) continue;
                    this->visit(&inst);
                }
            }
        }

        // close all the output streams
        for(auto& f : outputMap) f.second.close();
        // Do not
        return false;
    }

    void getAnalysisUsage(AnalysisUsage &AU) const override { }

    //
    // Instruction Visitor
    //

    void visitInstruction(Instruction& inst) {
        errs() << "UNSUPPORTED: " << inst << "\n";
        //llvm_unreachable("Unsupported instruction encountered");
    }

    template <typename InstT> void visitCallLikeInst(InstT &inst) {
        if (auto *func = inst.getCalledFunction()) {
            dumpFacts("inst_direct_call", llvm::cast<Instruction>(inst), *func);
        } else {
            //TODO unknown functions
            errs() << "UNLINKED: " << inst << "\n";
            //llvm_unreachable("Function with unknown linkage");
            return;
        }
        for(unsigned i=0; i<inst.getNumArgOperands(); i++){
            dumpFacts("actual_parameter", llvm::cast<Instruction>(inst), *inst.getArgOperand(i), i);
        }
    }

    void visitCallInst(CallInst &inst) {
        visitCallLikeInst(inst);
    }

    void visitInvokeInst(InvokeInst &inst) {
        visitCallLikeInst(inst);
    }

    void visitReturnInst(ReturnInst& inst) {
        dumpFacts("inst_return", llvm::cast<Instruction>(inst), *inst.getReturnValue());
    }

    void visitCastInst(CastInst &inst) {
        dumpFacts("inst_cast", llvm::cast<Instruction>(inst), *inst.getOperand(0));
    }

    void visitBinaryOperator(BinaryOperator &inst) {
        dumpFacts("inst_assign", llvm::cast<Instruction>(inst), *inst.getOperand(0));
        dumpFacts("inst_assign", llvm::cast<Instruction>(inst), *inst.getOperand(1));
    }

    void visitAtomicCmpXchgInst(AtomicCmpXchgInst &inst) {
        dumpFacts("inst_store", *inst.getPointerOperand(), *inst.getNewValOperand());
    }

    void visitAtomicRMWInst(AtomicRMWInst &inst) {
        dumpFacts("inst_store", *inst.getPointerOperand(), *inst.getValOperand());
    }

    void visitPHINode(PHINode &inst) {
        for (unsigned i=0, e=inst.getNumIncomingValues(); i!=e; ++i) {
            dumpFacts("inst_assign", llvm::cast<Instruction>(inst), *inst.getIncomingValue(i));
        }
    }

    //void visitGetElementPtrInst(GetElementPtrInst &Inst) {
    //    buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getPointerOperand()});
    //    for (auto I = Inst.idx_begin(), E = Inst.idx_end(); I != E; ++I)
    //        buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, *I});
    //}

    //void visitSelectInst(SelectInst &i) {
    //    buf_as.add(relation_buffer<Value*, Value*>::outer_type {&i, i.getCondition()});
    //    buf_as.add(relation_buffer<Value*, Value*>::outer_type {&i, i.getTrueValue()});
    //    buf_as.add(relation_buffer<Value*, Value*>::outer_type {&i, i.getFalseValue()});
    //}

    void visitAllocaInst(AllocaInst &inst) {
        dumpFacts("inst_alloca", llvm::cast<Instruction>(inst), *inst.getAllocatedType());
    }

    void visitLoadInst(LoadInst& i) {
        dumpFacts("inst_load", i, *i.getPointerOperand());
    }

    void visitStoreInst(StoreInst &i) {
        dumpFacts("inst_store", *i.getPointerOperand(), *i.getValueOperand());
    }

    //void visitExtractElementInst(ExtractElementInst &i) {
    //    buf_lo.add(relation_buffer<Value*, Value*>::outer_type {&i, i.getVectorOperand()});
    //}

    //void visitInsertElementInst(InsertElementInst &Inst) {
    //    buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(0)});
    //    buf_st.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(1)});
    //}

    //void visitInsertValueInst(InsertValueInst &Inst) {
    //    buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(0)});
    //    buf_st.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(1)});
    //}

    //void visitExtractValueInst(ExtractValueInst &i) {
    //    buf_lo.add(relation_buffer<Value*, Value*>::outer_type {&i, i.getAggregateOperand()});
    //}

    //void visitShuffleVectorInst(ShuffleVectorInst &Inst) {
    //    buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(0)});
    //    buf_as.add(relation_buffer<Value*, Value*>::outer_type {&Inst, Inst.getOperand(1)});
    //}

};

static const std::string separator = "$"; // conveniently this cannot appear in (disassembled) llvm bitcode

template<typename T> std::string raw_out(const T& t){
    std::string out;
    llvm::raw_string_ostream rsos(out);
    rsos << const_cast<T&>(t); // ~evil~
    return rsos.str();
}

//template<typename T> std::string format_atom_as_string(const T& atom){
//    std::stringstream ss;
//    ss << atom;
//    return ss.str();
//}
template<> std::string format_atom_as_string<std::string>(const std::string& s){
    return s;
}
template<> std::string format_atom_as_string<unsigned>(const unsigned& u){
    std::stringstream ss;
    ss << u;
    return ss.str();
}
template<> std::string format_atom_as_string<llvm::Type>(const Type& t){
    if(t.isStructTy()) return "T" + raw_out(t.getStructName());
    return "T" + raw_out(t);
}
template<> std::string format_atom_as_string<Module>(const Module& m){
    return m.getName().str();
}
template<> std::string format_atom_as_string<Function>(const Function& f){
    return format_atom_as_string(*f.getParent()) + separator + f.getName().str();
}
template<> std::string format_atom_as_string<BasicBlock>(const BasicBlock& b){
    return format_atom_as_string(*b.getParent()) + separator + b.getName().str();
}
template<> std::string format_atom_as_string<Instruction>(const Instruction& i){
    return format_atom_as_string(*i.getParent()) + separator + "I" + raw_out(i);
}
template<> std::string format_atom_as_string<Argument>(const Argument& a){
    return format_atom_as_string(*a.getParent()) + separator + "A" + a.getName().str();
}
template<> std::string format_atom_as_string<Constant>(const Constant& c){
    return "C" + raw_out(c);
}
template<> std::string format_atom_as_string<Value>(const Value& v){
    if(llvm::isa<BasicBlock>(v)) return format_atom_as_string(llvm::cast<BasicBlock>(v));
    else if(llvm::isa<Instruction>(v)) return format_atom_as_string(llvm::cast<Instruction>(v));
    else if(llvm::isa<Argument>(v)) return format_atom_as_string(llvm::cast<Argument>(v));
    else if(llvm::isa<Constant>(v)) return format_atom_as_string(llvm::cast<Constant>(v));
    else{
        errs() << "==========\n" << v << "\n=================\n";
        llvm_unreachable("I cannot convert this atom to a string");
        return ""; // also unreachable, but keeps the "control reaches end of non-void..." quiet
    }
}

template<typename Out, typename Nxt, typename...Rest> struct tuple_printer<Out, Nxt, Rest...> {
    static void print(Out& o, const Nxt& n, const Rest&...r){
        o << "\"" << desugar(format_atom_as_string(n)) << "\"";
        if(sizeof...(Rest) != 0) o << ",";
        tuple_printer<Out, Rest...>::print(o, r...);
    }
};
template<typename Out> struct tuple_printer<Out> {
    static void print(Out& o){
        o << "\n";
    }
};


}

char fact_dumper::ID = 0;
static RegisterPass<fact_dumper> X("fact-dump", "Dump the program statements as doop-style facts", true, true);

