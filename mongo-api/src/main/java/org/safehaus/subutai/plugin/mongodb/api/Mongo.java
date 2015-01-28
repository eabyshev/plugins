/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.safehaus.subutai.plugin.mongodb.api;


import java.util.UUID;

import org.safehaus.subutai.common.protocol.EnvironmentBlueprint;
import org.safehaus.subutai.common.tracker.TrackerOperation;
import org.safehaus.subutai.core.env.api.Environment;
import org.safehaus.subutai.plugin.common.api.ApiBase;
import org.safehaus.subutai.plugin.common.api.ClusterSetupStrategy;


/**
 * Mongo manager interface
 */
public interface Mongo extends ApiBase<MongoClusterConfig>
{

    /**
     * adds node to the specified cluster
     *
     * @param clusterName - name of cluster
     * @param nodeType - type of node to add
     * @param peerId - peer Identity
     *
     * @return - UUID of operation to track
     */
    public UUID addNode( String clusterName, NodeType nodeType, UUID peerId );

    /**
     * destroys node in the specified cluster
     *
     * @param clusterName - name of cluster
     * @param lxcHostName - hostname of node
     *
     * @return - UUID of operation to track
     */
    public UUID destroyNode( String clusterName, String lxcHostName );

    /**
     * Starts the specified node
     *
     * @param clusterName - name of cluster
     * @param lxcHostName - hostname of node
     *
     * @return - UUID of operation to track
     */
    public UUID startNode( String clusterName, String lxcHostName );

    /**
     * Stops the specified node
     *
     * @param clusterName - name of cluster
     * @param lxcHostName - hostname of node
     *
     * @return - UUID of operation to track
     */
    public UUID stopNode( String clusterName, String lxcHostName );

    /**
     * Checks status of the specified node
     *
     * @param clusterName - name of cluster
     * @param lxcHostName - hostname of node
     *
     * @return - UUID of operation to track
     */
    public UUID checkNode( String clusterName, String lxcHostName );


    /**
     * Returns Mongo cluster setup strategy
     *
     * @param config - mongo cluster configuration
     * @param po - product operation tracker
     *
     * @return - strategy
     */
    public ClusterSetupStrategy getClusterSetupStrategy( Environment environment, MongoClusterConfig config,
                                                         TrackerOperation po );


    public EnvironmentBlueprint getDefaultEnvironmentBlueprint(
            MongoClusterConfig config );


    public MongoClusterConfig newMongoClusterConfigInstance();


    public UUID configureEnvironmentCluster( MongoClusterConfig config );
}
