package io.subutai.plugin.cassandra.cli;


import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.cassandra.api.Cassandra;
import io.subutai.plugin.cassandra.api.CassandraClusterConfig;
import io.subutai.plugin.cassandra.cli.CheckAllNodesCommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckAllNodesCommandTest
{
    private CheckAllNodesCommand checkAllNodesCommand;
    @Mock
    Cassandra cassandra;
    @Mock
    Tracker tracker;
    @Mock
    TrackerOperationView trackerOperationView;

    @Before
    public void setUp() 
    {
        checkAllNodesCommand = new CheckAllNodesCommand();
    }

    @Test
    public void testGetCassandraManager() 
    {
        checkAllNodesCommand.setCassandraManager(cassandra);
        checkAllNodesCommand.getCassandraManager();

        // assertions
        assertNotNull(checkAllNodesCommand.getCassandraManager());
        assertEquals(cassandra,checkAllNodesCommand.getCassandraManager());
    }

    @Test
    public void testSetCassandraManager() 
    {
        checkAllNodesCommand.setCassandraManager(cassandra);
        checkAllNodesCommand.getTracker();

        // assertions
        assertNotNull(checkAllNodesCommand.getCassandraManager());
        assertEquals(cassandra,checkAllNodesCommand.getCassandraManager());
    }

    @Test
    public void testGetTracker() 
    {
        checkAllNodesCommand.setTracker(tracker);
        checkAllNodesCommand.getTracker();

        // assertions
        assertNotNull(checkAllNodesCommand.getTracker());
        assertEquals(tracker,checkAllNodesCommand.getTracker());
    }

    @Test
    public void testSetTracker() 
    {
        checkAllNodesCommand.setTracker(tracker);
        checkAllNodesCommand.getTracker();

        // assertions
        assertNotNull(checkAllNodesCommand.getTracker());
        assertEquals(tracker, checkAllNodesCommand.getTracker());

    }

    @Test
    public void testDoExecute() throws IOException
    {
        UUID uuid = new UUID(50,50);
        checkAllNodesCommand.setTracker(tracker);
        checkAllNodesCommand.setCassandraManager(cassandra);
        when(cassandra.checkCluster(anyString())).thenReturn(uuid);
        when(tracker.getTrackerOperation(CassandraClusterConfig.PRODUCT_KEY, uuid)).thenReturn(trackerOperationView);
        when(trackerOperationView.getLog()).thenReturn("test");

        checkAllNodesCommand.doExecute();

        // assertions
        assertNotNull(tracker.getTrackerOperation(CassandraClusterConfig.PRODUCT_KEY,uuid));
        verify(cassandra).checkCluster(anyString());
        assertNotEquals(OperationState.RUNNING,trackerOperationView.getState());
    }

    @Test
    public void testDoExecuteWhenTrackerOperationViewIsNull() throws IOException
    {
        UUID uuid = new UUID(50,50);
        checkAllNodesCommand.setTracker(tracker);
        checkAllNodesCommand.setCassandraManager(cassandra);
        when(cassandra.checkCluster(anyString())).thenReturn(uuid);
        when(tracker.getTrackerOperation(CassandraClusterConfig.PRODUCT_KEY,uuid)).thenReturn(null);

        checkAllNodesCommand.doExecute();

        // assertions
        assertNull(tracker.getTrackerOperation(CassandraClusterConfig.PRODUCT_KEY,uuid));
        verify(cassandra).checkCluster(anyString());
    }

}