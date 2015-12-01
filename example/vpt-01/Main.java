//Sample snippet that 'would have' generated this test case
public class Main{

    public Main f1 = null;

    public static void main(String[] args){
        Main v1, v2, v3, v4, v5, v6, v7, v8, v9;
        v1 = new Main();//hobj1
        v2 = v1;
        v3 = new Main();//hobj3
        v3.f1 = v2;
        v4 = v3;
        v5 = v4.f1;
        v6 = v5;
        v7 = new Main();//hobj2
        v8 = v7;
        v9 = v8;
        v5 = v8;
    }
}

