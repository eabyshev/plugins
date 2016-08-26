package io.subutai.plugin.hadoop.impl.handler;


import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import io.subutai.common.tracker.TrackerOperation;
import io.subutai.core.metric.api.Monitor;
import io.subutai.core.plugincommon.api.PluginDAO;
import io.subutai.core.strategy.api.StrategyManager;
import io.subutai.core.template.api.TemplateManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.hadoop.impl.HadoopImpl;
import io.subutai.plugin.hadoop.impl.HadoopWebModule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@Ignore
public class AddOperationHandlerTest
{
    AddOperationHandler addOperationHandler;
    TrackerOperation trackerOperation;
    UUID uuid;
    ExecutorService executorService;
    Tracker tracker;
    Monitor monitor;
    HadoopWebModule webModule;

    @Mock
    PluginDAO pluginDAO;

    @Mock
    private StrategyManager strategyManager;

    @Mock
    TemplateManager templateManager;


    @Before
    public void setUp()
    {
        executorService = mock( ExecutorService.class );
        trackerOperation = mock( TrackerOperation.class );
        monitor = mock( Monitor.class );
        uuid = new UUID( 50, 50 );
        tracker = mock( Tracker.class );
        webModule = mock (HadoopWebModule.class);

        String clusterName = "test";
        HadoopImpl hadoop = new HadoopImpl( strategyManager, monitor, pluginDAO, webModule );
        when( trackerOperation.getId() ).thenReturn( uuid );
        when( tracker.createTrackerOperation( anyString(), anyString() ) ).thenReturn( trackerOperation );
        hadoop.setTracker( tracker );
        hadoop.setExecutor( executorService );
        addOperationHandler = new AddOperationHandler( hadoop, templateManager, clusterName, 5 );

        assertEquals( uuid, trackerOperation.getId() );
        assertEquals( tracker, hadoop.getTracker() );
        assertEquals( executorService, hadoop.getExecutor() );
    }


    @Test
    public void testRun()
    {
        HadoopImpl hadoop = new HadoopImpl( strategyManager, monitor, pluginDAO, webModule );
        when( trackerOperation.getId() ).thenReturn( uuid );
        when( tracker.createTrackerOperation( anyString(), anyString() ) ).thenReturn( trackerOperation );
        hadoop.setTracker( tracker );
        hadoop.setExecutor( executorService );
        addOperationHandler.run();

        assertEquals( uuid, trackerOperation.getId() );
        assertEquals( tracker, hadoop.getTracker() );
        assertEquals( executorService, hadoop.getExecutor() );
    }
}