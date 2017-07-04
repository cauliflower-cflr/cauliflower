int main(int argc, char** argv){
    int* one = &argc;
    int* two = one;
    return *two;
}
