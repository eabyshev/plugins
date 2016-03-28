package io.subutai.plugin.appscale.impl;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Iterators;

import io.subutai.common.environment.Node;
import io.subutai.common.environment.NodeSchema;
import io.subutai.common.environment.Topology;
import io.subutai.common.peer.ContainerSize;
import io.subutai.common.quota.ContainerQuota;
import io.subutai.common.resource.PeerGroupResources;
import io.subutai.common.resource.PeerResources;
import io.subutai.core.plugincommon.api.PluginDAO;
import io.subutai.core.strategy.api.ContainerPlacementStrategy;
import io.subutai.core.strategy.api.StrategyException;


/**
 * Container placement strategy for appscale plugin
 */
public class AppscalePlacementStrategy implements ContainerPlacementStrategy
{
    private static final String APPSCALE_PLACEMENT_STRATEGY_ID = "APPSCALE_PLACEMENT_STRATEGY";
    private AppScaleImpl appScaleImpl;
    private PluginDAO pluginDAO;


    @Override
    public String getId()
    {
        return APPSCALE_PLACEMENT_STRATEGY_ID;
    }


    @Override
    public String getTitle()
    {
        return "This default strategy for appscale plugin.";
    }


    @Override
    public List<NodeSchema> getScheme()
    {
        final List<NodeSchema> result = new ArrayList<>();
        NodeSchema schema =
                new NodeSchema( "appscale-" + UUID.randomUUID().toString(), ContainerSize.HUGE, "appscale", 0, 0 );

        result.add( schema );

        return result;
    }


    @Override
    public Topology distribute( final String environmentName, PeerGroupResources peerGroupResources,
                                Map<ContainerSize, ContainerQuota> quotas ) throws StrategyException
    {
        Topology result = new Topology( environmentName );

        Set<Node> nodes = distribute( getScheme(), peerGroupResources, quotas );
        for ( Node node : nodes )
        {
            result.addNodePlacement( node.getPeerId(), node );
        }

        return result;
    }


    @Override
    public Topology distribute( final String environmentName, final List<NodeSchema> nodeSchema,
                                final PeerGroupResources peerGroupResources,
                                final Map<ContainerSize, ContainerQuota> quotas ) throws StrategyException
    {
        Topology result = new Topology( environmentName );

        Set<Node> ng = distribute( nodeSchema, peerGroupResources, quotas );
        for ( Node node : ng )
        {
            result.addNodePlacement( node.getPeerId(), node );
        }

        return result;
    }


    protected Set<Node> distribute( List<NodeSchema> nodeSchemas, PeerGroupResources peerGroupResources,
                                    Map<ContainerSize, ContainerQuota> quotas ) throws StrategyException
    {

        // build list of allocators
        List<Allocator> allocators = new ArrayList<>();
        for ( PeerResources peerResources : peerGroupResources.getResources() )
        {
            Allocator resourceAllocator = new Allocator( peerResources );
            allocators.add( resourceAllocator );
        }

        if ( allocators.size() < 1 )
        {
            throw new StrategyException( "There are no resource hosts to place containers." );
        }

        final Iterator<Allocator> iterator = Iterators.cycle( allocators );
        // distribute node groups
        for ( NodeSchema nodeSchema : nodeSchemas )
        {
            String containerName = generateContainerName( nodeSchema );

            boolean allocated = false;
            int counter = 0;
            while ( counter < allocators.size() )
            {
                final Allocator resourceAllocator = iterator.next();
                allocated = resourceAllocator
                        .allocate( containerName, nodeSchema.getTemplateName(), nodeSchema.getSize(),
                                quotas.get( nodeSchema.getSize() ) );
                if ( allocated )
                {
                    break;
                }
                counter++;
            }

            if ( !allocated )
            {
                throw new StrategyException(
                        "Could not allocate containers. There is no space for container: '" + containerName + "'" );
            }
        }

        Set<Node> nodes = new HashSet<>();

        for ( Allocator resourceAllocator : allocators )
        {
            List<Allocator.AllocatedContainer> containers = resourceAllocator.getContainers();
            if ( !containers.isEmpty() )
            {
                for ( Allocator.AllocatedContainer container : containers )
                {
                    Node node =
                            new Node( UUID.randomUUID().toString(), container.getName(), container.getTemplateName(),
                                    container.getSize(), 0, 0, container.getPeerId(), container.getHostId() );
                    nodes.add( node );
                }
            }
        }


        return nodes;
    }


    private List<Allocator> getPreferredAllocators( final List<Allocator> allocators )
    {
        List<Allocator> result = new ArrayList<>( allocators );
        Collections.shuffle( result );
        return result;
    }


    private String generateContainerName( final NodeSchema nodeSchema )
    {
        return nodeSchema.getName().replaceAll( "\\s+", "_" );
    }
}

