package io.subutai.plugin.ceph.rest;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.common.util.JsonUtil;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.ceph.api.Ceph;
import io.subutai.plugin.ceph.api.CephClusterConfig;


public class RestServiceImpl implements RestService
{
    private Ceph cephManager;
    private Tracker tracker;


    @Override
    public Response getClusters()
    {
        List<CephClusterConfig> configs = cephManager.getClusters();
        ArrayList<String> clusterNames = Lists.newArrayList();

        for ( CephClusterConfig config : configs )
        {
            clusterNames.add( config.getClusterName() );
        }

        String clusters = JsonUtil.GSON.toJson( clusterNames );
        return Response.status( Response.Status.OK ).entity( clusters ).build();
    }


    @Override
    public Response installCluster( final String environmentId, final String lxcHostName, final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( environmentId );
        Preconditions.checkNotNull( lxcHostName );

        CephClusterConfig config = new CephClusterConfig();
        config.setClusterName( clusterName );
        config.setEnvironmentId( environmentId );
        config.setRadosGW( lxcHostName );

        UUID uuid = cephManager.installCluster( config );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response uninstallCluster( final String clusterName )
    {
        return null;
    }


    @Override
    public Response getCluster( final String clusterName )
    {
        CephClusterConfig config = cephManager.getCluster( clusterName );
        if ( config == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( clusterName + "cluster not found" )
                           .build();
        }

        String cluster = JsonUtil.GSON.toJson( config );
        return Response.status( Response.Status.OK ).entity( cluster ).build();
    }


    private OperationState waitUntilOperationFinish( UUID uuid )
    {
        OperationState state = null;
        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( CephClusterConfig.PRODUCT_KEY, uuid );
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
        TrackerOperationView po = tracker.getTrackerOperation( CephClusterConfig.PRODUCT_KEY, uuid );
        if ( state == OperationState.FAILED )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).build();
        }
        else if ( state == OperationState.SUCCEEDED )
        {
            return Response.status( Response.Status.OK ).build();
        }
        else
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).build();
        }
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    public void setCeph( final Ceph ceph )
    {
        this.cephManager = ceph;
    }
}
