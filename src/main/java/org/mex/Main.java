package org.mex;

import java.util.Scanner;

/**
 * 自动跃动星穹
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("自动跃动星穹!");
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your cookie: ");
        String cookie = scanner.nextLine();
        System.out.print("Enter aim score: ");
        int aimScore = scanner.nextInt();
        Ydxq ydxq = new Ydxq(cookie, aimScore);
        ydxq.run();
    }
}