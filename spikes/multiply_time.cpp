#include <chrono>
#include <random>
#include <iostream>
#include <string>
#include "relation_buffer.h"
#include "concise_tree.h"
using namespace std;
using namespace std::chrono;
using namespace cflr;

typedef std::array<std::array<bool, 8>, 8> m_t;

void amat(m_t mat, ostream& out){
    for(unsigned r=0; r<8; r++){
        for(unsigned c=0; c<8; c++){
            out << (mat[r][c] ? "X" : ".") << " ";
        }
        out << endl;
    }
}

m_t matmul(const m_t& a, const m_t& b){
    m_t ret;
    for(unsigned i=0; i<8; i++)
        for(unsigned j=0; j<8; j++){
            ret[i][j] = false;
            for(unsigned k=0; k<8; k++)
                ret[i][j] = ret[i][j] || (a[i][k] && b[k][j]);
        }
    return ret;
}

void dmp(const m_t& mat, ostream& out) {
    for(int r=0; r<8; r++){
        for(int c=0; c<8;c++){
            out << (mat[r][c] ? "1 " : "0 ");
        }
        out << endl;
    }
}

void bmat(ident mat, ostream& out) {
    const unsigned sl = csqrt<ident>(sizeof(ident)*8);
    for(unsigned r=0; r<sl; r++){
        for(unsigned c=0; c<sl; c++){
            out << ((mat >> (c + sl*r)) & 1) << " ";
        }
        out << endl;
    }
}

bool is_samez(ident a, m_t b){
    ident chk = 0;
    for(int r=0; r<8; r++)
        for(int c=0; c<8;c++)
            if(b[r][c])
                chk |= ((ident)1) << (8*r + c);
    return a == chk;
}

static const unsigned SIZE = 64;
static const unsigned CASES = 200;

int main(int argc, char* argv[]) {
    ident igroup[SIZE*CASES];
    m_t agroup[SIZE*CASES];
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, 7);
    m_t empt;
    for(int i=0; i<8; i++){
        for(int j=0; j<8; j++){
            empt[i][j] = false;
        }
    }
    for(unsigned s=0; s<SIZE; s++){
        for(unsigned x=0; x<CASES; x++){
            unsigned idx = s*CASES + x;
            igroup[idx] = (ident)0;
            agroup[idx] = empt;
            for(unsigned i=0; i<s; i++){
                unsigned r = dis(gen);
                unsigned c = dis(gen);
                agroup[idx][r][c] = true;
                igroup[idx] |= ((ident)1) << (8*r + c);
            }
        }
    }
    ident ires[SIZE*CASES];
    m_t ares[SIZE*CASES];
    std::chrono::steady_clock::time_point t1 = std::chrono::steady_clock::now();
    for(unsigned s=0; s<SIZE; s++){
        for(unsigned x=0; x<CASES; x+=2){
            unsigned idx = s*CASES + x;
            ires[idx] = concise_tree::index_mult(igroup[idx], igroup[idx+1]);
        }
    }
    std::chrono::steady_clock::time_point t2 = std::chrono::steady_clock::now();
    for(unsigned s=0; s<SIZE; s++){
        for(unsigned x=0; x<CASES; x+=2){
            unsigned idx = s*CASES + x;
            ares[idx] = matmul(agroup[idx], agroup[idx+1]);
        }
    }
    std::chrono::steady_clock::time_point t3 = std::chrono::steady_clock::now();
    for(unsigned s=0; s<SIZE; s++){
        for(unsigned x=0; x<CASES; x+=2){
            unsigned idx = s*CASES + x;
            if(!is_samez(ires[idx], ares[idx])){
                cout << "FAIL " << idx << endl;
                bmat(igroup[idx], cout);
                cout << "X" << endl;
                bmat(igroup[idx+1], cout);
                cout << "=" << endl;
                bmat(ires[idx], cout);
                cout << endl << endl;
                dmp(agroup[idx], cout);
                cout << "X" << endl;
                dmp(agroup[idx+1], cout);
                cout << "=" << endl;
                dmp(ares[idx], cout);
                cout << endl << endl;
                return 1;
            }
        }
    }
    cout << "all good" << endl;
    cout << std::chrono::duration_cast<chrono::duration<double>>(t2 - t1).count() << endl;
    cout << std::chrono::duration_cast<chrono::duration<double>>(t3 - t2).count() << endl;
}
