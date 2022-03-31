/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liquidms;

import com.liquid.utility;
import com.liquid.workspace;
import oshi.software.os.OSProcess;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manage the clients connections
 */
public class LooperThread extends Thread {

    public class LooperEvent {
        String name;
        String className;
        public String methodName;
        Class<?> cls;
        Method m;
        Object clsInstance;
        long delay;
        long interval;
        long maxExec;

        long execCounter;
        long last_time;
    }


    public ArrayList<LooperEvent> looperEvents;
    public String error;

    public int pause = 250;

    public boolean run = false;

    public void run() {

        run = true;

        while(run) {

            try {

                for (LooperEvent lc:looperEvents) {

                    long ctime = System.currentTimeMillis();

                    if(lc != null) {
                        // exec
                        if (lc.clsInstance != null) {
                            boolean exec = false;

                            if(lc.delay > 0) {
                                // delay start
                                if(ctime - lc.last_time >= lc.delay) {
                                    exec = true;
                                }
                            } else if(lc.interval > 0) {
                                // periodic
                                if(ctime - lc.last_time >= lc.interval) {
                                    if(lc.maxExec > 0) {
                                        if (lc.execCounter < lc.maxExec) {
                                            exec = true;
                                        }
                                    } else {
                                        exec = true;
                                    }
                                }
                            } else {
                                // max exec
                                if(lc.execCounter < lc.maxExec) {
                                    exec = true;
                                } else {
                                    if(lc.execCounter == 0) {
                                        exec = true;
                                    }
                                }
                            }

                            if(exec) {
                                try {
                                    lc.m.invoke(lc);
                                } catch (Throwable th) {
                                    Logger.getLogger(LooperThread.class.getName()).log(Level.SEVERE, "ERROR in event '"+lc.name+"' : "+th.getMessage());
                                }
                                lc.execCounter++;
                            }
                        }
                    }
                }

                Thread.sleep(pause);

            } catch (Exception ex) {
                error = ex.getLocalizedMessage();
                Logger.getLogger(LooperThread.class.getName()).log(Level.SEVERE, "ERROR in event looper : "+ex.getMessage());
                try {
                    Thread.sleep(pause);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Add a class to be execute on event
     *
     * @param name
     * @param className
     * @param method
     * @param delay_msec
     * @param interval_msec
     * @param maxExec
     * @return
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    boolean addEvent(String name, String className, String method, long delay_msec, long interval_msec, long maxExec) throws NoSuchMethodException, InstantiationException, IllegalAccessException {
        // init
        if(name != null && !name.isEmpty()) {
            LooperEvent lc = new LooperEvent();
            lc.className = className;
            if (lc.className != null) {
                lc.m = lc.cls.getMethod(lc.methodName, Object.class);
                lc.clsInstance = (Object) lc.cls.newInstance();
                lc.last_time = System.currentTimeMillis();
            }
        }
        return false;
    }
}
