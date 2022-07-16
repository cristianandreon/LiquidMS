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
    public ArrayList<String> processArgs;
    public String error;

    public int pause = 250;
    public int counter = 0;
    public int maxCounter = 30;

    public boolean run = false;

    Process p = null;
    BufferedReader errStream = null;
    BufferedReader inStream = null;


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
                            System.out.print("Running Micorservice..");
                        } else {
                            System.out.print("Restaring Micorservice..");
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
                            System.out.println("FAILED");
                        } else {
                            System.out.println("OK");
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

/*
"/home/ubuntu/.jdks/corretto-11.0.13/bin/java" \
-Dfile.encoding=UTF-8 \
-classpath \
"/home/ubuntu/IdeaProjects/LiquidMS-demo/out/production/LiquidMS-demo:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/activation.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/apache-commons-lang.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/commons-codec-1.14.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/ganymed-ssh2-262.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/gson-2.8.2.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javassist.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javax.activation.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javax.xml-1.3.4.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javax.xml.bind.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/jsch-0.1.55.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/jsoup-1.13.1.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/mail-1.4.7.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/mssql-jdbc-8.2.2.jre13.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/mysql-connector-java-8.0.19.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/ojdbc8.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/postgresql-42.2.6.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/json-20211205.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javax.servlet.jsp-3.1.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javax.servlet-api-3.1.0.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/commons-logging-1.2.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/commons-pool2-2.6.0.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/commons-dbcp2-2.9.0.jar:/home/ubuntu/IdeaProjects/LiquidMS-demo/out/production/Liquid:/home/ubuntu/IdeaProjects/LiquidMS/target/classes:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-server/9.4.3.v20170317/jetty-server-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-http/9.4.3.v20170317/jetty-http-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-util/9.4.3.v20170317/jetty-util-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-io/9.4.3.v20170317/jetty-io-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-servlet/9.4.3.v20170317/jetty-servlet-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-security/9.4.3.v20170317/jetty-security-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/org/apache/logging/log4j/log4j-api/2.13.3/log4j-api-2.13.3.jar:/home/ubuntu/.m2/repository/org/apache/logging/log4j/log4j-core/2.13.3/log4j-core-2.13.3.jar:/home/ubuntu/.m2/repository/org/apache/logging/log4j/log4j-slf4j-impl/2.13.3/log4j-slf4j-impl-2.13.3.jar:/home/ubuntu/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar:/home/ubuntu/IdeaProjects/LiquidMS/libs/jna-jpms-5.10.0.jar:/home/ubuntu/IdeaProjects/LiquidMS/libs/slf4j-api-1.7.36.jar:/home/ubuntu/IdeaProjects/LiquidMS/libs/oshi-core-java11-6.1.5.jar:/home/ubuntu/IdeaProjects/LiquidMS/libs/jna-platform-jpms-5.10.0.jar:/snap/intellij-idea-ultimate/348/lib/idea_rt.jar" \
com.customer.app.Main

/home/ubuntu/IdeaProjects/LiquidMS-demo/out/production/LiquidMS-demo:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/activation.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/apache-commons-lang.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/commons-codec-1.14.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/ganymed-ssh2-262.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/gson-2.8.2.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javassist.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javax.activation.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javax.xml-1.3.4.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javax.xml.bind.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/jsch-0.1.55.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/jsoup-1.13.1.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/mail-1.4.7.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/mssql-jdbc-8.2.2.jre13.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/mysql-connector-java-8.0.19.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/ojdbc8.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/postgresql-42.2.6.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/json-20211205.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javax.servlet.jsp-3.1.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/javax.servlet-api-3.1.0.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/commons-logging-1.2.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/commons-pool2-2.6.0.jar:/home/ubuntu/IdeaProjects/Liquid/WEB-INF/lib/commons-dbcp2-2.9.0.jar:/home/ubuntu/IdeaProjects/LiquidMS-demo/out/production/Liquid:/home/ubuntu/IdeaProjects/LiquidMS/target/classes:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-server/9.4.3.v20170317/jetty-server-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-http/9.4.3.v20170317/jetty-http-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-util/9.4.3.v20170317/jetty-util-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-io/9.4.3.v20170317/jetty-io-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-servlet/9.4.3.v20170317/jetty-servlet-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/org/eclipse/jetty/jetty-security/9.4.3.v20170317/jetty-security-9.4.3.v20170317.jar:/home/ubuntu/.m2/repository/org/apache/logging/log4j/log4j-api/2.13.3/log4j-api-2.13.3.jar:/home/ubuntu/.m2/repository/org/apache/logging/log4j/log4j-core/2.13.3/log4j-core-2.13.3.jar:/home/ubuntu/.m2/repository/org/apache/logging/log4j/log4j-slf4j-impl/2.13.3/log4j-slf4j-impl-2.13.3.jar:/home/ubuntu/.m2/repository/org/slf4j/slf4j-api/1.7.25/slf4j-api-1.7.25.jar:/home/ubuntu/IdeaProjects/LiquidMS/libs/jna-jpms-5.10.0.jar:/home/ubuntu/IdeaProjects/LiquidMS/libs/slf4j-api-1.7.36.jar:/home/ubuntu/IdeaProjects/LiquidMS/libs/oshi-core-java11-6.1.5.jar:/home/ubuntu/IdeaProjects/LiquidMS/libs/jna-platform-jpms-5.10.0.jar:/snap/intellij-idea-ultimate/348/lib/idea_rt.jar
*/
