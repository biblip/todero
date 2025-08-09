package com.social100.todero;

import com.social100.processor.beans.BeanFactory;
import com.social100.todero.aiaserver.AIAServer;
import com.social100.todero.common.config.AppConfig;
import com.social100.todero.common.config.ServerType;
import com.social100.todero.common.log.LogRedirector;
import com.social100.todero.console.senses.SensesClient;
import com.social100.todero.server.RawServer;

import java.io.IOException;

import static com.social100.todero.common.config.Utils.loadAppConfig;

public class AIAServerMain {
    static private AppConfig appConfig;
    //static private AppConfig appConfig_aia;

    public static void main(String[] args) throws Exception {
        try {
            LogRedirector.initialize();
        } catch (IOException e) {
            e.printStackTrace(); // fallback to console
        }

        appConfig = loadAppConfig(args);
        RawServer aiaServer_ai = new AIAServer(appConfig, ServerType.AI);
        aiaServer_ai.start();

        RawServer aiaServer_aia = new AIAServer(appConfig, ServerType.AIA);
        aiaServer_aia.start();

        BeanFactory.registerBeanClass(SensesClient.class);
        SensesClient sensesClient = BeanFactory.getBean(SensesClient.class);
        sensesClient.start();

        //RawServer sshServer = new SshServer(appConfig);
        //sshServer.start();
    }
}
