package org.safehaus.subutai.plugin.hadoop.impl.handler;


import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.metric.api.Monitor;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.hadoop.impl.HadoopImpl;

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

    @Before
    public void setUp()
    {
        executorService = mock(ExecutorService.class);
        trackerOperation = mock(TrackerOperation.class);
        monitor = mock( Monitor.class );
        uuid = new UUID(50, 50);
        tracker = mock(Tracker.class);

        String clusterName = "test";
        HadoopImpl hadoop = new HadoopImpl( monitor );
        when(trackerOperation.getId()).thenReturn(uuid);
        when(tracker.createTrackerOperation(anyString(), anyString())).thenReturn(trackerOperation);
        hadoop.setTracker(tracker);
        hadoop.setExecutor(executorService);
        addOperationHandler = new AddOperationHandler(hadoop, clusterName, 5);

        assertEquals(uuid, trackerOperation.getId());
        assertEquals(tracker, hadoop.getTracker());
        assertEquals(executorService, hadoop.getExecutor());

    }

    @Test
    public void testRun()
    {
        HadoopImpl hadoop = new HadoopImpl( monitor );
        when(trackerOperation.getId()).thenReturn(uuid);
        when(tracker.createTrackerOperation(anyString(), anyString())).thenReturn(trackerOperation);
        hadoop.setTracker(tracker);
        hadoop.setExecutor(executorService);
        addOperationHandler.run();

        assertEquals(uuid, trackerOperation.getId());
        assertEquals(tracker, hadoop.getTracker());
        assertEquals(executorService, hadoop.getExecutor());
    }
}