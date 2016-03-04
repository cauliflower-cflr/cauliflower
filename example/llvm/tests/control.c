#include <stdio.h>

int main(int argc, char** argv){
    char* exe = argv[0];
    char* arg =  argv[1];
    char** undef;
    if(argc == 1){
        undef = &arg;
    } else {
        undef = &exe;
    }
    printf("%p %p %p\n", exe, arg, *undef);
    return 0;
}
