/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.subutai.plugin.appscale.impl;


import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.subutai.common.command.CommandException;
import io.subutai.common.command.CommandResult;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.environment.ContainerHostNotFoundException;
import io.subutai.common.environment.Environment;
import io.subutai.common.peer.AlertHandlerPriority;
import io.subutai.common.peer.EnvironmentContainerHost;
import io.subutai.common.peer.HostNotFoundException;
import io.subutai.common.peer.LocalPeer;
import io.subutai.common.peer.ResourceHost;
import io.subutai.common.tracker.TrackerOperation;
import io.subutai.core.environment.api.exception.EnvironmentManagerException;
import io.subutai.core.peer.api.PeerManager;
import io.subutai.core.plugincommon.api.ClusterConfigurationException;
import io.subutai.core.plugincommon.api.ClusterConfigurationInterface;
import io.subutai.core.plugincommon.api.ConfigBase;
import io.subutai.plugin.appscale.api.AppScaleConfig;
import io.subutai.plugin.appscale.impl.handler.AppscaleAlertHandler;


/**
 * @author caveman
 * @author Beyazıt Kelçeoğlu
 */
public class ClusterConfiguration implements ClusterConfigurationInterface
{

    private final TrackerOperation po;
    private final AppScaleImpl appscaleManager;

    private static final Logger LOG = LoggerFactory.getLogger ( ClusterConfiguration.class.getName () );


    public ClusterConfiguration ( final TrackerOperation operation, final AppScaleImpl appScaleImpl )
    {
        this.po = operation;
        this.appscaleManager = appScaleImpl;
    }


    /**
     * @throws ClusterConfigurationException configure cluster with appscale pre - requirements
     *
     * tutorial: https://confluence.subutai.io/display/APPSTALK/How+to+configure+container+for+the+needs+of+AppScale
     */
    @Override
    public void configureCluster ( ConfigBase configBase, Environment environment ) throws ClusterConfigurationException
    {
        LOG.info ( "ClusterConfiguration :: configureCluster " );
        AppScaleConfig config = ( AppScaleConfig ) configBase;

        if ( "static".equals ( config.getScaleOption () ) )
        {
            this.installAsStatic ( configBase, environment );
        }
        else
        {
            this.installAsScale ( configBase, environment );
            // this.installAsStatic ( configBase, environment );
        }
    }


    private void installAsScale ( ConfigBase configBase, Environment environment )
    {
        AppScaleConfig config = ( AppScaleConfig ) configBase;
        String userDomain = config.getUserDomain ();

        if ( userDomain == null )
        {
            po.addLogFailed ( "User Domain Must be Set!" );
        }
        EnvironmentContainerHost containerHost = null;
        Set<EnvironmentContainerHost> cn = environment.getContainerHosts ();
        try
        {
            containerHost = environment.getContainerHostByHostname ( config.getClusterName () );

            LOG.info ( "Container Host Found: " + containerHost.getContainerId () + "\n" + "\n" + containerHost
                    .getHostname () + "\n" );
        }
        catch ( ContainerHostNotFoundException ex )
        {
            LOG.error ( "configureCluster " + ex );
        }
        this.commandExecute ( containerHost, Commands.getCreateLogDir () );
        this.appscaleInitCloud ( containerHost, environment, config );
        this.runAfterInitCommands ( containerHost, config );
        this.commandExecute ( containerHost, "sudo /root/appscale-tools/bin/appscale up" );
        config.setEnvironmentId ( environment.getId () );
        boolean saveInfo = appscaleManager.getPluginDAO ()
                .saveInfo ( AppScaleConfig.PRODUCT_KEY, configBase.getClusterName (),
                            configBase );

        try
        {
            appscaleManager.getEnvironmentManager ()
                    .startMonitoring ( AppscaleAlertHandler.HANDLER_ID, AlertHandlerPriority.NORMAL,
                                       environment.getId () );
            po.addLog ( "Alert handler added successfully." );
        }
        catch ( EnvironmentManagerException e )
        {
            LOG.error ( e.getMessage (), e );
            po.addLogFailed ( "Could not add alert handler to monitor this environment." );
        }
        LOG.info ( "SAVE INFO: " + saveInfo );
        LOG.info ( "Appscale saved to database" );
        po.addLogDone ( "DONE" );
    }


    private void installAsStatic ( ConfigBase configBase, Environment environment )
    {
        AppScaleConfig config = ( AppScaleConfig ) configBase;
        String userDomain = config.getUserDomain ();

        if ( userDomain == null )
        {
            po.addLogFailed ( "User Domain Must be Set!" );
        }

        EnvironmentContainerHost containerHost = null;
        Set<EnvironmentContainerHost> cn = environment.getContainerHosts ();
        Properties p = System.getProperties ();
        p.setProperty ( "user.dir", "/root" );
        int numberOfContainers = cn.size ();

        try
        {
            containerHost = environment.getContainerHostByHostname ( config.getClusterName () );

            LOG.info ( "Container Host Found: " + containerHost.getContainerId () + "\n" + "\n" + containerHost
                    .getHostname () + "\n" );
        }
        catch ( ContainerHostNotFoundException ex )
        {
            LOG.error ( "configureCluster " + ex );
        }
        this.commandExecute ( containerHost, Commands.getCreateLogDir () );

        LOG.info ( "installing appscale can take several minutes." );
        // AppScalefile configuration
        this.appscaleInitCluster ( containerHost, environment, config ); // writes AppScalefile
        // this.appscaleInitIPS ( containerHost, environment, config );
        // end of AppScalefile configuration
        LOG.info ( "cleaning up..." );
        this.makeCleanUpPreviousInstallation ( containerHost );
        LOG.info ( "clean up ended..." );
        LOG.info ( "START AFTER INIT" );
        this.runAfterInitCommands ( containerHost, config );
        this.addKeyPairSH ( containerHost );
        this.runInstances ( containerHost );
//        this.commandExecute ( containerHost, "sudo /root/addKey.sh " + numberOfContainers );
//        this.commandExecute ( containerHost, "sudo /root/runIns.sh " + 1 );
        LOG.info ( "Run shell starting..." );
        this.createRunSH ( containerHost ); // we only need this in master container...
        String runShell = Commands.getRunShell ();
        runShell = runShell + " " + numberOfContainers;
        LOG.info ( "RUN SHELL COMMAND: " + runShell );
        try
        {
            containerHost.execute ( new RequestBuilder ( runShell ).withTimeout ( 10000 ) );
        }
        catch ( CommandException ex )
        {
            LOG.error ( "RUN SHELL ERROR" + ex );
        }
        LOG.info ( "Run shell completed..." );
        this.createUpShell ( containerHost );
        LOG.info ( "RH command started" );

        this.runInRH ( containerHost, config.getClusterName (), config );
        LOG.info ( "RH command ended" );

        LOG.info ( "Environment ID: " + environment.getId () );

        config.setEnvironmentId ( environment.getId () );
        boolean saveInfo = appscaleManager.getPluginDAO ()
                .saveInfo ( AppScaleConfig.PRODUCT_KEY, configBase.getClusterName (),
                            configBase );
        try
        {
            appscaleManager.getEnvironmentManager ()
                    .startMonitoring ( AppscaleAlertHandler.HANDLER_ID, AlertHandlerPriority.NORMAL,
                                       environment.getId () );
            po.addLog ( "Alert handler added successfully." );
        }
        catch ( EnvironmentManagerException e )
        {
            LOG.error ( e.getMessage (), e );
            po.addLogFailed ( "Could not add alert handler to monitor this environment." );
        }
        LOG.info ( "SAVE INFO: " + saveInfo );
        LOG.info ( "Appscale saved to database" );
        po.addLogDone ( "DONE" );
    }


    /**
     *
     * @param conf
     * @param env
     * @return
     *
     * this method only for the scale up appscale should be created already
     *
     */
    public Boolean scaleUP ( ConfigBase conf, Environment env )
    {
        LOG.info ( "//// scaling started..." );
        Boolean scaled = false;
        AppScaleConfig localConfig = ( AppScaleConfig ) conf;

        for ( String a : localConfig.getAppenList () )
        {
            LOG.info ( "Appengine: " + a );
        }

        EnvironmentContainerHost containerHost = null;
        try
        {
            containerHost = env.getContainerHostByHostname ( localConfig.getClusterName () );
            LOG.info ( "container host found..." );
        }
        catch ( ContainerHostNotFoundException ex )
        {
            LOG.error ( ex.toString () );
        }
        LOG.info ( "appscale stopping" );
        this.commandExecute ( containerHost, Commands.getAppScaleStopCommand () ); // stop it
        LOG.info ( "appscale stopped and cleaning process started" );
        // this.makeCleanUpPreviousInstallation ( containerHost ); // this is just cleaning ssh etc..
        LOG.info ( "init cluster" );
        // this.appscaleInitIPS ( containerHost, env, localConfig ); // creates AppScalefile
        this.appscaleInitCluster ( containerHost, env, localConfig );
        // this.commandExecute ( containerHost, "cat /AppScalefile" );
        Set<EnvironmentContainerHost> cn = env.getContainerHosts ();
        int numberOfContainers = cn.size ();
//        this.commandExecute ( containerHost, "sudo /root/addKey.sh " + 1 );
//        this.commandExecute ( containerHost, addInstances () );
        String runShell = Commands.getRunShell ();
        runShell = runShell + " " + 1;
        try
        {
            containerHost.execute ( new RequestBuilder ( runShell ).withTimeout ( 10000 ) ); // will take time
            scaled = true;
            LOG.info ( "appscale restarted" );
        }
        catch ( CommandException ex )
        {
            LOG.error ( "RUN SHELL ERROR" + ex );
        }
        appscaleManager.getPluginDAO ()
                .saveInfo ( AppScaleConfig.PRODUCT_KEY, conf.getClusterName (),
                            conf );
        return scaled;
    }


    private void runInRH ( EnvironmentContainerHost containerHost, String clusterName, AppScaleConfig config )
    {
        LOG.info ( "RUN IN RH" );
        PeerManager peerManager = appscaleManager.getPeerManager ();
        LOG.info ( "PeerManager: " + peerManager );
        LocalPeer localPeer = peerManager.getLocalPeer ();
        LOG.info ( "LocalPeer: " + localPeer );
        String ipAddress = this.getIPAddress ( containerHost ); // only for master

        if ( config.getVlanNumber () == null )
        {
            po.addLogFailed ( "we have a problem here" );
        }

        try
        {
            ResourceHost resourceHostByContainerId = localPeer.getResourceHostByContainerId ( containerHost.getId () );
            LOG.info ( "resouceHostID: " + resourceHostByContainerId );
            LOG.info ( "HERE IS THE RESOURCE HOST: " + resourceHostByContainerId.getHostname () );

            CommandResult resultStr = resourceHostByContainerId
                    .execute ( new RequestBuilder ( "grep vlan /mnt/lib/lxc/" + clusterName + "/config" ) );

            String stdOut = resultStr.getStdOut ();

            String vlanString = stdOut.substring ( 11, 14 );

            LOG.info ( "****************** STDOUT *******************" + vlanString );
            resourceHostByContainerId.execute ( new RequestBuilder ( "subutai proxy del " + vlanString + " -d" ) );
            resourceHostByContainerId.execute ( new RequestBuilder (
                    "subutai proxy add " + vlanString + " -d \"*." + config.getUserDomain () + "\" -f /mnt/lib/lxc/"
                    + clusterName + "/rootfs/etc/nginx/ssl.pem" ) );

            resourceHostByContainerId
                    .execute ( new RequestBuilder ( "subutai proxy add " + vlanString + " -h " + ipAddress ) );
        }
        catch ( HostNotFoundException | CommandException ex )
        {
            LOG.error ( ex.toString () );
        }
        LOG.info ( "END OF RUN IN RH" );
    }


    private void makeCleanUpPreviousInstallation ( EnvironmentContainerHost containerHost )
    {
        try
        {
            CommandResult cr;
            cr = containerHost.execute ( new RequestBuilder ( "ls /root/.ssh" ) );
            if ( !cr.toString ().equals ( "" ) )
            {
                this.commandExecute ( containerHost, "rm /root/.ssh/*" );
                this.commandExecute ( containerHost, "touch /root/.ssh/known_hosts" );
            }
            cr = containerHost.execute ( new RequestBuilder ( "ls /root/.appscale" ) );
            if ( !cr.toString ().equals ( "" ) )
            {
                this.commandExecute ( containerHost, "rm /root/.appscale/*" );
            }
        }
        catch ( CommandException ex )
        {
            LOG.error ( " clean process command exception: " + ex );
            // po.addLogFailed( "Clean up failed..." );
        }
    }


    private void commandExecute ( EnvironmentContainerHost containerHost, String command )
    {
        try
        {
            // downloading takes time...
            CommandResult responseFrom = containerHost.execute ( new RequestBuilder ( command ).withTimeout ( 10000 ) );
        }
        catch ( CommandException e )
        {
            LOG.error ( "Error while executing \"" + command + "\".\n" + e );
            // po.addLogFailed( command + " can not be executed properly..." );
        }
    }


    private void appscaleInitCloud ( EnvironmentContainerHost containerHost, Environment environment,
                                     AppScaleConfig config )
    {
        String ipaddr = getIPAddress ( containerHost );
        this.commandExecute ( containerHost, "rm -f /root/AppScalefile && touch /root/AppScalefile" );
        LOG.info ( "AppScalefile file created." );
        this.commandExecute ( containerHost, "echo infrastructure: 'ss' >> /root/AppScalefile" );
        this.commandExecute ( containerHost, "echo ss_ACCESS_KEY: XXXXXXX >> /root/AppScalefile" );
        this.commandExecute ( containerHost, "echo ss_SECRET_KEY: XXXXXXX >> /root/AppScalefile" );
        this.commandExecute ( containerHost, "echo min: 1 >> /root/AppScalefile" );
        this.commandExecute ( containerHost, "echo max: 1 >> /root/AppScalefile" );
        // this.commandExecute ( containerHost, "echo machine: '" + config.getClusterName () + "'  >> /root/AppScalefile" );
        this.commandExecute ( containerHost, "echo machine: 'appscale'" ); // this will be pointing to our template to add one more
        // this may not be needed
        // this.commandExecute ( containerHost, "echo static_ip: '" + config.getUserDomain () + "'  >> /root/AppScalefile" );
        this.commandExecute ( containerHost, "cp /root/AppScalefile /" );
    }


    /**
     * @param config we will use bash commands to create AppScale file
     */
    private void appscaleInitCluster ( EnvironmentContainerHost containerHost, Environment environment,
                                       AppScaleConfig config )
    {
        String ipaddr = getIPAddress ( containerHost );
        this.commandExecute ( containerHost, "rm -f /root/AppScalefile && touch /root/AppScalefile" );
        LOG.info ( "AppScalefile file created." );
        this.commandExecute ( containerHost, "echo ips_layout: >> /root/AppScalefile" );
        LOG.info ( "ips_layout inserted" );
        String cmdmaster = "echo '  master : " + ipaddr + "' >> /root/AppScalefile";
        this.commandExecute ( containerHost, cmdmaster );
        LOG.info ( "master ip address inserted" );
        this.commandExecute ( containerHost, "echo '  appengine:' >> /root/AppScalefile" );
        List<String> appenList = config.getAppenList ();
        for ( String a : appenList )
        {

            try
            {
                EnvironmentContainerHost appContainerHost = environment.getContainerHostByHostname ( a );
                String aip = getIPAddress ( appContainerHost );
                this.commandExecute ( containerHost, "echo '  - " + aip + "' >> /root/AppScalefile" );
                LOG.info ( "appengine ip " + aip + " inserted" );
            }
            catch ( ContainerHostNotFoundException ex )
            {
                LOG.error ( "AppScalefile: appengine: " + ex );
            }
        }
        this.commandExecute ( containerHost, "echo '  zookeeper:' >> /root/AppScalefile" );
        List<String> zooList = config.getZooList ();
        for ( String z : zooList )
        {
            try
            {
                EnvironmentContainerHost zooContainerHost = environment.getContainerHostByHostname ( z );
                String zip = getIPAddress ( zooContainerHost );
                this.commandExecute ( containerHost, "echo '  - " + zip + "' >> /root/AppScalefile" );
                LOG.info ( "zookeeper ip " + zip + " inserted" );
            }
            catch ( ContainerHostNotFoundException ex )
            {
                LOG.error ( "AppScalefile: zookeeper: " + ex );
            }
        }
        this.commandExecute ( containerHost, "echo '  database:' >> /root/AppScalefile" );
        List<String> cassList = config.getCassList ();
        for ( String cis : cassList )
        {
            try
            {
                EnvironmentContainerHost cassContainerHost = environment.getContainerHostByHostname ( cis );
                String cip = getIPAddress ( cassContainerHost );
                this.commandExecute ( containerHost, "echo '  - " + cip + "' >> /root/AppScalefile" );
                LOG.info ( "cassandra ip " + cip + " inserted" );
            }
            catch ( ContainerHostNotFoundException ex )
            {
                LOG.error ( "AppScalefile : cassandra: " + ex );
            }
        }
        this.commandExecute ( containerHost, "echo login: " + config.getUserDomain () + " >> /root/AppScalefile" );
        this.commandExecute ( containerHost, "cp /root/AppScalefile /" );
    }


    /////////////////////////////////////////////////////
    private void appscaleInitIPS ( EnvironmentContainerHost containerHost, Environment environment,
                                   AppScaleConfig config )
    {
        String ipaddr = getIPAddress ( containerHost );
        this.commandExecute ( containerHost, "rm -f /root/ips.yaml && touch /root/ips.yaml" );
        LOG.info ( "ips.yaml file created." );
        String cmdmaster = "echo 'master : " + ipaddr + "' >> /root/ips.yaml";
        this.commandExecute ( containerHost, cmdmaster );
        LOG.info ( "master ip address inserted" );
        this.commandExecute ( containerHost, "echo 'appengine:' >> /root/ips.yaml" );
        List<String> appenList = config.getAppenList ();
        for ( String a : appenList )
        {

            try
            {
                EnvironmentContainerHost appContainerHost = environment.getContainerHostByHostname ( a );
                String aip = getIPAddress ( appContainerHost );
                this.commandExecute ( containerHost, "echo '- " + aip + "' >> /root/ips.yaml" );
                LOG.info ( "appengine ip " + aip + " inserted" );
            }
            catch ( ContainerHostNotFoundException ex )
            {
                LOG.error ( "ips.yaml: appengine: " + ex );
            }
        }
        this.commandExecute ( containerHost, "echo 'zookeeper:' >> /root/ips.yaml" );
        List<String> zooList = config.getZooList ();
        for ( String z : zooList )
        {
            try
            {
                EnvironmentContainerHost zooContainerHost = environment.getContainerHostByHostname ( z );
                String zip = getIPAddress ( zooContainerHost );
                this.commandExecute ( containerHost, "echo '- " + zip + "' >> /root/ips.yaml" );
                LOG.info ( "zookeeper ip " + zip + " inserted" );
            }
            catch ( ContainerHostNotFoundException ex )
            {
                LOG.error ( "ips.yaml: zookeeper: " + ex );
            }
        }
        this.commandExecute ( containerHost, "echo 'database:' >> /root/ips.yaml" );
        List<String> cassList = config.getCassList ();
        for ( String cis : cassList )
        {
            try
            {
                EnvironmentContainerHost cassContainerHost = environment.getContainerHostByHostname ( cis );
                String cip = getIPAddress ( cassContainerHost );
                this.commandExecute ( containerHost, "echo '- " + cip + "' >> /root/ips.yaml" );
                LOG.info ( "cassandra ip " + cip + " inserted" );
            }
            catch ( ContainerHostNotFoundException ex )
            {
                LOG.error ( "ips.yaml : cassandra: " + ex );
            }
        }
        this.commandExecute ( containerHost, "echo login: " + ipaddr + " >> /root/ips.yaml" );
        this.commandExecute ( containerHost, "cp /root/ips.yaml /" );

        if ( config.getAppengine () != null )
        {
            this.commandExecute ( containerHost, "rm -f /root/new.yaml && touch /root/new.yaml" );
            try
            {
                String newip = this.getIPAddress ( environment.getContainerHostByHostname ( config.getAppengine () ) );
                this.commandExecute ( containerHost, "echo 'appengine:' >> /root/new.yaml" );
                this.commandExecute ( containerHost, "echo '- " + newip + "' >> /root/new.yaml" );
                this.commandExecute ( containerHost, "cp /root/new.yaml /" );
                config.setAppengine ( null );
            }
            catch ( ContainerHostNotFoundException ex )
            {
                LOG.error ( ex.toString () );
            }

        }
    }

    ///////////////////////////////////////////////////

    private void runAfterInitCommands ( EnvironmentContainerHost containerHost, AppScaleConfig config )
    {
        this.commandExecute ( containerHost,
                              "sed -i 's/{0}:{1}/{1}.{0}/g' /root/appscale/AppDashboard/lib/app_dashboard_data.py" );
        this.commandExecute ( containerHost, "echo -e '127.0.0.1 " + config.getUserDomain () + "' >> /etc/hosts" );
        this.commandExecute ( containerHost,
                              "sed -i 's/127.0.0.1 localhost.localdomain localhost/127.0.0.1 localhost.localdomain localhost "
                              + config.getUserDomain () + "/g' " + "/root/appscale/AppController/djinn.rb" );
        this.commandExecute ( containerHost,
                              "sed -i 's/127.0.0.1 localhost.localdomain localhost/127.0.0.1 localhost.localdomain localhost "
                              + config.getUserDomain () + "/g' " + "/etc/hosts" );

        this.commandExecute ( containerHost, "cat /etc/nginx/mykey.pem /etc/nginx/mycert.pem > /etc/nginx/ssl.pem" );

        // modify navigation.html
        String addButton = "<li align=\"center\" class=\"tab\"><a class=\"btn btn-info\" href=\"{{ flower_url }}\">TaskQueue Monitor<\\/a><\\/li>";
        String replaceString = addButton + "<br><li align=\"center\" class=\"tab\"><a class=\"btn btn-info\" href=\"http:\\/\\/linkToRestCall\">Add Appengine<\\/a><\\/li>";
        this.commandExecute ( containerHost,
                              "sed -i 's/ " + addButton + "/" + replaceString + "/g' /root/appscale/AppDashboard/templates/shared/navigation.html" );
        String changeMonitURL = "sed -i 's/{{ monit_url }}/http:\\/\\/2812." + config.getUserDomain () + "/g' /root/appscale/AppDashboard/templates/shared/navigation.html";
        this.commandExecute ( containerHost, changeMonitURL );
        String changeFlowerURL = "sed -i 's/{{ flower_url }}/http:\\/\\/5555." + config.getUserDomain () + "/g' /root/appscale/AppDashboard/templates/shared/navigation.html";
        this.commandExecute ( containerHost, changeFlowerURL );

        String nginx
                = "echo 'server {\n" + "        listen        80;\n" + "        server_name   ~^(?<port>.+)\\." + config
                .getUserDomain () + "$;\n" + "\n" + "    set $appbackend \"127.0.0.1:${port}\";\n" + "\n"
                + "    # proxy to AppScale over http\n" + "    if ($port = 1443) {\n"
                + "        set $appbackend \"appscaledashboard\";\n" + "    }\n" + "\n" + "    location / {\n"
                + "        proxy_pass http://$appbackend;\n"
                + "        proxy_set_header   X-Real-IP $remote_addr;\n"
                + "        proxy_set_header   Host $http_host;\n"
                + "        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;\n" + "\n" + "    }\n"
                + "}' > /etc/nginx/sites-enabled/default";

        this.commandExecute ( containerHost, nginx );
    }


    private String getIPAddress ( EnvironmentContainerHost ch )
    {
        String ipaddr = null;
        try
        {

            String localCommand = "ip addr | grep eth0 | grep \"inet\" | cut -d\" \" -f6 | cut -d\"/\" -f1";
            CommandResult resultAddr = ch.execute ( new RequestBuilder ( localCommand ) );
            ipaddr = resultAddr.getStdOut ();
            ipaddr = ipaddr.replace ( "\n", "" );
            LOG.info ( "Container IP: " + ipaddr );
        }
        catch ( CommandException ex )
        {
            LOG.error ( "ip address command error : " + ex );
        }
        return ipaddr;
    }


    private void createRunSH ( EnvironmentContainerHost containerHost )
    {

        try
        {
            containerHost.execute ( new RequestBuilder ( "rm /root/run.sh " ) );
            containerHost.execute ( new RequestBuilder ( "touch /root/run.sh" ) );
            containerHost.execute ( new RequestBuilder ( "echo '" + returnRunSH () + "' > /root/run.sh" ) );
            containerHost.execute ( new RequestBuilder ( "chmod +x /root/run.sh" ) );
            LOG.info ( "RUN.SH CREATED..." );
        }
        catch ( CommandException ex )
        {
            LOG.error ( "createRunSSH error" + ex );
        }
    }


    private String returnRunSH ()
    {
        String sh = "#!/usr/bin/expect -f\n" + "set timeout -1\n" + "set num $argv\n"
                + "spawn /root/appscale-tools/bin/appscale up\n" + "\n"
                + "for {set i 1} {\"$i\" <= \"$num\"} {incr i} {\n"
                + "    expect \"Are you sure you want to continue connecting (yes/no)?\"\n" + "    send -- \"yes\\n\"\n"
                + "    expect \" password:\"\n" + "    send -- \"a\\n\"\n" + "}\n" + "\n"
                + "expect \"Enter your desired admin e-mail address:\"\n" + "send -- \"a@a.com\\n\"\n"
                + "expect \"Enter new password:\"\n" + "send -- \"aaaaaa\\n\"\n" + "expect \"Confirm password:\"\n"
                + "send -- \"aaaaaa\\n\"\n" + "\n" + "expect EOD";

        return sh;
    }


    private void addKeyPairSH ( EnvironmentContainerHost containerHost )
    {
        try
        {
            String add = "#!/usr/bin/expect -f\n"
                    + "set timeout -1\n"
                    + "set num argv\n"
                    //                + "lassign $argv num existing\n"
                    //                + "if { $existing == \"no\" } {\n"
                    + "spawn /root/appscale-tools/bin/appscale-add-keypair --ips ips.yaml --keyname appscale\n"
                    //                + " } else {\n"
                    //                + "spawn /root/appscale-tools/bin/appscale-add-keypair --ips ips.yaml --add_to_existing --keyname appscale\n"
                    //                + "}\n"
                    //                + "\n"
                    + "for {set i 1} {\"$i\" <= \"$num\"} {incr i} {\n"
                    + "    expect \"Are you sure you want to continue connecting (yes/no)?\"\n"
                    + "    send -- \"yes\\n\"\n"
                    + "    expect \" password:\"\n"
                    + "    send -- \"a\\n\"\n"
                    + "}\n"
                    + "expect EOD";
            containerHost.execute ( new RequestBuilder ( "mkdir .ssh" ) );
            containerHost.execute ( new RequestBuilder ( "rm /root/addKey.sh " ) );
            containerHost.execute ( new RequestBuilder ( "touch /root/addKey.sh" ) );
            containerHost.execute ( new RequestBuilder ( "echo '" + add + "' > /root/addKey.sh" ) );
            containerHost.execute ( new RequestBuilder ( "chmod +x /root/addKey.sh" ) );
        }
        catch ( CommandException ex )
        {
            LOG.error ( ex.toString () );
        }
    }


    private void runInstances ( EnvironmentContainerHost containerHost )
    {
        try
        {
            String runins = "#!/usr/bin/expect -f\n"
                    + "set timeout -1\n"
                    + "set num $argv\n"
                    + "spawn /root/appscale-tools/bin/appscale-run-instances --ips ips.yaml -v --keyname appscale\n"
                    + "for {set i 1} {\"$i\" <= \"$num\"} {incr i} {\n"
                    + "expect \"Enter your desired admin e-mail address:\"\n"
                    + "send -- \"a@a.com\\n\"\n"
                    + "expect \"Enter new password:\"\n"
                    + "send -- \"aaaaaa\\n\"\n"
                    + "expect \"Confirm password:\"\n"
                    + "send -- \"aaaaaa\\n\""
                    + "}\n"
                    + "expect EOD";
            containerHost.execute ( new RequestBuilder ( "rm /root/runIns.sh " ) );
            containerHost.execute ( new RequestBuilder ( "touch /root/runIns.sh" ) );
            containerHost.execute ( new RequestBuilder ( "echo '" + runins + "' > /root/runIns.sh" ) );
            containerHost.execute ( new RequestBuilder ( "chmod +x /root/runIns.sh" ) );
        }
        catch ( CommandException ex )
        {
            LOG.error ( ex.toString () );
        }
    }


    private String addInstances ()
    {
        String addIns = "sudo /root/appscale-tools/bin/appscale-add-instances --ips new.yaml --keyname appscale";
        return addIns;
    }


    private void createUpShell ( EnvironmentContainerHost containerHost )
    {

        String a = null;
        a = "#!/usr/bin/expect -f\n" + "set timeout -1\n" + "set num $argv\n"
                + "spawn /root/appscale-tools/bin/appscale up\n" + "\n"
                + "for {set i 1} {\"$i\" <= \"$num\"} {incr i} {\n"
                + "expect \"Enter your desired admin e-mail address:\"\n" + "send -- \"a@a.com\\n\"\n"
                + "expect \"Enter new password:\"\n" + "send -- \"aaaaaa\\n\"\n" + "expect \"Confirm password:\"\n"
                + "send -- \"aaaaaa\\n\"\n" + "\n" + "}\n" + "\n" + "expect EOD";
        Environment env;
        try
        {

            containerHost.execute ( new RequestBuilder ( "touch /root/up.sh" ) );
            containerHost.execute ( new RequestBuilder ( "echo '" + a + "' > /root/up.sh" ) );
            containerHost.execute ( new RequestBuilder ( "chmod +x /root/up.sh" ) );
        }
        catch ( CommandException ex )
        {
            LOG.error ( "Error on create up shell: " + ex );
        }
    }
}

