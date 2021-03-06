package io.subutai.plugin.sqoop.impl.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.common.peer.ContainerHost;
import io.subutai.core.plugincommon.api.AbstractOperationHandler;
import io.subutai.core.plugincommon.api.ClusterException;
import io.subutai.core.plugincommon.api.NodeOperationType;
import io.subutai.plugin.sqoop.api.SqoopConfig;
import io.subutai.plugin.sqoop.api.setting.ExportSetting;
import io.subutai.plugin.sqoop.api.setting.ImportSetting;
import io.subutai.plugin.sqoop.impl.CommandFactory;
import io.subutai.plugin.sqoop.impl.SqoopImpl;


public class NodeOperationHandler extends AbstractOperationHandler<SqoopImpl, SqoopConfig>
{
    private static final Logger LOG = LoggerFactory.getLogger( NodeOperationHandler.class.getName() );

    private String hostname;
    private NodeOperationType operationType;
    private Environment environment;
    private ContainerHost node;

    // only import/export operations
    private ImportSetting importSettings;
    private ExportSetting exportSettings;


    public NodeOperationHandler( SqoopImpl manager, SqoopConfig config, String hostname,
                                 NodeOperationType operationType )
    {
        super( manager, config );
        this.hostname = hostname;
        this.operationType = operationType;

        String desc = String.format( "Executing %s operation on node %s", operationType.name(), hostname );
        this.trackerOperation = manager.getTracker().createTrackerOperation( SqoopConfig.PRODUCT_KEY, desc );
    }


    public void setImportSettings( ImportSetting importSettings )
    {
        this.importSettings = importSettings;
    }


    public void setExportSettings( ExportSetting exportSettings )
    {
        this.exportSettings = exportSettings;
    }


    @Override
    public void run()
    {
        try
        {
            if ( manager.getCluster( clusterName ) == null )
            {
                throw new ClusterException( String.format( "Cluster with name %s does not exist", clusterName ) );
            }

            try
            {
                environment = manager.getEnvironmentManager().loadEnvironment( config.getEnvironmentId() );
            }
            catch ( EnvironmentNotFoundException e )
            {
                e.printStackTrace();
            }
            if ( environment == null )
            {
                throw new ClusterException( "Environment not found: " + config.getEnvironmentId() );
            }

            try
            {
                node = environment.getContainerHostByHostname( hostname );
            }
            catch ( ContainerHostNotFoundException e )
            {
                e.printStackTrace();
            }
            if ( node == null )
            {
                throw new ClusterException( "Node not found in environment: " + hostname );
            }
            if ( !node.isConnected() )
            {
                throw new ClusterException( "Node is not connected: " + hostname );
            }

            switch ( operationType )
            {
                case STATUS:
                    checkNode();
                    break;
                case UNINSTALL:
                    removeSlaveNode();
                    break;
                case IMPORT:
                    importOperation();
                    break;
                case EXPORT:
                    export();
                    break;
                default:
                    LOG.warn( "Operation not applicable: " + operationType );
            }

            trackerOperation.addLogDone( String.format( "Operation %s finished", operationType.name() ) );
        }
        catch ( ClusterException e )
        {
            LOG.error( "Error in NodeOperationHandler", e );
            trackerOperation
                    .addLogFailed( String.format( "Operation %s failed: %s", operationType.name(), e.getMessage() ) );
        }
    }


    private void removeSlaveNode() throws ClusterException
    {
        if ( !config.getNodes().contains( node.getId() ) )
        {
            throw new ClusterException( String.format( "Node %s is not a member of Sqoop insallation %s", hostname,
                    config.getClusterName() ) );
        }
        if ( config.getNodes().size() == 1 )
        {
            throw new ClusterException( "This is the node of Sqoop installation. Please, destroy cluster instead" );
        }


        String cmd = CommandFactory.build( NodeOperationType.UNINSTALL, null );
        try
        {
            node.execute( new RequestBuilder( cmd ) );
        }
        catch ( CommandException ex )
        {
            throw new ClusterException( ex );
        }

        // remove node from collection
        config.getNodes().remove( node.getId() );

        trackerOperation.addLog( "Updating db..." );
        boolean updated = manager.getPluginDao().saveInfo( SqoopConfig.PRODUCT_KEY, config.getClusterName(), config );
        if ( updated )
        {
            trackerOperation.addLog( "Installation info successfully updated." );
        }
        else
        {
            throw new ClusterException( "Failed to update installation info" );
        }
    }


    private void checkNode() throws ClusterException
    {
        String cmd = CommandFactory.build( NodeOperationType.STATUS, null );
        try
        {
            CommandResult res = node.execute( new RequestBuilder( cmd ) );
            if ( res.hasSucceeded() )
            {
                if ( res.getStdOut().contains( CommandFactory.PACKAGE_NAME ) )
                {
                    trackerOperation.addLog( "Sqoop installed on " + node.getHostname() );
                }
                else
                {
                    trackerOperation.addLog( "Sqoop not installed on " + node.getHostname() );
                }
            }
            else
            {
                trackerOperation.addLog( "Failed to check Sqoop on " + node.getHostname() );
            }
        }
        catch ( CommandException ex )
        {
            throw new ClusterException( ex );
        }
    }


    private void export() throws ClusterException
    {
        String cmd = CommandFactory.build( NodeOperationType.EXPORT, exportSettings );
        try
        {
            CommandResult res = node.execute( new RequestBuilder( cmd ).withTimeout( 3600 ) );
            if ( res.hasSucceeded() )
            {
                trackerOperation.addLog( res.getStdOut() );
                trackerOperation.addLog( res.getStdErr() );
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                if ( res.getStdOut() != null )
                {
                    sb.append( res.getStdOut() );
                }
                if ( res.getStdErr() != null )
                {
                    sb.append( res.getStdErr() );
                }
                throw new ClusterException( sb.toString() );
            }
        }
        catch ( CommandException ex )
        {
            throw new ClusterException( ex );
        }
        trackerOperation.addLogDone( "Export operation is finished." );
    }


    private void importOperation() throws ClusterException
    {
        String cmd = CommandFactory.build( NodeOperationType.IMPORT, importSettings );
        try
        {
            CommandResult res = node.execute( new RequestBuilder( cmd ).withTimeout( 360000 ) );
            if ( res.hasSucceeded() )
            {
                trackerOperation.addLog( res.getStdOut() );
                trackerOperation.addLog( res.getStdErr() );
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                if ( res.getStdOut() != null )
                {
                    sb.append( res.getStdOut() );
                }
                if ( res.getStdErr() != null )
                {
                    sb.append( res.getStdErr() );
                }
                throw new ClusterException( sb.toString() );
            }
        }
        catch ( CommandException ex )
        {
            throw new ClusterException( ex );
        }
        trackerOperation.addLogDone( "Import operation is finished." );
    }
}

