#include <stdio.h>

int main(int argc, char** argv){
    int* baz;
    int var = argc;
    int* foo = &var;
    int** bar = &foo;
    baz = *bar;
    printf("%d\n", *baz);
    return 0;
}
