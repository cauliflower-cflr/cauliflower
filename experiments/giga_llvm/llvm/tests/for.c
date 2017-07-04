#include <stdio.h>

int main(int argc, char** argv){
    char* out;
    char** tall = &out;
    for(int i=0; i<argc; i++){
        out = argv[i];
    }
    printf("%s\n", *tall);
    return 0;
}
