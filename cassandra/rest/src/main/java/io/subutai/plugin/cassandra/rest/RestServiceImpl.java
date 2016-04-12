package io.subutai.plugin.cassandra.rest;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;

import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.common.util.JsonUtil;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.cassandra.api.Cassandra;
import io.subutai.plugin.cassandra.api.CassandraClusterConfig;
import io.subutai.plugin.cassandra.rest.pojo.ClusterDto;
import io.subutai.plugin.cassandra.rest.pojo.ContainerDto;
import io.subutai.plugin.cassandra.rest.pojo.VersionPojo;
import io.subutai.core.plugincommon.api.ClusterException;


public class RestServiceImpl implements RestService
{
    private Cassandra cassandraManager;
    private Tracker tracker;
    private EnvironmentManager environmentManager;


    @Override
    public Response listClusters()
    {
        List<CassandraClusterConfig> configs = cassandraManager.getClusters();
        List<String> clusterNames = new ArrayList<>();
        for ( CassandraClusterConfig config : configs )
        {
            clusterNames.add( config.getClusterName() );
        }

        return Response.ok( JsonUtil.toJson( clusterNames ) ).build();
    }


    @Override
    public Response getCluster( final String clusterName )
    {
        CassandraClusterConfig config = cassandraManager.getCluster( clusterName );

        boolean thrownException = false;
        if ( config == null )
        {
            thrownException = true;
        }


        Map<String, ContainerDto> map = new HashMap<>();

        for ( String node : config.getNodes() )
        {
            try
            {
                ContainerDto containerDto = new ContainerDto();

                Environment environment = environmentManager.loadEnvironment( config.getEnvironmentId() );
                EnvironmentContainerHost containerHost = environment.getContainerHostById( node );

                String ip = containerHost.getInterfaceByName( "eth0" ).getIp();
                containerDto.setIp( ip );

                UUID uuid = cassandraManager.checkNode( clusterName, node );
                OperationState state = waitUntilOperationFinish( uuid );
                Response response = createResponse( uuid, state );
                if ( response.getStatus() == 200 && !response.getEntity().toString().toUpperCase().contains( "NOT" ) )
                {
                    containerDto.setStatus( "RUNNING" );
                }
                else
                {
                    containerDto.setStatus( "STOPPED" );
                }

                map.put( node, containerDto );
            }
            catch ( Exception e )
            {
                map.put( node, new ContainerDto() );
                thrownException = true;
            }
        }

        ClusterDto result = new ClusterDto();
        result.setSeeds( config.getSeedNodes() );
        result.setContainers( config.getNodes() );
        result.setContainersStatuses( map );
        result.setName( config.getClusterName() );
        result.setScaling( config.isAutoScaling() );

        String cluster = JsonUtil.toJson( result );


        if ( thrownException )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
                           .entity( clusterName + " cluster not found." ).build();
        }
        return Response.status( Response.Status.OK ).entity( cluster ).build();
    }


    @Override
    public Response destroyCluster( final String clusterName )
    {
        if ( cassandraManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = cassandraManager.uninstallCluster( clusterName );
        String operationId = wrapUUID( uuid );
        return Response.status( Response.Status.OK ).entity( operationId ).build();
    }


    @Override
    public Response removeCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( cassandraManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = cassandraManager.removeCluster( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response configureCluster( final String environmentId, final String clusterName, final String nodes,
                                      final String seeds )
    {
        Preconditions.checkNotNull( environmentId );
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( nodes );
        Preconditions.checkNotNull( seeds );
        Environment environment = null;
        try
        {
            environment = environmentManager.loadEnvironment( environmentId );
        }
        catch ( EnvironmentNotFoundException e )
        {
            e.printStackTrace();
        }

        if ( environment == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( "Could not find environment with id : " + environmentId ).build();
        }

        if ( cassandraManager.getCluster( clusterName ) != null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( "There is already a cluster with same name !" ).build();
        }

        CassandraClusterConfig config = new CassandraClusterConfig();
        config.setEnvironmentId( environmentId );
        config.setClusterName( clusterName );
        Set<String> allNodes = new HashSet<>();
        Set<String> allSeeds = new HashSet<>();
        String[] configNodes = nodes.replaceAll( "\\s+", "" ).split( "," );
        String[] configSeeds = seeds.replaceAll( "\\s+", "" ).split( "," );
        Collections.addAll( allNodes, configNodes );
        Collections.addAll( allSeeds, configSeeds );
        config.setNodes( allNodes );
        config.setSeedNodes( allSeeds );


        UUID uuid = cassandraManager.installCluster( config );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( cassandraManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = cassandraManager.startCluster( clusterName );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response stopCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( cassandraManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = cassandraManager.stopCluster( clusterName );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response checkCluster( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( cassandraManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = cassandraManager.checkCluster( clusterName );
        waitUntilOperationFinish( uuid );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response autoScaleCluster( final String clusterName, final boolean scale )
    {
        CassandraClusterConfig config = cassandraManager.getCluster( clusterName );
        config.setAutoScaling( scale );
        try
        {
            cassandraManager.saveConfig( config );
        }
        catch ( ClusterException e )
        {
            e.printStackTrace();
        }


        return Response.ok().build();
    }


    @Override
    public Response addNode( final String clusterName )
    {
        Preconditions.checkNotNull( clusterName );
        if ( cassandraManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = cassandraManager.addNode( clusterName );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response destroyNode( final String clusterName, final String hostId )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( hostId );
        if ( cassandraManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = cassandraManager.destroyNode( clusterName, hostId );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response checkNode( final String clusterName, final String hostId )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( hostId );
        if ( cassandraManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found." ).build();
        }
        UUID uuid = cassandraManager.checkNode( clusterName, hostId );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startNode( final String clusterName, final String lxchostId )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxchostId );
        if ( cassandraManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found" ).build();
        }
        UUID uuid = cassandraManager.startService( clusterName, lxchostId );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response startNodes( final String clusterName, final String lxcHosts )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHosts );

        List<String> hosts = JsonUtil.fromJson( lxcHosts, new TypeToken<List<String>>()
        {}.getType() );

        if ( hosts == null || hosts.isEmpty() )
        {
            return Response.status( Response.Status.BAD_REQUEST ).entity( "Error parsing lxc hosts" ).build();
        }

        int errors = 0;

        for ( String host : hosts )
        {
            UUID uuid = cassandraManager.startService( clusterName, host );
            OperationState state = waitUntilOperationFinish( uuid );
            Response response = createResponse( uuid, state );

            if ( response.getStatus() != 200 )
            {
                errors++;
            }
        }

        if ( errors > 0 )
        {
            return Response.status( Response.Status.EXPECTATION_FAILED )
                           .entity( errors + " nodes are failed to execute" ).build();
        }

        return Response.ok().build();
    }


    @Override
    public Response stopNodes( final String clusterName, final String lxcHosts )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHosts );

        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxcHosts );

        List<String> hosts = JsonUtil.fromJson( lxcHosts, new TypeToken<List<String>>()
        {}.getType() );

        if ( hosts == null || hosts.isEmpty() )
        {
            return Response.status( Response.Status.BAD_REQUEST ).entity( "Error parsing lxc hosts" ).build();
        }

        int errors = 0;

        for ( String host : hosts )
        {
            UUID uuid = cassandraManager.stopService( clusterName, host );
            OperationState state = waitUntilOperationFinish( uuid );
            Response response = createResponse( uuid, state );

            if ( response.getStatus() != 200 )
            {
                errors++;
            }
        }

        if ( errors > 0 )
        {
            return Response.status( Response.Status.EXPECTATION_FAILED )
                           .entity( errors + " nodes are failed to execute" ).build();
        }

        return Response.ok().build();
    }


    @Override
    public Response stopNode( final String clusterName, final String lxchostId )
    {
        Preconditions.checkNotNull( clusterName );
        Preconditions.checkNotNull( lxchostId );
        if ( cassandraManager.getCluster( clusterName ) == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( clusterName + " cluster not found" ).build();
        }
        UUID uuid = cassandraManager.stopService( clusterName, lxchostId );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }


    @Override
    public Response getPluginInfo()
    {
        Properties prop = new Properties();
        VersionPojo pojo = new VersionPojo();
        InputStream input = null;
        try
        {
            input = getClass().getResourceAsStream( "/git.properties" );

            prop.load( input );
            pojo.setGitCommitId( prop.getProperty( "git.commit.id" ) );
            pojo.setGitCommitTime( prop.getProperty( "git.commit.time" ) );
            pojo.setGitBranch( prop.getProperty( "git.branch" ) );
            pojo.setGitCommitUserName( prop.getProperty( "git.commit.user.name" ) );
            pojo.setGitCommitUserEmail( prop.getProperty( "git.commit.user.email" ) );
            pojo.setProjectVersion( prop.getProperty( "git.build.version" ) );

            pojo.setGitBuildUserName( prop.getProperty( "git.build.user.name" ) );
            pojo.setGitBuildUserEmail( prop.getProperty( "git.build.user.email" ) );
            pojo.setGitBuildHost( prop.getProperty( "git.build.host" ) );
            pojo.setGitBuildTime( prop.getProperty( "git.build.time" ) );

            pojo.setGitClosestTagName( prop.getProperty( "git.closest.tag.name" ) );
            pojo.setGitCommitIdDescribeShort( prop.getProperty( "git.commit.id.describe-short" ) );
            pojo.setGitClosestTagCommitCount( prop.getProperty( "git.closest.tag.commit.count" ) );
            pojo.setGitCommitIdDescribe( prop.getProperty( "git.commit.id.describe" ) );
        }
        catch ( IOException ex )
        {
            ex.printStackTrace();
        }
        finally
        {
            if ( input != null )
            {
                try
                {
                    input.close();
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }

        String projectInfo = JsonUtil.GSON.toJson( pojo );

        return Response.status( Response.Status.OK ).entity( projectInfo ).build();
    }


    private Response createResponse( UUID uuid, OperationState state )
    {

        TrackerOperationView po = tracker.getTrackerOperation( CassandraClusterConfig.PRODUCT_NAME, uuid );


        if ( state == OperationState.FAILED )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( JsonUtil.toJson( po.getLog() ) )
                           .build();
        }
        else if ( state == OperationState.SUCCEEDED )
        {
            String operationId = JsonUtil.toJson( "log", po.getLog() );
            return Response.status( Response.Status.OK ).entity( operationId ).build();
        }
        else
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).entity( JsonUtil.toJson( "Timeout" ) )
                           .build();
        }
    }


    private String wrapUUID( UUID uuid )
    {
        return JsonUtil.toJson( "OPERATION_ID", uuid );
    }


    private OperationState waitUntilOperationFinish( UUID uuid )
    {
        OperationState state = null;
        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( CassandraClusterConfig.PRODUCT_NAME, uuid );
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
            if ( System.currentTimeMillis() - start > ( 90 * 1000 ) )
            {
                break;
            }
        }
        return state;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    public void setEnvironmentManager( final EnvironmentManager environmentManager )
    {
        this.environmentManager = environmentManager;
    }


    public Cassandra getCassandraManager()
    {
        return cassandraManager;
    }


    public void setCassandraManager( final Cassandra cassandraManager )
    {
        this.cassandraManager = cassandraManager;
    }


    public Response installCluster( String config )
    {
        ClusterDto clusterDto = JsonUtil.fromJson( config, ClusterDto.class );
        CassandraClusterConfig clusterConfig = new CassandraClusterConfig();

        clusterConfig.setClusterName( clusterDto.getName() );
        clusterConfig.setDomainName( clusterDto.getDomainName() );
        clusterConfig.setDataDirectory( clusterDto.getDataDir() );
        clusterConfig.setCommitLogDirectory( clusterDto.getCommitDir() );
        clusterConfig.setSavedCachesDirectory( clusterDto.getCacheDir() );
        clusterConfig.setNodes( clusterDto.getContainers() );
        clusterConfig.setSeedNodes( clusterDto.getSeeds() );
        clusterConfig.setNumberOfNodes( clusterDto.getContainers().size() );
        clusterConfig.setNumberOfSeeds( clusterDto.getSeeds().size() );
        clusterConfig.setEnvironmentId( clusterDto.getEnvironmentId() );
        Environment environment = null;
        try
        {
            environment = environmentManager.loadEnvironment( clusterDto.getEnvironmentId() );
        }
        catch ( EnvironmentNotFoundException e )
        {
            e.printStackTrace();
        }

        if ( environment == null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( "Could not find environment with id : " + clusterDto.getEnvironmentId() ).build();
        }

        if ( cassandraManager.getCluster( clusterDto.getName() ) != null )
        {
            return Response.status( Response.Status.INTERNAL_SERVER_ERROR ).
                    entity( "There is already a cluster with same name !" ).build();
        }

        UUID uuid = cassandraManager.installCluster( clusterConfig );
        OperationState state = waitUntilOperationFinish( uuid );
        return createResponse( uuid, state );
    }
}
