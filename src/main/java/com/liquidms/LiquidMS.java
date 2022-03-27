package com.liquidms;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.lang.management.ManagementFactory;
import java.util.List;

public class LiquidMS {

    static public String version = "1.01";
    static public int port = 8090;


    public static List<OSProcess> getOSProcesses() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        return os.getProcesses();
    }

    public static List<String> getOSProcessArgs(OSProcess osp) {
        return osp.getArguments();
    }

    public static String getOSProcessCommandLine(OSProcess osp) {
        return osp.getCommandLine();
    }

    public static int getOSProcessID(OSProcess osp) {
        return osp.getProcessID();
    }

    public static int getOSProcessParentID(OSProcess osp) {
        return osp.getParentProcessID();
    }

    public static String getOSProcessName(OSProcess osp) {
        return osp.getName();
    }

    public static String getOSProcessPath(OSProcess osp) {
        return osp.getPath();
    }

    public static int getCurrentProcessId() {
        String fullPid = ManagementFactory.getRuntimeMXBean().getName();
        String [] pidParts = fullPid.split("@");
        String spid = null;
        if(pidParts.length > 1) {
            spid = pidParts[0];
        } else {
            // Warning ?
            spid = pidParts[0];
        }
        return Integer.parseInt(spid);
    }

    public static OSProcess getOSProcessById(int pid) {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        return os.getProcess(pid);
    }

}
