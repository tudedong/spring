package com.lagou.edu.annotationHandler;

import com.lagou.edu.annotationRegister.MyAutowired;
import com.lagou.edu.annotationRegister.MyService;
import com.lagou.edu.annotationRegister.MyTransactional;
import com.lagou.edu.factory.ProxyFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;

/**
 * @author tudedong
 * @description
 * @date 2020-05-02 17:44:29
 */
public class AnnotationHandler {

    //存放所有被注解的实现类或接口
    private static Set<Class<?>> classSet = new HashSet<>();

    /**
     * 将对象按名字存储起来
     * key：com.lagou.edu.dao.impl.JdbcAccountDaoImpl
     * value：JdbcAccountDaoImpl
     */
    private static Map<Class<?>, String> classNameMap = new HashMap<>();

    /**
     * 将所有的对象实例化存储起来，然后对外提供get接口获取实例化对象
     * key:JdbcAccountDaoImpl
     * value:实例化对象
     */
    private static Map<String,Object> map = new HashMap<>();

    //绝对路径长度
    private static int ABSOLUTE_PATH_LEN = 0;

    static {
        /**
         * @MyService 注入实现类或接口
         * @MyAutowired 注入，共实现三种注入 构造器注入/方法注入/属性注入
         * @MyTransactional 在service层注入，作为事物管理器，生成代理对象完成事务管理
         */
        handlerAnnotation();
    }

    /**
     * 初始化所有注解的实现类或接口
     */
    public static void handlerAnnotation(){
        try{
            //扫描com.lagou.edu包下面的所有实现类或接口
            classSet = scanClass("com.lagou.edu");

            //针对@MyService 定义存放被注解的实现类或接口
            //com.lagou.edu.factory.ProxyFactory
            Set<Class<?>> serviceList = new HashSet<>();

            //针对@MyAutowired  将对象的属性进行注入，这里定义属性存储集合
            //key= class com.lagou.edu.factory.ProxyFactory
            //value= {name=transactionManager,type=class com.lagou.edu.utils.TransactionManager}
            Map<Class<?>, List<Field>> fieldMap = new HashMap<>(16);

            //针对@MyTransactional 定义存储动态代理的接口
            //com.lagou.edu.service.impl.TransferServiceImpl
            Set<Class<?>> transactionList = new HashSet<>();

            //遍历所有被注解的实现类或接口
            for (Class<?> aClass : classSet) {
                //表示一个注解类型返回true,跳过
                if(aClass.isAnnotation()){
                    continue;
                }
                //如果是@MyService的对象，加入serviceList
                if(aClass.isAnnotationPresent(MyService.class)){
                    serviceList.add(aClass);
                }

                // 扫描属性上的Autowired
                Field[] field = aClass.getDeclaredFields();
                for (Field f : field) {
                    if(f.isAnnotationPresent(MyAutowired.class)){
                        if(fieldMap.containsKey(aClass)){
                            fieldMap.get(aClass).add(f);
                        }else{
                            fieldMap.put(aClass,new ArrayList<Field>(){{add(f);}});
                        }
                    }
                }

                //如果是@MyTransactional对象，加入transactionList
                if(aClass.isAnnotationPresent(MyTransactional.class)){
                    transactionList.add(aClass);
                }
            }

            /**
             * 将某个实现类及接口注册为同一个bean对象
             * 例如：
             * key：com.lagou.edu.dao.impl.JdbcAccountDaoImpl或com.lagou.edu.dao.AccountDao
             * value：JdbcAccountDaoImpl
             */
            for (Class<?> aClass : serviceList) {
                String beanName = getBeanName(aClass);
                classNameMap.put(aClass,beanName);
            }

            /**
             * 将所有@MyService注解的实例化对象放入map中
             */
            for (Class<?> aClass : serviceList) {
                String beanName = classNameMap.get(aClass);
                if(map.containsKey(beanName)){
                    continue;
                }
                map.put(beanName,aClass.newInstance());
            }

            //属性注入
            for (Class<?> aClass : fieldMap.keySet()) {
                doAutowiredField(aClass,fieldMap.get(aClass));
            }

            //transaction 代理
            for (Class<?> aClass : transactionList) {
                String beanName = getBeanName(aClass);
                Object object = map.get(beanName);
                Class<?>[] interfaces = aClass.getInterfaces();
                String proxyFactoryName = classNameMap.get(ProxyFactory.class);
                ProxyFactory proxyFactory = (ProxyFactory) map.get(proxyFactoryName);
                if(interfaces.length == 0){
                    //cglib代理
                    Object proxy = proxyFactory.getCglibProxy(object);
                    map.put(beanName,proxy);
                }else{
                    // jdk代理
                    Object proxy = proxyFactory.getJdkProxy(object);
                    map.put(beanName,proxy);
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * 属性注入
     * @param aClass
     * @param fields
     * @throws IllegalAccessException
     */
    private static void doAutowiredField(Class<?> aClass, List<Field> fields) throws IllegalAccessException {
        for (Field field : fields) {
            field.setAccessible(true);
            Class<?> type = field.getType();
            String beanName = classNameMap.get(type);
            String sourceBeanName = classNameMap.get(aClass);
            Object o = map.get(beanName);
            Object bean = map.get(sourceBeanName);
            field.set(bean,o);
        }
    }

    /**
     * 将某个实现类及接口注册为同一个bean对象
     * @param aclass
     * @return
     */
    private static String getBeanName(Class<?> aclass){
        MyService annotation = aclass.getAnnotation(MyService.class);
        String value = annotation.value();
        if(value == null || "".equals(value)) {
            String beanPath = aclass.getName();
            String beanName = beanPath.substring(beanPath.lastIndexOf('.')+1);
            //首字母小写
            char c = beanName.charAt(0);
            String temp = c + "";
            if (c >= 'A' && c <= 'Z') {
                //当为字母时，则转换为小写
                beanName.replaceFirst(temp, temp.toLowerCase());
            }
            value = beanName;
        }
        //父接口注册
        Class<?>[] interfaces = aclass.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            classNameMap.put(anInterface,value);
        }
        return value;
    }

    /**
     * 扫描包
     * @return
     */
    public static Set<Class<?>> scanClass(String packageName) throws IOException, ClassNotFoundException {
        //包转绝对路径
        String packageDir = packageName.replace('.', '/');
        URL url = Thread.currentThread().getContextClassLoader().getResource(packageDir);
        String filePath = url.getFile();
        File file = new File(filePath);
        // 绝对路径长度
        ABSOLUTE_PATH_LEN = filePath.length() - packageDir.length() - 1;
        Set<Class<?>> classSet = scanFileDir(file);
        //移除空值
        classSet.remove(null);
        return classSet;
    }

    /**
     * 递归扫描类文件
     * @param file
     * @return
     * @throws IOException
     */
    private static Set<Class<?>> scanFileDir(File file) throws IOException, ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<>();
        if(file.isFile()){
            classes.add(getClassObject(file));
            return classes;
        }
        File[] files = file.listFiles();
        for (File f : files) {
            if(file.isFile()){
                classes.add(getClassObject(file));
            }else{
                Set<Class<?>> classSet = scanFileDir(f);
                classes.addAll(classSet);
            }
        }
        return classes;
    }

    /**
     * 根据引用路径生成Class
     * @param file
     * @return
     */
    private static Class<?> getClassObject(File file) {
        String absolutePath = file.getAbsolutePath();
        String sub = absolutePath.substring(ABSOLUTE_PATH_LEN);
        String className = sub.replace(File.separator, ".");
        className = className.substring(0,className.length()-6);
        try{
            //main运行无法读取Tomcat HttpServlet
            if(className.contains("Servlet")){
                return null;
            }
            Class<?> aClass = Class.forName(className);
            return aClass;
        }catch (Exception ex){
            //略过无法被加载Class
            ex.printStackTrace();
        }
        return null;
    }

    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static  Object getBean(String id) {
        return map.get(id);
    }

}
