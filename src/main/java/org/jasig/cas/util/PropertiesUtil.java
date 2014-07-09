/*
 * Copyright (c) 2012 Sohu. All Rights Reserved
 */
package org.jasig.cas.util;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertiesFactoryBean;

/**
 * <p>
 * Description: 
 * </p>
 * @author Tony Wei
 * @version 1.0
 * @Date Jul 7, 2014
 */
public class PropertiesUtil {
    
    private static Properties instance;
    
    private static byte[] lock=new byte[1];
    
    public static Properties getInstance(){
        
        if(instance==null){
            synchronized (lock) {
                instance=new Properties();
                try {
                    instance.load(PropertiesUtil.class.getClassLoader().getResourceAsStream("validation.properties"));
                } catch (IOException e) {
                    e.printStackTrace(); 
                }
            }
        }
        
        return instance;
        
    }

}

    