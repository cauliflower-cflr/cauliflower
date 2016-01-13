#include <stdio.h>

void swap(int** m, int** n){
    int* tmp = *m;
    *m = *n;
    *n = tmp;
}

int main(int argc, char** argv){
    int y = 42;
    int* a = &argc;
    int* b = &y;
    printf("%d %d\n", *a, *b);
    swap(&a, &b);
    printf("%d %d\n", *a, *b);
    return 0;
}
