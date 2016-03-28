/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.subutai.plugin.appscale.api;


import java.util.UUID;

import io.subutai.common.peer.ContainerHost;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.core.tracker.api.Tracker;
import io.subutai.core.plugincommon.api.CompleteEvent;
import io.subutai.core.plugincommon.api.ConfigBase;
import io.subutai.core.plugincommon.api.NodeOperationType;
import io.subutai.core.plugincommon.api.NodeState;
import io.subutai.core.plugincommon.api.NodeType;
import io.subutai.core.plugincommon.impl.AbstractNodeOperationTask;


/**
 *
 * @author caveman
 * @author Beyazıt Kelçeoğlu
 */
public class AppScaleNodeOperationTask extends AbstractNodeOperationTask implements Runnable
{
    private final String clusterName;
    private final EnvironmentContainerHost containerHost;
    private final AppScaleInterface appscale;
    private NodeOperationType operationType;
    private NodeType nodeType;


    public AppScaleNodeOperationTask ( String clusterName, EnvironmentContainerHost environmentContainerHost,
                                       AppScaleInterface appscale, NodeOperationType operationType,
                                       NodeType nodeType,
                                       Tracker tracker, ConfigBase clusterConfig, CompleteEvent completeEvent,
                                       UUID trackID,
                                       ContainerHost containerHost )
    {
        super ( tracker, appscale.getCluster ( clusterName ), completeEvent, trackID, containerHost );
        this.clusterName = clusterName;
        this.containerHost = environmentContainerHost;
        this.appscale = appscale;
        this.operationType = operationType;
        this.nodeType = nodeType;
    }


    @Override
    public UUID runTask ()
    {
        UUID returnID = null;

        switch ( operationType )
        {
            case START:
            {
                returnID = appscale.startCluster ( clusterName );
                break;
            }
            case STOP:
            {
                returnID = appscale.stopCluster ( clusterName );
                break;
            }
            case STATUS:
            {
                returnID = appscale.statusCluster ( clusterName );
                break;
            }
        }

        return returnID;
    }


    @Override
    public String getProductStoppedIdentifier ()
    {
        return NodeState.STOPPED.name ();
    }


    @Override
    public String getProductRunningIdentifier ()
    {
        return NodeState.RUNNING.name ();
    }

}

