/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.subutai.plugin.appscale.impl.handler;


import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentModificationException;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.core.plugincommon.api.AbstractOperationHandler;
import io.subutai.core.plugincommon.api.ClusterConfigurationException;
import io.subutai.core.plugincommon.api.ClusterOperationHandlerInterface;
import io.subutai.core.plugincommon.api.ClusterOperationType;
import io.subutai.plugin.appscale.api.AppScaleConfig;
import io.subutai.plugin.appscale.impl.AppScaleImpl;
import io.subutai.plugin.appscale.impl.ClusterConfiguration;
import io.subutai.plugin.appscale.impl.Commands;


/**
 * @author caveman
 */
public class ClusterOperationHandler extends AbstractOperationHandler<AppScaleImpl, AppScaleConfig>
        implements ClusterOperationHandlerInterface
{
    private final ClusterOperationType operationType;
    private final AppScaleConfig config;

    private final String clstrName;
    private static final Logger LOG = LoggerFactory.getLogger ( ClusterConfiguration.class.getName () );


    public ClusterOperationHandler ( final AppScaleImpl manager, final AppScaleConfig config,
                                     final ClusterOperationType operationType )
    {
        super ( manager, config );
        this.config = config;
        clstrName = config.getClusterName ();
        this.operationType = operationType;
        String msg = String.format ( "Starting %s operation on %s(%s) cluster...", operationType, clstrName,
                                     config.getProductKey () );

        LOG.info ( msg );
        trackerOperation = manager.getTracker ().createTrackerOperation ( AppScaleConfig.PRODUCT_KEY, msg );
        if ( trackerOperation == null )
        {
            LOG.error ( "trackerOperation is null " );
        }
        else
        {
            LOG.info ( "trackerOperation: " + trackerOperation );
        }


    }


    @Override
    public void run ()
    {
        Preconditions.checkNotNull ( config, "Configuration is null" );
        switch ( operationType )
        {


            case INSTALL:
            {

                setupCluster ();
                break;

            }
            case UNINSTALL:
            {
                destroyCluster ();
                break;
            }
            case REMOVE:
            {
                removeCluster ( clstrName );
                break;
            }
            case START_ALL:
            {
                runOperationOnContainers ( operationType );
                break;
            }
            case CUSTOM:
            {
                scaleUpAppScale ();
                break;
            }

            case DECOMISSION_STATUS:
            {

            }
        }
    }


    /**
     * @param cot run operations in containers... like starting up container etc.
     */
    @Override
    public void runOperationOnContainers ( ClusterOperationType cot )
    {
        try
        {
            Environment environment
                    = manager.getEnvironmentManager ().loadEnvironment ( config.getClusterName () );
            EnvironmentContainerHost containerHostById
                    = environment.getContainerHostByHostname ( config.getClusterName () );
            CommandResult res;
            switch ( cot )
            {
                case START_ALL:
                {
                    Environment env = manager.getEnvironmentManager ().loadEnvironment ( config.getEnvironmentId () );
                    Set<EnvironmentContainerHost> containerHosts = env.getContainerHosts ();
                    int numberOfContainers = containerHosts.size ();
                    String cmd = Commands.getAppScaleStartCommand () + Integer.toString ( numberOfContainers );
                    res = containerHostById.execute ( new RequestBuilder ( cmd ) );
                    if ( res.hasSucceeded () )
                    {
                        trackerOperation.addLogDone ( res.getStdOut () );
                    }
                    else
                    {
                        trackerOperation.addLogFailed ( res.getStdErr () );
                    }
                    break;
                }
                case STOP_ALL:
                {
                    res = containerHostById.execute ( new RequestBuilder ( Commands.getAppScaleStopCommand () ) );
                    if ( res.hasSucceeded () )
                    {
                        trackerOperation.addLogDone ( res.getStdOut () );
                    }
                    else
                    {
                        trackerOperation.addLogFailed ( res.getStdErr () );
                    }
                    break;
                }

            }
        }
        catch ( EnvironmentNotFoundException | ContainerHostNotFoundException | CommandException ex )
        {
            LOG.error ( ex.getLocalizedMessage () );
        }
    }


    public void scaleUpAppScale ()
    {
        LOG.info ( "SCALE UP started" );
        List<String> appenList = config.getAppenList ();
        for ( String a : appenList )
        {
            LOG.info ( "appengine List................ " + a );
        }
        try
        {
            Environment env = manager.getEnvironmentManager ().loadEnvironment ( config.getEnvironmentId () );
            Boolean scaleUP = new ClusterConfiguration ( trackerOperation, manager ).scaleUP ( config, env );
            if ( scaleUP )
            {
                LOG.info ( "Appscale Scaled UP" );
            }
            else
            {
                LOG.error ( "error occured in scale up" );
            }

        }
        catch ( EnvironmentNotFoundException ex )
        {
            LOG.error ( ex.toString () );
        }
    }


    /**
     * set up cluster appscale with pre - requirements in tutorial
     */
    @Override
    public void setupCluster ()
    {
        LOG.info ( "setupCluster started..." );
        Environment env = null;

        try
        {
            env = manager.getEnvironmentManager ().loadEnvironment ( config.getEnvironmentId () );
            trackerOperation.addLog ( String.format (
                    "Configuring environment name: %s for cluster name: %s(%s)", env.getName (),
                    config.getClusterName (), config.getProductKey () ) );
            LOG.info ( "tracker operations" );
        }
        catch ( EnvironmentNotFoundException e )
        {
            LOG.error ( "EnvironmentNotFound: " + e );
        }


        LOG.info ( String.format (
                "Configuring environment name: %s for cluster name: %s(%s)", env.getName (),
                config.getClusterName (), config.getProductKey () ) );
        try
        {
            LOG.info ( "Before" );
            new ClusterConfiguration ( trackerOperation, manager ).configureCluster ( config, env );
            LOG.info ( "After" );
        }
        catch ( ClusterConfigurationException cce )
        {
            LOG.error ( "ClusterConfigurationException: " + cce );
        }


    }


    /**
     * destroy cluster process... if needed..
     */
    @Override
    public void destroyCluster ()
    {
        LOG.info ( "setupCluster started..." );
        Environment env = null;

        try
        {
            env = manager.getEnvironmentManager ().loadEnvironment ( config.getEnvironmentId () );

            Set<EnvironmentContainerHost> containerHosts = env.getContainerHosts ();
            for ( EnvironmentContainerHost ech : containerHosts )
            {
                env.destroyContainer ( ech, true );
            }

            trackerOperation.addLogDone ( "Containers destroyed successfully" );
            LOG.info ( "Containers destroyed successfully..." );

        }
        catch ( EnvironmentNotFoundException ex )
        {
            trackerOperation.addLogFailed ( "Destroy cluster failed..." );
            LOG.error ( "Destroy cluster failed..." );
        }
        catch ( EnvironmentModificationException ex )
        {
            java.util.logging.Logger.getLogger ( ClusterOperationHandler.class.getName () ).log ( Level.SEVERE, null, ex );
        }

    }


    /**
     * @param clusterName remove cluster process... if needed...
     */
    private void removeCluster ( String clusterName )
    {
        throw new UnsupportedOperationException (
                "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
    }
}

