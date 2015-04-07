package org.safehaus.subutai.plugin.sqoop.impl;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.safehaus.subutai.common.command.CommandException;
import org.safehaus.subutai.common.command.CommandResult;
import org.safehaus.subutai.common.command.RequestBuilder;
import org.safehaus.subutai.common.environment.ContainerHostNotFoundException;
import org.safehaus.subutai.common.environment.Environment;
import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.common.settings.Common;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.plugin.common.api.ClusterSetupException;
import org.safehaus.subutai.plugin.common.api.ConfigBase;
import org.safehaus.subutai.plugin.common.api.NodeOperationType;
import org.safehaus.subutai.plugin.hadoop.api.HadoopClusterConfig;
import org.safehaus.subutai.plugin.sqoop.api.SqoopConfig;


class SetupStrategyOverHadoop extends SqoopSetupStrategy
{

    public SetupStrategyOverHadoop( SqoopImpl manager, SqoopConfig config, Environment env, TrackerOperation po )
    {
        super( manager, config, env, po );
    }


    @Override
    public ConfigBase setup() throws ClusterSetupException
    {

        checkConfig();

        //check if nodes are connected
        Set<ContainerHost> nodes = null;
        try
        {
            nodes = environment.getContainerHostsByIds( config.getNodes() );
        }
        catch ( ContainerHostNotFoundException e )
        {
            e.printStackTrace();
        }

        if ( nodes.size() < config.getNodes().size() )
        {
            throw new ClusterSetupException( "Fewer nodes found in the environment than expected" );
        }

        for ( ContainerHost node : nodes )
        {
            if ( !node.isConnected() )
            {
                throw new ClusterSetupException( String.format( "Node %s is not connected", node.getHostname() ) );
            }
        }

        HadoopClusterConfig hc = manager.hadoopManager.getCluster( config.getHadoopClusterName() );
        if ( hc == null )
        {
            throw new ClusterSetupException( "Could not find Hadoop cluster " + config.getHadoopClusterName() );
        }

        if ( !hc.getAllNodes().containsAll( config.getNodes() ) )
        {
            throw new ClusterSetupException(
                    "Not all nodes belong to Hadoop cluster " + config.getHadoopClusterName() );
        }
        config.setHadoopNodes( new HashSet<>( hc.getAllNodes() ) );

        // check if already installed
        String s = CommandFactory.build( NodeOperationType.STATUS, null );
        String hadoop_pack = Common.PACKAGE_PREFIX + HadoopClusterConfig.PRODUCT_NAME.toLowerCase();
        Iterator<ContainerHost> it = nodes.iterator();
        while ( it.hasNext() )
        {
            ContainerHost node = it.next();
            try
            {
                CommandResult res = node.execute( new RequestBuilder( s ) );
                if ( res.hasSucceeded() )
                {
                    if ( res.getStdOut().contains( CommandFactory.PACKAGE_NAME ) )
                    {
                        to.addLog( String.format( "Node %s has already Sqoop installed.", node.getHostname() ) );
                        it.remove();
                    }
                    else if ( ! res.getStdOut().contains( hadoop_pack ) )
                    {
                        throw new ClusterSetupException( "Hadoop not installed on node " + node.getHostname() );
                    }
                }
                else
                {
                    throw new ClusterSetupException( "Failed to check installed packges on " + node.getHostname() );
                }
            }
            catch ( CommandException ex )
            {
                throw new ClusterSetupException( ex );
            }
        }
        if ( nodes.isEmpty() )
        {
            throw new ClusterSetupException( "No nodes to install Sqoop" );
        }

        // installation
        s = CommandFactory.build( NodeOperationType.INSTALL, null );
        it = nodes.iterator();
        while ( it.hasNext() )
        {
            ContainerHost node = it.next();
            try
            {
                CommandResult res = node.execute( new RequestBuilder( s ).withTimeout( 600 ) );
                checkInstalled( node, res );
            }
            catch ( CommandException ex )
            {
                throw new ClusterSetupException( ex );
            }
        }

        to.addLog( "Saving to db..." );
        config.setEnvironmentId( environment.getId() );

        boolean saved = manager.getPluginDao().saveInfo( SqoopConfig.PRODUCT_KEY, config.getClusterName(), config );
        if ( saved )
        {
            to.addLog( "Installation info successfully saved" );
            configure();
        }
        else
        {
            throw new ClusterSetupException( "Failed to save installation info" );
        }

        return config;
    }

    public void checkInstalled( ContainerHost host, CommandResult result) throws ClusterSetupException
    {
        CommandResult statusResult;
        try
        {
            statusResult = host.execute( new RequestBuilder( CommandFactory.build( NodeOperationType.STATUS, null ) ));
        }
        catch ( CommandException e )
        {
            throw new ClusterSetupException( String.format( "Error on container %s:", host.getHostname()) );
        }

        if ( !( result.hasSucceeded() && statusResult.getStdOut().contains( CommandFactory.PACKAGE_NAME ) ) )
        {
            to.addLogFailed( String.format( "Error on container %s:", host.getHostname()) );
            throw new ClusterSetupException( String.format( "Error on container %s: %s", host.getHostname(),
                    result.hasCompleted() ? result.getStdErr() : "Command timed out" ) );
        }
    }
}

