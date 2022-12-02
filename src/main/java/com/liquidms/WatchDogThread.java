/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liquidms;

import oshi.software.os.OSProcess;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


/**
 * Manage the clients connections
 */
public class WatchDogThread extends Thread {

    public String processName;
    public String processPath;
    public String processFullPath;

    public ArrayList<String> processArgs;
    public String error;

    public int pause = 250;
    public int counter = 0;
    public int maxCounter = 30;

    public boolean run = false;

    Process p = null;
    BufferedReader errStream = null;
    BufferedReader inStream = null;
    private boolean debug = false;


    public void run() {

        run = true;

        if(processName == null || processName.isEmpty()) {
            OSProcess cosp = LiquidMS.getOSProcessById(LiquidMS.getCurrentProcessId());
            String cmdLine = LiquidMS.getOSProcessCommandLine(cosp);
            List<String> args = LiquidMS.getOSProcessArgs(cosp);
            processName = args.get(0).substring(args.get(0).lastIndexOf(File.separator)+1);
        }


        while(run) {

            try {

                List<OSProcess> pss = LiquidMS.getOSProcesses();
                boolean processFound = false;
                for (OSProcess ps:pss) {
                    String pn = LiquidMS.getOSProcessName(ps);
                    if(pn.equalsIgnoreCase(processName)) {
                        List<String> args = LiquidMS.getOSProcessArgs(ps);
                        for (String arg:args) {
                            if("-LiquidMS:run".equalsIgnoreCase(arg)) {
                                processFound = true;
                                break;
                            }
                        }
                    }
                }

                if(!processFound) {
                    if(counter > maxCounter || counter == 0) {
                        if(counter == 0) {
                            System.out.println("Running microservice..");
                        } else {
                            System.out.println("Restaring microservice..");
                        }
                        if(processPath == null) {
                            OSProcess cosp = LiquidMS.getOSProcessById(LiquidMS.getCurrentProcessId());
                            if(cosp != null) {
                                List<String> args = LiquidMS.getOSProcessArgs(cosp);
                                processPath = args.get(0);
                                processArgs = new ArrayList<String>();
                                for (int i = 1; i < args.size(); i++) {
                                    processArgs.add(args.get(i));
                                }
                                processArgs.add("-LiquidMS:run");

                                // This will perform garbage collection inline with the thread allocating the heap memory instead of a dedicated GC thread(s)
                                processArgs.add("XX:+UseSerialGC");

                                // This will limit each threads stack memory to 512KB instead of the default 1MB
                                processArgs.add("-Xss512k");

                                // This will restrict the JVM's calculations for the heap and non heap managed memory to be within the limits of this value.
                                processArgs.add("-XX:MaxRAM=72m");
                            }
                        }

                        // Add full path to jar
                        String jarFileName = processArgs.get(1);
                        if(jarFileName.indexOf(".jar") > 0) {
                            System.out.println("JAR file found at args[1] .. appending full path ");
                            processFullPath = LiquidMS.glCurrentFolder + File.separator + processArgs.get(1);
                            processArgs.set(1, processFullPath);
                        } else {
                            System.out.println("JAR file not found at args[1] .. running from IDE");
                        }


                        if(debug) {
                            System.out.println("command line args:");
                            for (int i = 0; i < processArgs.size(); i++) {
                                System.out.println("" + (i + 1) + " : " + processArgs.get(i));
                            }
                        }

                        p = com.liquid.utility.startProcess( processPath, processArgs.toArray(new String[0]) );

                        errStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                        inStream = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        Thread.sleep(1000);
                        if(!p.isAlive()) {
                            System.err.println("premature process end .. exit code:"+p.exitValue());
                            if (errStream != null) {
                                String line = null;
                                while (errStream.ready()) {
                                    line = errStream.readLine();
                                    if (line != null) {
                                        System.err.print("[ChildProcess]" + line);
                                    }
                                }
                            }
                            p = null;
                            try {
                                if (errStream != null) {
                                    errStream.close();
                                }
                            } catch (Throwable th) {}
                            try {
                                if (inStream != null) {
                                    inStream.close();
                                }
                            } catch (Throwable th) {}
                            System.out.println("Watchdog ERROR: process not alive");
                        } else {
                        }
                        counter = 1;

                    } else {
                        counter++;
                    }
                } else {
                    counter = 1;
                }

                if(p != null) {
                    if (p.isAlive()) {
                        if (errStream != null) {
                            while(errStream.ready()) {
                                System.err.println("[WD.ERR]-"+errStream.readLine());
                            }
                        }
                        if (inStream != null) {
                            while(inStream.ready()) {
                                System.out.println("[WD]-"+inStream.readLine());
                            }
                        }
                    }
                }

                Thread.sleep(pause);

            } catch (Exception ex) {
                error = ex.getLocalizedMessage();
                System.err.println(ex);
                try {
                    Thread.sleep(pause);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

