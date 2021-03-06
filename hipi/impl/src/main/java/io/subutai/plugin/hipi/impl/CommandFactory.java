package io.subutai.plugin.hipi.impl;


import io.subutai.common.command.OutputRedirection;
import io.subutai.common.command.RequestBuilder;
import io.subutai.common.settings.Common;
import io.subutai.core.plugincommon.api.NodeOperationType;
import io.subutai.plugin.hipi.api.HipiConfig;


public class CommandFactory
{
    public static final String INSTALL = "apt-get --force-yes --assume-yes install " + HipiConfig.PRODUCT_PACKAGE;
    public static final String UNINSTALL = "apt-get --force-yes --assume-yes purge " + HipiConfig.PRODUCT_PACKAGE;
    public static final String CHECK = "dpkg -l | grep '^ii' | grep " + Common.PACKAGE_PREFIX_WITHOUT_DASH;


    public static String build( final NodeOperationType status )
    {

        switch ( status )
        {
            case CHECK_INSTALLATION:
                return CHECK;
            case INSTALL:
                return INSTALL;
            case UNINSTALL:
                return UNINSTALL;
            default:
                throw new IllegalArgumentException( "Unsupported operation type" );
        }
    }

    public static RequestBuilder getAptUpdate()
    {
        return new RequestBuilder( "apt-get --force-yes --assume-yes update" ).withTimeout( 2000 ).withStdOutRedirection(
                OutputRedirection.NO );
    }

}
