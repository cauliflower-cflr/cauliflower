#include <stdio.h>

int main(int argc, char** argv) {
    int *x, *y;
    int **a, **b, **c;
    int i = 5;
    x = &i;
    y = &argc;
    a = &x;
    b = &x;
    c = &y;
    b = &y;
    printf("a %d, b %d, c %d", **a, **b, **c);
    return 0;
}
