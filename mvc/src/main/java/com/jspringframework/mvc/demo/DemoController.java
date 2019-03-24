package com.jspringframework.mvc.demo;

import com.jspringframework.mvc.annotation.JAutowired;
import com.jspringframework.mvc.annotation.JController;
import com.jspringframework.mvc.annotation.JRequestMapping;
import com.jspringframework.mvc.annotation.JRequestParam;

@JController
@JRequestMapping("demo")
public class DemoController {
    @JAutowired
    private DemoService demoService;

    @JRequestMapping("helloworld")
    public String helloWorld(@JRequestParam("name") String name) {
        return "Hello world! I'm " + name + ".";
    }
}
