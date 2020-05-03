package com.lagou.edu.annotationRegister;

import java.lang.annotation.*;

/**
 * @author tudedong
 * @description
 * @date 2020-05-02 16:40:45
 */
//@Target元注解表示允许这个注解可以使用的范围，这里我们只完成域的作用范围。,ElementType.METHOD,ElementType.CONSTRUCTOR
//@Retention元注解在这里表示该注解保留到运行时期。
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {

}
