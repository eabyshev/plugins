package org.safehaus.subutai.plugin.common.api;



import org.safehaus.subutai.common.peer.ContainerHost;

import com.vaadin.ui.Table;


public interface BaseManagerInterface
{
    public void refreshClustersInfo();

    public abstract void addRowComponents( Table table, final ContainerHost containerHost );

    public abstract Table createTableTemplate( String caption );
}
