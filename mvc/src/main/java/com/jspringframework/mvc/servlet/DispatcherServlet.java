package com.jspringframework.mvc.servlet;

import com.jspringframework.mvc.annotation.JAutowired;
import com.jspringframework.mvc.annotation.JController;
import com.jspringframework.mvc.annotation.JRequestMapping;
import com.jspringframework.mvc.annotation.JService;
import com.jspringframework.mvc.utils.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DispatcherServlet extends HttpServlet {
    Map<String, Object> ioc = new ConcurrentHashMap();
    Properties contextConfig = new Properties();
    List<String> classNames = new ArrayList<>();
    Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件，解析配置文件
        readConfig(config.getInitParameter("contextConfigLocation"));

        //2.扫描所有相关联类
        scanPackage(this.contextConfig.getProperty("scanPackage"));
        //3. 初始化所有相关联类，并放入ioc容器中
        try {
            initBeans();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        //4. 依赖注入
        doAtutowired();
        //5. 初始化handlerMapping容器，将url与method关联
        initHandlerMapping();
    }

    private void scanPackage(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classDir = null;
        try {
            classDir = new File(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        for (File file : classDir.listFiles()) {
            if (file.isDirectory()) {
                scanPackage(scanPackage + "." + file.getName());
            } else {
                String s = scanPackage + "." + file.getName();
                classNames.add(s);
            }
        }

    }

    private void initHandlerMapping() {
        ioc.forEach((beanName, bean) -> {
            Class<?> clazz = bean.getClass();
            if (!clazz.isAnnotationPresent(JController.class)) {
                return;
            }
            String baseUrl = "/";
            if (clazz.isAnnotationPresent(JRequestMapping.class)) {
                JRequestMapping jRequestMapping = clazz.getAnnotation(JRequestMapping.class);
                baseUrl = "/" + jRequestMapping.value().trim();
            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(JRequestMapping.class)) {
                    continue;
                }
                JRequestMapping mapping = method.getAnnotation(JRequestMapping.class);
                String url = StringUtils.urlPattern(baseUrl + "/" + mapping.value().trim());
                handlerMapping.put(url, method);
            }
        });
    }

    private void doAtutowired() {
        ioc.forEach((key, value) -> {
            Class<?> clazz = value.getClass();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(JAutowired.class)) {
                    continue;
                }
                field.setAccessible(true);
                JAutowired jAutowired = field.getAnnotation(JAutowired.class);
                Class<?> declaringClass = field.getDeclaringClass();
                String beanName = declaringClass.getName();
                if (!"".equals(jAutowired.value().trim())) {
                    beanName = jAutowired.value().trim();
                }
                try {
                    field.set(value, ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initBeans() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        for (String className : classNames) {
            Class<?> clazz = Class.forName(className);
            String beanName = StringUtils.lowerFirstChar(clazz.getSimpleName());
            if (clazz.isAnnotationPresent(JController.class)) {
                Object o = clazz.newInstance();
                ioc.put(beanName, o);
            } else if (clazz.isAnnotationPresent(JService.class)) {
                JService jService = clazz.getAnnotation(JService.class);
                if (!"".equals(jService.value().trim())) {
                    beanName = jService.value().trim();
                }
                Object o = clazz.newInstance();
                ioc.put(beanName, o);
                //通过接口
                Class<?>[] interfaces = clazz.getInterfaces();
                for (Class<?> anInterface : interfaces) {
                    String interfacePath = anInterface.getName();
                    ioc.put(interfacePath, o);
                }
            } else {
                continue;
            }
        }
    }

    private void readConfig(String contextConfigLocation) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            this.contextConfig.load(inputStream);
        } catch (IOException e) {
            System.out.println("加载配置文件失败:" + e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
