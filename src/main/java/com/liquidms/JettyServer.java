package com.liquidms;


import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.Servlet;
import java.net.URL;

public class JettyServer {

    public Server server;

    public void init() throws Exception {

        int maxThreads = 100;
        int minThreads = 10;
        int idleTimeout = 120;

        QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads, idleTimeout);
        server = new Server(threadPool);
        if (LiquidMS.https) {
            final HttpConfiguration httpConfiguration = new HttpConfiguration();
            httpConfiguration.setSecureScheme("https");
            httpConfiguration.setSecurePort(LiquidMS.port);

            URL resourcePath = JettyServer.class.getResource("/mykey.jks");
            System.out.println("*** HTTPS keystore.jks path:" + resourcePath.getPath());
            final SslContextFactory sslContextFactory = new SslContextFactory(resourcePath.getPath());
            sslContextFactory.setKeyStorePassword("LiquidMS");
            final HttpConfiguration httpsConfiguration = new HttpConfiguration(httpConfiguration);
            httpsConfiguration.addCustomizer(new SecureRequestCustomizer());
            final ServerConnector httpsConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(httpsConfiguration));
            System.out.println("*** HTTPS port:" + LiquidMS.port);
            httpsConnector.setPort(LiquidMS.port);
            server.addConnector(httpsConnector);
        } else {
            System.out.println("*** HTTP port:" + LiquidMS.port);
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(LiquidMS.port);
            server.setConnectors(new Connector[]{connector});
        }
    }

    public void start(boolean waitForEnding) throws Exception {
        server.start();
        if (waitForEnding)
            server.join();
    }

    public void addServletWithMapping(Class<? extends Servlet> servlet, String url) {
        ServletHandler servletHandler = new ServletHandler();
        server.setHandler(servletHandler);
        servletHandler.addServletWithMapping(servlet, url);
    }
}