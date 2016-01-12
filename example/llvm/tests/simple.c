#include <stdio.h>

int main(int argc, char** argv){
    int** bar;
    int* baz;
    int var = argc;
    int* foo = &var;
    *bar = foo;
    baz = *bar;
    printf("%d\n", *baz);
    return 0;
}
