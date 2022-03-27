package com.liquidms;


import com.liquidms.LiquidMS;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.Servlet;

public class JettyServer {

    public Server server;

    public void init() throws Exception {

        int maxThreads = 100;
        int minThreads = 10;
        int idleTimeout = 120;

        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);
        server = new Server(threadPool);

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(LiquidMS.port);
        server.setConnectors(new Connector[]{connector});
    }

    public void start() throws Exception {
        server.start();
        server.join();
    }

    public void addServletWithMapping(Class<? extends Servlet> servlet, String url) {
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);
        servletHandler.addServletWithMapping(servlet, url);
    }
}