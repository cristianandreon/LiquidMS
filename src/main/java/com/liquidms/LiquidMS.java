package com.liquidms;

import com.liquid.connection;
import com.liquid.wsStreamerClient;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LiquidMS implements Servlet {


    static public String version = "1.02";
    static public int port = 8090;

    static public String runMode = "";

    static WatchDogThread wd = null;
    static LooperThread lt = null;

    static String glCurrentFolder = null;



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


    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    /**
     * Dispatching servlet
     *
     * @param servletRequest
     * @param servletResponse
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        servlet s = get_servlet(servletRequest);
        if(s != null) {
            if(s.inst != null) {
                ((Servlet)s.inst).service(servletRequest, servletResponse);
                s.counter++;
            } else {
                servletResponse.getOutputStream().println("[LiquidMS] Internal Error : Missing class instance");
            }
        } else {
            servletResponse.setContentType("text/html");
            servletResponse.getOutputStream().println("[LiquidMS] : servlet not registered" + "<br/><br/>" + defaultServlet.print_servlets());
        }
    }

    private servlet get_servlet(ServletRequest servletRequest) {
        String url = ((HttpServletRequest)servletRequest).getRequestURI();
        url = url.length() > 1 && url.endsWith("/") ? url.substring(0, url.length()-1) : url;
        url = url.length() > 0 && url.startsWith("/") ? url : "/" + url;
        for (servlet sl : servletList) {
            if(sl.url.equalsIgnoreCase(url)) {
                return sl;
            }
        }
        return null;
    }


    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {

    }


    public static class servlet {
        public Object inst;
        public int counter;
        Class<?> cls;
        String url;

        servlet(Class<?> cls, String url) throws Exception {
            this.cls = cls;
            this.url = url;
        }
    }

    static private ArrayList<servlet> servletList = new ArrayList<servlet>();


    public static ArrayList<LooperThread.LooperEvent> eventList = new ArrayList<LooperThread.LooperEvent>();

    /**
     * Add a servel
     *
     * @param cls
     * @param url
     * @param update
     * @throws Exception
     */
    public static void addServlet(Class<?> cls, String url, boolean update) throws Exception {
        System.out.println("Adding servlet to localhost:"+LiquidMS.port+url);
        for (servlet sl : servletList) {
            if(sl.url.equalsIgnoreCase(url)) {
                if(update) {
                    sl.cls = cls;
                }
                return;
            }
        }
        servletList.add( new servlet(cls, url) );
    }
    /**
     * Add a servel
     * @param cls
     * @param url
     * @throws Exception
     */
    public static void addServlet(Class<?> cls, String url) throws Exception {
        addServlet(cls, url, true);
    }

    static String printServlet() {
        String out = "<table cellpadding=5 cellspacing=10 style='text-align:left'>";
        out += "<thead><tr><th>url</th><th>class</th><th>counter</th></tr></thead><tbody>";
        if(servletList != null) {
            for (servlet sl : servletList) {
                out += "<tr><td><a href='" + sl.url + "'>" + sl.url + "</a></td><td>" + sl.cls + "</td><td>" + sl.counter + "</td></tr>";
            }
        }
        return out + "</tbody></table>";
    }


    /**
     * Register all servlet to the web server
     * @param js
     * @throws Exception
     */
    private static void registerAllServlet(JettyServer js) throws Exception {
        for (servlet s: servletList) {
            js.addServletWithMapping((Class<? extends Servlet>) LiquidMS.class, s.url);
            s.inst = s.cls.newInstance();
        }
    }

    /**
     * Add a class to be executed on event
     *
     * @param name
     * @param className
     * @param methodName
     * @param delay_msec
     * @param interval_msec
     * @param maxExec
     * @return
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    static public boolean addEvent(String name, String className, String methodName, long delay_msec, long interval_msec, long maxExec) throws NoSuchMethodException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        if(name != null && !name.isEmpty()) {
            LooperThread.LooperEvent lc = new LooperThread.LooperEvent();
            if (className != null && !className.isEmpty()) {
                try {
                    lc.name = name;
                    lc.cls = Class.forName(className);
                    if (methodName != null && !methodName.isEmpty()) {
                        System.out.print("Adding event to "+className+"."+methodName+"..");
                        lc.m = lc.cls.getMethod(methodName, Object.class);
                        lc.clsInstance = (Object) lc.cls.newInstance();
                        lc.delay = delay_msec;
                        lc.interval = interval_msec;
                        lc.maxExec = maxExec;
                        lc.last_time = System.currentTimeMillis();
                        eventList.add(lc);
                        System.out.println("OK");
                        return true;
                    }
                } catch (Throwable th) {
                    Logger.getLogger(wsStreamerClient.class.getName()).log(Level.SEVERE, "ERROR : cannot add event .. "+th.getMessage());
                    System.out.println("FAILED");
                }
            }
        }
        return false;
    }

    static String printLoopers() {
        String out = "<table cellpadding=5 cellspacing=10 style='text-align:left'>";
        out += "<thead><tr><th>name</th><th>class</th><th>interval</th><th>max execs</th><th>delay</th><th>counter</th></tr></thead><tbody>";
        if(eventList != null) {
            for (LooperThread.LooperEvent evt : eventList) {
                out += "<tr><td>" + evt.name + "</td><td>" + evt.clsInstance.getClass().getName() + "</td><td>" + evt.interval + "</td><td>" + evt.maxExec + "</td><td>" + evt.delay + "</td><td>" + evt.execCounter + "</<td></tr>";
            }
        }
        return out + "</tbody></table>";
    }


    static String printDatasources() {
        String out = "<table cellpadding=5 cellspacing=10 style='text-align:left'>";
        out += "<thead><tr><th>driver</th><th>host</th><th>port</th><th>database</th><th>user</th><th>service</th></tr></thead><tbody>";
        if(com.liquid.connection.jdbcSources != null) {
            for (com.liquid.connection.JDBCSource ds : com.liquid.connection.jdbcSources) {
                out += "<tr><td>" + (ds.driver != null ? ds.driver: "[N/D]")
                        + "</td><td>" + (ds.host != null ? ds.driver: "[locahost]")
                        + "</td><td>" + (ds.port != null ? ds.driver: "[default]")
                        + "</td><td>" + (ds.database != null ? ds.driver: "[N/D]")
                        + "</td><td>" + (ds.user != null ? ds.driver: "[N/D]")
                        + "</td><td>" + (ds.service != null ? ds.driver: "[N/D]")
                        + "</<td></tr>";
            }
        }
        return out + "</tbody></table>";
    }


    public static void run(String[] args) throws Exception {

        boolean runFrontEnd = false;
        boolean runWatchdog = true;
        boolean runLooper = false;

        glCurrentFolder = Paths.get("").toAbsolutePath().toString();
        System.out.println("Current folder:"+glCurrentFolder);

        runMode = "Controller";

        if(args.length > 0) {

            for (int i = 0; i < args.length; i++) {
                if("-LiquidMS:run".equalsIgnoreCase(args[i])) {
                    // Esecuzione dal watchdog
                    runFrontEnd = true;
                    runWatchdog = false;
                    runLooper = true;
                    runMode = "Run";
                } else if("-LiquidMS:debug".equalsIgnoreCase(args[i])) {
                    // Esecuzione dal debugger
                    runFrontEnd = true;
                    runWatchdog = false;
                    runLooper = true;
                    runMode = "Debug";
                }
            }
        }

        if("Controller".equalsIgnoreCase(runMode) || "Debug".equalsIgnoreCase(runMode)) {
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
        }

        System.out.println("Mode:"+runMode);

        if(runFrontEnd) {
            // run server
            System.out.print("Running server..");

            // Add default servlet if not defined
            addServlet(defaultServlet.class, "/", false);

            JettyServer js = new JettyServer();
            js.init();
            registerAllServlet(js);
            js.start(false);
            System.out.println("OK");
        }

        // run looper
        if(runLooper) {
            System.out.print("Running looper..");
            lt = new LooperThread();
            lt.start();
            System.out.println("OK");
        }

        // run watch dog
        if(runWatchdog) {
            System.out.print("Running watchdog..");
            wd = new WatchDogThread();
            wd.start();
            System.out.println("OK");
        }


        // prevent program exit
        while(run) {
            Thread.sleep(5000);
        }
    }
}


