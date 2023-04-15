package course.concurrency.m3_shared.intro;

import java.util.Scanner;

public class SimpleCode {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int m = scanner.nextInt();

        System.out.println(n + m);
        System.out.println(n * m);
    }
}
