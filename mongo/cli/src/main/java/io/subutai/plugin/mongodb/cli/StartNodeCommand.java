package io.subutai.plugin.mongodb.cli;


import java.util.UUID;

import io.subutai.core.tracker.api.Tracker;
import io.subutai.plugin.mongodb.api.NodeType;
import io.subutai.plugin.mongodb.api.Mongo;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;


@Command( scope = "mongo", name = "start-node", description = "Starts node" )
public class StartNodeCommand extends OsgiCommandSupport
{
    @Argument( index = 0, name = "clusterName", description = "The name of the cluster.", required = true,
            multiValued = false ) String clusterName = null;

    @Argument( index = 1, name = "hostname", description = "The hostname of mongo node.", required = true,
            multiValued = false )
    String hostname = null;

    @Argument( index = 2, name = "type", description = "The type of mongo node.", required = true,
            multiValued = false )
    String nodeType = null;

    private Tracker tracker;
    private Mongo mongoManager;

    @Override
    protected Object doExecute() throws Exception
    {
        UUID uuid = getMongoManager().startNode( clusterName, hostname, NodeType.valueOf( nodeType ) );
        System.out.println( "Start node operation is " + InstallClusterCommand.waitUntilOperationFinish(
                getTracker(), uuid ) + "." );
        return null;
    }


    public Tracker getTracker()
    {
        return tracker;
    }


    public void setTracker( final Tracker tracker )
    {
        this.tracker = tracker;
    }


    public Mongo getMongoManager()
    {
        return mongoManager;
    }


    public void setMongoManager( final Mongo mongoManager )
    {
        this.mongoManager = mongoManager;
    }
}
