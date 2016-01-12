#include <stdio.h>

void swap(int** a, int** b){
    int* tmp = *a;
    *a = *b;
    *b = tmp;
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
