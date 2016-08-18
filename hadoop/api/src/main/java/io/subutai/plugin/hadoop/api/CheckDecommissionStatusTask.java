/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
*/
package io.subutai.plugin.hadoop.api;


import java.util.UUID;

import io.subutai.common.tracker.OperationState;
import io.subutai.common.tracker.TrackerOperationView;
import io.subutai.core.plugincommon.api.NodeState;
import io.subutai.core.tracker.api.Tracker;


/**
 */
public class CheckDecommissionStatusTask implements Runnable
{

    private final CompleteEvent completeEvent;
    private UUID trackID;
    private HadoopClusterConfig hadoopClusterConfig;
    private Hadoop hadoop;
    private Tracker tracker;


    public CheckDecommissionStatusTask( Hadoop hadoop, Tracker tracker, HadoopClusterConfig hadoopClusterConfig,
                                        CompleteEvent completeEvent, UUID trackID )
    {
        this.hadoop = hadoop;
        this.tracker = tracker;
        this.completeEvent = completeEvent;
        this.trackID = trackID;
        this.hadoopClusterConfig = hadoopClusterConfig;
    }


    public void run()
    {

        String operationLog = "";

        if ( hadoopClusterConfig == null )
        {
            completeEvent.onComplete( operationLog );
            return;
        }

        if ( trackID != null )
        {
            while ( true )
            {
                TrackerOperationView prevPo = tracker.getTrackerOperation( HadoopClusterConfig.PRODUCT_KEY, trackID );
                if ( prevPo.getState() == OperationState.RUNNING )
                {
                    try
                    {
                        Thread.sleep( 1000 );
                    }
                    catch ( InterruptedException ex )
                    {
                        break;
                    }
                }
                else
                {
                    break;
                }
            }
        }

        NodeState state = NodeState.UNKNOWN;

//        trackID = hadoop.statusNameNode( hadoopClusterConfig );

        long start = System.currentTimeMillis();
        while ( !Thread.interrupted() )
        {
            TrackerOperationView po = tracker.getTrackerOperation( HadoopClusterConfig.PRODUCT_KEY, trackID );
            if ( po != null && po.getState() != OperationState.RUNNING )
            {
                if ( po.getLog().contains( NodeState.STOPPED.toString() ) )
                {
                    state = NodeState.STOPPED;
                }
                else if ( po.getLog().contains( NodeState.RUNNING.toString() ) )
                {
                    state = NodeState.RUNNING;
                }
                break;
            }

            try
            {
                Thread.sleep( 1000 );
            }
            catch ( InterruptedException ex )
            {
                break;
            }
            if ( System.currentTimeMillis() - start > ( 30 + 3 ) * 1000 )
            {
                break;
            }
        }

        if ( state.equals( NodeState.RUNNING ) )
        {
//            trackID = hadoop.checkDecomissionStatus( hadoopClusterConfig );
            start = System.currentTimeMillis();
            while ( !Thread.interrupted() )
            {
                TrackerOperationView po = tracker.getTrackerOperation( HadoopClusterConfig.PRODUCT_KEY, trackID );
                if ( po != null && po.getState() != OperationState.RUNNING )
                {
                    operationLog = po.getLog();
                    break;
                }

                try
                {
                    Thread.sleep( 1000 );
                }
                catch ( InterruptedException ex )
                {
                    break;
                }
                if ( System.currentTimeMillis() - start > ( 5 + 3 ) * 1000 )
                {
                    break;
                }
            }
        }

        completeEvent.onComplete( operationLog );
    }
}
