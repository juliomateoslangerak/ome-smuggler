package ome.smuggler.config;

import static util.sequence.Arrayz.asMutableList;
import ome.smuggler.config.items.HornetQPersistenceConfig;
import ome.smuggler.config.items.ImportQConfig;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.server.JournalType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.hornetq.HornetQConfigurationCustomizer;
import org.springframework.stereotype.Component;

import util.config.ConfigProvider;

/**
 * Implements the Spring Boot auto-configuration hook to customize the HornetQ
 * server configuration created by Spring Boot.
 */
@Component
public class HornetQServerCfgCustomizer implements HornetQConfigurationCustomizer {

    @Autowired
    private ConfigProvider<HornetQPersistenceConfig> persistenceConfig;
    
    @Autowired
    private ConfigProvider<ImportQConfig> importQProvider;
    
    private void configurePersistence(Configuration cfg) {
        HornetQPersistenceConfig params = persistenceConfig
                                         .defaultReadConfig()
                                         .findFirst()
                                         .get();
        
        cfg.setPersistenceEnabled(params.isPersistenceEnabled());
        cfg.setJournalType(JournalType.NIO);
        cfg.setJournalDirectory(params.getJournalDirPath());
        cfg.setLargeMessagesDirectory(params.getLargeMessagesDirPath());
        cfg.setBindingsDirectory(params.getBindingsDirPath());
        cfg.setPagingDirectory(params.getPagingDirPath());
    }
    
    private void configureQueues(Configuration cfg) {
        ImportQConfig importQ = importQProvider
                .defaultReadConfig()
                .findFirst()
                .get();
        cfg.setQueueConfigurations(asMutableList(importQ));
    }
    
    @Override
    public void customize(Configuration cfg) {
        configurePersistence(cfg);
        configureQueues(cfg);
    }

}
/* NOTES.
 * 1. HornetQ server configuration. 
 * The customize method is passed a HornetQ Configuration instance created with 
 * the values in HornetQProperties. For the details, see the source code of:
 * 
 *  - HornetQEmbeddedServerConfiguration
 *  - HornetQEmbeddedConfigurationFactory
 * 
 * HornetQ Configuration is created by Spring Boot pretty much as shown in the
 * HornetQ EmbeddedExample class.
 * 
 * 2. Performance.
 * The HornetQ docs (49.6. Avoiding Anti-Patterns) state that connections, 
 * sessions, consumers, and producers are supposed to be shared, but the
 * Spring JMS template does not. So you shouldn't use it with HornetQ...
 */