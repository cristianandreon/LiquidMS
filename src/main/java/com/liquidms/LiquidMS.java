package com.liquidms;

import com.liquid.connectionPool;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import javax.servlet.Servlet;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class LiquidMS {


    static public String version = "1.01";
    static public int port = 8090;


    static WatchDogThread wd = null;
    static LooperThread lt = null;




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

    static boolean run = true;
    static connectionPool ds = null;


    public static class servlet {
        Class<?> cls;
        String url;

        servlet(Class<?> cls, String url) throws Exception {
            this.cls = cls;
            this.url = url;
        }
    }

    static private ArrayList<servlet> servletList = new ArrayList<servlet>();

    public static void addServlet(Class<?> cls, String url) throws Exception {
        servletList.add( new servlet(cls, url) );
    }

    private static void registerAllServlet(JettyServer js) throws Exception {
        for (servlet s: servletList) {
            js.addServletWithMapping((Class<? extends Servlet>) s.cls, s.url);
        }
    }

    static public boolean addEvent(String name, String className, String method, long delay_msec, long interval_msec, long maxExec) throws NoSuchMethodException, InstantiationException, IllegalAccessException {
        return lt.addEvent( name, className, method, delay_msec, interval_msec, maxExec);
    }

    public static void run(String[] args) throws Exception {

        System.out.println(
                "██╗     ██╗ ██████╗ ██╗   ██╗██╗██████╗                                                    \n" +
                        "██║     ██║██╔═══██╗██║   ██║██║██╔══██╗                                                   \n" +
                        "██║     ██║██║   ██║██║   ██║██║██║  ██║                                                   \n" +
                        "██║     ██║██║▄▄ ██║██║   ██║██║██║  ██║                                                   \n" +
                        "███████╗██║╚██████╔╝╚██████╔╝██║██████╔╝                                                   \n" +
                        "╚══════╝╚═╝ ╚══▀▀═╝  ╚═════╝ ╚═╝╚═════╝                                                    \n" +
                        "                                                                                           \n" +
                        "███╗   ███╗██╗ ██████╗██████╗  ██████╗ ███████╗███████╗██████╗ ██╗   ██╗██╗ ██████╗███████╗\n" +
                        "████╗ ████║██║██╔════╝██╔══██╗██╔═══██╗██╔════╝██╔════╝██╔══██╗██║   ██║██║██╔════╝██╔════╝\n" +
                        "██╔████╔██║██║██║     ██████╔╝██║   ██║███████╗█████╗  ██████╔╝██║   ██║██║██║     █████╗  \n" +
                        "██║╚██╔╝██║██║██║     ██╔══██╗██║   ██║╚════██║██╔══╝  ██╔══██╗╚██╗ ██╔╝██║██║     ██╔══╝  \n" +
                        "██║ ╚═╝ ██║██║╚██████╗██║  ██║╚██████╔╝███████║███████╗██║  ██║ ╚████╔╝ ██║╚██████╗███████╗\n" +
                        "╚═╝     ╚═╝╚═╝ ╚═════╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚══════╝╚═╝  ╚═╝  ╚═══╝  ╚═╝ ╚═════╝╚══════╝\n" +
                        "                                                                                           "
        );

        if(args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if("-LiquidMS:run".equalsIgnoreCase(args[i])) {
                    System.out.println("Running server..");
                    JettyServer js = new JettyServer();
                    js.init();
                    registerAllServlet(js);
                    js.start();
                } else if("-LiquidMS:test".equalsIgnoreCase(args[i])) {
                    if(args.length >= 2) {
                        if(args[1] != null && !args[1].isEmpty()) {
                            if("-test".equalsIgnoreCase(args[1])) {
                                // testServer();
                            }
                        }
                    }
                }
            }
        }

        // run watch dog
        System.out.println("Running watchdog..");
        WatchDogThread wd = new WatchDogThread();
        wd.start();

        // run watch dog
        System.out.println("Running looper..");
        LooperThread lt = new LooperThread();
        lt.start();


        while(run) {
            Thread.sleep(250);
        }
    }

    /*
    static public void testServer() throws URISyntaxException, IOException, InterruptedException {
        String url = "http://localhost:8090/status";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .headers("Host", "localhost", "Accept", "*")
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }
    */

}
