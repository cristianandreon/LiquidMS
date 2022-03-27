/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.liquidms;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Manage the clients connections
 */
public class WatchDogThread extends Thread {

    public String processName;
    public String processPath;
    public ArrayList<String> processArgs;
    public String error;

    public int pause = 5000;
    public int counter = 0;
    public int maxCounter = 3;

    public boolean run = false;

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
                            System.out.println("Running Micorservice..");
                        } else {
                            System.out.println("Restaring Micorservice..");
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
                        Process p = com.liquid.utility.startProcess( processPath, processArgs.toArray(new String[0]) );
                        Thread.sleep(1000);
                        if(!p.isAlive()) {
                            System.err.println("premature process end .. exit code:"+p.exitValue());
                            BufferedReader inStream = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                            if (inStream != null) {
                                String line = null;
                                while (inStream.ready()) {
                                    line = inStream.readLine();
                                    if (line != null) {
                                        System.err.print("[ChildProcess]" + line);
                                    }
                                }
                            }
                        } else {
                            System.out.println("Micorservice active..");
                        }
                        counter = 1;
                    } else {
                        counter++;
                    }
                } else {
                    counter = 1;
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
