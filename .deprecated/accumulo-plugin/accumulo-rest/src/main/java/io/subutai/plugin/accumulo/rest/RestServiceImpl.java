package io.subutai.plugin.accumulo.rest;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.google.common.base.Preconditions;

import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.common.util.JsonUtil;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.accumulo.api.Accumulo;
import io.subutai.plugin.accumulo.api.AccumuloClusterConfig;
import io.subutai.core.plugincommon.api.ClusterException;
import io.subutai.core.plugincommon.api.NodeType;
import io.subutai.plugin.hadoop.api.Hadoop;


/**
 * REST implementation of Accumulo API
 */

public class RestServiceImpl implements RestService
{

    private Accumulo accumuloManager;
    private Hadoop hadoop;
    private Tracker tracker;


    public RestServiceImpl( final Accumulo accumuloManager )
    {
        this.accumuloManager = accumuloManager;
    }


    @Override
    public Response listClusters()
    {
        List<AccumuloClusterConfig> configs = accumuloManager.getClusters();
        List<String> clusterNames = new ArrayList<>();
        for ( AccumuloClusterConfig config : configs )
        {
            clusterNames.add( config.getClusterName() );
        }
        String clusters = JsonUtil.toJson( clusterNames );
        return Response.status( Response.Status.OK ).entity( clusters ).build();
    }


    @Override
    public Response getCluster( final String clusterName )
    {
        AccumuloClusterConfig config = accumuloManager.getCluster( clusterName );
        if ( config == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found " ).build();
        }
        String cluster = JsonUtil.toJson( accumuloManager.getCluster( clusterName ) );
        return Response.status( Response.Status.OK ).entity( cluster ).build();
    }


    @Override
    public Response installCluster( final String config )
    {
        TrimmedAccumuloConfig trimmedAccumuloConfig = JsonUtil.fromJson( config, TrimmedAccumuloConfig.class );
        AccumuloClusterConfig expandedConfig = new AccumuloClusterConfig();
        expandedConfig.setClusterName( trimmedAccumuloConfig.getClusterName() );
        expandedConfig.setInstanceName( trimmedAccumuloConfig.getInstanceName() );
        expandedConfig.setPassword( trimmedAccumuloConfig.getPassword() );
        expandedConfig.setHadoopClusterName( trimmedAccumuloConfig.getHadoopClusterName() );
        expandedConfig.setZookeeperClusterName( trimmedAccumuloConfig.getZkClusterName() );
        expandedConfig.setMasterNode( trimmedAccumuloConfig.getMasterNode() );
        expandedConfig.setGcNode( trimmedAccumuloConfig.getGcNode() );
        expandedConfig.setMonitor( trimmedAccumuloConfig.getMonitor() );

        Set<String> tracers = new HashSet<>();
        Set<String> slaves = new HashSet<>();
        for ( String tracer : trimmedAccumuloConfig.getTracers() )
        {
            tracers.add( tracer );
        }
        for ( String slave : trimmedAccumuloConfig.getSlaves() )
        {
            slaves.add( slave );
        }

        expandedConfig.setTracers( tracers );
        expandedConfig.setSlaves( slaves );

        UUID uuid = accumuloManager.installCluster( expandedConfig );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response destroyCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( accumuloManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = accumuloManager.uninstallCluster( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( accumuloManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = accumuloManager.startCluster( clusterName );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response stopCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( accumuloManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = accumuloManager.stopCluster( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response checkCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( accumuloManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = accumuloManager.checkCluster( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response addNode( final String clusterName, final String lxcHostname, String nodeType )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( nodeType );
        if ( accumuloManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        NodeType accumuloNodeType;
        nodeType = nodeType.toLowerCase();
        if ( nodeType.contains( "tracer" ) )
        {
            accumuloNodeType = NodeType.ACCUMULO_TRACER;
        }
        else if ( nodeType.contains( "tablet" ) )
        {
            accumuloNodeType = NodeType.ACCUMULO_TABLET_SERVER;
        }
        else
        {
            accumuloNodeType = NodeType.valueOf( nodeType.toUpperCase() );
        }
        //UUID uuid = accumuloManager.addNode( clusterName, accumuloNodeType );
        UUID uuid = accumuloManager.addNode( clusterName, lxcHostname, accumuloNodeType );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response destroyNode( final String clusterName, final String lxcHostname, String nodeType )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( nodeType );
        if ( accumuloManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        NodeType accumuloNodeType;
        nodeType = nodeType.toLowerCase();
        if ( nodeType.contains( "tracer" ) )
        {
            accumuloNodeType = NodeType.ACCUMULO_TRACER;
        }
        else if ( nodeType.contains( "tablet" ) )
        {
            accumuloNodeType = NodeType.ACCUMULO_TABLET_SERVER;
        }
        else
        {
            accumuloNodeType = NodeType.valueOf( nodeType.toUpperCase() );
        }
        UUID uuid = accumuloManager.destroyNode( clusterName, lxcHostname, accumuloNodeType );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response checkNode( final String clusterName, final String lxcHostname )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHostname );
        if ( accumuloManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = accumuloManager.checkNode( clusterName, lxcHostname );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response autoScaleCluster( final String clusterName, final boolean scale )
    {
        String message = "enabled";
        AccumuloClusterConfig config = accumuloManager.getCluster( clusterName );
        config.setAutoScaling( scale );
        try
        {
            accumuloManager.saveConfig( config );
        }
        catch ( ClusterException e )
        {
            e.printStackTrace();
        }
        if ( !scale )
        {
            message = "disabled";
        }

        return Response.status( Response.Status.OK ).entity( "Auto scale is " + message + " successfully" ).build();
    }


    private Response createResponse( UUID uuid, OperationState state )
    {
        TrackerOperationView po = tracker.getTrackerOperation( AccumuloClusterConfig.PRODUCT_KEY, uuid );
        if ( state == OperationState.FAILED )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( po.getLog() ).build();
        }
        else if ( state == OperationState.SUCCEEDED )
        {
            return Response.status( Response.Status.OK ).entity( po.getLog() ).build();
        }
        else
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( "Timeout" ).build();
        }
    }


    private OperationState waitUntilOperationFinish( UUID uuid )
    {
        OperationState state = null;
        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( AccumuloClusterConfig.PRODUCT_KEY, uuid );
            if ( po != null )
            {
                if ( po.getState() != OperationState.RUNNING )
                {
                    state = po.getState();
                    break;
                }
            }
            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException ex )
            {
                break;
            }
            if ( System.currentTimeMillis() - start > ( 200 * 1000 ) )
            {
                break;
            }
        }
        return state;
    }


    public Accumulo getAccumuloManager()
    {
        return accumuloManager;
    }


    public void setAccumuloManager( final Accumulo accumuloManager )
    {
        this.accumuloManager = accumuloManager;
    }


    public Hadoop getHadoop()
    {
        return hadoop;
    }


    public void setHadoop( final Hadoop hadoop )
    {
        this.hadoop = hadoop;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }
}