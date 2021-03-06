package io.subutai.plugin.sqoop.cli;


import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.core.environment.api.EnvironmentManager;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.hadoop.api.Hadoop;
import io.subutai.plugin.sqoop.api.Sqoop;
import io.subutai.plugin.sqoop.api.SqoopConfig;


/**
 * sample command : sqoop:install-cluster test \ {cluster name} test \ { hadoop cluster name } [ sqoop1, sqoop2 ] \ {
 * list of nodes }
 */
@Command( scope = "sqoop", name = "install-cluster", description = "Command to install Sqoop cluster" )
public class InstallClusterCommand extends OsgiCommandSupport
{

    @Argument( index = 0, name = "clusterName", description = "The name of the cluster.", required = true,
            multiValued = false )
    String clusterName = null;

    @Argument( index = 1, name = "hadoopClusterName", description = "The name of hadoop cluster.", required = true,
            multiValued = false )
    String hadoopClusterName = null;

    @Argument( index = 2, name = "nodes", description = "The list of nodes", required = true,
            multiValued = false )
    String nodes[] = null;

    private static final Logger LOG = LoggerFactory.getLogger( InstallClusterCommand.class.getName() );
    private Sqoop sqoopManager;
    private Hadoop hadoopManager;
    private EnvironmentManager environmentManager;
    private Tracker tracker;


    protected Object doExecute() throws IOException
    {
        try
        {
            Environment environment = environmentManager
                    .loadEnvironment( hadoopManager.getCluster( hadoopClusterName ).getEnvironmentId() );

            SqoopConfig config = new SqoopConfig();
            config.setClusterName( clusterName );
            config.setHadoopClusterName( hadoopClusterName );
            Set<String> workerUUIS = new HashSet<>();
            for ( String node : nodes )
            {
                if ( checkGivenUUID( environment, node ) )
                {
                    workerUUIS.add( node );
                }
                else
                {
                    System.out.println( "Could not find container host with given uuid : " + node );
                    return null;
                }
            }
            config.setNodes( workerUUIS );
            config.setEnvironmentId( hadoopManager.getCluster( hadoopClusterName ).getEnvironmentId() );

            System.out.println( "Installing sqoop cluster..." );
            UUID uuid = getSqoopManager().installCluster( config );
            System.out.println( "Install operation is " + waitUntilOperationFinish( tracker, uuid ) );
        }
        catch ( EnvironmentNotFoundException e )
        {
            LOG.error( "Could not find environment !!!" );
            e.printStackTrace();
        }

        return null;
    }


    private boolean checkGivenUUID( Environment environment, String uuid )
    {
        for ( EnvironmentContainerHost host : environment.getContainerHosts() )
        {
            if ( host.getId().equals( uuid ) )
            {
                return true;
            }
        }
        return false;
    }


    protected static OperationState waitUntilOperationFinish( Tracker tracker, UUID uuid )
    {
        OperationState state = null;
        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( SqoopConfig.PRODUCT_KEY, uuid );
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


    public Tracker getTracker()
    {
        return tracker;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    public Hadoop getHadoopManager()
    {
        return hadoopManager;
    }


    public void setHadoopManager( final Hadoop hadoopManager )
    {
        this.hadoopManager = hadoopManager;
    }


    public EnvironmentManager getEnvironmentManager()
    {
        return environmentManager;
    }


    public void setEnvironmentManager( final EnvironmentManager environmentManager )
    {
        this.environmentManager = environmentManager;
    }


    public Sqoop getSqoopManager()
    {
        return sqoopManager;
    }


    public void setSqoopManager( final Sqoop sqoopManager )
    {
        this.sqoopManager = sqoopManager;
    }
}
