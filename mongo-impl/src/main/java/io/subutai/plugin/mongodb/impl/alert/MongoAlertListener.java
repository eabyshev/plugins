package io.subutai.plugin.mongodb.impl.alert;


import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.CommandUtil;
import io.subutai.common.environment.Environment;
import io.subutai.common.metric.ProcessResourceUsage;
import io.subutai.common.metric.QuotaAlertValue;
import io.subutai.common.peer.AlertHandlerException;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.peer.ExceededQuotaAlertHandler;
import io.subutai.common.resource.MeasureUnit;
import io.subutai.core.metric.api.MonitorException;
import io.subutai.core.metric.api.MonitoringSettings;
import io.subutai.plugin.mongodb.api.MongoClusterConfig;
import io.subutai.plugin.mongodb.api.NodeType;
import io.subutai.plugin.mongodb.impl.MongoImpl;
import io.subutai.plugin.mongodb.impl.common.Commands;


public class MongoAlertListener extends ExceededQuotaAlertHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger( MongoAlertListener.class );
    private static final String HANDLER_ID = "DEFAULT_MONGO_EXCEEDED_QUOTA_ALERT_HANDLER";

    private static double MAX_RAM_QUOTA_MB;
    private static int RAM_QUOTA_INCREMENT_PERCENTAGE = 25;
    private static int MAX_CPU_QUOTA_PERCENT = 100;
    private static int CPU_QUOTA_INCREMENT_PERCENT = 15;

    private MongoImpl mongo;
    private CommandUtil commandUtil = new CommandUtil();


    public MongoAlertListener( final MongoImpl mongo )
    {
        this.mongo = mongo;
    }


    @Override
    public String getId()
    {
        return HANDLER_ID;
    }


    @Override
    public String getDescription()
    {
        return "Default mongo node quota exceed alert handler.";
    }


    @Override
    public void process( final Environment environment, final QuotaAlertValue alert ) throws AlertHandlerException
    {
        String environmentId = environment.getId();
        String containerId = alert.getValue().getHostId().getId();

        //find mongo cluster by environment id
        List<MongoClusterConfig> clusters = mongo.getClusters();

        MongoClusterConfig targetCluster = null;
        for ( MongoClusterConfig cluster : clusters )
        {
            if ( cluster.getEnvironmentId().equals( environmentId ) )
            {
                targetCluster = cluster;
                break;
            }
        }

        if ( targetCluster == null )
        {
            throw new AlertHandlerException( String.format( "Cluster not found by environment id %s", environmentId ) );
        }

        //get environment containers and find alert source host
        Set<EnvironmentContainerHost> containers = environment.getContainerHosts();

        EnvironmentContainerHost sourceHost = null;
        for ( EnvironmentContainerHost containerHost : containers )
        {
            if ( containerHost.getId().equals( containerId ) )
            {
                sourceHost = containerHost;
                break;
            }
        }

        if ( sourceHost == null )
        {
            throw new AlertHandlerException(
                    String.format( "Alert source host %s not found in environment", containerId ) );
        }

        //check if source host belongs to found mongo cluster
        if ( !targetCluster.getAllNodes().contains( sourceHost.getId() ) )
        {
            LOGGER.info( String.format( "Alert source host %s does not belong to Mongo cluster", containerId ) );
            return;
        }

        // Set 80 percent of the available ram capacity of the resource host
        // to maximum ram quota limit assignable to the container
        //        MAX_RAM_QUOTA_MB = Float.parseFloat (sourceHost.getAvailableRamQuota().getValue()) * 0.8;

        //figure out Mongo  process pid
        int mongoPid;
        try
        {
            CommandResult result = commandUtil.execute( Commands.getPidCommand(), sourceHost );
            mongoPid = parsePid( result.getStdOut() );
        }
        catch ( NumberFormatException | CommandException e )
        {
            throw new AlertHandlerException( "Error obtaining Mongo process PID", e );
        }

        //get Zookeeper process resource usage by Mongo pid
        ProcessResourceUsage processResourceUsage;
        try
        {
            processResourceUsage = mongo.getMonitor().getProcessResourceUsage( sourceHost.getContainerId(), mongoPid );
        }
        catch ( MonitorException e )
        {
            throw new AlertHandlerException( "Error obtaining Mongo process usage", e );
        }

        //confirm that Mongo is causing the stress, otherwise no-op
        MonitoringSettings thresholds = mongo.getAlertSettings();
        double ramLimit = alert.getValue().getCurrentValue().getValue( MeasureUnit.MB ).doubleValue() * (
                thresholds.getRamAlertThreshold() / 100 ); // 0.8
        double redLine = 0.7;
        boolean cpuStressedByMongo = false;
        boolean ramStressedByMongo = false;

        if ( processResourceUsage.getUsedRam() >= ramLimit * redLine )
        {
            ramStressedByMongo = true;
        }
        if ( processResourceUsage.getUsedCpu() >= thresholds.getCpuAlertThreshold() * redLine )
        {
            cpuStressedByMongo = true;
        }

        if ( !ramStressedByMongo && !cpuStressedByMongo )
        {
            LOGGER.info( "Mongo cluster runs ok" );
            return;
        }


        //auto-scaling is enabled -> scale cluster
        if ( targetCluster.isAutoScaling() )
        {
            // check if a quota limit increase does it
            boolean quotaIncreased = false;

            if ( ramStressedByMongo )
            {
                //read current RAM quota
                /*int ramQuota = (int) Float.parseFloat (sourceHost.getRamQuota().getValue());


                if ( ramQuota < MAX_RAM_QUOTA_MB )
                {
                    // if available quota on resource host is greater than 10 % of calculated increase amount,
                    // increase quota, otherwise scale horizontally
                    int newRamQuota = ramQuota * ( 100 + RAM_QUOTA_INCREMENT_PERCENTAGE ) / 100;

                    if ( MAX_RAM_QUOTA_MB > newRamQuota )
                    {
                        LOGGER.info( "Increasing ram quota of {} from {} MB to {} MB.", sourceHost.getHostname(),
                                sourceHost.getRamQuota(), newRamQuota );

                        //we can increase RAM quota
                        sourceHost.setRamQuota( newRamQuota );
                        quotaIncreased = true;
                    }
                }
            }
            if ( cpuStressedByMongo )
            {

                //read current CPU quota
                int cpuQuota = (int) Float.parseFloat (sourceHost.getCpuQuota().getValue ());

                if ( cpuQuota < MAX_CPU_QUOTA_PERCENT )
                {
                    int newCpuQuota = Math.min( MAX_CPU_QUOTA_PERCENT, cpuQuota + CPU_QUOTA_INCREMENT_PERCENT );
                    //we can increase CPU quota
                    sourceHost.setCpuQuota( new CpuQuota (newCpuQuota) );

                    quotaIncreased = true;
                }*/
            }

            //quota increase is made, return
            if ( quotaIncreased )
            {
                return;
            }


            // add new node
            MongoClusterConfig mongoClusterConfig = mongo.getCluster( targetCluster.getClusterName() );
            if ( mongoClusterConfig == null )
            {
                throw new AlertHandlerException(
                        String.format( "Mongo cluster %s not found", targetCluster.getClusterName() ) );
            }

            boolean isDataNode = mongoClusterConfig.getDataHosts().contains( sourceHost.getId() );


            //no available nodes -> notify user
            if ( !isDataNode )
            {
                notifyUser();
            }
            //add first available node
            else
            {
                //launch node addition process
                mongo.addNode( targetCluster.getClusterName(), NodeType.DATA_NODE );
            }
        }
        else
        {
            notifyUser();
        }
    }


    /*@Override
    public String getSubscriberId()
    {
        return MONGO_ALERT_LISTENER;
    }*/


    protected int parsePid( String output ) throws AlertHandlerException
    {
        int pid;
        output = output.replaceAll( "\n", "" );
        pid = Integer.parseInt( output );
        if ( pid == 0 )
        {
            throw new AlertHandlerException( "Couldn't parse pid" );
        }
        return pid;
    }


    protected void notifyUser()
    {
        //TODO implement me when user identity management is complete and we can figure out user email
    }
}
