/* 
 *                                 main.cpp
 * 
 * Author: Nic H.
 * Date: 2015-Nov-10
 */

#include <map>
#include <set>
#include <iostream>

#include "concise_tree.h"

using namespace std;

int main(){
    cflr::concise_tree ct;
    ct.initialise_import();
    ct.import(0, 2);
    ct.import(1, 1);
    ct.import(0, 0);
    ct.import(1, 0);
    ct.import(1, 0);
    ct.import(0, 1);
    ct.import(8, 0);
    ct.import(0, 7);
    ct.import(7, 0);
    ct.import(7, 4);
    ct.import(16, 0);
    ct.finalise_import();
    ct.dump();
    return 0;
}

