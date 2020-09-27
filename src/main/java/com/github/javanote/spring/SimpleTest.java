package com.github.javanote.spring;

/**
 * @author: starc
 * @date: 2020/3/18
 */
public class SimpleTest {

    static int test;
    static boolean   final1(){
        test++;
        return true;
    }

    public static void main(String[] args) {
        test = 0;
        if ((   final1 () |   final1 ())  ||  final1 ())
        test++;
        System.out.println(test);
    }
}
