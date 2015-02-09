package org.safehaus.subutai.plugin.etl.ui;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.safehaus.subutai.common.peer.ContainerHost;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.etl.api.ETL;
import org.safehaus.subutai.plugin.sqoop.api.Sqoop;
import org.safehaus.subutai.plugin.sqoop.api.setting.ExportSetting;

import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;


public class ExportPanel extends ImportExportBase
{

    private final ETL etl;
    private final Sqoop sqoop;
    private final ExecutorService executorService;
    AbstractTextField hdfsPathField = UIUtil.getTextField( "HDFS file path:" );

    public ExportPanel( ETL etl, Sqoop sqoop, ExecutorService executorService, Tracker tracker )
    {
        super( tracker );
        this.etl = etl;
        this.sqoop = sqoop;
        this.executorService = executorService;

        init();
    }


    @Override
    public void setHost( ContainerHost host )
    {
        super.setHost( host );
        init();
    }


    @Override
    ExportSetting makeSettings()
    {
        ExportSetting s = new ExportSetting();
        s.setClusterName( clusterName );
        s.setHostname( host.getHostname() );
        s.setConnectionString( connStringField.getValue() );
        s.setTableName( tableField.getValue() );
        s.setUsername( usernameField.getValue() );
        s.setPassword( passwordField.getValue() );
        s.setHdfsPath( hdfsPathField.getValue() );
        s.setOptionalParameters( optionalParams.getValue() );
        return s;
    }


    @Override
    final void init()
    {
        removeAllComponents();
        super.init();

        fields.add( hdfsPathField );

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing( true );
        buttons.addComponent( UIUtil.getButton( "Review Query", new Button.ClickListener()
        {
            @Override
            public void buttonClick( final Button.ClickEvent event )
            {
                if ( host.getId() == null  ){
                    Notification.show( "Please select sqoop node!" );
                    return;
                }
                ExportSetting es = makeSettings();
                String cmd = sqoop.reviewExportQuery( es );
                Notification.show( cmd );
            }
        }) );

        buttons.addComponent( UIUtil.getButton( "Export", new Button.ClickListener()
        {
            @Override
            public void buttonClick( Button.ClickEvent event )
            {
                clearLogMessages();
                if ( !checkFields() )
                {
                    return;
                }
                setFieldsEnabled( false );
                ExportSetting es = makeSettings();
                final UUID trackId = sqoop.exportData( es );

                OperationWatcher watcher = new OperationWatcher( trackId );
                watcher.setCallback( new OperationCallback()
                {

                    @Override
                    public void onComplete()
                    {
                        setFieldsEnabled( true );
                    }
                } );
                executorService.execute( watcher );
            }
        } ) );

        buttons.addComponent( UIUtil.getButton( "Cancel", new Button.ClickListener()
        {

            @Override
            public void buttonClick( Button.ClickEvent event )
            {
                detachFromParent();
            }
        } ) );

        List<Component> ls = new ArrayList<>();
        ls.add( UIUtil.getLabel( "<h1>Sqoop Export</h1>", Unit.PERCENTAGE ) );
        ls.add( connStringField );
        ls.add( tableField );
        ls.add( usernameField );
        ls.add( passwordField );
        hdfsPathField = UIUtil.getTextField( "HDFS file path:" );
        ls.add( hdfsPathField );
        ls.add( optionalParams );
        ls.add( buttons );

        addComponentsVertical( ls );
    }


    @Override
    boolean checkFields()
    {
        if ( super.checkFields() )
        {
            if ( !hasValue( tableField, "Table name not specified" ) )
            {
                return false;
            }
            if ( !hasValue( hdfsPathField, "HDFS file path not specified" ) )
            {
                return false;
            }
            // every field has value
            return true;
        }
        return false;
    }
}
