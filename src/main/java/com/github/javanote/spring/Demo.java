package com.github.javanote.spring;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author: starc
 * @date: 2020/3/18
 */
public class Demo {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
        Object bean = context.getBean("");
    }
}

