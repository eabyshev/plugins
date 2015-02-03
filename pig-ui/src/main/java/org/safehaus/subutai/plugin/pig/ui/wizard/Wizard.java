package org.safehaus.subutai.plugin.pig.ui.wizard;


import java.util.concurrent.ExecutorService;

import javax.naming.NamingException;

import org.safehaus.subutai.core.env.api.EnvironmentManager;
import org.safehaus.subutai.core.tracker.api.Tracker;
import org.safehaus.subutai.plugin.hadoop.api.Hadoop;
import org.safehaus.subutai.plugin.pig.api.Pig;
import org.safehaus.subutai.plugin.pig.api.PigConfig;

import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;


public class Wizard
{

    private final GridLayout grid;
    private final ExecutorService executorService;
    private final Hadoop hadoop;
    private final Pig pig;
    private final Tracker tracker;
    private final EnvironmentManager environmentManager;
    private int step = 1;
    private PigConfig config = new PigConfig();


    public Wizard( ExecutorService executorService, Pig pig, Hadoop hadoop, Tracker tracker,
                   EnvironmentManager environmentManager ) throws NamingException
    {

        this.executorService = executorService;
        this.pig = pig;
        this.hadoop = hadoop;
        this.tracker = tracker;
        this.environmentManager = environmentManager;

        grid = new GridLayout( 1, 20 );
        grid.setMargin( true );
        grid.setSizeFull();

        putForm();
    }


    private void putForm()
    {
        grid.removeComponent( 0, 1 );
        Component component = null;
        switch ( step )
        {
            case 1:
            {
                component = new WelcomeStep( this );
                break;
            }
            case 2:
            {
                component = new ConfigurationStep( hadoop, this, environmentManager );
                break;
            }
            case 3:
            {
                component = new VerificationStep( pig, hadoop, executorService, tracker, environmentManager, this );
                break;
            }
            default:
            {
                break;
            }
        }

        if ( component != null )
        {
            grid.addComponent( component, 0, 1, 0, 19 );
        }
    }


    public Component getContent()
    {
        return grid;
    }


    protected void next()
    {
        step++;
        putForm();
    }


    protected void back()
    {
        step--;
        putForm();
    }


    protected void init()
    {
        step = 1;
        config = new PigConfig();
        putForm();
    }


    public PigConfig getConfig()
    {
        return config;
    }
}
