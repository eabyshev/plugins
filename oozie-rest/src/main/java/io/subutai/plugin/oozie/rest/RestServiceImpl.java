package io.subutai.plugin.oozie.rest;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.common.util.JsonUtil;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.oozie.api.Oozie;
import io.subutai.plugin.oozie.api.OozieClusterConfig;
import io.subutai.plugin.oozie.api.SetupType;


public class RestServiceImpl implements RestService
{
    private Oozie oozieManager;
    private Tracker tracker;


    public RestServiceImpl( final Oozie oozieManager )
    {
        Preconditions.checkNotNull( oozieManager );

        this.oozieManager = oozieManager;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    @Override
    public Response getClusters()
    {
        List<OozieClusterConfig> configs = oozieManager.getClusters();
        ArrayList<String> clusterNames = Lists.newArrayList();

        for ( OozieClusterConfig config : configs )
        {
            clusterNames.add( config.getClusterName() );
        }

        String clusters = JsonUtil.GSON.toJson( clusterNames );
        return Response.status( Response.Status.OK ).entity( clusters ).build();
    }


    @Override
    public Response getCluster( String clusterName )
    {
        OozieClusterConfig config = oozieManager.getCluster( clusterName );
        if ( config == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( clusterName + "cluster not found" )
                           .build();
        }

        String cluster = JsonUtil.GSON.toJson( config );
        return Response.status( Response.Status.OK ).entity( cluster ).build();
    }


    @Override
    public Response installCluster( String clusterName, String hadoopClusterName, String server, String clients )
    {
        Set<String> uuidSet = new HashSet<>();
        OozieClusterConfig config = new OozieClusterConfig();
        config.setSetupType( SetupType.OVER_HADOOP );
        config.setClusterName( clusterName );
        config.setHadoopClusterName( hadoopClusterName );
        config.setServer( server );

        String[] arr = clients.replaceAll( "\\s+", "" ).split( "," );
        Collections.addAll( uuidSet, arr );

        config.setClients( uuidSet );


        UUID uuid = oozieManager.installCluster( config );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response uninstallCluster( String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( oozieManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }

        UUID uuid = oozieManager.uninstallCluster( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response checkNode( String clusterName, String hostName )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( hostName );
        if ( oozieManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = oozieManager.checkNode( clusterName, hostName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startNode( String clusterName, String hostName )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( hostName );
        if ( oozieManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = oozieManager.startNode( clusterName, hostName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response stopNode( String clusterName, String hostName )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( hostName );
        if ( oozieManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = oozieManager.stopNode( clusterName, hostName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response addNode( String clusterName, String hostName )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( hostName );
        if ( oozieManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = oozieManager.addNode( clusterName, hostName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response destroyNode( String clusterName, String hostName )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( hostName );
        if ( oozieManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = oozieManager.destroyNode( clusterName, hostName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    private OperationState waitUntilOperationFinish( UUID uuid )
    {
        OperationState state = null;
        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( OozieClusterConfig.PRODUCT_KEY, uuid );
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


    private Response createResponse( UUID uuid, OperationState state )
    {
        TrackerOperationView po = tracker.getTrackerOperation( OozieClusterConfig.PRODUCT_KEY, uuid );
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
}