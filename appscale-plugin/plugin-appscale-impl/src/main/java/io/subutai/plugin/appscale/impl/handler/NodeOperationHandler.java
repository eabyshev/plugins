/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.subutai.plugin.appscale.impl.handler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.subutai.common.environment.Environment;
import io.subutai.common.environment.EnvironmentNotFoundException;
import io.subutai.plugin.appscale.api.AppScaleConfig;
import io.subutai.plugin.appscale.impl.AppScaleImpl;
import io.subutai.core.plugincommon.api.AbstractOperationHandler;
import io.subutai.core.plugincommon.api.NodeOperationType;
import io.subutai.core.plugincommon.api.NodeType;


/**
 *
 * @author caveman
 * @author Beyazıt Kelçeoğlu
 */
public class NodeOperationHandler extends AbstractOperationHandler<AppScaleImpl, AppScaleConfig>
{
    private String clusterName;
    private String hostname;
    private NodeOperationType operationType;
    private AppScaleConfig config;
    private AppScaleImpl manager;
    private NodeType nodeType;
    private static final Logger LOG = LoggerFactory.getLogger ( NodeOperationHandler.class.getName () );


    public NodeOperationHandler ( final AppScaleImpl manager, final String clusterName, final String hostName,
                                  NodeOperationType operationType )
    {
        super ( manager, clusterName );
        this.clusterName = clusterName;
        this.hostname = hostName;
        this.operationType = operationType;
    }


    @Override
    public void run ()
    {
        AppScaleConfig appScaleConfig = manager.getCluster ( clusterName );
        if ( appScaleConfig == null )
        {
            LOG.error ( "NodeOperationHandler appscaleconfig null: " );
            return;
        }
        Environment environment = null;
        try
        {
            environment = manager.getEnvironmentManager ().loadEnvironment ( config.getEnvironmentId () );
        }
        catch ( EnvironmentNotFoundException e )
        {
            LOG.error ( "Environment: " + e );
        }
    }

}

